package org.j2b.test;

import java.io.Serializable;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.field.ClassDescriptorStore;
import org.j2b.serializer.field.VolatileClassDescriptorStore;

public class InnerClassTest {

    public static class A implements Serializable {
        private class A1 implements Serializable {
            String s = "Hello";
            A other;
            public A1(boolean createOther) {
                if (createOther) {
                    other = new A(false);
                    other.a1.other = A.this;
                }
            }

            public A1() {
                this(true);
            }
        }

        private int i = -39;

        private String s = "Hello World";

        private A1 a1;

        public A(boolean createOther) {
            a1 = new A1(createOther);
        }

        public A() {
            this(true);
        }

        public B getB() {
            B b = new B();
            b.o = a1;
            return b;
        }
    }

    public static class B implements Serializable {
        private String s = new String("Hello World");

        Object o;
    }

    public static void main(String [] args) throws Exception {
        ClassDescriptorStore store = new VolatileClassDescriptorStore();
        SerializationContext context = new SerializationContext(store);
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        A a = new A();
        a.s = new String("Hello");
        oop.writeObject(a);
        byte [] buf = oop.removeBuf();
        int count = oop.getCount();
        ObjectInputImpl oip = new ObjectInputImpl(context, buf, 0, count);
        a = (A) oip.readObject();
        oop.reset(buf);

        B b = new B();
        oop.writeObject(b);
        buf = oop.removeBuf();
        count = oop.getCount();
        oip = new ObjectInputImpl(context, buf, 0, count);
        b = (B) oip.readObject();
        oop.reset(buf);

        a.i = Integer.MIN_VALUE;
        b = a.getB();
        oop.writeObject(b);
        buf = oop.removeBuf();
        count = oop.getCount();
        oip = new ObjectInputImpl(context, buf, 0, count);
        b = (B) oip.readObject();
        oop.reset(buf);
    }
}
