package org.j2b.reflect;

import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class ClassUtil {

    private static final Map<String, Class> atomicClassMap;

    static {
        Map<String, Class> m = new HashMap<String, Class>();
        m.put(byte.class.getName(), byte.class);
        m.put(boolean.class.getName(), boolean.class);
        m.put(short.class.getName(), short.class);
        m.put(int.class.getName(), int.class);
        m.put(long.class.getName(), long.class);
        m.put(float.class.getName(), float.class);
        m.put(double.class.getName(), double.class);
        m.put(char.class.getName(), char.class);
        atomicClassMap = m;
    }

    public static Class getClass(String name, boolean raiseException) {
        Class cl = atomicClassMap.get(name);
        if (cl == null) {
            try {
                cl = Class.forName(name);
            } catch (ClassNotFoundException e) {
                if (raiseException) {
                    throw new RuntimeException(e);
                }
            }
        }

        return cl;
    }

    public static boolean isInnerClass(Class<?> cl) {
        return cl.getEnclosingClass() != null && !Modifier.isStatic(cl.getModifiers());
    }

    public static Method findMethod(Class cl, String methodName) {
        Class parentCl = cl;
        Method m = null;
        do {
            try {
                m = parentCl.getDeclaredMethod("readObject", ObjectInputStream.class);
                break;
            } catch (Exception e) {
                parentCl = parentCl.getSuperclass();
            }
        } while (parentCl != null);
        return m;
    }
}
