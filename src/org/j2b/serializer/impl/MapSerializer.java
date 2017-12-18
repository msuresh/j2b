package org.j2b.serializer.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class MapSerializer implements StreamSerializer<Map> {

    public static final MapSerializer INSTANCE = new MapSerializer();

    static {
        SerializationContext.addDefaultSerializer(Map.class, INSTANCE, false);
    }

    private MapSerializer() {
    }

    @Override
    public Class<Map> getHandledClass() {
        return Map.class;
    }

    @Override
    public void serialize(Map m, ObjectOutputImpl oop) throws Exception {
        int len = m.size();
        oop.writeVariableLengthInteger(len);
        if (len > 0) {
            for (Object e : m.entrySet()) {
                Entry entry = (Entry) e;
                oop.writeAnObject(entry.getKey(), null);
                oop.writeAnObject(entry.getValue(), null);
            }
        }
    }

    @Override
    public Map deSerialize(Class<? extends Map> cl, ObjectInputImpl oip, int refIndex)
            throws Exception {
        int i, len = oip.readVariableLengthInteger();
        int mapSize = (int) Math.ceil(len / 0.75);
        if (mapSize < 8) {
            mapSize = 8;
        }
        Map m;
        if (cl == HashMap.class) {
            m = new HashMap(mapSize);
        } else if (cl == LinkedHashMap.class) {
            m = new LinkedHashMap(mapSize);
        } else {
            m = cl.newInstance();
        }
        oip.registerReference(m, refIndex);
        for (i = 0; i < len; i++) {
            Object key = oip.readAnObject(null);
            Object value = oip.readAnObject(null);
            m.put(key, value);
        }
        return m;
    }

}
