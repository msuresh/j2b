package org.j2b.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.field.ClassDescriptorStore;
import org.j2b.serializer.field.VolatileClassDescriptorStore;

public class BasicTest {

    private static enum E {A, B, C, D}

    private static class A {
        String s;
    }

    private static class S implements Serializable {
        int a = 1;
        int b = 2;
        A [][] as;
        E e = E.B;
        String [] names = {"a", "b", "c"};
    }

    public static void main1(String [] args) throws Exception {
        ByteArrayOutputStream bop = new ByteArrayOutputStream();
        ObjectOutputStream oop = new ObjectOutputStream(bop);
        S s = new S();
        oop.writeObject(s);
        oop.flush();
        byte [] b = bop.toByteArray();
        ByteArrayInputStream bip = new ByteArrayInputStream(b);
        ObjectInputStream oip = new ObjectInputStream(bip);
        S s1 = (S) oip.readObject();
        System.out.println(s == s1);
    }

    public static void main2(String [] args) throws Exception {
        ClassDescriptorStore store = new VolatileClassDescriptorStore();
        SerializationContext context = new SerializationContext(store);
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        S s = new S();
        s.a = 4;
        oop.writeAnObject(s, null);
        byte [] b = oop.removeBuf();
        int count = oop.getCount();
        ObjectInputImpl oip = new ObjectInputImpl(context, b, 0, count);
        s = (S) oip.readObject();
        System.out.println("a = " + s.a + " b = " + s.b);
    }

    public static void main3(String [] args) throws Exception {
        VolatileClassDescriptorStore store = null;
        try {
            store = VolatileClassDescriptorStore.load("D:/Temp/descStore.ser");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (store == null) {
            store = new VolatileClassDescriptorStore();
        }
        S s = null;
        SerializationContext context = new SerializationContext(store);
        try {
            ObjectInputImpl oip = new ObjectInputImpl(context, new FileInputStream("D:/Temp/obj.ser"));
            s = (S) oip.readObject();
            oip.close();
            System.out.println("a = " + s.a + " b = " + s.b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        s = new S();
        s.a = 4;
        A [] [] as = new A[3][];
        as[0] = new A[1];
        as[1] = new A[2];
        as[0][0] = new A();
        as[0][0].s = "b";
        as[1][0] = new A();
        as[1][0].s = "c";
        as[1][1] = as[0][0];
        s.as = as;
        s.b = -444;
        s.e = E.D;
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        oop.writeAnObject(s, null);
        byte [] b = oop.removeBuf();
        int count = oop.getCount();
        FileOutputStream fos = new FileOutputStream("D:/Temp/obj.ser");
        fos.write(b, 0, count);
        fos.close();
        store.save("D:/Temp/descStore.ser");
    }
}
