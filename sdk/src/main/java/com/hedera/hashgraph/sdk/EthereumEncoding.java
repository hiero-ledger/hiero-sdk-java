// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.math.BigInteger;

/**
 * Helpers for Ethereum RLP canonical integer encoding: big-endian, no leading zeros, zero is the empty byte string.
 */
final class EthereumEncoding {

    private EthereumEncoding() {}

    /**
     * Encode {@code v} as canonical minimal big-endian bytes. Zero is encoded as an empty byte array.
     *
     * @param v the unsigned value to encode
     * @return the canonical big-endian byte representation
     */
    static byte[] uint64ToEthBytes(long v) {
        if (v == 0) {
            return new byte[] {};
        }
        byte[] buf = new byte[8];
        for (int i = 7; i >= 0; i--) {
            buf[i] = (byte) (v & 0xFF);
            v >>>= 8;
        }
        int i = 0;
        while (i < buf.length && buf[i] == 0) {
            i++;
        }
        byte[] result = new byte[buf.length - i];
        System.arraycopy(buf, i, result, 0, result.length);
        return result;
    }

    /**
     * Decode canonical bytes to a long. An empty array decodes to 0. Input longer than 8 bytes is truncated to the
     * low 8 bytes.
     *
     * @param b the canonical big-endian bytes
     * @return the decoded value
     */
    static long ethBytesToLong(byte[] b) {
        if (b == null || b.length == 0) {
            return 0;
        }
        if (b.length > 8) {
            b = java.util.Arrays.copyOfRange(b, b.length - 8, b.length);
        }
        long result = 0;
        for (byte value : b) {
            result = (result << 8) | (value & 0xFF);
        }
        return result;
    }

    /**
     * Encode {@code v} as canonical minimal big-endian bytes. Null and zero are encoded as an empty byte array.
     * Negative values encode their absolute value.
     *
     * @param v the value to encode
     * @return the canonical big-endian byte representation
     */
    static byte[] bigIntToEthBytes(BigInteger v) {
        if (v == null || v.signum() == 0) {
            return new byte[] {};
        }
        if (v.signum() < 0) {
            v = v.abs();
        }
        byte[] bytes = v.toByteArray();
        // BigInteger.toByteArray() may prepend a leading zero byte to keep the value positive; strip it.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }

    /**
     * Decode canonical bytes to a {@link BigInteger}. An empty array decodes to 0.
     *
     * @param b the canonical big-endian bytes
     * @return the decoded value
     */
    static BigInteger ethBytesToBigInt(byte[] b) {
        if (b == null || b.length == 0) {
            return BigInteger.ZERO;
        }
        return new BigInteger(1, b);
    }
}
