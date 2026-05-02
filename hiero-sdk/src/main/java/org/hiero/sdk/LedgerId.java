// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

/**
 * Internal utility class for ledger id manipulation.
 */
public class LedgerId {
    private final byte[] idBytes;

    /**
     * The mainnet ledger id
     */
    public static final LedgerId MAINNET = new LedgerId(new byte[] {0});

    /**
     * The testnet ledger id
     */
    public static final LedgerId TESTNET = new LedgerId(new byte[] {1});

    /**
     * The previewnet ledger id
     */
    public static final LedgerId PREVIEWNET = new LedgerId(new byte[] {2});

    /**
     * Constructor.
     *
     * @param idBytes                   the id (0=mainnet, 1=testnet, 2=previewnet, ...)
     */
    LedgerId(byte[] idBytes) {
        this.idBytes = idBytes;
    }

    /**
     * Assign the ledger id via a string name or Hex encoded String.
     *
     * @param string                    the string containing the ledger id
     * @return                          the ledger id
     */
    public static LedgerId fromString(String string) {
        switch (string) {
            case "mainnet":
                return MAINNET;
            case "testnet":
                return TESTNET;
            case "previewnet":
                return PREVIEWNET;
            default:
                return new LedgerId(Hex.decode(string));
        }
    }

    /**
     * Create a ledger id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the ledger id
     */
    public static LedgerId fromBytes(byte[] bytes) {
        return new LedgerId(bytes);
    }

    /**
     * Create a ledger id from a string.
     *
     * @param byteString                the string
     * @return                          the ledger id
     */
    static LedgerId fromByteString(ByteString byteString) {
        return fromBytes(byteString.toByteArray());
    }

    /**
     * Create a ledger id from a network name.
     *
     * @param networkName               the network name
     * @return                          the ledger id
     */
    @Deprecated
    public static LedgerId fromNetworkName(NetworkName networkName) {
        switch (networkName) {
            case MAINNET:
                return MAINNET;
            case TESTNET:
                return TESTNET;
            case PREVIEWNET:
                return PREVIEWNET;
            default:
                throw new IllegalArgumentException("networkName must be MAINNET, TESTNET, or PREVIEWNET");
        }
    }

    /**
     * Are we on Mionnet?
     *
     * @return                          is it mainnet
     */
    public boolean isMainnet() {
        return this.equals(MAINNET);
    }

    /**
     * Are we on Testnet?
     *
     * @return                          is it testnet
     */
    public boolean isTestnet() {
        return this.equals(TESTNET);
    }

    /**
     * Are we on Previewnet?
     *
     * @return                          is it previewnet
     */
    public boolean isPreviewnet() {
        return this.equals(PREVIEWNET);
    }

    /**
     * Are we one of the three standard networks?
     *
     * @return                          is it one of the three standard networks
     */
    boolean isKnownNetwork() {
        return isMainnet() || isTestnet() || isPreviewnet();
    }

    /**
     * Extract the string representation.
     *
     * @return                          the string representation
     */
    public String toString() {
        if (isMainnet()) {
            return "mainnet";
        } else if (isTestnet()) {
            return "testnet";
        } else if (isPreviewnet()) {
            return "previewnet";
        } else {
            return Hex.toHexString(idBytes);
        }
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return Arrays.copyOf(idBytes, idBytes.length);
    }

    /**
     * Extract the byte string representation.
     *
     * @return                          the byte string representation
     */
    ByteString toByteString() {
        return ByteString.copyFrom(idBytes);
    }

    /**
     * Extract the network name.
     *
     * @return                          the network name
     */
    @Deprecated
    public NetworkName toNetworkName() {
        if (isMainnet()) {
            return NetworkName.MAINNET;
        } else if (isTestnet()) {
            return NetworkName.TESTNET;
        } else if (isPreviewnet()) {
            return NetworkName.PREVIEWNET;
        } else {
            return NetworkName.OTHER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LedgerId)) {
            return false;
        }

        LedgerId otherId = (LedgerId) o;
        return Arrays.equals(idBytes, otherId.idBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(idBytes);
    }
}
