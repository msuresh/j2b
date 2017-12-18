package org.j2b;

import java.nio.charset.Charset;

import org.j2b.io.DataInputImpl;
import org.j2b.io.DataOutputImpl;
import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.StreamSerializer;

public final class J2BConstants {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static final int SEVEN_BITS = 127;

    public static final int EIGTH_BIT = 128;

    public static final int LAST_INT_BIT = 1 << 31;

    public static final long SEVEN_BITS_L = 127;

    public static final long EIGTH_BIT_L = 128;

    public static final int J2B_HEADER_BYTE_1 = 96;

    public static final int J2B_HEADER_BYTE_2 = 131;

    public static final int J2B_VERSION = 1;

    public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    private J2BConstants () {
    }
}
