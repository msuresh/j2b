package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class ShortSerializer implements StreamSerializer<Short> {

    public static final ShortSerializer INSTANCE = new ShortSerializer();

    static {
        SerializationContext.addDefaultSerializer(Short.class, INSTANCE, false);
    }

    private ShortSerializer() {
    }

    @Override
    public Class<Short> getHandledClass() {
        return Short.class;
    }

    @Override
    public void serialize(Short s, ObjectOutputImpl oop) throws Exception {
        oop.writeInt(s);
    }

    @Override
    public Short deSerialize(Class<? extends Short> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Short s = (short) oip.readInt();
        oip.registerReference(s, refIndex);
        return s;
    }

}
