// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ID for a cryptocurrency account on Hedera.
 */
public final class EvmAddress extends Key {
    private final byte[] bytes;

    /**
     * Constructor
     *
     * @param bytes the byte array representation of the address
     */
    public EvmAddress(byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Convert a string to an ethereum address.
     *
     * @param text                      the string
     * @return                          the ethereum address
     */
    public static EvmAddress fromString(String text) {
        String address = text.startsWith("0x") ? text.substring(2) : text;
        if (address.length() == 40) {
            return new EvmAddress(Hex.decode(address));
        }
        throw new IllegalArgumentException("Invalid EvmAddress: " + text);
    }

    @Nullable
    static EvmAddress fromAliasBytes(ByteString aliasBytes) {
        if (!aliasBytes.isEmpty() && aliasBytes.size() == 20) {
            return new EvmAddress(aliasBytes.toByteArray());
        }
        return null;
    }

    /**
     * Convert a byte array to an ethereum address.
     *
     * @param bytes                     the byte array
     * @return                          the ethereum address
     */
    public static EvmAddress fromBytes(byte[] bytes) {
        return new EvmAddress(bytes);
    }

    @Override
    com.hedera.hashgraph.sdk.proto.Key toProtobufKey() {
        throw new UnsupportedOperationException("toProtobufKey() not implemented for EvmAddress");
    }

    public byte[] toBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public String toString() {
        return Hex.toHexString(bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof EvmAddress)) {
            return false;
        }

        EvmAddress other = (EvmAddress) o;
        return Arrays.equals(bytes, other.bytes);
    }
}
