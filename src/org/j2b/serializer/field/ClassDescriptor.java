package org.j2b.serializer.field;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.j2b.reflect.ClassUtil;
import org.j2b.serializer.SerializationMethod;
import org.j2b.serializer.StreamSerializer;
import org.j2b.util.ArrayWrappingList;

public class ClassDescriptor implements Serializable {

	public static class FieldInfo implements Comparable<FieldInfo>, Serializable {

        private static final long serialVersionUID = 1142092923120463770L;

        private transient Field field;

		private long createTime;

		private String fieldName;

		private int parentLevel;

		private boolean isArray;

		private FieldType fieldType;

		private String className;

		private transient Class fieldClass;

		private transient FieldInfo mappedCurrentField;

		public FieldInfo(Field pfield, int pparentLevel) {
			field = pfield;
			fieldName = pfield.getName();
			parentLevel = pparentLevel;
            Class fcl = field.getType();
            fieldType = FieldType.fromClass(fcl);
			if (fieldType == FieldType.ARRAY) {
			    isArray = true;
			    do {
			        fcl = fcl.getComponentType();
			    } while (fcl.isArray());
	            fieldType = FieldType.fromClass(fcl);
			}
			className = fcl.getName();
			fieldClass = fcl;
		}

		@Override
		public int compareTo(FieldInfo o) {
			int result = o.parentLevel - parentLevel;
			if (result == 0) {
			    if (fieldType == FieldType.BOOLEAN) {
			        result--;
			    }
                if (o.fieldType == FieldType.BOOLEAN) {
                    result++;
                }
			}
			if (result == 0) {
			    result = fieldName.compareTo(o.fieldName);
			}
			// The below cases will not happen within same class version, but is added for
			// equals method to work across class versions
			if (result == 0) {
			    if (isArray != o.isArray) {
			        result = isArray ? -1 : 1;
			    }
			}
			if (result == 0) {
			    result = fieldType.ordinal() - o.fieldType.ordinal();
			}
			if (result == 0) {
			    result = className.compareTo(o.className);
			}
			return result;
		}

		public void initForRuntime() {
		    try {
                fieldClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
            }
		}

		@Override
		public boolean equals(Object o) {
		    boolean result = false;
		    if (o instanceof FieldInfo) {
		        result = compareTo((FieldInfo) o) == 0;
		    }
		    return result;
		}

		public Field getField() {
            return field;
        }

        public long getCreateTime() {
            return createTime;
        }

        public String getFieldName() {
            return fieldName;
        }

        public int getParentLevel() {
            return parentLevel;
        }

        public boolean isArray() {
            return isArray;
        }

        public FieldType getFieldType() {
            return fieldType;
        }

        public String getClassName() {
            return className;
        }

        public Class getFieldClass() {
            return fieldClass;
        }

        public FieldInfo getMappedCurrentField() {
            return mappedCurrentField;
        }
	}

	public static class SerializationInfo {

	    private Class cl;

	    private Field enclosingObjectField;

	    private Constructor constructor;

	    private SerializationMethod serializationMethod;

        private StreamSerializer serializer;

        private Method writeReplaceMethod;

        private Method readResolveMethod;

