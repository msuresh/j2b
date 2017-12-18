package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class ByteSerializer implements StreamSerializer<Byte> {

    public static final ByteSerializer INSTANCE = new ByteSerializer();

    static {
        SerializationContext.addDefaultSerializer(Byte.class, INSTANCE, false);
    }

    private ByteSerializer() {
    }

    @Override
    public Class<Byte> getHandledClass() {
        return Byte.class;
    }

    @Override
    public void serialize(Byte b, ObjectOutputImpl oop) throws Exception {
        oop.write(b);
    }

    @Override
    public Byte deSerialize(Class<? extends Byte> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Byte b = (byte) oip.read(true);
        oip.registerReference(b, refIndex);
        return b;
    }

}
