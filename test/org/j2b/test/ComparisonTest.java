package org.j2b.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.j2b.io.ObjectInputImpl;
import org.j2b.io.ObjectOutputImpl;
import org.j2b.serializer.SerializationContext;
import org.j2b.serializer.field.ClassDescriptorStore;
import org.j2b.serializer.field.VolatileClassDescriptorStore;

class ReusableByteArrayOutputStream extends ByteArrayOutputStream {

    /**
     * Creates a ByteArrayOutputStream by calling the super class's constructor.
     */
    public ReusableByteArrayOutputStream() {
        super();
    }

    /**
     * Creates a ByteArrayOutputStream by calling the super class's constructor.
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    public ReusableByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Resets the stream and in addition limits the internal buffer's size to maxBufSize.
     * @param maxBufSize Max Buf Size after reset
     */
    public void reset(int maxBufSize) {
        super.reset();
        if(maxBufSize > 0) {
            if(maxBufSize < 32) {
                maxBufSize = 32;
            }
            if(buf != null && buf.length > maxBufSize) {
                buf = new byte[maxBufSize];
            }
        }
    }

    /**
     * Gets an Input Stream from which one can read the contents of the data written to the stream.
     * @return Input Stream for reading data
     */
    public ByteArrayInputStream getInputStream() {
        ByteArrayInputStream bip = new ByteArrayInputStream(buf, 0, count);
        return bip;
    }

    /**
     * Current count of the bytes in the buffer. The data starts from index 0.
     * @return Count of bytes written
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count value. This is a low level functionality for manipulating the stream.
     * @param pcount
     */
    public void setCount(int pcount) {
        if(pcount < 0 || pcount > buf.length) {
            throw new IndexOutOfBoundsException(pcount + " is not in the range [0-" + buf.length + ']');
        }
        count = pcount;
    }

    /**
     * Gets the underlying buffer. This is again a low level functionality.
     * @return Internal buffer being used which might be replaced when capacity is expanded
     */
    public byte [] getBuf() {
        return buf;
    }

    /**
     * Sets a new buffer and resets the stream. This is also a low level functionality.
     * @param b New buffer to use
     */
    public void setBuf(byte [] b) {
        buf = b;
        reset();
    }
}

public class ComparisonTest {

    private static class B implements Serializable {
        private int i = 8;

        private double d = 147.38;

        private String s = "Hello B";

        public void writeCustom(DataOutput op) throws IOException {
            op.writeInt(i);
            op.writeDouble(d);
            op.writeUTF(s);
        }

        public void readCustom(DataInput ip) throws IOException {
            i = ip.readInt();
            d = ip.readDouble();
            s = ip.readUTF();
        }
    }

    private static class A implements Serializable {
        private int i = 7;

        private double d = 47.38;

        private String s = "Hello A";

        private B b = new B();

        public void writeCustom(DataOutput op) throws IOException {
            op.writeInt(i);
            op.writeDouble(d);
            op.writeUTF(s);
            b.writeCustom(op);
        }

        public void readCustom(DataInput ip) throws IOException {
            i = ip.readInt();
            d = ip.readDouble();
            s = ip.readUTF();
            b = new B();
            b.readCustom(ip);
        }
    }

