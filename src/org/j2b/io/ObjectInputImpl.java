package org.j2b.io;

import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.j2b.J2BConstants;
import org.j2b.reflect.ClassUtil;
import org.j2b.serializer.FormatInfo;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.SerializationContext.SerializationInfoCache;
import org.j2b.serializer.StreamSerializer;
import org.j2b.serializer.field.ClassDescriptor;
import org.j2b.serializer.field.FieldType;
import org.j2b.serializer.field.ClassDescriptor.FieldInfo;
import org.j2b.serializer.field.ClassDescriptor.SerializationInfo;

public class ObjectInputImpl extends DataInputImpl implements ObjectInput {

    private static final Logger log = Logger.getLogger(ObjectInputImpl.class.getName());

    private SerializationContext context;

    private SerializationInfoCache contextCache;

    private boolean headerRead;

    private int j2bVersion;

    private Object [] refs = new Object[512];

    private int refCount;

    // In the below array null means the class has not been looked up, ObjectInput.class means the class
    // was not found (During forward compatibility deSerialization)
    private Class [] classRefs = new Class[0];

    private int strIndex;

    public void reset(InputStream pip, int bufSize) {
        super.reset(pip, bufSize);
        refCount = 0;
        headerRead = false;
    }

    public void reset(byte [] b, int poff, int plen) {
        super.reset(b, poff, plen);
        refCount = 0;
        headerRead = false;
    }

