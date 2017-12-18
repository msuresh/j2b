package org.j2b.serializer.field;

import java.lang.reflect.Field;

public enum FieldType {
    BOOLEAN(false), CHAR(false), BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE), SHORT(Short.MIN_VALUE, Short.MAX_VALUE), INT(true), LONG(true), FLOAT(true), DOUBLE(true), STRING(false), ARRAY(false), ENUM(false), OBJECT(false);

    private boolean isNumber;

    private int minVal;

    private int maxVal;

    private FieldType(int pminVal, int pmaxVal) {
        isNumber = true;
        minVal = pminVal;
        maxVal = pmaxVal;
    }

    private FieldType(boolean pisNumber) {
        isNumber = pisNumber;
        minVal = Integer.MIN_VALUE;
        maxVal = Integer.MAX_VALUE;
    }

    public static FieldType fromClass(Class<?> cl) {
        FieldType f = null;
        if (cl.isPrimitive()) {
            if (cl == Boolean.TYPE) {
                f = BOOLEAN;
            } else if (cl == Character.TYPE) {
                f = CHAR;
            } else if (cl == Byte.TYPE) {
                f = BYTE;
            } else if (cl == Short.TYPE) {
                f = SHORT;
            } else if (cl == Integer.TYPE) {
                f = INT;
            } else if (cl == Long.TYPE) {
                f = LONG;
            } else if (cl == Float.TYPE) {
                f = FLOAT;
            } else if (cl == Double.TYPE) {
                f = DOUBLE;
            }
            if (f == null) {
                throw new RuntimeException("Failed to get Primitive type for class " + cl);
            }
        } else if (cl == String.class) {
            f = STRING;
        } else if (cl.isArray()) {
            f = ARRAY;
        } else if (cl.isEnum()) {
            f = ENUM;
        } else {
            f = OBJECT;
        }
        return f;
    }

    public boolean isNumber() {
        return isNumber;
    }

    public int getMinVal() {
        return minVal;
    }

    public int getMaxVal() {
        return maxVal;
    }
}
