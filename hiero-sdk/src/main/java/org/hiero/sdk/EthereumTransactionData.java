// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;

/**
 * This class represents the data of an Ethereum transaction.
 * <p>
 * It may be of subclass {@link EthereumTransactionDataLegacy} or of subclass {@link EthereumTransactionDataEip1559}
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
        } else {
            return EthereumTransactionDataEip1559.fromBytes(bytes);
        }
    }

    /**
     * Serialize the ethereum transaction data into bytes using RLP
     *
     * @return the serialized transaction as a byte array
     */
    public abstract byte[] toBytes();

    /**
     * Serialize the ethereum transaction data into a string
     *
     * @return the serialized transaction as a string
     */
    public abstract String toString();
}
