package org.j2b.io;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.j2b.J2BConstants;

public class DataInputImpl extends InputStream implements DataInput {

    private InputStream ip;

    private byte [] buf;

    private int bufEnd;

    private int pos;

    private char[] lineBuffer;

    public void reset(InputStream pip, int bufSize) {
        if (bufSize < 1) {
            throw new IllegalArgumentException("Illegal Buffer Size " + bufSize);
        }
        ip = pip;
        int curBufSize = 0;
        if (buf != null) {
            curBufSize = buf.length;
        }
        if (curBufSize != bufSize) {
            buf = new byte[bufSize];
        }
        pos = 0;
        bufEnd = 0;
    }

    public void reset(InputStream pip) {
        reset(pip, J2BConstants.DEFAULT_BUFFER_SIZE);
    }

    public void reset(byte [] b, int poff, int plen) {
        if ((poff < 0) || (poff > b.length) || (plen < 0) ||
                ((poff + plen) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        ip = null;
        buf = b;
        pos = poff;
        bufEnd = poff + plen;
    }

    public void reset(byte [] b) {
        reset(b, 0, b.length);
    }

    public DataInputImpl(InputStream pip, int bufSize) {
        reset(pip, bufSize);
    }

    public DataInputImpl(InputStream pip) {
        this(pip, J2BConstants.DEFAULT_BUFFER_SIZE);
    }

    public DataInputImpl(byte [] b, int poff, int plen) {
        reset(b, poff, plen);
    }

    public DataInputImpl(byte [] b) {
        this(b, 0, b.length);
    }

    private int fill(boolean raiseException) throws IOException {
        int bytesRead = 0;
        if (ip != null) {
            byte [] b = buf;
            int p = pos;
            int l = bufEnd - p;
            if (p > 0) {
                if (l > 0) {
                    System.arraycopy(b, p, b, 0, l);
                }
                pos = 0;
            }
            bytesRead = ip.read(b, l, b.length - l);
            if (bytesRead > 0) {
                bufEnd = l + bytesRead;
            } else {
                bytesRead = 0;
            }
        }
        if (bytesRead == 0 && raiseException) {
            throw new EOFException();
        }
        return bytesRead;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int ofs, int l) throws IOException {
        if (l > 0) {
            int remain = l;
            do {
                int numBytes = bufEnd - pos;
                if (numBytes > remain) {
                    numBytes = remain;
                }
                System.arraycopy(buf, pos, b, ofs, numBytes);
                ofs += numBytes;
                remain -= numBytes;
                if (remain > 0) {
                    fill(true);
                }
            } while (remain > 0);
        } else if (l < 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        int total = bufEnd - pos;
        if (total > n) {
            total = n;
        }
        pos += total;
        if (total < n && ip != null) {
            int cur = 0;
            while ((total<n) && ((cur = (int) ip.skip(n-total)) > 0)) {
                total += cur;
            }
        }

        return total;
    }

    public int read(boolean raiseException) throws IOException {
        int result = -1;
        if (pos < bufEnd || fill(raiseException) > 0) {
            result = buf[pos] & 0xFF;
            pos++;
        }
        return result;
    }

    public int peek(boolean raiseException) throws IOException {
        int result = -1;
        if (pos < bufEnd || fill(raiseException) > 0) {
            result = buf[pos] & 0xFF;
        }
        return result;
    }

    @Override
    public int read() throws IOException {
        return read(false);
    }

    private void unread(int b) throws IOException {
        if (pos > 0) {
            buf[--pos] = (byte) b;
        } else {
            throw new IOException("No byte read");
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        int ch = read(true);
        return (ch != 0);
    }

    @Override
    public byte readByte() throws IOException {
        byte b = (byte) read(true);
        return b;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    public int readVariableLengthInteger() throws IOException {
        int byteCounter = 0;
        int result = 0;
        int nextByte, nextIndicator;
        int i = pos;
        int m = bufEnd;
        byte [] b = buf;
        do {
            if (i >= m) {
                pos = i;
                fill(true);
                i = 0;
                m = bufEnd;
            }
            nextByte = b[i];
            i++;
            byteCounter++;
            nextIndicator = nextByte & J2BConstants.EIGTH_BIT;
            result = (result << 7) | (nextByte & J2BConstants.SEVEN_BITS);
        } while (nextIndicator != 0 && byteCounter < 5);
        pos = i;
        if (nextIndicator != 0) {
            throw new IOException("Invalid integer with more the 4 bytes encountered");
        }
        return result;
    }

    public int readVariableLengthInteger(int bitsInLeadingByte) throws IOException {
        int byteCounter = 0;
        int result = 0;
        int nextByte, nextIndicator;
        int i = pos;
        int m = bufEnd;
        byte [] b = buf;
        int nextFlag = 1 << bitsInLeadingByte;
        int mask = nextFlag - 1;
        do {
            if (i >= m) {
                pos = i;
                fill(true);
                i = 0;
                m = bufEnd;
            }
            nextByte = b[i];
            i++;
            byteCounter++;
            nextIndicator = nextByte & nextFlag;
            result = (result << 7) | (nextByte & mask);
            nextFlag = J2BConstants.EIGTH_BIT;
            mask = J2BConstants.SEVEN_BITS;
        } while (nextIndicator != 0 && byteCounter < 5);
        pos = i;
        if (nextIndicator != 0) {
            throw new IOException("Invalid integer with more the 4 bytes encountered");
        }
        return result;
    }

    public long readVariableLengthLong() throws IOException {
        int byteCounter = 0;
        long result = 0;
        long nextByte, nextIndicator;
        int i = pos;
        int m = bufEnd;
        byte [] b = buf;
        do {
            if (i >= m) {
                pos = i;
                fill(true);
                i = 0;
                m = bufEnd;
            }
            nextByte = b[i];
            i++;
            byteCounter++;
            nextIndicator = nextByte & J2BConstants.EIGTH_BIT_L;
            result = (result << 7) | (nextByte & J2BConstants.SEVEN_BITS_L);
        } while (nextIndicator != 0 && byteCounter < 10);
        pos = i;
        if (nextIndicator != 0) {
            throw new IOException("Invalid long with more the 8 bytes encountered");
        }
        return result;
    }

    @Override
    public short readShort() throws IOException {
        short s = (short) readInt();
        return s;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        int s = readInt() & 0xFFFF;
        return s;
    }

    @Override
    public char readChar() throws IOException {
        char ch = (char) readVariableLengthInteger();
        return ch;
    }

    @Override
    public int readInt() throws IOException {
        int i = readVariableLengthInteger();
        int decoded = (i >>> 1) ^ -(i & 1);
        return decoded;
    }

    @Override
    public long readLong() throws IOException {
        long i = readVariableLengthLong();
        long decoded = (i >>> 1) ^ -(i & 1);
        return decoded;
    }

    @Override
    public float readFloat() throws IOException {
        int i = readVariableLengthInteger();
        return Float.intBitsToFloat(i);
    }

    @Override
    public double readDouble() throws IOException {
        long l = readVariableLengthLong();
        return Double.longBitsToDouble(l);
    }

    @Override
    public String readLine() throws IOException {
        char buf[] = lineBuffer;

        if (buf == null) {
            buf = lineBuffer = new char[128];
        }

        int room = buf.length;
        int offset = 0;
        int c;

loop:   while (true) {
            switch (c = read()) {
              case -1:
              case '\n':
                break loop;

              case '\r':
                int c2 = read();
                if ((c2 != '\n') && (c2 != -1)) {
                    unread(c2);
                }
                break loop;

              default:
                if (--room < 0) {
                    buf = new char[offset + 128];
                    room = buf.length - offset - 1;
                    System.arraycopy(lineBuffer, 0, buf, 0, offset);
                    lineBuffer = buf;
                }
                buf[offset++] = (char) c;
                break;
            }
        }
        if ((c == -1) && (offset == 0)) {
            return null;
        }
        return String.copyValueOf(buf, 0, offset);
    }

    protected String readUTF(int l) throws IOException {
        byte [] b = buf;
        int ofs = pos;
        if (l < b.length) {
            while (bufEnd - ofs < l) {
                fill(true);
                ofs = 0;
            }
            pos = ofs + l;
        } else {
            b = new byte[l];
            ofs = 0;
            readFully(b, 0, l);
        }
        String s = new String(b, ofs, l, J2BConstants.UTF_8_CHARSET);
        return s;
    }

    @Override
    public String readUTF() throws IOException {
        int l = readVariableLengthInteger();
        return readUTF(l);
    }

    @Override
    public void close() throws IOException {
        pos = bufEnd;
        if (ip != null) {
            ip.close();
        }
    }
}
