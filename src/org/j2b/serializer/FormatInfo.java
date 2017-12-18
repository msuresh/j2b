package org.j2b.serializer;

public class FormatInfo {
	public static final int STRING_INDICATOR_BIT = 1 << 7;

	public static final int REF_INDICATOR_BIT = 1 << 6;

    public static final int FIELD_SERIALIZATION_INDICATOR_BIT = 1 << 5;

    public static final int IMPLICIT_CLASS_INDICATOR_BIT = 1 << 4;

    public static final int ARRAY_INDICATOR_BIT = 1 << 3;

    public static final int UNREGISTERED_ENUM = 0;

	public static final int CUSTOM_SERIALIZED_OBJECT = 1;

	public static final int JAVA_SERIALIZED_OBJECT = 2;

    public static final int JAVA_EXTERNALIZED_OBJECT = 3;

	public static final int NULL_REFERENCE = REF_INDICATOR_BIT;
}
