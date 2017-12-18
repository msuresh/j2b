package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class LongSerializer implements StreamSerializer<Long> {

    public static final LongSerializer INSTANCE = new LongSerializer();

    static {
        SerializationContext.addDefaultSerializer(Long.class, INSTANCE, false);
    }

    private LongSerializer() {
    }

    @Override
    public Class<Long> getHandledClass() {
        return Long.class;
    }

    @Override
    public void serialize(Long l, ObjectOutputImpl oop) throws Exception {
        oop.writeLong(l);
    }

    @Override
    public Long deSerialize(Class<? extends Long> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Long l = oip.readLong();
        oip.registerReference(l, refIndex);
        return l;
    }

}
