import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

public class Container {
    private final HashMap<String, Object> instances = new HashMap<String, Object>();

    public Object getInstance(String key) throws Exception {
        Object instance = instances.get(key);
        if (instance == null) {
            Class<?> c = Class.forName(key);
            instance = getInstance(c);
            instances.put(key, instance);
        }
        return instance;
    }

    public <T> T getInstance(Class<T> type) throws Exception {
        T instance = (T) instances.get(type.getName());
        if (instance != null)
            return instance;

        if (type.isInterface() && type.isAnnotationPresent(Default.class)) {
            Class<?> value = type.getAnnotation(Default.class).value();
            instance = (T) getInstance(value);
            instances.put(type.getName(), instance);
            return instance;
        } else if (type.isInterface())
            throw new RegistryException("No implementation registered for interface " + type.getName());

        instance = createInstance(type);
        instances.put(type.getName(), instance);
        return instance;
    }

    public void decorateInstance(Object o) throws Exception {
        injectFieldsIntoInstance(o);
    }

    public void registerInstance(String key, Object instance) throws Exception {
        injectFieldsIntoInstance(instance);
        instances.put(key, instance);
    }

    public void registerInstance(Class c, Object instance) throws Exception {
        injectFieldsIntoInstance(instance);
        instances.put(c.getName(), instance);
    }

    public void registerImplementation(Class c, Class subClass) throws Exception {
        Object instance = getInstance(subClass);
        injectFieldsIntoInstance(instance);
        instances.put(c.getName(), instance);
    }

    private <T> T createInstance(Class<T> type) throws Exception {
        T instance = null;
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) type.getDeclaredConstructors();
        for (Constructor<T> constructor : declaredConstructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++)
                    parameters[i] = getInstance(parameterTypes[i]);

                instance = constructor.newInstance(parameters);
            }
        }

        if (instance == null) {
            if (declaredConstructors[0].getParameterCount() == 0) {
                declaredConstructors[0].setAccessible(true);
                instance = declaredConstructors[0].newInstance();
            } else
                throw new RegistryException("No constructor annotated with @Inject found for class " + type.getName());
        }

        injectFieldsIntoInstance(instance);
        if (instance instanceof Initializer)
            ((Initializer) instance).init();
        return instance;
    }

    private void injectFieldsIntoInstance(Object instance) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                if (!field.isAnnotationPresent(Named.class)) {
                    Object inject = getInstance(field.getType());
                    field.set(instance, inject);
                    continue;
                }

                String value = field.getAnnotation(Named.class).value();
                if (value == null || value.isEmpty())
                    value = field.getName();
                Object inject = getInstance(value);
                field.set(instance, inject);
            }
        }
    }
}
