// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in <a
 * href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
 */
public class EthereumTransactionDataEip1559 extends EthereumTransactionData {

    /**
     * ID of the chain
     */
    public byte[] chainId;

    /**
     * Transaction's nonce
     */
    public byte[] nonce;

    /**
     * An 'optional' additional fee in Ethereum that is paid directly to miners in order to incentivize them to include
     * your transaction in a block. Not used in Hedera
     */
    public byte[] maxPriorityGas;

    /**
     * The maximum amount, in tinybars, that the payer of the hedera transaction is willing to pay to complete the
     * transaction
     */
    public byte[] maxGas;

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
     * specifies an array of addresses and storage keys that the transaction plans to access
     */
    public byte[] accessList;

    /**
     * recovery parameter used to ease the signature verification
     */
    public byte[] recoveryId;

    /**
     * The R value of the signature
     */
    public byte[] r;

    /**
     * The S value of the signature
     */
    public byte[] s;

    EthereumTransactionDataEip1559(
            byte[] chainId,
            byte[] nonce,
            byte[] maxPriorityGas,
            byte[] maxGas,
            byte[] gasLimit,
            byte[] to,
            byte[] value,
            byte[] callData,
            byte[] accessList,
            byte[] recoveryId,
            byte[] r,
            byte[] s) {
        super(callData);

        this.chainId = chainId;
        this.nonce = nonce;
        this.maxPriorityGas = maxPriorityGas;
        this.maxGas = maxGas;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.accessList = accessList;
        this.recoveryId = recoveryId;
        this.r = r;
        this.s = s;
    }

    /**
     * Convert a byte array to an ethereum transaction data.
     *
     * @param bytes the byte array
     * @return the ethereum transaction data
     */
    public static EthereumTransactionDataEip1559 fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        // typed transaction?
        byte typeByte = rlpItem.asByte();
        if (typeByte != 2) {
            throw new IllegalArgumentException("rlp type byte " + typeByte + "is not supported");
        }
        rlpItem = decoder.next();
        if (!rlpItem.isList()) {
            throw new IllegalArgumentException("expected RLP element list");
        }
        List<RLPItem> rlpList = rlpItem.asRLPList().elements();
        if (rlpList.size() != 12) {
            throw new IllegalArgumentException("expected 12 RLP encoded elements, found " + rlpList.size());
        }

        return new EthereumTransactionDataEip1559(
                rlpList.get(0).data(),
                rlpList.get(1).data(),
                rlpList.get(2).data(),
                rlpList.get(3).data(),
                rlpList.get(4).data(),
                rlpList.get(5).data(),
                rlpList.get(6).data(),
                rlpList.get(7).data(),
                rlpList.get(8).data(),
                rlpList.get(9).data(),
                rlpList.get(10).data(),
                rlpList.get(11).data());
    }

    public byte[] toBytes() {
        return RLPEncoder.sequence(
                Integers.toBytes(0x02),
                List.of(
                        chainId,
                        nonce,
                        maxPriorityGas,
                        maxGas,
                        gasLimit,
                        to,
                        value,
                        callData,
                        new ArrayList<String>(),
                        recoveryId,
                        r,
                        s));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chainId", Hex.toHexString(chainId))
                .add("nonce", Hex.toHexString(nonce))
                .add("maxPriorityGas", Hex.toHexString(maxPriorityGas))
                .add("maxGas", Hex.toHexString(maxGas))
                .add("gasLimit", Hex.toHexString(gasLimit))
                .add("to", Hex.toHexString(to))
                .add("value", Hex.toHexString(value))
                .add("accessList", Hex.toHexString(accessList))
                .add("recoveryId", Hex.toHexString(recoveryId))
                .add("r", Hex.toHexString(r))
                .add("s", Hex.toHexString(s))
                .toString();
    }
}