    public ObjectInputImpl(SerializationContext pcontext, InputStream pip, int bufSize) {
        super(pip, bufSize);
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectInputImpl(SerializationContext pcontext, InputStream pip) {
        super(pip);
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectInputImpl(SerializationContext pcontext, byte [] b, int poff, int plen) {
        super(b, poff, plen);
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectInputImpl(SerializationContext pcontext, byte [] b) {
        super(b);
        context = pcontext;
        contextCache = context.getCache();
    }

    private ClassDescriptor lookup(int providerId, int classIndex, int versionIndex) {
        ClassDescriptor desc = contextCache.lkup(providerId, classIndex, versionIndex);
        if (desc == null) {
            desc = context.lookup(providerId, classIndex, versionIndex);
            contextCache = context.getCache();
        }
        return desc;
    }

    private ClassDescriptor lookup(Class cl) {
        ClassDescriptor desc = contextCache.lkup(cl);
        if (desc == null) {
            desc = context.lookup(cl);
            contextCache = context.getCache();
        }
        return desc;
    }

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        boolean resetHeaderRead = false;
        if (!headerRead) {
            int byte1 = read(true);
            int byte2 = read(true);
            if (byte1 != J2BConstants.J2B_HEADER_BYTE_1 || byte2 != J2BConstants.J2B_HEADER_BYTE_2) {
                throw new RuntimeException("Invalid Header Bytes " + byte1 + ' ' + byte1);
            }
            j2bVersion = readVariableLengthInteger();
            resetHeaderRead = true;
        }
        Object o = readAnObject(null, null, false);
        if (resetHeaderRead) {
            headerRead = false;
        }
        return o;
    }

    private String readStringUnchecked(int refIndex) throws IOException {
        int l = readVariableLengthInteger(6);
        String s = readUTF(l);
        refs[refIndex] = s;
        return s;
    }

    public String readString() throws IOException {
        String s = null;
        int firstByte = peek(true);
        int refIndex;
        if ((firstByte & FormatInfo.REF_INDICATOR_BIT) != 0) {
            refIndex = readVariableLengthInteger(5) - 1;
            if (refIndex >= 0) {
                s = (String) refs[refIndex];
            }
        } else if ((firstByte & FormatInfo.STRING_INDICATOR_BIT) != 0) {
            refIndex = refCount;
            refCount++;
            if (refIndex >= refs.length) {
                refs = Arrays.copyOf(refs, refs.length + (refs.length >> 1));
            }
            s = readStringUnchecked(refIndex);
        } else {
            throw new RuntimeException("Data Format error for String, first byte: " + firstByte);
        }
        strIndex = refIndex;
        return s;
    }

    private Enum readEnum(ClassDescriptor desc, int refIndex, boolean skipErrors) throws IOException {
        int ord = readVariableLengthInteger();
        Enum e = desc.getCurrentEnumVals().get(ord);
        refs[refIndex] = e;
        if (e == null && !skipErrors) {
            throw new RuntimeException("No Value " + desc.getEnumNames().get(ord) + " found in enum " + desc.getClassName());
        }
        return e;
    }

    private Object readFields(ClassDescriptor desc, int refIndex, boolean skipErrors) throws IOException {
        Object o = null;
        ClassDescriptor currentDesc = desc.getCurrentDesc();
        if (currentDesc == null && !skipErrors) {
            throw new RuntimeException("Current Descriptor missing for class " + desc.getClassName() + " version: " + desc.getVersionIndex() + ", clIndex: " + desc.getClassIndex() + ", providerId: " + desc.getProviderId());
        }
        SerializationInfo serializationInfo = desc.getSerializationInfo();
        Object enclosingO = null;
        if (desc.isInnerClass()) {
            int nextByte = peek(true);
            boolean refCheckRequired = (nextByte & FormatInfo.REF_INDICATOR_BIT) == 0;
            enclosingO = readAnObject(null, null, skipErrors);
            if (refCheckRequired) {
                nextByte = peek(true);
                if (nextByte != 0) {
                    return readAnObject(null, null, skipErrors);
                } else {
                    read();
                }
            }
        }
        try {
            if (serializationInfo != null) {
                Constructor constructor = serializationInfo.getConstructor();
                if (constructor == null) {
                    throw new NullPointerException("Empty argument constructor for " + desc.getClassName() + " missing or not accessible");
                }
                if (serializationInfo.getEnclosingObjectField() != null) {
                    if (enclosingO != null) {
                        o = constructor.newInstance(enclosingO);
                    } else if (!skipErrors) {
                        throw new IOException("Require enclosing class object not found for class " + desc.getClassName());

                    }
                } else {
                    o = constructor.newInstance();
                }
            }
        } catch (Exception e) {
            if (!skipErrors) {
                throw new IOException(e);
            }
        }
        refs[refIndex] = o;
        List<FieldInfo> fieldInfos = desc.getFields();
        int i, len = fieldInfos.size();
        int flag = 0;
        int nextBooleans = 0;
        int prevLevel = 0;
        for (i = 0; i < len; i++) {
            FieldInfo fieldInfo = fieldInfos.get(i);
            if (fieldInfo.getParentLevel() != prevLevel) {
                prevLevel = fieldInfo.getParentLevel();
                // Since we are moving not next class field in hierarchy new integer has to be
                // set for booleans in that class
                flag = 0;
            }
            FieldType fieldType = fieldInfo.getFieldType();
            FieldInfo mappedFieldInfo = fieldInfo;
            if (currentDesc != null && desc != currentDesc) {
                mappedFieldInfo = fieldInfo.getMappedCurrentField();
            }
            FieldType mappedFieldType = null;
            Field mappedField = null;
            if (o != null && mappedFieldInfo != null) {
                mappedField = mappedFieldInfo.getField();
                mappedFieldType = mappedFieldInfo.getFieldType();
            }
            boolean settingField = false;
            try {
                if (fieldType == FieldType.BOOLEAN) {
                    if (flag == 0) {
                        nextBooleans = readVariableLengthInteger();
                        flag = 1;
                    }
                    if (mappedField != null) {
                        settingField = true;
                        mappedField.setBoolean(o, (nextBooleans & flag) != 0);
                    }
                    flag <<= 1;
                } else {
                    flag = 0;
                    switch (fieldType) {
                    case BYTE:
                        byte b = (byte) read(true);
                        if (mappedField != null) {
                            settingField = true;
                            mappedField.setByte(o, b);
                        }
                        break;
                    case SHORT:
                    case INT:
                        int val = readInt();
                        if (mappedField != null) {
                            settingField = true;
                            if (mappedFieldType.isNumber() && mappedFieldType.ordinal() < FieldType.INT.ordinal()) {
                                settingField = true;
                                if (val >= mappedFieldType.getMinVal() && val <= mappedFieldType.getMaxVal()) {
                                    if (mappedFieldType == FieldType.SHORT) {
                                        mappedField.setShort(o, (short) val);
                                    } else {
                                        mappedField.setByte(o, (byte) val);
                                    }
                                } else if (!skipErrors) {
                                    throw new IllegalArgumentException("Value " + val + " cannot be assigned to " + mappedFieldType);
                                }
                            } else {
                                mappedField.setInt(o, val);
                            }
                        }
                        break;
                    case LONG:
                        long l = readLong();
                        if (mappedField != null) {
                            settingField = true;
                            if (mappedFieldType.isNumber() && mappedFieldType.ordinal() < FieldType.LONG.ordinal()) {
                                if (l >= mappedFieldType.getMinVal() && l <= mappedFieldType.getMaxVal()) {
                                    switch (mappedFieldType) {
                                      case INT:
                                        mappedField.setInt(o, (int) l);
                                        break;
                                      case SHORT:
                                        mappedField.setShort(o, (short) l);
                                        break;
                                      case BYTE:
                                        mappedField.setByte(o, (byte) l);
                                        break;
                                      default:
                                        mappedField.setLong(o, l);
                                        break;
                                    }
                                } else if (!skipErrors) {
                                    throw new IllegalArgumentException("Long " + l + " cannot be assigned to " + mappedFieldType);
                                }
                            } else {
                                mappedField.setLong(o, l);
                            }
                        }
                        break;
                    case FLOAT:
                        float f = readFloat();
                        if (mappedField != null) {
                            settingField = true;
                            mappedField.setFloat(o, f);
                        }
                        break;
                    case DOUBLE:
                        double d = readDouble();
                        if (mappedField != null) {
                            settingField = true;
                            if (mappedFieldType == FieldType.FLOAT) {
                                if (d >= Float.MIN_VALUE && d <= Float.MAX_VALUE) {
                                    mappedField.setFloat(o, (float) d);
                                } else {
                                    throw new IllegalArgumentException("Double " + d + " cannot be assigned to float");
                                }
                            } else {
                                mappedField.setDouble(o, d);
                            }
                        }
                        break;
                    case CHAR:
                        char ch = readChar();
                        if (mappedField != null) {
                            settingField = true;
                            mappedField.setChar(o, ch);
                        }
                        break;
                    default:
                        Class fieldClass = fieldInfo.getFieldClass();
                        Object fieldObject = readAnObject(fieldInfo.getClassName(), fieldClass, skipErrors | (mappedFieldInfo == null));
                        if (mappedField != null) {
                            settingField = true;
                            mappedField.set(o, fieldObject);
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                if (!(skipErrors && settingField)) {
                    if (e instanceof RuntimeException) {
                        throw ((RuntimeException) e);
                    } else if (e instanceof IOException){
                        throw (IOException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return o;
    }

    private ClassDescriptor readClassDescriptor(boolean skipErrors) throws IOException {
        int providerId = readVariableLengthInteger(4);
        int classIndex = readVariableLengthInteger();
        int versionIndex = readVariableLengthInteger();
        ClassDescriptor desc = lookup(providerId, classIndex, versionIndex);
        if (desc == null) {
            throw new RuntimeException("Missing Class Descriptor, providerId: " + providerId + ", classIndex: " + classIndex + ", versionIndex: " + versionIndex);
        }
        SerializationInfo serializationInfo = desc.getSerializationInfo();
        if (serializationInfo == null && !skipErrors) {
            throw new IOException("Class " + desc.getClassName() + " is not found");
        }
        return desc;
    }

    private Class readClass(int refIndex) throws IOException {
        String clName = (String) refs[refIndex];
        Class [] clRefs = classRefs;
        if (clRefs.length <= refIndex) {
            clRefs = Arrays.copyOf(clRefs, refs.length);
            classRefs = clRefs;
        }
        Class cl = clRefs[refIndex];
        if (cl == null) {
            cl = ClassUtil.getClass(clName, false);
            if (cl == null) {
                cl = DataInput.class;
            }
            clRefs[refIndex] = cl;
        }
        if (cl == DataInput.class) {
            cl = null;
        }
        return cl;
    }

    private void readBooleanArray(int len, boolean [] b) throws IOException {
        if (len > 0) {
            int i = 0;
            int flag = 0;
            int nextBooleans = 0;
            do {
                if (flag == 0) {
                    flag = 1;
                    nextBooleans = readVariableLengthInteger();
                }
                if (b != null) {
                    b[i] = (flag & nextBooleans) != 0;
                }
                flag <<= 1;
                i++;
            } while (i < len);
        }
    }

    private Object readArray(int refIndex, String defaultClassName, Class defaultClass, boolean isImplicitClass, boolean skipErrors) throws IOException {
        int i, n, dimension = readVariableLengthInteger(2) + 1;
        int len = readVariableLengthInteger();
        Class arrayComponentCl = defaultClass;
        String arrayClName = defaultClassName;
        Object o = null;
        if (!isImplicitClass) {
            arrayClName = readString();
            arrayComponentCl = readClass(strIndex);
        }
        if (arrayComponentCl == null && !skipErrors) {
            throw new RuntimeException(new ClassNotFoundException(arrayClName));
        }
        Class arrayCl = arrayComponentCl;
        if (arrayComponentCl != null) {
            for (i = 1; i < dimension; i++) {
                o = Array.newInstance(arrayCl, 0);
                arrayCl = o.getClass();
            }
            o = Array.newInstance(arrayCl, len);
        }
        refs[refIndex] = o;
        if (len > 0) {
            i = 0;
            if (dimension == 1 && arrayComponentCl != null && arrayComponentCl.isPrimitive()) {
                if (arrayComponentCl == Boolean.TYPE) {
                    readBooleanArray(len, (boolean[]) o);
                } else if (arrayComponentCl == Byte.TYPE) {
                    byte [] b = (byte[]) o;
                    if (b != null) {
                        readFully(b);
                    } else {
                        skip(len);
                    }
                } else if (arrayComponentCl == Short.TYPE) {
                    short [] s = (short[]) o;
                    do {
                        int si = readInt();
                        if (s != null) {
                            s[i] = (short) si;
                        }
                    } while (++i < len);
                } else if (arrayComponentCl == Integer.TYPE) {
                    int [] ia = (int[]) o;
                    do {
                        int iai = readInt();
                        if (ia != null) {
                            ia[i] = iai;
                        }
                    } while (++i < len);
                } else if (arrayComponentCl == Long.TYPE) {
                    long [] l = (long[]) o;
                    do {
                        long li = readLong();
                        if (l != null) {
                            l[i] = li;
                        }
                    } while (++i < len);
                } else if (arrayComponentCl == Float.TYPE) {
                    float [] f = (float[]) o;
                    do {
                        float fi = readFloat();
                        if (f != null) {
                            f[i] = fi;
                        }
                    } while (++i < len);
                } else if (arrayComponentCl == Double.TYPE) {
                    double [] d = (double[]) o;
                    do {
                        double di = readDouble();
                        if (d != null) {
                            d[i] = di;
                        }
                    } while (++i < len);
                } else if (arrayComponentCl == Character.TYPE) {
                    char [] c = (char[]) o;
                    do {
                        char ch = readChar();
                        if (c != null) {
                            c[i] = ch;
                        }
                    } while (++i < len);
                }
            } else {
                Object [] elems = (Object[]) o;
                do {
                    Object nextObj = readAnObject(arrayClName, arrayComponentCl, skipErrors);
                    if (elems != null) {
                        elems[i] = nextObj;
                    }
                } while (++i < len);
            }
        }
        return o;
    }

    private Object readCustom(Class cl, int refIndex) throws IOException {
        if (cl == null) {
            String clName = readString();
            cl = readClass(strIndex);
            if (cl == null) {
                throw new RuntimeException(new ClassNotFoundException(clName));
            }
        }
        ClassDescriptor desc = lookup(cl);
        if (desc == null) {
            throw new RuntimeException("Serialization Info not found for custom serialized class: " + cl);
        }
        StreamSerializer serializer = desc.getSerializationInfo().getSerializer();
        if (serializer == null) {
            throw new RuntimeException("Serializer not found for custom serialized class: " + cl);
        }
        Object o;
        try {
            o = serializer.deSerialize(cl, this, refIndex);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        return  o;
    }

    private Object readSerializedJavaObject() throws IOException {
        ObjectInputStream oip = new ObjectInputStream(this);
        Object o = null;
        try {
            o = oip.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        log.log(Level.SEVERE, "Warning: Java Serialization used to read class: " + o.getClass() + ". This is likely to make serialization slow");
        return o;
    }

    private Object readExternalizedJavaObject(Class cl, int refIndex) throws IOException {
        if (cl == null) {
            String clName = readString();
            cl = readClass(strIndex);
            if (cl == null) {
                throw new RuntimeException(new ClassNotFoundException(clName));
            }
        }
        ClassDescriptor desc = lookup(cl);
        if (desc == null) {
            throw new RuntimeException("Serialization Info not found for externalized class: " + cl);
        }
        Object o;
        SerializationInfo sInfo = desc.getSerializationInfo();
        try {
            Constructor cons = sInfo.getConstructor();
            Externalizable e = (Externalizable) cons.newInstance();
            refs[refIndex] = e;
            e.readExternal(this);
            Method readResolveMethod = sInfo.getReadResolveMethod();
            o = e;
            if (readResolveMethod != null) {
                o = readResolveMethod.invoke(o);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return  o;
    }

    private Enum readUnRegisteredEnum(String clName, Class cl, boolean isImplicitClass, boolean skipErrors) throws IOException {
        if (!isImplicitClass) {
            clName = readString();
            cl = readClass(strIndex);
        }
        if (cl == null && !skipErrors) {
            throw new RuntimeException(new ClassNotFoundException(clName));
        }
        String enumName = readString();
        Class<Enum> enumClass = cl;
        Enum e = null;
        try {
            e = Enum.valueOf(enumClass, enumName);
        } catch (Exception ex) {
            if (!skipErrors) {
                throw new RuntimeException(ex);
            }
        }
        return e;
    }

    private Object readAnObject(String defaultClassName, Class defaultClass, boolean skipErrors) throws IOException {
        Object o = null;
        int refIndex = 0;
        int firstByte = peek(true);
        if ((firstByte & FormatInfo.REF_INDICATOR_BIT) != 0) {
            refIndex = readVariableLengthInteger(5) - 1;
            if (refIndex >= 0) {
                o = refs[refIndex];
                if (o == null && !skipErrors) {
                    throw new RuntimeException("Got null reference at index " + refIndex);
                }
            }
        } else {
            refIndex = refCount;
            refCount++;
            if (refIndex >= refs.length) {
                refs = Arrays.copyOf(refs, refs.length + (refs.length >> 1));
            }
            if ((firstByte & FormatInfo.STRING_INDICATOR_BIT) != 0){
                o = readStringUnchecked(refIndex);
            } else if ((firstByte & FormatInfo.FIELD_SERIALIZATION_INDICATOR_BIT) != 0) {
                ClassDescriptor desc = readClassDescriptor(skipErrors);
                if (desc.getEnumNames() != null) {
                    o = readEnum(desc, refIndex, skipErrors);
                } else {
                    o = readFields(desc, refIndex, skipErrors);
                }
            } else {
                boolean isImplicitClass = (firstByte & FormatInfo.IMPLICIT_CLASS_INDICATOR_BIT) != 0;
                if (isImplicitClass && defaultClass == null && !skipErrors) {
                    throw new RuntimeException(new ClassNotFoundException(defaultClassName));
                }
                if ((firstByte & FormatInfo.ARRAY_INDICATOR_BIT) != 0) {
                    o = readArray(refIndex, defaultClassName, defaultClass, isImplicitClass, skipErrors);
                } else {
                    firstByte = readVariableLengthInteger() & 7;
                    Class defClToUse = defaultClass;
                    if (!isImplicitClass) {
                        defClToUse = null;
                    }
                    switch (firstByte) {
                      case FormatInfo.UNREGISTERED_ENUM:
                        o = readUnRegisteredEnum(defaultClassName, defaultClass, isImplicitClass, skipErrors);
                        refs[refIndex] = o;
                        break;
                      case FormatInfo.CUSTOM_SERIALIZED_OBJECT:
                        o = readCustom(defClToUse, refIndex);
                        break;
                      case FormatInfo.JAVA_SERIALIZED_OBJECT:
                        o = readSerializedJavaObject();
                        refs[refIndex] = o;
                        break;
                      case FormatInfo.JAVA_EXTERNALIZED_OBJECT:
                          o = readExternalizedJavaObject(defClToUse, refIndex);
                          break;
                    }
                }
            }
        }
        return o;
    }

    public void registerReference(Object o, int refIndex) {
        refs[refIndex] = o;
    }

    public Object readAnObject(Class defaultClass) throws IOException {
        String defaultClassName = null;
        if (defaultClass != null) {
            defaultClassName = defaultClass.getName();
        }
        return readAnObject(defaultClassName, defaultClass, false);
    }

}
