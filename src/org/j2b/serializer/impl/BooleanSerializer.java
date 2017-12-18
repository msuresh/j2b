package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class BooleanSerializer implements StreamSerializer<Boolean> {

    public static final BooleanSerializer INSTANCE = new BooleanSerializer();

    static {
        SerializationContext.addDefaultSerializer(Boolean.class, INSTANCE, false);
    }

    private BooleanSerializer() {
    }

    @Override
    public Class<Boolean> getHandledClass() {
        return Boolean.class;
    }

    @Override
    public void serialize(Boolean b, ObjectOutputImpl oop) throws Exception {
        oop.writeBoolean(b);
    }

    @Override
    public Boolean deSerialize(Class<? extends Boolean> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        Boolean b = oip.readBoolean();
        oip.registerReference(b, refIndex);
        return b;
    }

}
