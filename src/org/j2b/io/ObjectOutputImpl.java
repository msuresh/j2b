package org.j2b.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.j2b.J2BConstants;
import org.j2b.serializer.FormatInfo;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.SerializationMethod;
import org.j2b.serializer.StreamSerializer;
import org.j2b.serializer.SerializationContext.SerializationInfoCache;
import org.j2b.serializer.field.ClassDescriptor;
import org.j2b.serializer.field.FieldType;
import org.j2b.serializer.field.ClassDescriptor.FieldInfo;
import org.j2b.serializer.field.ClassDescriptor.SerializationInfo;

public class ObjectOutputImpl extends DataOutputImpl implements ObjectOutput {
    private static class ObjectReference {
        Object o;

        int refNumber;
    }

    private List<ObjectReference> refPool = new ArrayList<ObjectReference>();

    private ObjectReference returnedRef;

    private int poolPos;

    private static final Logger log = Logger.getLogger(ObjectOutputImpl.class.getName());

    private boolean headerWritten;

    private SerializationContext context;

    private SerializationInfoCache contextCache;

    private Map<Object, ObjectReference> matchedRefs = new HashMap<Object, ObjectReference>();

    private Map<Object, ObjectReference> objRefs = new IdentityHashMap<Object, ObjectReference>();

    private int refCount;

    private int maxStringRefLength = 64;

