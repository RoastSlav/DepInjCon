import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

public class Container {
    private final Map<String, Object> instances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();
    private final Properties properties = new Properties();

    public Container() {
    }

    public Container(Properties properties) {
        this.properties.putAll(properties);
    }

    public Object getInstance(String key) throws RegistryException {
        Object instance = properties.get(key);
        if (instance != null)
            return instance;

        instance = instances.get(key);
        if (instance == null)
            throw new RegistryException("No instance registered for key: " + key);
        return instance;
    }

    public <T> T getInstance(Class<T> type) throws RegistryException {
        return getInstance(type, new HashSet<>());
    }

    private <T> T getInstance(Class<T> type, HashSet<Class<?>> visited) throws RegistryException {
        T instance = (T) instances.get(type.getName());
        if (instance == null) {
            if (type.isInterface())
                return (T) getInterfaceInstance(type);

            instance = (T) createInstance(type, visited);
            instances.put(type.getName(), instance);
            return instance;
        }

        if (type.isInterface()) {
            Class<?> impl = implementations.get(type);
            if (impl != null && !impl.equals(instance.getClass())) {
                throw new RegistryException("Implementation of " + type.getName() + " has changed");
            }
        }
        return instance;
    }

    private Object getInterfaceInstance(Class<?> c) throws RegistryException {
        Class<?> impl = implementations.get(c);
        if (impl == null && c.isAnnotationPresent(Default.class)) {
            impl = c.getAnnotation(Default.class).value();
        } else
            throw new RegistryException("No implementation registered for interface: " + c.getName());

        Object instance = getInstance(impl, new HashSet<>());
        instances.put(c.getName(), instance);
        return instance;
    }

    public void decorateInstance(Object o) throws Exception {
        injectFieldsIntoInstance(o, new HashSet<>());
    }

    public void registerInstance(String key, Object instance) throws Exception {
        injectFieldsIntoInstance(instance, new HashSet<>());
        instances.put(key, instance);
    }

    public void registerInstance(Class c, Object instance) throws Exception {
        injectFieldsIntoInstance(instance, new HashSet<>());
        instances.put(c.getName(), instance);
    }

    public void registerImplementation(Class c, Class subClass) {
        implementations.put(c, subClass);
    }

    private Constructor<?> getConstructor(Class<?> c) throws NoSuchMethodException, RegistryException {
        Constructor<?> constructor = null;

        Constructor<?>[] declaredConstructors = c.getDeclaredConstructors();
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (constructor == null && declaredConstructor.isAnnotationPresent(Inject.class)) {
                constructor = declaredConstructor;
            } else if (constructor != null && declaredConstructor.isAnnotationPresent(Inject.class)) {
                throw new RegistryException("More than one constructor annotated with @Inject");
            }
        }

        if (constructor == null)
            constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        return constructor;
    }

    private Object[] getConstructorParams(Constructor<?> constructor) throws RegistryException {
        Parameter[] parameterTypes = constructor.getParameters();
        Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isAnnotationPresent(Named.class)) {
                String name = parameterTypes[i].getName();
                params[i] = getInstance(name);
                continue;
            }

            Class<?> parameterType = parameterTypes[i].getType();
            try {
                params[i] = getInstance(parameterType);
            } catch (Exception e) {
                throw new RegistryException("Failed to get instance for constructor parameter", e);
            }
        }
        return params;
    }

    private <T> Object createInstance(Class<?> type, HashSet<Class<?>> visited) throws RegistryException {
        Object instance;
        try {
            Constructor<?> constructor = getConstructor(type);
            Object[] params = getConstructorParams(constructor);
            instance = constructor.newInstance(params);
            injectFieldsIntoInstance(instance, visited);
        } catch (RegistryException e) {
            throw new RegistryException("Failed to inject the fields into the instance", e);
        } catch (Exception e) {
            throw new RegistryException("Failed to create instance for object: " + type.getName(), e);
        }

        if (instance instanceof Initializer) {
            try {
                ((Initializer) instance).init();
            } catch (Exception e) {
                throw new RegistryException("Failed to initialize instance for object: " + type.getName(), e);
            }
        }
        return instance;
    }

    private void injectFieldsIntoInstance(Object instance, HashSet<Class<?>> visited) throws RegistryException {
        if (visited.contains(instance.getClass()))
            throw new RegistryException("Circular dependency detected in class: " + instance.getClass().getName());
        visited.add(instance.getClass());

        for (Field field : instance.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class))
                continue;

            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                setPrimitiveFieldValue(instance, field);
                continue;
            }
            setObjectFieldValue(instance, field, visited);
        }
    }

    private void setPrimitiveFieldValue(Object instance, Field field) throws RegistryException {
        Object value = properties.get(field.getName());
        if (field.isAnnotationPresent(Named.class)) {
            String annotationValue = field.getAnnotation(Named.class).value();
            if (annotationValue != null && !annotationValue.isEmpty())
                value = properties.get(annotationValue);
        }

        if (value == null)
            throw new RegistryException("No value found for primitive field: " + field.getName());

        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RegistryException("Failed to set value for primitive field: " + field.getName(), e);
        }
    }

    private void setObjectFieldValue(Object instance, Field field, HashSet<Class<?>> visited) throws RegistryException {
        if (!field.isAnnotationPresent(Named.class)) {
            Object inject = getInstance(field.getType(), visited);
            try {
                field.set(instance, inject);
            } catch (IllegalAccessException e) {
                throw new RegistryException("Failed to inject field: " + field.getName(), e);
            }
            return;
        }

        String value = field.getAnnotation(Named.class).value();
        if (value == null || value.isEmpty())
            value = field.getName();
        Object inject = getInstance(value);
        try {
            field.set(instance, inject);
        } catch (IllegalAccessException e) {
            throw new RegistryException("Failed to inject field: " + field.getName(), e);
        }
    }
}
