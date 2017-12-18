package org.j2b.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.j2b.J2BConstants;

public class DataOutputImpl extends OutputStream implements DataOutput {

    private OutputStream op;

    private boolean ignoreFlush;

    private byte [] buf;

    private int count;

    public DataOutputImpl(OutputStream pop, int bufSize) {
        buf = new byte[bufSize];
        op = pop;
    }

    public DataOutputImpl(OutputStream pop) {
        this(pop, J2BConstants.DEFAULT_BUFFER_SIZE);
    }

    public DataOutputImpl(int bufSize) {
        this(null, bufSize);
    }

    public DataOutputImpl() {
        this(null, J2BConstants.DEFAULT_BUFFER_SIZE);
    }

    public void reset(OutputStream newOp, byte [] b) {
        op = newOp;
        buf = b;
        count = 0;
    }

    public void reset(OutputStream newOp) {
        reset(newOp, buf);
    }

    public void reset(byte [] b) {
        reset(null, b);
    }

    public void reset() {
        reset(null, buf);
    }

    public boolean isIgnoreFlush() {
        return ignoreFlush;
    }

    public void setIgnoreFlush(boolean pignoreFlush) {
        ignoreFlush = pignoreFlush;
    }

    private void expand(int numBytes) throws IOException {
        if (op != null) {
            op.write(buf, 0, count);
            count = 0;
        } else {
            int requiredLen = count + numBytes;
            int bufLen = buf.length;
            if (bufLen < requiredLen) {
                do {
                    bufLen = bufLen + (bufLen >> 1);
                    if (bufLen < 0) {
                        bufLen = requiredLen;
                    }
                } while (bufLen < requiredLen);
                buf = Arrays.copyOf(buf, bufLen);
            }
        }
    }

    public void writeVariableLengthInteger(int n) throws IOException {
        int numBytes = 0;
        int i = n;
        do {
            numBytes++;
            i >>>= 7;
        } while (i > 0);
        int len = count;
        if (len + numBytes > buf.length) {
            expand(numBytes);
            len = count;
        }
        if (numBytes > 1) {
            int shiftCount = 7 * (numBytes - 1);
            do {
                i = J2BConstants.EIGTH_BIT | ((n >>> shiftCount) & J2BConstants.SEVEN_BITS);
                shiftCount -= 7;
                buf[len] = (byte) i;
                len++;
                numBytes--;
            } while (numBytes > 1);
        }
        buf[len] = (byte) (n & J2BConstants.SEVEN_BITS);
        len++;
        count = len;
    }

    public void writeVariableLengthInteger(int n, int bitsInLeadingByte, int leadingBits) throws IOException {
        int numBytes = 0;
        int i = n;
        int prevI;
        do {
            numBytes++;
            prevI = i;
            i >>>= 7;
        } while (i > 0);
        if (prevI >>> bitsInLeadingByte > 0) {
            numBytes++;
        }
        int len = count;
        if (len + numBytes > buf.length) {
            expand(numBytes);
            len = count;
        }
        if (numBytes > 1) {
            int shiftCount = 7 * (numBytes - 1);
            int nextFlag = leadingBits | (1 << bitsInLeadingByte);
            leadingBits = 0;
            do {
                i = nextFlag | ((n >>> shiftCount) & J2BConstants.SEVEN_BITS);
                nextFlag = J2BConstants.EIGTH_BIT;
                shiftCount -= 7;
                buf[len] = (byte) i;
                len++;
                numBytes--;
            } while (numBytes > 1);
        }
        buf[len] = (byte) (leadingBits | (n & J2BConstants.SEVEN_BITS));
        len++;
        count = len;
    }

    public void writeVariableLengthLong(long n) throws IOException {
        int numBytes = 0;
        long i = n;
        do {
            numBytes++;
            i >>>= 7;
        } while (i > 0);
        int len = count;
        if (len + numBytes > buf.length) {
            expand(numBytes);
            len = count;
        }
        if (numBytes > 1) {
            int shiftCount = 7 * (numBytes - 1);
            do {
                i = J2BConstants.EIGTH_BIT_L | ((n >>> shiftCount) & J2BConstants.SEVEN_BITS_L);
                shiftCount -= 7;
                buf[len] = (byte) i;
                len++;
                numBytes--;
            } while (numBytes > 1);
        }
        buf[len] = (byte) (n & J2BConstants.SEVEN_BITS_L);
        len++;
        count = len;
    }

    @Override
    public void write(int b) throws IOException {
        int len = count;
        if (len >= buf.length) {
            expand(1);
            len = count;
        }
        buf[len] = (byte) b;
        count++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int bLen) throws IOException {
        if ((off < 0) || (off > b.length) || (bLen < 0) ||
                ((off + bLen) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        int len = count;
        if (len + bLen > buf.length) {
            expand(bLen);
            len = count;
        }
        if (bLen < buf.length) {
            System.arraycopy(b, off, buf, len, bLen);
            count += bLen;
        } else {
            op.write(b, off, bLen);
        }
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        writeInt(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        writeVariableLengthInteger(v & 0xFFFF);
    }

    @Override
    public void writeInt(int v) throws IOException {
        int encoded = (v >> 31) ^ (v << 1);
        writeVariableLengthInteger(encoded);
    }

    @Override
    public void writeLong(long v) throws IOException {
        long encoded = (v >> 63) ^ (v << 1);
        writeVariableLengthLong(encoded);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeVariableLengthInteger(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeVariableLengthLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        int len = count;
        int sLen = s.length();
        int bufLen = buf.length;
        if (len + sLen > bufLen) {
            expand(sLen);
            len = count;
        }
        for (int i = 0 ; i < sLen ; i++) {
            if (len >= bufLen) {
                // This happens when this is a buffered stream and sLen > bufLen
                expand(sLen);
                len = count;
            }
            buf[len] = (byte)s.charAt(i);
            len++;
        }
        count = len;
    }

    @Override
    public void writeChars(String s) throws IOException {
        int len = count;
        int sLen = s.length();
        int bufLen = buf.length;
        if (len + (sLen << 1) > bufLen) {
            expand(sLen << 1);
            len = count;
        }
        bufLen--;
        for (int i = 0 ; i < sLen ; i++) {
            if (len >= bufLen) {
                // This happens when this is a buffered stream and sLen > bufLen
                expand(sLen);
                len = count;
            }
            int v = s.charAt(i);
            buf[len] = (byte) ((v >>> 8) & 0xFF);
            len++;
            buf[len] = (byte) (v & 0xFF);
            len++;
        }
        count = len;
    }

    @Override
    public void writeUTF(String s) throws IOException {
        byte [] b = s.getBytes(J2BConstants.UTF_8_CHARSET);
        writeVariableLengthInteger(b.length);
        write(b);
    }

    @Override
    public void flush() throws IOException {
        if (!ignoreFlush) {
            expand(0);
        }
    }

    @Override
    public void close() throws IOException {
        if (op != null) {
            flush();
            op.close();
        }
    }

    public byte[] removeBuf() {
        byte [] b = buf;
        buf = null;
        return b;
    }

    public int getCount() {
        return count;
    }
}
