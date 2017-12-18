package org.j2b.serializer.field;

public interface ClassDescriptorStore {
    int version();

    boolean lookup(ClassDescriptor desc);

    boolean supports(String className);

    ClassDescriptor lookupByClassIndexAndVersion(int providerId, int classIndex, int versionIndex);
}
