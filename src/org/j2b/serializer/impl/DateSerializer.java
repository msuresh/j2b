package org.j2b.serializer.impl;

import java.util.Date;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class DateSerializer implements StreamSerializer<Date> {

    public static final DateSerializer INSTANCE = new DateSerializer();

    static {
        SerializationContext.addDefaultSerializer(Date.class, INSTANCE, false);
    }

    private DateSerializer() {
    }

    @Override
    public Class<Date> getHandledClass() {
        return Date.class;
    }

    @Override
    public void serialize(Date d, ObjectOutputImpl oop) throws Exception {
        oop.writeVariableLengthLong(d.getTime());
    }

    @Override
    public Date deSerialize(Class<? extends Date> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        long millis = oip.readVariableLengthLong();
        Date d = new Date(millis);
        oip.registerReference(d, refIndex);
        return d;
    }

}
