package org.j2b.serializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.j2b.serializer.field.ClassDescriptor;
import org.j2b.serializer.field.ClassDescriptor.SerializationInfo;
import org.j2b.serializer.impl.BooleanSerializer;
import org.j2b.serializer.impl.ByteSerializer;
import org.j2b.serializer.impl.CharacterSerializer;
import org.j2b.serializer.impl.CollectionSerializer;
import org.j2b.serializer.impl.DateSerializer;
import org.j2b.serializer.impl.DoubleSerializer;
import org.j2b.serializer.impl.FloatSerializer;
import org.j2b.serializer.impl.IntegerSerializer;
import org.j2b.serializer.impl.LongSerializer;
import org.j2b.serializer.impl.MapSerializer;
import org.j2b.serializer.impl.ShortSerializer;
import org.j2b.serializer.field.ClassDescriptorStore;

public class SerializationContext {

    public static class SerializationInfoCache {
        private Map<Class, ClassDescriptor> classMap = new HashMap<Class, ClassDescriptor>();

        private ClassDescriptor [][][] directIdLookup = new ClassDescriptor[NUM_DIRECT_LOOKUP_PROVIDER_IDS][0][0];

        private ClassDescriptor [][][] packedIdLookup = new ClassDescriptor[0][0][0];

        private int [] packedProviderIds = new int[0];

        private int numClassInfos = 0;

        public ClassDescriptor lkup(Class cl) {
            return classMap.get(cl);
        }

        public ClassDescriptor lkup(int providerId, int classIndex, int versionIndex) {
            ClassDescriptor desc = null;
            int i;
            ClassDescriptor [][] classDescs = null;
            if (providerId < NUM_DIRECT_LOOKUP_PROVIDER_IDS) {
                classDescs = directIdLookup[providerId];
            } else {
                i = Arrays.binarySearch(packedProviderIds, providerId);
                if (i >= 0) {
                    classDescs = packedIdLookup[i];
                }
            }
            if (classDescs != null && classDescs.length > classIndex) {
                ClassDescriptor [] classVersions = classDescs[classIndex];
                if (classVersions != null && classVersions.length > versionIndex) {
                    desc = classVersions[versionIndex];
                }
            }

            return desc;
        }