    public static void main1(String [] args) throws IOException, ClassNotFoundException {
        A a = new A();
        ReusableByteArrayOutputStream bop = new ReusableByteArrayOutputStream();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ObjectOutputStream oop = new ObjectOutputStream(bop);
            oop.writeObject(a);
            oop.close();
            ObjectInputStream oip = new ObjectInputStream(bop.getInputStream());
            /*
            if (i < 1) {
                System.out.println(bop.getCount());
            }
            */
            A a1 = (A) oip.readObject();
            oip.close();
            bop.reset();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    public static void main2(String [] args) throws IOException, ClassNotFoundException {
        A a = new A();
        ReusableByteArrayOutputStream bop = new ReusableByteArrayOutputStream();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            DataOutputStream dop = new DataOutputStream(bop);
            a.writeCustom(dop);
            dop.close();
            /*
            if (i < 1) {
                System.out.println(bop.getCount());
            }
            */
            DataInputStream dip = new DataInputStream(bop.getInputStream());
            A a1 = new A();
            a1.readCustom(dip);
            dip.close();
            bop.reset();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    public static void main3(String [] args) throws Exception {
        A a = new A();
        Constructor aCons = A.class.getDeclaredConstructor();
        Field aiField = A.class.getDeclaredField("i");
        Field adField = A.class.getDeclaredField("d");
        Field asField = A.class.getDeclaredField("s");
        Field abField = A.class.getDeclaredField("b");
        Constructor bCons = B.class.getDeclaredConstructor();
        Field biField = B.class.getDeclaredField("i");
        Field bdField = B.class.getDeclaredField("d");
        Field bsField = B.class.getDeclaredField("s");
        aCons.setAccessible(true);
        aiField.setAccessible(true);
        adField.setAccessible(true);
        asField.setAccessible(true);
        abField.setAccessible(true);
        bCons.setAccessible(true);
        biField.setAccessible(true);
        bdField.setAccessible(true);
        bsField.setAccessible(true);
        ReusableByteArrayOutputStream bop = new ReusableByteArrayOutputStream();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            DataOutputStream dop = new DataOutputStream(bop);
            dop.writeInt(aiField.getInt(a));
            dop.writeDouble(adField.getDouble(a));
            dop.writeUTF((String) asField.get(a));
            B b = (B) abField.get(a);
            dop.writeInt(biField.getInt(b));
            dop.writeDouble(bdField.getDouble(b));
            dop.writeUTF((String) bsField.get(b));
            dop.close();
            /*
            if (i < 1) {
                System.out.println(bop.getCount());
            }
            */
            DataInputStream dip = new DataInputStream(bop.getInputStream());
            A a1 = (A) aCons.newInstance();
            aiField.setInt(a1, dip.readInt());
            adField.setDouble(a1, dip.readDouble());
            asField.set(a1, dip.readUTF());
            b = (B) bCons.newInstance();
            biField.setInt(b, dip.readInt());
            bdField.setDouble(b, dip.readDouble());
            bsField.set(b, dip.readUTF());
            abField.set(a1, b);
            dip.close();
            bop.reset();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    public static void main4(String [] args) throws IOException, ClassNotFoundException {
        ClassDescriptorStore store = new VolatileClassDescriptorStore();
        SerializationContext context = new SerializationContext(store);
        A a = new A();
        A a1 = null;
        long start = System.currentTimeMillis();
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        ObjectInputImpl oip = new ObjectInputImpl(context, new byte[0]);
        for (int i = 0; i < 10000; i++) {
            oop.writeObject(a);
            byte [] b = oop.removeBuf();
            int count = oop.getCount();
            oop.reset(b);
            /*
            if (i < 1) {
                System.out.println(count);
            }
            */
            oip.reset(b, 0, count);
            a1 = (A) oip.readObject();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    // Read Perf testing..

    public static void main5(String [] args) throws IOException, ClassNotFoundException {
        A a = new A();
        ReusableByteArrayOutputStream bop = new ReusableByteArrayOutputStream();
        ObjectOutputStream oop = new ObjectOutputStream(bop);
        oop.writeObject(a);
        oop.close();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ObjectInputStream oip = new ObjectInputStream(bop.getInputStream());
            A a1 = (A) oip.readObject();
            oip.close();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    public static void main6(String [] args) throws IOException, ClassNotFoundException {
        A a = new A();
        ReusableByteArrayOutputStream bop = new ReusableByteArrayOutputStream();
        DataOutputStream dop = new DataOutputStream(bop);
        a.writeCustom(dop);
        dop.close();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            DataInputStream dip = new DataInputStream(bop.getInputStream());
            A a1 = new A();
            a1.readCustom(dip);
            dip.close();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

    public static void main7(String [] args) throws IOException, ClassNotFoundException {
        ClassDescriptorStore store = new VolatileClassDescriptorStore();
        SerializationContext context = new SerializationContext(store);
        A a = new A();
        A a1 = null;
        ObjectOutputImpl oop = new ObjectOutputImpl(context);
        oop.reset();
        oop.writeAnObject(a, null);
        byte [] b = oop.removeBuf();
        int count = oop.getCount();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ObjectInputImpl oip = new ObjectInputImpl(context, b, 0, count);
            a1 = (A) oip.readObject();
        }
        System.out.println("Time = " + (System.currentTimeMillis() - start));
    }

}
