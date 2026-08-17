// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import java.util.Arrays;

/**
 * This class represents the data of an Ethereum transaction.
 * <p>
 * It may be of subclass {@link EthereumTransactionDataLegacy}, {@link EthereumTransactionDataEip2930} or
 * {@link EthereumTransactionDataEip1559}.
 */
public abstract class EthereumTransactionData {
    /**
     * The raw call data.
     */
    public byte[] callData;

    EthereumTransactionData(byte[] callData) {
        this.callData = callData;
    }

    static EthereumTransactionData fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();
        if (rlpItem.isList()) {
            return EthereumTransactionDataLegacy.fromBytes(bytes);
        }
        byte typeByte = rlpItem.asByte();
        switch (typeByte) {
            case 0x01:
                return EthereumTransactionDataEip2930.fromBytes(bytes);
            case 0x02:
                return EthereumTransactionDataEip1559.fromBytes(bytes);
            default:
                throw new IllegalArgumentException("rlp type byte " + typeByte + " is not supported");
        }
    }

    /**
     * Serialize the ethereum transaction data into bytes using RLP
     *
     * @return the serialized transaction as a byte array
     */
    public abstract byte[] toBytes();

    /**
     * Sign this transaction body with the given ECDSA private key, populating the signature fields (r, s and the
     * recovery id / v), and return the resulting signed RLP encoding.
     *
     * @param privateKey the ECDSA secp256k1 key to sign with
     * @return the signed RLP-encoded transaction
     */
    public abstract byte[] sign(PrivateKey privateKey);

    /**
     * Serialize the ethereum transaction data into a string
     *
     * @return the serialized transaction as a string
     */
    public abstract String toString();

    /**
     * The result of signing a transaction: the {@code r} and {@code s} signature components and the recovery id.
     */
    static final class SignatureData {
        final byte[] r;
        final byte[] s;
        final int recoveryId;

        SignatureData(byte[] r, byte[] s, int recoveryId) {
            this.r = r;
            this.s = s;
            this.recoveryId = recoveryId;
        }
    }

    /**
     * Sign a typed (non-legacy) Ethereum transaction. The message that is signed is the type prefix byte concatenated
     * with the RLP-encoded unsigned payload list. {@link PrivateKeyECDSA#sign(byte[])} applies keccak256 internally,
     * so the raw message is passed without pre-hashing.
     *
     * @param unsignedListRlp the RLP encoding of the unsigned payload list (without the type prefix)
     * @param typePrefix      the EIP transaction type prefix byte
     * @param privateKey      the ECDSA key
     * @return the r/s/recoveryId signature components
     */
    static SignatureData signTypedTransaction(byte[] unsignedListRlp, int typePrefix, PrivateKey privateKey) {
        byte[] message = new byte[unsignedListRlp.length + 1];
        message[0] = (byte) typePrefix;
        System.arraycopy(unsignedListRlp, 0, message, 1, unsignedListRlp.length);
        return signMessage(message, privateKey);
    }

    /**
     * Sign a raw message (already keccak256-able) with an ECDSA key and extract the r/s/recoveryId components.
     *
     * @param message    the message to sign
     * @param privateKey the ECDSA key
     * @return the r/s/recoveryId signature components
     */
    static SignatureData signMessage(byte[] message, PrivateKey privateKey) {
        if (!(privateKey instanceof PrivateKeyECDSA)) {
            throw new IllegalArgumentException("Ethereum transactions must be signed with an ECDSA secp256k1 key");
        }
        PrivateKeyECDSA ecdsaKey = (PrivateKeyECDSA) privateKey;
        byte[] signature = ecdsaKey.sign(message);
        byte[] r = Arrays.copyOfRange(signature, 0, 32);
        byte[] s = Arrays.copyOfRange(signature, 32, 64);
        int recoveryId = ecdsaKey.getRecoveryId(r, s, message);
        return new SignatureData(r, s, recoveryId);
    }

    /**
     * @param body an ethereum transaction body
     * @return whether the body carries a non-empty signature (both r and s present)
     */
    static boolean ethereumBodyIsSigned(EthereumTransactionData body) {
        if (body instanceof EthereumTransactionDataLegacy) {
            var legacy = (EthereumTransactionDataLegacy) body;
            return legacy.r != null && legacy.r.length > 0 && legacy.s != null && legacy.s.length > 0;
        } else if (body instanceof EthereumTransactionDataEip2930) {
            var eip2930 = (EthereumTransactionDataEip2930) body;
            return eip2930.r != null && eip2930.r.length > 0 && eip2930.s != null && eip2930.s.length > 0;
        } else if (body instanceof EthereumTransactionDataEip1559) {
            var eip1559 = (EthereumTransactionDataEip1559) body;
            return eip1559.r != null && eip1559.r.length > 0 && eip1559.s != null && eip1559.s.length > 0;
        }
        return false;
    }
}
