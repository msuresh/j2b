package org.j2b.test;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.field.ClassDescriptorStore;
import org.j2b.serializer.field.VolatileClassDescriptorStore;

class A implements Serializable {
    Map<String, B> bMap;

    HashMap<Integer, Object> m;

    LocalDate d = LocalDate.now();
}

class B implements Serializable {
    A a;
    int i;
    String s;
    char c;
    List<String> l;
}

public class CustomSerializationTest {
    public static void main(String [] args) throws Exception {
        A a = new A();
        B b = new B();
        b.a = a;
        b.i = -12345;
        b.s = "Custom Serialization Test";
        b.c = 'B';
        List<String> l = new ArrayList<String>();
        l.add("item1");
        l.add("item2");
        b.l = l;
        Map<String, B> bMap = new LinkedHashMap<String, B>();
        bMap.put("k1", b);
        bMap.put("k2", new B());
        bMap.put("k3", b);
        a.bMap = bMap;
        HashMap<Integer, Object> m = new HashMap<Integer, Object>();
        m.put(1, 450);
        m.put(2, 450);
        m.put(450, bMap);
        m.put(300, a);
        m.put(350, b);
        a.m = m;
        ClassDescriptorStore store = new VolatileClassDescriptorStore();
        SerializationContext context = new SerializationContext(store);
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        oop.writeObject(a);
        int count = oop.getCount();
        byte [] buf = oop.removeBuf();
        ObjectInputImpl oip = new ObjectInputImpl(context, buf, 0, count);
        A a1 = (A) oip.readObject();
        System.out.println("Got: " + a1);
    }
}