        SerializationInfoCache addClassDescriptor(ClassDescriptor desc) {
            SerializationInfoCache newCache = null;
            Class cl = null;
            SerializationInfo sInfo = desc.getSerializationInfo();
            if (sInfo != null) {
                cl = sInfo.getCl();
            }
            if (cl != null && desc.getCurrentDesc() == desc) {
                if (newCache == null) {
                    newCache = new SerializationInfoCache();
                }
                newCache.classMap = new HashMap<Class, ClassDescriptor>(classMap);
                newCache.classMap.put(cl, desc);
            }
            int providerId = desc.getProviderId();
            int len;
            if (providerId >= 0) {
                if (newCache == null) {
                    newCache = new SerializationInfoCache();
                    newCache.classMap = classMap;
                }
                int classIndex = desc.getClassIndex();
                int versionIndex = desc.getVersionIndex();
                ClassDescriptor [][][] idLookup;
                int idLookupIndex = providerId;
                if (providerId < NUM_DIRECT_LOOKUP_PROVIDER_IDS) {
                    newCache.packedIdLookup = packedIdLookup;
                    newCache.packedProviderIds = packedProviderIds;
                    idLookup = Arrays.copyOf(directIdLookup, directIdLookup.length);
                    newCache.directIdLookup = idLookup;
                } else {
                    newCache.directIdLookup = directIdLookup;
                    int [] packedIds = packedProviderIds;
                    idLookup = packedIdLookup;
                    idLookupIndex = Arrays.binarySearch(packedIds, providerId);
                    if (idLookupIndex < 0) {
                        int [] newPackedIds = new int[packedIds.length + 1];
                        idLookupIndex = -idLookupIndex -1;
                        System.arraycopy(packedIds, 0, newPackedIds, 0, idLookupIndex);
                        System.arraycopy(packedIds, idLookupIndex, newPackedIds, idLookupIndex + 1, packedIds.length - idLookupIndex);
                        newPackedIds[idLookupIndex] = providerId;
                        newCache.packedProviderIds = newPackedIds;
                        ClassDescriptor [][][] newIdLookup = new ClassDescriptor[packedIds.length + 1][][];
                        System.arraycopy(idLookup, 0, newIdLookup, 0, idLookupIndex);
                        System.arraycopy(idLookup, idLookupIndex, newIdLookup, idLookupIndex + 1, packedIds.length - idLookupIndex);
                        idLookup = newIdLookup;
                        newCache.packedIdLookup = idLookup;
                    } else {
                        idLookup = Arrays.copyOf(idLookup, idLookup.length);
                        newCache.packedIdLookup = idLookup;
                    }
                }
                ClassDescriptor [][] classLookup = idLookup[idLookupIndex];
                if (classLookup == null) {
                    classLookup = new ClassDescriptor[0][];
                }
                len = classLookup.length;
                if (len <= classIndex) {
                    len = classIndex + 1;
                }
                classLookup = Arrays.copyOf(classLookup, len);
                idLookup[idLookupIndex] = classLookup;
                ClassDescriptor [] versionLookup = classLookup[classIndex];
                if (versionLookup == null) {
                    versionLookup = new ClassDescriptor[0];
                }
                len = versionLookup.length;
                if (len <= versionIndex) {
                    len = versionIndex + 1;
                }
                versionLookup = Arrays.copyOf(versionLookup, len);
                classLookup[classIndex] = versionLookup;
                versionLookup[versionIndex] = desc;
            }
            if (newCache != null) {
                newCache.numClassInfos = numClassInfos + 1;
            } else {
                throw new RuntimeException("Failed to add Serialization Info for: " + desc.getClassName() + " as provider id is " + desc.getProviderId() + " and there is no valid class");
            }

            return newCache;
        }
    }

    private static final int NUM_DIRECT_LOOKUP_PROVIDER_IDS = 32;

    private static volatile StreamSerializer [] defaultSerializers = new StreamSerializer[0];

    static {
        synchronized (SerializationContext.class) {
            defaultSerializers = addSerializer(defaultSerializers, Boolean.class, BooleanSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Byte.class, ByteSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Character.class, CharacterSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Collection.class, CollectionSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Date.class, DateSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Double.class, DoubleSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Float.class, FloatSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Integer.class, IntegerSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Long.class, LongSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Map.class, MapSerializer.INSTANCE, false);
            defaultSerializers = addSerializer(defaultSerializers, Short.class, ShortSerializer.INSTANCE, false);
        }
    }

    private StreamSerializer [] serializers = new StreamSerializer[0];

    private volatile SerializationInfoCache cache = new SerializationInfoCache();

    private ClassDescriptorStore descStore;

    public SerializationContext(ClassDescriptorStore store) {
        descStore = store;
        StreamSerializer [] ds = defaultSerializers;
        serializers = Arrays.copyOf(ds, ds.length);
    }

    private static <T> StreamSerializer [] addSerializer(StreamSerializer [] serializers, Class<T> cl, StreamSerializer<T> serializer, boolean overwrite) {
        if (cl == null) {
            throw new NullPointerException("Class is null");
        }
        if (serializer == null) {
            throw new NullPointerException("Serializer is null");
        }
        int i = 0;
        for (i = 0; i < serializers.length; i++) {
            if (serializers[i].getHandledClass() == cl) {
                break;
            }
        }
        if (i >= serializers.length) {
            serializers = Arrays.copyOf(serializers, serializers.length + 1);
            serializers[i] = serializer;
        } else if (overwrite) {
            serializers[i] = serializer;
        }

        return serializers;
    }

    public static <T> void addDefaultSerializer(Class<T> cl, StreamSerializer<T> serializer, boolean overwrite) {
        synchronized (SerializationContext.class) {
            defaultSerializers = addSerializer(defaultSerializers, cl, serializer, overwrite);
        }
    }

