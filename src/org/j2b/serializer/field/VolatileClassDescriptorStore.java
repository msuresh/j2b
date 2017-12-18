package org.j2b.serializer.field;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Externalizable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.j2b.reflect.ClassUtil;

public class VolatileClassDescriptorStore implements ClassDescriptorStore, Serializable {

    private static final long serialVersionUID = -5811341386022427786L;

    private int count;

    private Map<String, List<ClassDescriptor>> nameMap = new HashMap<String, List<ClassDescriptor>>();

    private List<List<ClassDescriptor>> descriptorsByIndex = new ArrayList<List<ClassDescriptor>>();

    @Override
    public int version() {
        return count;
    }

    @Override
    public synchronized boolean lookup(ClassDescriptor desc) {
        if (desc.getClassName().equals("java.lang.String")) {
            return false;
        }
        Class cl = desc.getSerializationInfo().getCl();
        boolean fieldSerializable = true;
        boolean found = false;
        if (!Serializable.class.isAssignableFrom(cl) || Externalizable.class.isAssignableFrom(cl)) {
            fieldSerializable = false;
        }
        Class parentCl = cl;
        Method m;
        if (fieldSerializable) {
            if (ClassUtil.findMethod(cl, "readObject") != null
                || ClassUtil.findMethod(cl, "writeObject") != null
                || ClassUtil.findMethod(cl, "writeReplace") != null
                || ClassUtil.findMethod(cl, "readResolve") != null) {
                fieldSerializable = false;
            }
        }
        if (fieldSerializable) {
            int classIndex = -1;
            int versionIndex = 0;
            List<ClassDescriptor> vals = nameMap.get(desc.getClassName());
            if (vals != null) {
                for (ClassDescriptor val : vals) {
                    if (val.equals(desc)) {
                        classIndex = val.classIndex;
                        versionIndex = val.versionIndex;
                        break;
                    }
                }
            }
            if (classIndex < 0) {
                if (vals != null) {
                    classIndex = vals.get(0).classIndex;
                    versionIndex = vals.size();
                    vals.add(desc);
                } else {
                    vals = new ArrayList<ClassDescriptor>(1);
                    classIndex = descriptorsByIndex.size();
                    vals.add(desc);
                    descriptorsByIndex.add(vals);
                    nameMap.put(desc.getClassName(), vals);
                }
                count++;
            }
            if (classIndex >= 0) {
                found = true;
                desc.providerId = 0;
                desc.classIndex = classIndex;
                desc.versionIndex = versionIndex;
            }
        }
        return found;
    }

    @Override
    public boolean supports(String className) {
        return true;
    }

    @Override
    public synchronized ClassDescriptor lookupByClassIndexAndVersion(int providerId, int classIndex,
            int versionIndex) {
        ClassDescriptor desc = null;
        List<ClassDescriptor> vals = descriptorsByIndex.get(classIndex);
        if (vals != null && vals.size() > versionIndex) {
            desc = vals.get(versionIndex);
        }
        return desc;
    }

    public void save(String path) throws IOException {
        ObjectOutputStream oop = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
        oop.writeObject(this);
        oop.close();
    }

    public static VolatileClassDescriptorStore load(String path) throws IOException, ClassNotFoundException {
        ObjectInputStream oip = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
        VolatileClassDescriptorStore store = (VolatileClassDescriptorStore) oip.readObject();
        oip.close();
        return store;
    }
}
