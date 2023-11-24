/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 *
 * Licensed under Apache License v2.0.
 */
package org.xipki.apppackage.cbor;

/**
 * Constant values used by the CBOR format.
 */
public interface CborConstants {
    /** Major type 0: unsigned integers. */
    int TYPE_UNSIGNED_INTEGER = 0x00;
    /** Major type 1: negative integers. */
    int TYPE_NEGATIVE_INTEGER = 0x01;
    /** Major type 2: byte string. */
    int TYPE_BYTE_STRING = 0x02;
    /** Major type 3: text/UTF8 string. */
    int TYPE_TEXT_STRING = 0x03;
    /** Major type 4: array of items. */
    int TYPE_ARRAY = 0x04;
    /** Major type 5: map of pairs. */
    int TYPE_MAP = 0x05;
    /** Major type 6: semantic tags. */
    int TYPE_TAG = 0x06;
    /** Major type 7: floating point, simple data types. */
    int TYPE_FLOAT_SIMPLE = 0x07;
    
    /** Denotes a one-byte value (uint8). */
    int ONE_BYTE = 0x18;
    /** Denotes a two-byte value (uint16). */
    int TWO_BYTES = 0x19;
    /** Denotes a four-byte value (uint32). */
    int FOUR_BYTES = 0x1a;
    /** Denotes a eight-byte value (uint64). */
    int EIGHT_BYTES = 0x1b;

    /** The CBOR-encoded boolean <code>false</code> value (encoded as "simple value"). */
    int FALSE = 0x14;
    /** The CBOR-encoded boolean <code>true</code> value (encoded as "simple value"). */
    int TRUE = 0x15;
    /** The CBOR-encoded <code>null</code> value (encoded as "simple value"). */
    int NULL = 0x16;
    /** The CBOR-encoded "undefined" value (encoded as "simple value"). */
    int BREAK = 0x1f;

}
