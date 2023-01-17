

interface AI { }

class A implements AI {
    public void work() { }
}

class B {
    @Inject
    A aField;
}

class C {
    @Inject B bField;
}

@Default(D.class)
interface DI { }

class D implements DI { }

class E {
    A aField;

    @Inject
    public E(A afield) {
        this.aField = afield;
    }
}

class F {
    @Inject @Named A iname;
}

class FS {
    @Inject @Named
    String email;
}

class FSI implements Initializer {
    @Inject @Named String email;

    public void init() throws Exception {
        email = "mailto:" + email;
    }
}

class G {
    @Inject
    H hField;

    public void work() { }
}

class H {
    @Inject
    G gField;

    public void work() { }
}

class I {
    @Lazy
    @Inject
    A aField;
}

class J {
    @Inject
    @Named
    int anInt;

    @Inject
    @Named("counter")
    double aDouble;
}

class K {
    String name;
    A aField;

    @Inject
    public K(@Named String name, A aField) {
        this.name = name;
        this.aField = aField;
    }
}