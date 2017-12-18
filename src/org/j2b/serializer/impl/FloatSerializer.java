package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class FloatSerializer implements StreamSerializer<Float> {

    public static final FloatSerializer INSTANCE = new FloatSerializer();

    static {
        SerializationContext.addDefaultSerializer(Float.class, INSTANCE, false);
    }

    private FloatSerializer() {
    }

    @Override
    public Class<Float> getHandledClass() {
        return Float.class;
    }

    @Override
    public void serialize(Float f, ObjectOutputImpl oop) throws Exception {
        oop.writeFloat(f);
    }

    @Override
    public Float deSerialize(Class<? extends Float> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Float f = oip.readFloat();
        oip.registerReference(f, refIndex);
        return f;
    }

}
