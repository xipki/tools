/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 *
 * Licensed under Apache License v2.0.
 */
package org.xipki.apppackage.cbor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides an encoder capable of encoding data into CBOR format to a given {@link OutputStream}.
 */
public class CborEncoder {
    private static final int NEG_INT_MASK = CborConstants.TYPE_NEGATIVE_INTEGER << 5;

    private final OutputStream m_os;

    /**
     * Creates a new {@link CborEncoder} instance.
     * 
     * @param os the actual output stream to write the CBOR-encoded data to, cannot be <code>null</code>.
     */
    public CborEncoder(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException("OutputStream cannot be null!");
        }
        m_os = os;
    }

    /**
     * Writes the start of a definite-length array.
     * <p>
     * After calling this method, one is expected to write the given number of array elements, which can be of any type. No length checks are performed.
     * </p>
     * 
     * @param length the number of array elements to write, should &gt;= 0.
     * @throws IllegalArgumentException in case the given length was negative;
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeArrayStart(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Invalid array-length!");
        }
        writeType(CborConstants.TYPE_ARRAY, length);
    }

    /**
     * Writes a boolean value in canonical CBOR format.
     * 
     * @param value the boolean to write.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeBoolean(boolean value) throws IOException {
        writeSimpleType(CborConstants.TYPE_FLOAT_SIMPLE, value ? CborConstants.TRUE : CborConstants.FALSE);
    }

    /**
     * Writes a byte string in canonical CBOR-format.
     * 
     * @param bytes the byte string to write, can be <code>null</code> in which case a byte-string of length 0 is written.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeByteString(byte[] bytes) throws IOException {
        writeString(CborConstants.TYPE_BYTE_STRING, bytes);
    }

    /**
     * Writes a signed or unsigned integer value in canonical CBOR format, that is, tries to encode it in a little bytes as possible.
     * 
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeInt(long value) throws IOException {
        // extends the sign over all bits...
        long sign = value >> 63;
        // in case value is negative, this bit should be set...
        int mt = (int) (sign & NEG_INT_MASK);
        // complement negative value...
        value = (sign ^ value);

        writeUInt(mt, value);
    }

    /**
     * Writes a <code>null</code> value in canonical CBOR format.
     * 
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeNull() throws IOException {
        writeSimpleType(CborConstants.TYPE_FLOAT_SIMPLE, CborConstants.NULL);
    }

    /**
     * Writes an UTF-8 string in canonical CBOR-format.
     * <p>
     * Note that this method is <em>platform</em> specific, as the given string value will be encoded in a byte array
     * using the <em>platform</em> encoding! This means that the encoding must be standardized and known.
     * </p>
     * 
     * @param value the UTF-8 string to write, can be <code>null</code> in which case an UTF-8 string of length 0 is written.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    public void writeTextString(String value) throws IOException {
        writeString(CborConstants.TYPE_TEXT_STRING, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes and writes the major type and value as a simple type.
     * 
     * @param majorType the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from [0..31] are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeSimpleType(int majorType, int value) throws IOException {
        m_os.write((majorType << 5) | (value & 0x1f));
    }

    /**
     * Writes a byte string in canonical CBOR-format.
     * 
     * @param majorType the major type of the string, should be either 0x40 or 0x60;
     * @param bytes the byte string to write, can be <code>null</code> in which case a byte-string of length 0 is written.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeString(int majorType, byte[] bytes) throws IOException {
        int len = (bytes == null) ? 0 : bytes.length;
        writeType(majorType, len);
        for (int i = 0; i < len; i++) {
            m_os.write(bytes[i]);
        }
    }

    /**
     * Encodes and writes the major type indicator with a given payload (length).
     * 
     * @param majorType the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeType(int majorType, long value) throws IOException {
        writeUInt((majorType << 5), value);
    }

    /**
     * Encodes and writes an unsigned integer value, that is, tries to encode it in a little bytes as possible.
     * 
     * @param mt the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeUInt(int mt, long value) throws IOException {
        if (value < 0x18L) {
            m_os.write((int) (mt | value));
        } else if (value < 0x100L) {
            writeUInt8(mt, (int) value);
        } else if (value < 0x10000L) {
            writeUInt16(mt, (int) value);
        } else if (value < 0x100000000L) {
            writeUInt32(mt, (int) value);
        } else {
            writeUInt64(mt, value);
        }
    }

    /**
     * Encodes and writes an unsigned 16-bit integer value
     * 
     * @param mt the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeUInt16(int mt, int value) throws IOException {
        m_os.write(mt | CborConstants.TWO_BYTES);
        m_os.write(value >> 8);
        m_os.write(value & 0xFF);
    }

    /**
     * Encodes and writes an unsigned 32-bit integer value
     * 
     * @param mt the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeUInt32(int mt, int value) throws IOException {
        m_os.write(mt | CborConstants.FOUR_BYTES);
        m_os.write(value >> 24);
        m_os.write(value >> 16);
        m_os.write(value >> 8);
        m_os.write(value & 0xFF);
    }

    /**
     * Encodes and writes an unsigned 64-bit integer value
     * 
     * @param mt the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeUInt64(int mt, long value) throws IOException {
        m_os.write(mt | CborConstants.EIGHT_BYTES);
        m_os.write((int) (value >> 56));
        m_os.write((int) (value >> 48));
        m_os.write((int) (value >> 40));
        m_os.write((int) (value >> 32));
        m_os.write((int) (value >> 24));
        m_os.write((int) (value >> 16));
        m_os.write((int) (value >> 8));
        m_os.write((int) (value & 0xFF));
    }

    /**
     * Encodes and writes an unsigned 8-bit integer value
     * 
     * @param mt the major type of the value to write, denotes what semantics the written value has;
     * @param value the value to write, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected void writeUInt8(int mt, int value) throws IOException {
        m_os.write(mt | CborConstants.ONE_BYTE);
        m_os.write(value & 0xFF);
    }
}
