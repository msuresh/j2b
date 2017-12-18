package org.j2b.serializer.impl;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.StreamSerializer;

public class CharacterSerializer implements StreamSerializer<Character> {

    public static final CharacterSerializer INSTANCE = new CharacterSerializer();

    static {
        SerializationContext.addDefaultSerializer(Character.class, INSTANCE, false);
    }

    private CharacterSerializer() {
    }

    @Override
    public Class<Character> getHandledClass() {
        return Character.class;
    }

    @Override
    public void serialize(Character c, ObjectOutputImpl oop) throws Exception {
        oop.writeChar(c);
    }

    @Override
    public Character deSerialize(Class<? extends Character> cl,
            ObjectInputImpl oip, int refIndex) throws Exception {
        Character c = oip.readChar();
        oip.registerReference(c, refIndex);
        return c;
    }

}
