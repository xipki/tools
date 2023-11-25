/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package org.xipki.apppackage.cbor;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Provides a decoder capable of handling CBOR encoded data from a {@link InputStream}.
 */
public class CborDecoder {
    protected final PushbackInputStream m_is;

    /**
     * Creates a new {@link CborDecoder} instance.
     * 
     * @param is the actual input stream to read the CBOR-encoded data from, cannot be <code>null</code>.
     */
    public CborDecoder(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null!");
        }
        m_is = (is instanceof PushbackInputStream) ? (PushbackInputStream) is : new PushbackInputStream(is);
    }

    private static void fail(String msg, Object... args) throws IOException {
        throw new IOException(String.format(msg, args));
    }

    /**
     * Peeks in the input stream for the upcoming type.
     * 
     * @return the upcoming type in the stream, or <code>null</code> in case of an end-of-stream.
     * @throws IOException in case of I/O problems reading the CBOR-type from the underlying input stream.
     */
    public CborType peekType() throws IOException {
        int p = m_is.read();
        if (p < 0) {
            // EOF, nothing to peek at...
            return null;
        }
        m_is.unread(p);
        return CborType.valueOf(p);
    }

    /**
     * Prolog to reading an array value in CBOR format.
     * 
     * @return the number of elements in the array to read, or -1 in case of infinite-length arrays.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readArrayLength() throws IOException {
        return readMajorTypeWithSize(CborConstants.TYPE_ARRAY);
    }

    /**
     * Reads a boolean value in CBOR format.
     * 
     * @return the read boolean.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public boolean readBoolean() throws IOException {
        int b = readMajorType(CborConstants.TYPE_FLOAT_SIMPLE);
        if (b != CborConstants.FALSE && b != CborConstants.TRUE) {
            fail("Unexpected boolean value: %d!", b);
        }
        return b == CborConstants.TRUE;
    }

    /**
     * Reads a byte string value in CBOR format.
     *
     * @return the read byte string, never <code>null</code>. In case the encoded string has a length of 0, an empty string is returned.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public byte[] readByteString() throws IOException {
        long len = readMajorTypeWithSize(CborConstants.TYPE_BYTE_STRING);
        if (len < 0) {
            fail("Infinite-length byte strings not supported!");
        }
        if (len > Integer.MAX_VALUE) {
            fail("String length too long!");
        }
        return readFully(new byte[(int) len]);
    }

    /**
     * Reads a signed or unsigned integer value in CBOR format.
     * 
     * @return the read integer value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readInt() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return ui ^ readUInt(ib & 0x1f, false /* breakAllowed */);
    }

    /**
     * Reads a <code>null</code>-value in CBOR format.
     * 
     * @return always <code>null</code>.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public Object readNull() throws IOException {
        readMajorTypeExact(CborConstants.TYPE_FLOAT_SIMPLE, CborConstants.NULL);
        return null;
    }

    /**
     * Reads an UTF-8 encoded string value in CBOR format.
     * 
     * @return the read UTF-8 encoded string, never <code>null</code>. In case the encoded string has a length of 0, an empty string is returned.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public String readTextString() throws IOException {
        long len = readMajorTypeWithSize(CborConstants.TYPE_TEXT_STRING);
        if (len < 0) {
            fail("Infinite-length text strings not supported!");
        }
        if (len > Integer.MAX_VALUE) {
            fail("String length too long!");
        }
        return new String(readFully(new byte[(int) len]), "UTF-8");
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectation.
     * 
     * @param ib the expected major type, cannot be <code>null</code> (unchecked).
     * @return either -1 if the major type was an signed integer, or 0 otherwise.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected long expectIntegerType(int ib) throws IOException {
        int majorType = ((ib & 0xFF) >>> 5);
        if ((majorType != CborConstants.TYPE_UNSIGNED_INTEGER) && (majorType != CborConstants.TYPE_NEGATIVE_INTEGER)) {
            fail("Unexpected type: %s, expected type %s or %s!", CborType.getName(majorType), CborType.getName(CborConstants.TYPE_UNSIGNED_INTEGER),
                CborType.getName(CborConstants.TYPE_NEGATIVE_INTEGER));
        }
        return -majorType;
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectation.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked).
     * @return the read subtype, or payload, of the read major type.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected int readMajorType(int majorType) throws IOException {
        int ib = m_is.read();
        if (majorType != ((ib >>> 5) & 0x07)) {
            fail("Unexpected type: %s, expected: %s!", CborType.getName(ib), CborType.getName(majorType));
        }
        return ib & 0x1F;
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectations.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked);
     * @param subtype the expected subtype.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected void readMajorTypeExact(int majorType, int subtype) throws IOException {
        int st = readMajorType(majorType);
        if ((st ^ subtype) != 0) {
            fail("Unexpected subtype: %d, expected: %d!", st, subtype);
        }
    }

    /**
     * Reads the next major type from the underlying input stream, verifies whether it matches the given expectation, and decodes the payload into a size.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked).
     * @return the number of succeeding bytes, &gt;= 0, or -1 if an infinite-length type is read.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected long readMajorTypeWithSize(int majorType) throws IOException {
        return readUInt(readMajorType(majorType), true /* breakAllowed */);
    }

    /**
     * Reads an unsigned integer with a given length-indicator.
     * 
     * @param length the length indicator to use;
     * @param breakAllowed whether break is allowed.
     * @return the read unsigned integer, as long value.
     * @throws IOException in case of I/O problems reading the unsigned integer from the underlying input stream.
     */
    protected long readUInt(int length, boolean breakAllowed) throws IOException {
        long result = -1;
        if (length < CborConstants.ONE_BYTE) {
            result = length;
        } else if (length == CborConstants.ONE_BYTE) {
            result = readUInt8();
        } else if (length == CborConstants.TWO_BYTES) {
            result = readUInt16();
        } else if (length == CborConstants.FOUR_BYTES) {
            result = readUInt32();
        } else if (length == CborConstants.EIGHT_BYTES) {
            result = readUInt64();
        } else if (breakAllowed && length == CborConstants.BREAK) {
            return -1;
        }
        if (result < 0) {
            fail("Not well-formed CBOR integer found, invalid length: %d!", result);
        }
        return result;
    }

    /**
     * Reads an unsigned 16-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected int readUInt16() throws IOException {
        byte[] buf = readFully(new byte[2]);
        return (buf[0] & 0xFF) << 8 | (buf[1] & 0xFF);
    }

    /**
     * Reads an unsigned 32-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected long readUInt32() throws IOException {
        byte[] buf = readFully(new byte[4]);
        return ((buf[0] & 0xFF) << 24 | (buf[1] & 0xFF) << 16 | (buf[2] & 0xFF) << 8 | (buf[3] & 0xFF)) & 0xffffffffL;
    }

    /**
     * Reads an unsigned 64-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected long readUInt64() throws IOException {
        byte[] buf = readFully(new byte[8]);
        return (buf[0] & 0xFFL) << 56 | (buf[1] & 0xFFL) << 48 | (buf[2] & 0xFFL) << 40 | (buf[3] & 0xFFL) << 32 | //
            (buf[4] & 0xFFL) << 24 | (buf[5] & 0xFFL) << 16 | (buf[6] & 0xFFL) << 8 | (buf[7] & 0xFFL);
    }

    /**
     * Reads an unsigned 8-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected int readUInt8() throws IOException {
        return m_is.read() & 0xff;
    }

    private byte[] readFully(byte[] buf) throws IOException {
        int len = buf.length;
        int n = 0, off = 0;
        while (n < len) {
            int count = m_is.read(buf, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        return buf;
    }
}
