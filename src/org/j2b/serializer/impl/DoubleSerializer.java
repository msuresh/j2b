package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class DoubleSerializer implements StreamSerializer<Double> {

    public static final DoubleSerializer INSTANCE = new DoubleSerializer();

    static {
        SerializationContext.addDefaultSerializer(Double.class, INSTANCE, false);
    }

    private DoubleSerializer() {
    }

    @Override
    public Class<Double> getHandledClass() {
        return Double.class;
    }

    @Override
    public void serialize(Double d, ObjectOutputImpl oop) throws Exception {
        oop.writeDouble(d);
    }

    @Override
    public Double deSerialize(Class<? extends Double> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Double d = oip.readDouble();
        oip.registerReference(d, refIndex);
        return d;
    }

}