    public <T> void addCustomSerializer(StreamSerializer [] serializers, Class<T> cl, StreamSerializer<T> serializer) {
        SerializationInfoCache c = cache;
        if (c.numClassInfos > 0) {
            throw new RuntimeException("Cannot add Serializer after it has been used");
        }
        serializers = addSerializer(serializers, cl, serializer, true);
    }

    private StreamSerializer getSerializer(Class cl) {
        StreamSerializer serializer = null;
        int i;
        for (i = 0; i < serializers.length; i++) {
            Class registeredClass = serializers[i].getHandledClass();
            if (cl == registeredClass) {
                serializer = serializers[i];
                break;
            }
            if (registeredClass.isAssignableFrom(cl)) {
                serializer = serializers[i];
            }
        }

        return serializer;
    }

    private ClassDescriptor createCurrentClassDescriptor(Class cl) {
        StreamSerializer serializer = getSerializer(cl);
        SerializationMethod serializationMethod = null;
        if (serializer != null) {
            serializationMethod = SerializationMethod.CUSTOM;
        }
        if (serializationMethod == null) {
            serializationMethod = SerializationMethod.FIELD;
        }
        SerializationInfo serInfo = new SerializationInfo(cl, serializationMethod, serializer);
        ClassDescriptor desc = new ClassDescriptor(serInfo);
        return desc;
    }

    public ClassDescriptor lookup(Class cl) {
        SerializationInfoCache c = cache;
        ClassDescriptor desc = c.lkup(cl);
        if (desc == null) {
            desc = createCurrentClassDescriptor(cl);
            if (cl != String.class && !descStore.lookup(desc)) {
                SerializationInfo serInfo = desc.getSerializationInfo();
                SerializationMethod serializationMethod = serInfo.getSerializationMethod();
                if (serializationMethod == SerializationMethod.FIELD) {
                    serializationMethod = SerializationMethod.JAVA_OBJECT;
                    serInfo = new SerializationInfo(cl, serializationMethod, null);
                    desc = new ClassDescriptor(serInfo);
                }
            }
            synchronized (SerializationContext.class) {
                SerializationInfoCache c1 = cache;
                ClassDescriptor desc1 = null;
                if (c != c1) {
                    c = c1;
                    desc1 = c.lkup(cl);
                    if (desc1 != null) {
                        desc = desc1;
                    }
                }
                if (desc1 == null) {
                    c = c.addClassDescriptor(desc);
                    cache = c;
                }
            }
        }
        return desc;
    }

    public ClassDescriptor lookup(int providerId, int classIndex, int versionIndex) {
        SerializationInfoCache c = cache;
        ClassDescriptor desc = c.lkup(providerId, classIndex, versionIndex);
        ClassDescriptor currentDesc = null;
        if (desc == null) {
            desc = descStore.lookupByClassIndexAndVersion(providerId, classIndex, versionIndex);
            if (desc != null) {
                Class cl = null;
                try {
                    cl = Class.forName(desc.getClassName());
                    currentDesc = createCurrentClassDescriptor(cl);
                    if (currentDesc.copyIdsIfCurrent(desc)) {
                        desc = currentDesc;
                    } else {
                        currentDesc = lookup(cl);
                    }
                } catch (Exception e) {
                }
                if (desc != currentDesc && currentDesc != null) {
                    desc.mapToCurrentDescriptor(currentDesc);
                }
                synchronized (SerializationContext.class) {
                    SerializationInfoCache c1 = cache;
                    ClassDescriptor desc1 = null;
                    if (c != c1) {
                        c = c1;
                        desc1 = c.lkup(providerId, classIndex, versionIndex);
                        if (desc1 != null) {
                            desc = desc1;
                        }
                    }
                    if (desc1 == null) {
                        c = c.addClassDescriptor(desc);
                        cache = c;
                    }
                }
            }
        }
        return desc;
    }

    public SerializationInfoCache getCache() {
        return cache;
    }
}
