# j2b
J2B is a fast serializer for java supporting compatibility between versions of serialized classes

# Simple Example

```
ClassDescriptorStore store = new VolatileClassDescriptorStore();
SerializationContext context = new SerializationContext(store);
ObjectOutputImpl oop = new ObjectOutputImpl(context);
oop.writeObject("Hello World");
int count = oop.getCount();
byte [] b = oop.removeBuf();
ObjectInputImpl oip = new ObjectInputImpl(context, b, 0, count);
String s = (String) oip.readObject();
System.out.println(s);
```

ObjectOutputImpl and ObjectInputImpl are the main classes. They have features of ByteArrayOutputStream/ByteArrayInputStream and BufferedOutputStream/BufferedInputStream
built in, so you can either use them to directly work on byte arrays or buffer to an underlying output or input stream.
They can be re-used after calling the reset method. However they are not thread-safe.

SerializationContext required by ObjectOutputImpl and ObjectInputImpl is thread-safe and there should be only one instance created.

Presently there is only a simple VolatileClassDescriptorStore implementation which saves descriptors for all Serializable classes without
readObject, writeObject, writeReplace or readResolve methods in JVM memory. The Class can be serialized (In java format) and saved.
The plan is to have a descriptorgenerator to go through a jar file and update an existing descriptor store in a jar file and have a
read-only implementation of the ClassDescriptorStore.

# Custom Serialization

J2B Field serialization handles only classes with a no-arg constructor and which do not have customized java serialization indicated by
methods like readObject/writeObject. The default behavior of J2B is to serialize classes it cannot handle via Field Serialization via
Java serialization after printing a warning.

To avoid this a Custom Serializer should be written for such classes. There are already CustomSerializers for Collection, Map, Wrapper
Classes like Integer and Date.

# Compatibility
Java Serialization is relatively slow especially if we want to do an isolated serialization of an object for storing it in a cache or db.
However the main advantage it provides is backward compatibility for de-serialization of older objects when you edit a class and add or
remove a field.

J2B provides this feature of backward (and forward) compatibility by maintaining a versioned ClassDescriptor store of various classes to be
serialized. New Class descriptors for New Classes or Changed Classes get added to the Class Descriptor store. The required Class Descriptors
are fetched from the store and cached in the jvm memory. The serialized data only contains ids of the descriptors which store the field
information. This way the serialized data is compact and the serialization is also fast.

On a simple 2 Class Object graph serialization is around 3x and de-serialization around 6x faster than Java serialization.

Compatibility is done by matching the fields by name and ignoring non-matching names in older and newer version.
If there is a type change a conversion is tried at runtime failing which a Runtime exception is thrown.

# Releases

J2B is not currently released. You can fork the source and try it in an IDE like eclipse. There are not dependencies. There are test
programs in the test directory you can use for reference (There are not test cases yet).

# Contact

You can send any feedback, suggestions or comments to msuresh@cheerful.com
