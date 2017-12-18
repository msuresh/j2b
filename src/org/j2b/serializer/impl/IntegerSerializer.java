package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class IntegerSerializer implements StreamSerializer<Integer> {

    public static final IntegerSerializer INSTANCE = new IntegerSerializer();

    static {
        SerializationContext.addDefaultSerializer(Integer.class, INSTANCE, false);
    }

    private IntegerSerializer() {
    }

    @Override
    public Class<Integer> getHandledClass() {
        return Integer.class;
    }

    @Override
    public void serialize(Integer i, ObjectOutputImpl oop) throws Exception {
        oop.writeInt(i);
    }

    @Override
    public Integer deSerialize(Class<? extends Integer> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Integer i = oip.readInt();
        oip.registerReference(i, refIndex);
        return i;
    }

}
