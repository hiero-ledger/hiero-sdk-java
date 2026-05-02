// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the legacy format
 */
public class EthereumTransactionDataLegacy extends EthereumTransactionData {

    /**
     * ID of the chain
     */
    public byte[] chainId = new byte[] {};

    /**
     * Transaction's nonce
     */
    public byte[] nonce;

    /**
     * The price for 1 gas
     */
    public byte[] gasPrice;

    /**
     * The amount of gas available for the transaction
     */
    public byte[] gasLimit;

    /**
     * The receiver of the transaction
     */
    public byte[] to;

    /**
     * The transaction value
     */
    public byte[] value;

    /**
     * The V value of the signature
     */
    public byte[] v;

    /**
     * recovery parameter used to ease the signature verification
     */
    public int recoveryId;

    /**
     * The R value of the signature
     */
    public byte[] r;

    /**
     * The S value of the signature
     */
    public byte[] s;

    EthereumTransactionDataLegacy(
            byte[] nonce,
            byte[] gasPrice,
            byte[] gasLimit,
            byte[] to,
            byte[] value,
            byte[] callData,
            byte[] v,
            byte[] r,
            byte[] s) {
        super(callData);

        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.v = v;
        this.r = r;
        this.s = s;

        var vBI = new BigInteger(1, this.v);
        this.recoveryId = vBI.testBit(0) ? 0 : 1;

        if (vBI.compareTo(BigInteger.valueOf(34)) > 0) {
            this.chainId = vBI.subtract(BigInteger.valueOf(35)).shiftRight(1).toByteArray();
        }
    }

    /**
     * Convert a byte array to an ethereum transaction data.
     *
     * @param bytes                     the byte array
     * @return                          the ethereum transaction data
     */
    public static EthereumTransactionDataLegacy fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        List<RLPItem> rlpList = rlpItem.asRLPList().elements();
        if (rlpList.size() != 9) {
            throw new IllegalArgumentException("expected 9 RLP encoded elements, found " + rlpList.size());
        }

        return new EthereumTransactionDataLegacy(
                rlpList.get(0).data(),
                rlpList.get(1).asBytes(),
                rlpList.get(2).data(),
                rlpList.get(3).data(),
                rlpList.get(4).data(),
                rlpList.get(5).data(),
                rlpList.get(6).asBytes(),
                rlpList.get(7).data(),
                rlpList.get(8).data());
    }

    public byte[] toBytes() {
        return RLPEncoder.list(nonce, gasPrice, gasLimit, to, value, callData, v, r, s);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chainId", Hex.toHexString(this.chainId))
                .add("nonce", Hex.toHexString(this.nonce))
                .add("gasPrice", Hex.toHexString(this.gasPrice))
                .add("gasLimit", Hex.toHexString(this.gasLimit))
                .add("to", Hex.toHexString(this.to))
                .add("value", Hex.toHexString(this.value))
                .add("recoveryId", this.recoveryId)
                .add("v", Hex.toHexString(this.v))
                .add("r", Hex.toHexString(this.r))
                .add("s", Hex.toHexString(this.s))
                .toString();
    }
}