        public SerializationInfo(Class cls, SerializationMethod serMethod, StreamSerializer pserializer) {
	        cl = cls;
	        constructor = null;
	        if (ClassUtil.isInnerClass(cls)) {
	            Class enclosingCl = cls.getEnclosingClass();
	            try {
	                constructor = cls.getDeclaredConstructor(enclosingCl);
	            } catch (Exception e) {
	            }
	            int suffix = 0;
	            while ((enclosingCl = enclosingCl.getEnclosingClass()) != null) {
	                suffix++;
	            }
	            try {
                    enclosingObjectField = cl.getDeclaredField("this$" + suffix);
                    enclosingObjectField.setAccessible(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
	        } else {
	            try {
	                constructor = cls.getDeclaredConstructor();
	            } catch (Exception e) {
	            }
	        }
	        if (constructor != null) {
	            constructor.setAccessible(true);
	        }
	        serializer = pserializer;
	        if (serMethod == SerializationMethod.JAVA_OBJECT) {
	            Method m;
	            if (Externalizable.class.isAssignableFrom(cl)) {
	                serMethod = SerializationMethod.JAVA_EXTERNALIZABLE;
	                try {
	                    m = cl.getDeclaredMethod("readResolve");
	                    m.setAccessible(true);
	                    readResolveMethod = m;
	                } catch (Exception e) {
	                }
	            } else {
	                try {
	                    m = cl.getDeclaredMethod("writeReplace");
	                    m.setAccessible(true);
	                    boolean isExternalizable = true;
	                    if (!cl.getName().startsWith("java.time.")) {
                            isExternalizable = false;
                            Object o = cl.newInstance();
                            o = m.invoke(o);
                            if (o instanceof Externalizable) {
                                isExternalizable = true;
                            }
	                    }
	                    if (isExternalizable) {
                            serMethod = SerializationMethod.JAVA_EXTERNALIZABLE;
                            writeReplaceMethod = m;
	                    }
	                } catch (Exception e) {
	                }
	            }
	        }
            serializationMethod = serMethod;
	    }

        public Class getCl() {
            return cl;
        }

        public Field getEnclosingObjectField() {
            return enclosingObjectField;
        }

        public Constructor getConstructor() {
            return constructor;
        }

        public SerializationMethod getSerializationMethod() {
            return serializationMethod;
        }

        public StreamSerializer getSerializer() {
            return serializer;
        }

        public Method getWriteReplaceMethod() {
            return writeReplaceMethod;
        }

        public Method getReadResolveMethod() {
            return readResolveMethod;
        }

	}

	private static final long serialVersionUID = 5951650052001425950L;

	private boolean isInnerClass;

	private String [] enumNames;

	private transient List<String> enumNamesList;

	private String className;

	private FieldInfo [] fields;

    private transient List<FieldInfo> fieldsList;

    private transient SerializationInfo serializationInfo;

    int providerId = -1;

	int classIndex = -1;

	int versionIndex = -1;

	private transient ClassDescriptor currentDesc;

	private transient List<Enum> currentEnumVals;

	public ClassDescriptor(SerializationInfo sInfo) {
	    int i;
	    Class cls = sInfo.cl;
	    if (cls.isArray()) {
	        throw new IllegalArgumentException("Descriptor not supported for class type " + cls);
	    }
	    serializationInfo = sInfo;
	    className = cls.getName();
		List<FieldInfo> fList = new ArrayList<FieldInfo>();
		isInnerClass = ClassUtil.isInnerClass(cls);
		if (cls.isEnum()) {
		    Enum [] enumVals = (Enum[]) cls.getEnumConstants();
		    enumNames = new String[enumVals.length];
		    for (i = 0; i < enumVals.length; i++) {
		        enumNames[i] = enumVals[i].toString();
		    }
		    enumNamesList = new ArrayWrappingList<String>(enumNames);
		    currentEnumVals = new ArrayWrappingList<Enum>(enumVals);
		} else {
	        Class<?> superCl = cls;
	        int level = 0;
	        do {
	            Field [] fields = superCl.getDeclaredFields();
	            for (Field f : fields) {
	                if (!Modifier.isTransient(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
	                    f.setAccessible(true);
	                    FieldInfo fi = new FieldInfo(f, level);
	                    fList.add(fi);
	                }
	            }
	            superCl = superCl.getSuperclass();
	            level++;
	        } while (superCl != null);
	        Collections.sort(fList);
	        fields = fList.toArray(new FieldInfo[fList.size()]);
	        fieldsList = new ArrayWrappingList<FieldInfo>(fields);
		}
		currentDesc = this;
	}

	public void initRuntimeVals() {
	    int i;
	    if (enumNames != null) {
	        enumNamesList = new ArrayWrappingList<String>(enumNames);
	    }
	    if (fields != null) {
	        fieldsList = new ArrayWrappingList<FieldInfo>(fields);
	        for (i = 0; i < fields.length; i++) {
	            fields[i].initForRuntime();
	        }
	    }
	}

	private void readObject(ObjectInputStream oip) throws ClassNotFoundException, IOException {
	    oip.defaultReadObject();
	    initRuntimeVals();
	}

	private int matchField(FieldInfo f, FieldInfo [] currentFields, boolean [] alreadyMatched, boolean ignoreLevel) {
	    int matchedIndex = -1;
	    int i, closestMatchIndex = -1, closestMatchedDiff = 100, closestMatchedDiffAbs = 100;
	    for (i = 0; i < currentFields.length; i++) {
	        if (!alreadyMatched[i]) {
	            FieldInfo currentField = currentFields[i];
	            if (f.fieldName.equals(currentField.fieldName)) {
	                int diff = f.parentLevel - currentField.parentLevel;
	                if (diff == 0) {
	                    matchedIndex = i;
	                    alreadyMatched[i] = true;
	                    break;
	                }
	                int diffAbs = Math.abs(diff);
	                if (diffAbs < closestMatchedDiffAbs || diffAbs == closestMatchedDiffAbs && diff > closestMatchedDiff) {
	                    closestMatchIndex = i;
	                    closestMatchedDiff = diff;
	                    closestMatchedDiffAbs = diffAbs;
	                }
	            }
	        }
	    }
	    if (matchedIndex < 0 && ignoreLevel && closestMatchIndex >= 0) {
	        matchedIndex = closestMatchIndex;
	        alreadyMatched[closestMatchIndex] = true;
	    }
	    return matchedIndex;
	}

	public boolean copyIdsIfCurrent(ClassDescriptor descFromStore) {
	    boolean result = false;
	    if (this.equals(descFromStore)) {
	        result = true;
	        providerId = descFromStore.providerId;
	        classIndex = descFromStore.classIndex;
	        versionIndex = descFromStore.versionIndex;
	    }
	    return result;
	}

	public void mapToCurrentDescriptor(ClassDescriptor curDesc) {
	    int i;
	    if (this.currentDesc == null && !this.equals(curDesc)) {
	        if (!className.equals(curDesc.className)) {
	            throw new  RuntimeException("Unexpected Current Class " + curDesc.className + " not matching desc class " + className);
	        }
	        className = currentDesc.className; // Keeping reference same is better
	        currentDesc = curDesc;
	        serializationInfo = curDesc.serializationInfo;
	        Class cl = null;
	        if (serializationInfo != null) {
	            cl = serializationInfo.cl;
	        }
	        if (curDesc.enumNames != null) {
	            if (enumNames == null) {
	                throw new RuntimeException("Current class " + className + " is an enum, whereas previous is not");
	            }
	            Enum [] currentEnums = new Enum[enumNames.length];
	            if (cl != null) {
	                for (i = 0; i < enumNames.length; i++) {
	                    Enum e = null;
	                    try {
	                        e = Enum.valueOf(cl, enumNames[i]);
	                        enumNames[i] = e.name();
	                    } catch (Exception ex) {
	                    }
	                    currentEnums[i] = e;
	                }
	            }
	            currentEnumVals = new ArrayWrappingList<Enum>(currentEnums);
	        } else {
	            if (fields == null) {
                    throw new RuntimeException("Current class " + className + " has null fields");
	            }
	            FieldInfo [] oldFields = fields;
	            int [] mappedIndexes = new int[oldFields.length];
	            Arrays.fill(mappedIndexes, -1);
	            FieldInfo [] currentFields = curDesc.fields;
	            boolean [] currentMatchedFields = new boolean[currentFields.length];
	            int matchedIndex;
	            for (i = 0; i < oldFields.length; i++) {
	                FieldInfo f = oldFields[i];
	                matchedIndex = matchField(f, currentFields, currentMatchedFields, false);
	                if (matchedIndex >= 0) {
	                    mappedIndexes[i] = matchedIndex;
	                }
	            }
                for (i = 0; i < oldFields.length; i++) {
                    if (mappedIndexes[i] < 0) {
                        FieldInfo f = oldFields[i];
                        matchedIndex = matchField(f, currentFields, currentMatchedFields, true);
                        if (matchedIndex >= 0) {
                            mappedIndexes[i] = matchedIndex;
                        }
                    }
                }
                for (i = 0; i < oldFields.length; i++) {
                    FieldInfo f = oldFields[i];
                    try {
                        f.fieldClass = Class.forName(f.className);
                    } catch (Exception e) {
                    }
                    int currentFieldIndex = mappedIndexes[i];
                    if (currentFieldIndex >= 0) {
                        FieldInfo currentField = currentFields[currentFieldIndex];
                        f.mappedCurrentField  = currentField;
                    }
                }
	        }
	    }
	}

	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if (o instanceof ClassDescriptor) {
			ClassDescriptor od = (ClassDescriptor) o;
			if (className.equals(od.className) && isInnerClass == od.isInnerClass) {
				if (enumNames != null) {
				    result = Arrays.equals(enumNames, od.enumNames);
				} else {
				    result = Arrays.equals(fields, od.fields);
				}
			}
		}
		return result;
	}

    public boolean isInnerClass() {
        return isInnerClass;
    }

    public List<String> getEnumNames() {
        return enumNamesList;
    }

    public List<FieldInfo> getFields() {
        return fieldsList;
    }

    public String getClassName() {
        return className;
    }

    public SerializationInfo getSerializationInfo() {
        return serializationInfo;
    }

    public int getProviderId() {
        return providerId;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public int getVersionIndex() {
        return versionIndex;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public ClassDescriptor getCurrentDesc() {
        return currentDesc;
    }

    public List<Enum> getCurrentEnumVals() {
        return currentEnumVals;
    }

}
