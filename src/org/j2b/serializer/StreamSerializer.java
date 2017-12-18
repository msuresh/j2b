package org.j2b.serializer;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;

public interface StreamSerializer<T> {

    Class<T> getHandledClass();

    void serialize(T o, ObjectOutputImpl oop) throws Exception;

    T deSerialize(Class<? extends T> cl, ObjectInputImpl oip, int refIndex) throws Exception;
}
