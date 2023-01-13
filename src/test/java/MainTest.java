import static org.junit.Assert.*;


import org.junit.*;

import java.awt.*;
import java.util.Properties;

public class MainTest {
    Container r;

    @Before
    public void init() {
        Properties properties = new Properties();
        properties.put("counter", 2.4);
        properties.put("anInt", 5);
        r = new Container(properties);
    }

    @Test
    public void autoInject() throws Exception {
        B inst = r.getInstance(B.class);

        assertNotNull(inst);
        assertNotNull(inst.aField);
    }

    @Test
    public void injectNamedInstance() throws Exception {
        A a = new A();
        r.registerInstance("iname", a);
        F inst = r.getInstance(F.class);

        assertNotNull(inst);
        assertSame(a, inst.iname);
    }

    @Test
    public void injectStringProperty() throws Exception {
        String email = "name@yahoo.com";
        r.registerInstance("email", email);
        FS inst = r.getInstance(FS.class);

        assertNotNull(inst);
        assertNotNull(inst.email);
        assertSame(inst.email, email);
    }

    @Test
    public void constructorInject() throws Exception {
        E inst = r.getInstance(E.class);

        assertNotNull(inst);
        assertNotNull(inst.aField);
    }

    @Test
    public void injectInterface() throws Exception {
        r.registerImplementation(AI.class, A.class);
        B inst = r.getInstance(B.class);

        assertNotNull(inst);
        assertNotNull(inst.aField);
    }

    @Test
    public void injectDefaultImplementationForInterface() throws Exception {
        DI inst = r.getInstance(DI.class);
        assertNotNull(inst);
    }

    @Test(expected= RegistryException.class)
    public void injectMissingDefaultImplementationForInterface() throws Exception {
        AI inst = r.getInstance(AI.class);
        assertNull(inst);
    }

    @Test
    public void decorateInstance() throws Exception {
        C ci = new C();
        r.decorateInstance(ci);

        assertNotNull(ci.bField);
        assertNotNull(ci.bField.aField);
    }

    @Test
    public void initializer() throws Exception {
        String email = "name@yahoo.com";
        r.registerInstance("email", email);
        FSI inst = r.getInstance(FSI.class);

        assertNotNull(inst);
        assertNotNull(inst.email);
        assertEquals(inst.email, "mailto:" + email);
    }

    @Test
    public void testCircularDependency() throws Exception {
        assertThrows(RegistryException.class, () -> {
            r.getInstance(H.class);
        });
    }

    @Test
    public void testLazyLoadingNotCreating() throws Exception {
        I h = r.getInstance(I.class);
        assertNull(h.aField);
    }

    @Test
    public void testLazyLoadingCreatingWhenNeeded() throws Exception {
        fail();
        I h = r.getInstance(I.class);
        r.getInstance(A.class);
        assertNotNull(h.aField);
    }

    @Test
    public void testPrimitiveInjection() throws RegistryException {
        J j = r.getInstance(J.class);
        assertEquals(j.aDouble, 2.4, 0);
        assertEquals(j.anInt, 5, 0);
    }

//    @Test
//    public void injectImplementation() throws Exception {
//        r.registerImplementation(A.class);
//        B inst = r.getInstance(B.class);
//
//        assertNotNull(inst);
//        assertNotNull(inst.aField);
//    }

//    @Test
//    public void injectInstance() throws Exception {
//        A a = new A();
//        r.registerInstance(a);
//        B inst = r.getInstance(B.class);
//
//        assertNotNull(inst);
//        assertSame(a, inst.aField);
//    }
}