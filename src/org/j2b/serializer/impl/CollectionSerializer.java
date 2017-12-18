package org.j2b.serializer.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class CollectionSerializer implements StreamSerializer<Collection> {

    public static final CollectionSerializer INSTANCE = new CollectionSerializer();

    static {
        SerializationContext.addDefaultSerializer(Collection.class, INSTANCE, false);
    }

    private CollectionSerializer() {
    }

    @Override
    public Class<Collection> getHandledClass() {
        return Collection.class;
    }

    @Override
    public void serialize(Collection c, ObjectOutputImpl op) throws Exception {
        int len = c.size();
        op.writeVariableLengthInteger(len);
        if (len > 0) {
            for (Object item : c) {
                op.writeAnObject(item, null);
            }
        }
    }

    @Override
    public Collection deSerialize(Class<? extends Collection> cl, ObjectInputImpl oip, int refIndex) throws Exception {
        int i, len = oip.readVariableLengthInteger();
        Collection c;
        if (cl == ArrayList.class) {
            c = new ArrayList(len);
        } else {
            c = cl.newInstance();
        }
        oip.registerReference(c, refIndex);
        for (i = 0; i < len; i++) {
            Object item = oip.readAnObject(null);
            c.add(item);
        }
        return c;
    }

}