    public ObjectOutputImpl(SerializationContext pcontext) {
        super();
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectOutputImpl(SerializationContext pcontext, int bufSize) {
        super(bufSize);
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectOutputImpl(SerializationContext pcontext, OutputStream pop, int bufSize) {
        super(pop, bufSize);
        context = pcontext;
        contextCache = context.getCache();
    }

    public ObjectOutputImpl(SerializationContext pcontext, OutputStream pop) {
        super(pop);
        context = pcontext;
        contextCache = context.getCache();
    }

    private ClassDescriptor lookup(Class cl) {
        ClassDescriptor desc = contextCache.lkup(cl);
        if (desc == null) {
            desc = context.lookup(cl);
            contextCache = context.getCache();
        }
        return desc;
    }

    private ObjectReference getRef(Object o, int refNumber) {
        ObjectReference ref = returnedRef;
        returnedRef = null;
        if (ref == null) {
            if (poolPos < refPool.size()) {
                ref = refPool.get(poolPos++);
            } else {
                ref = new ObjectReference();
                refPool.add(ref);
                poolPos = refPool.size();
            }
        }
        ref.o = o;
        ref.refNumber = refNumber;
        return ref;
    }

    public void reset(OutputStream newOp, byte [] b) {
        if (matchedRefs.size() < 10000) {
            matchedRefs.clear();
        } else {
            matchedRefs = new HashMap<Object, ObjectReference>();
        }
        if (objRefs.size() < 10000) {
            objRefs.clear();
        } else {
            objRefs = new IdentityHashMap<Object, ObjectReference>();
        }
        refCount = 0;
        poolPos = 0;
        returnedRef = null;
        headerWritten = false;
        super.reset(newOp, b);
    }

    public void writeString(String s) throws IOException {
        if (s == null) {
            write(FormatInfo.NULL_REFERENCE);
        } else {
            ObjectReference newRef = getRef(s, refCount + 1);
            ObjectReference ref = null;
            if (s.length() <= maxStringRefLength) {
                ref = matchedRefs.put(s, newRef);
            } else {
                ref = objRefs.put(s, newRef);
            }
            if (ref == null) {
                refCount++;
                byte [] b = s.getBytes(J2BConstants.UTF_8_CHARSET);
                writeVariableLengthInteger(b.length, 6, FormatInfo.STRING_INDICATOR_BIT);
                write(b);
            } else {
                int refNumber = ref.refNumber;
                newRef.refNumber = refNumber;
                returnedRef = ref;
                writeVariableLengthInteger(refNumber, 5, FormatInfo.REF_INDICATOR_BIT);
            }
        }
    }

    private void writeBooleanArray(boolean [] b) throws IOException {
        int len = b.length;
        if (len > 0) {
            int i = 0;
            int flag = 1;
            int nextBooleans = 0;
            do {
                if (b[i]) {
                    nextBooleans |= flag;
                }
                flag <<= 1;
                if (flag == 0) {
                    writeVariableLengthInteger(nextBooleans);
                    nextBooleans = 0;
                    flag = 1;
                }
                i++;
            } while (i < len);
            if (flag != 1) {
                writeVariableLengthInteger(nextBooleans);
            }
        }
    }

    private void writeArray(Object o, Class expectedClass) throws IOException {
        Class<?> cl = o.getClass();
        int dimension = 0;
        Class arrayComponentCl = cl;
        do {
            dimension++;
            arrayComponentCl = arrayComponentCl.getComponentType();
        } while (arrayComponentCl.isArray());
        int leadingBits = FormatInfo.ARRAY_INDICATOR_BIT;
        if (arrayComponentCl == expectedClass) {
            leadingBits |= FormatInfo.IMPLICIT_CLASS_INDICATOR_BIT;
        }
        writeVariableLengthInteger(dimension - 1, 2, leadingBits);
        int i;
        int len = Array.getLength(o);
        writeVariableLengthInteger(len);
        if (arrayComponentCl != expectedClass) {
            writeString(arrayComponentCl.getClass().getName());
        }
        if (len > 0) {
            if (dimension == 1 && arrayComponentCl.isPrimitive()) {
                if (arrayComponentCl == Boolean.TYPE) {
                    writeBooleanArray((boolean[]) o);
                } else if (arrayComponentCl == Byte.TYPE) {
                    byte [] b = (byte[]) o;
                    write(b);
                } else if (arrayComponentCl == Short.TYPE) {
                    short [] s = (short[]) o;
                    for (i = 0; i < len; i++) {
                        writeInt(s[i]);
                    }
                } else if (arrayComponentCl == Integer.TYPE) {
                    int [] ia = (int[]) o;
                    for (i = 0; i < len; i++) {
                        writeInt(ia[i]);
                    }
                } else if (arrayComponentCl == Long.TYPE) {
                    long [] l = (long[]) o;
                    for (i = 0; i < len; i++) {
                        writeLong(l[i]);
                    }
                } else if (arrayComponentCl == Float.TYPE) {
                    float [] f = (float[]) o;
                    for (i = 0; i < len; i++) {
                        writeFloat(f[i]);
                    }
                } else if (arrayComponentCl == Double.TYPE) {
                    double [] d = (double[]) o;
                    for (i = 0; i < len; i++) {
                        writeDouble(d[i]);
                    }
                } else if (arrayComponentCl == Character.TYPE) {
                    char [] c = (char[]) o;
                    for (i = 0; i < len; i++) {
                        writeChar(c[i]);
                    }
                }
            } else {
                Object [] elems = (Object[]) o;
                for (i = 0; i < len; i++) {
                    writeAnObject(elems[i], arrayComponentCl);
                }
            }
        }
    }

    private void writeRegisteredEnum(Enum e, int providerId, ClassDescriptor desc) throws IOException {
        writeVariableLengthInteger(providerId, 4, FormatInfo.FIELD_SERIALIZATION_INDICATOR_BIT);
        writeVariableLengthInteger(desc.getClassIndex());
        writeVariableLengthInteger(desc.getVersionIndex());
        writeVariableLengthInteger(e.ordinal());
    }

    private void writeUnRegisteredEnum(Enum e, Class expectedClass) throws IOException {
        Class enumClass = e.getClass();
        if (enumClass == expectedClass) {
            write(FormatInfo.IMPLICIT_CLASS_INDICATOR_BIT | FormatInfo.UNREGISTERED_ENUM);
        } else {
            write(FormatInfo.UNREGISTERED_ENUM);
            writeString(enumClass.getName());
        }
        writeString(e.name());
    }

    private void writeCustom(Object o, Class expectedClass, StreamSerializer serializer) throws Exception {
        Class cl = o.getClass();
        if (cl == expectedClass) {
            write(FormatInfo.IMPLICIT_CLASS_INDICATOR_BIT | FormatInfo.CUSTOM_SERIALIZED_OBJECT);
        } else {
            write(FormatInfo.CUSTOM_SERIALIZED_OBJECT);
            writeString(cl.getName());
        }
        serializer.serialize(o, this);
    }

    private void writeJavaSerializedObject(Object o) throws IOException {
        write(FormatInfo.JAVA_SERIALIZED_OBJECT);
        log.log(Level.SEVERE, "Warning: Java Serialization used to write class: " + o.getClass() + ". This is likely to make serialization slow");
        ObjectOutputStream oop = new ObjectOutputStream(this);
        oop.writeObject(o);
        oop.flush();
    }

    private void writeJavaExternalizedObject(Object o, SerializationInfo sInfo, Class expectedClass) throws IOException {
        Method writeReplaceMethod = sInfo.getWriteReplaceMethod();
        if (writeReplaceMethod != null) {
            try {
                o = writeReplaceMethod.invoke(o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Class cl = o.getClass();
        if (cl == expectedClass) {
            write(FormatInfo.IMPLICIT_CLASS_INDICATOR_BIT | FormatInfo.JAVA_EXTERNALIZED_OBJECT);
        } else {
            write(FormatInfo.JAVA_EXTERNALIZED_OBJECT);
            writeString(cl.getName());
        }
        ((Externalizable) o).writeExternal(this);
    }

    private void writeFields(Object o, int providerId, ClassDescriptor desc) throws IOException {
        writeVariableLengthInteger(providerId, 4, FormatInfo.FIELD_SERIALIZATION_INDICATOR_BIT);
        writeVariableLengthInteger(desc.getClassIndex());
        writeVariableLengthInteger(desc.getVersionIndex());
        Class cl = o.getClass();
        boolean refCheckRequired = false;
        try {
            SerializationInfo serializationInfo = desc.getSerializationInfo();
            if (desc.isInnerClass()) {
                Field field = serializationInfo.getEnclosingObjectField();
                Object outer = field.get(o);
                if (objRefs.get(outer) == null) {
                    refCheckRequired = true;
                    objRefs.remove(o);
                }
                writeAnObject(outer, null);
                if (refCheckRequired) {
                    ObjectReference ref = objRefs.get(o);
                    if (ref != null) {
                        writeAnObject(o, null);
                        return;
                    } else {
                        write(0);
                    }
                }
            }
            List<FieldInfo> fieldInfos = desc.getFields();
            int i, len = fieldInfos.size();
            int flag = 1;
            int nextBooleans = 0;
            int prevLevel = 0;
            for (i = 0; i < len; i++) {
                FieldInfo fieldInfo = fieldInfos.get(i);
                if (fieldInfo.getParentLevel() != prevLevel) {
                    prevLevel = fieldInfo.getParentLevel();
                    // When moving down class hierarchy flush boolean variables for the class
                    if (flag != 1) {
                        writeVariableLengthInteger(nextBooleans);
                        flag = 1;
                        nextBooleans = 0;
                    }
                }
                Field field = fieldInfo.getField();
                FieldType fieldType = fieldInfo.getFieldType();
                if (fieldType == FieldType.BOOLEAN) {
                    if (field.getBoolean(o)) {
                        nextBooleans |= flag;
                    }
                    flag <<= 1;
                    if (flag == 0) {
                        writeVariableLengthInteger(nextBooleans);
                        flag = 1;
                        nextBooleans = 0;
                    }
                } else {
                    if (flag != 1) {
                        writeVariableLengthInteger(nextBooleans);
                        flag = 1;
                        nextBooleans = 0;
                    }
                    switch (fieldType) {
                      case BYTE:
                        byte b = field.getByte(o);
                        write(b);
                        break;
                      case SHORT:
                      case INT:
                        int val = field.getInt(o);
                        writeInt(val);
                        break;
                      case LONG:
                        long l = field.getLong(o);
                        writeLong(l);
                        break;
                      case FLOAT:
                        float f = field.getFloat(o);
                        writeFloat(f);
                        break;
                      case DOUBLE:
                        double d = field.getDouble(o);
                        writeDouble(d);
                        break;
                      case CHAR:
                        char ch = field.getChar(o);
                        writeChar(ch);
                        break;
                      default:
                        Class fieldClass = fieldInfo.getFieldClass();
                        Object fieldObject = field.get(o);
                        writeAnObject(fieldObject, fieldClass);
                        break;
                    }
                }
            }
            if (flag != 1) {
                writeVariableLengthInteger(nextBooleans);
            }
        } catch (Exception e) {
            RuntimeException re = null;
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                re = (RuntimeException) e;
            } else {
                re = new RuntimeException(e);
            }
            throw re;
        }
    }

    public void writeAnObject(Object o, Class expectedClass) throws IOException {
        if (o == null || o instanceof String) {
            writeString((String) o);
        } else {
            Class cl = o.getClass();
            int refNumber = 0;
            ObjectReference newRef = getRef(o, refCount + 1);
            ObjectReference ref = null;
            if (o instanceof Number) {
                ref = matchedRefs.put(o, newRef);
            } else {
                ref = objRefs.put(o, newRef);
            }
            if (ref == null) {
                refCount++;
            } else {
                refNumber = ref.refNumber;
                newRef.refNumber = refNumber;
                returnedRef = ref;
                writeVariableLengthInteger(refNumber, 5, FormatInfo.REF_INDICATOR_BIT);
            }
            if (refNumber == 0) {
                if (cl.isArray()) {
                    writeArray(o, expectedClass);
                } else {
                    ClassDescriptor desc = lookup(cl);
                    SerializationInfo sInfo = desc.getSerializationInfo();
                    SerializationMethod serializationMethod = sInfo.getSerializationMethod();
                    int providerId = desc.getProviderId();
                    switch (serializationMethod) {
                      case FIELD:
                          if (providerId < 0) {
                              throw new RuntimeException("Cannot Field Serialize " + desc.getClassName() + " since the latest version is not saved in ClassDescriptorStore, providerId is " + providerId);
                          }
                          if (o instanceof Enum) {
                              writeRegisteredEnum((Enum) o, providerId, desc);
                          } else {
                              writeFields(o, providerId, desc);
                          }
                          break;
                      case CUSTOM:
                          if (o instanceof Enum) {
                              writeUnRegisteredEnum((Enum) o, expectedClass);
                          } else {
                              try {
                                writeCustom(o, expectedClass, sInfo.getSerializer());
                            } catch (Exception e) {
                                if (e instanceof RuntimeException) {
                                    throw (RuntimeException) e;
                                } else {
                                    throw new RuntimeException(e);
                                }
                            }
                          }
                          break;
                      case JAVA_OBJECT:
                          writeJavaSerializedObject(o);
                          break;
                      case JAVA_EXTERNALIZABLE:
                          writeJavaExternalizedObject(o, sInfo, expectedClass);
                          break;
                    }
                }
            }
        }
    }

    @Override
    public void writeObject(Object o) throws IOException {
        boolean resetHeaderWritten = false;
        if (!headerWritten) {
            headerWritten = true;
            resetHeaderWritten = true;
            write(J2BConstants.J2B_HEADER_BYTE_1);
            write(J2BConstants.J2B_HEADER_BYTE_2);
            writeVariableLengthInteger(J2BConstants.J2B_VERSION);
        }
        writeAnObject(o, null);
        if (resetHeaderWritten) {
            headerWritten = false;
        }
    }

    public int getMaxStringRefLength() {
        return maxStringRefLength;
    }

    public void setMaxStringRefLength(int pmaxStringRefLength) {
        maxStringRefLength = pmaxStringRefLength;
    }

}
