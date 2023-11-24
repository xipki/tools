/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 *
 * Licensed under Apache License v2.0.
 */
package org.xipki.apppackage.cbor;

/**
 * Represents the various major types in CBOR, along with their .
 * <p>
 * The major type is encoded in the upper three bits of each initial byte. The lower 5 bytes represent any additional information.
 * </p>
 */
public class CborType {
    private final int m_major;
    private final int m_additional;

    private CborType(int major, int additional) {
        m_major = major;
        m_additional = additional;
    }

    /**
     * Returns a descriptive string for the given major type.
     * 
     * @param mt the major type to return as string, values from [0..7] are supported.
     * @return the name of the given major type, as String, never <code>null</code>.
     * @throws IllegalArgumentException in case the given major type is not supported.
     */
    public static String getName(int mt) {
        switch (mt) {
            case CborConstants.TYPE_ARRAY:
                return "array";
            case CborConstants.TYPE_BYTE_STRING:
                return "byte string";
            case CborConstants.TYPE_FLOAT_SIMPLE:
                return "float/simple value";
            case CborConstants.TYPE_MAP:
                return "map";
            case CborConstants.TYPE_NEGATIVE_INTEGER:
                return "negative integer";
            case CborConstants.TYPE_TAG:
                return "tag";
            case CborConstants.TYPE_TEXT_STRING:
                return "text string";
            case CborConstants.TYPE_UNSIGNED_INTEGER:
                return "unsigned integer";
            default:
                throw new IllegalArgumentException("Invalid major type: " + mt);
        }
    }

    /**
     * Decodes a given byte value to a {@link CborType} value.
     * 
     * @param i the input byte (8-bit) to decode into a {@link CborType} instance.
     * @return a {@link CborType} instance, never <code>null</code>.
     */
    public static CborType valueOf(int i) {
        return new CborType((i & 0xff) >>> 5, i & 0x1f);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CborType other = (CborType) obj;
        return (m_major == other.m_major) && (m_additional == other.m_additional);
    }

    /**
     * @return the additional information of this type, as integer value from [0..31].
     */
    public int getAdditionalInfo() {
        return m_additional;
    }

    /**
     * @return the major type, as integer value from [0..7].
     */
    public int getMajorType() {
        return m_major;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_additional;
        result = prime * result + m_major;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName(m_major)).append('(').append(m_additional).append(')');
        return sb.toString();
    }
}
