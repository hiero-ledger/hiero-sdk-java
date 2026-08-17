// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in <a
 * href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
 */
public class EthereumTransactionDataEip1559 extends EthereumTransactionData {

    /**
     * The EIP-2718 transaction type prefix.
     */
    static final int TYPE_BYTE = 0x02;

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

    /**
     * Constructor for building an unsigned EIP-1559 transaction. All fields are initialized to empty byte arrays and
     * should be populated via the setters before signing.
     */
    public EthereumTransactionDataEip1559() {
        super(new byte[] {});
        this.chainId = new byte[] {};
        this.nonce = new byte[] {};
        this.maxPriorityGas = new byte[] {};
        this.maxGas = new byte[] {};
        this.gasLimit = new byte[] {};
        this.to = new byte[] {};
        this.value = new byte[] {};
        this.accessList = new byte[] {};
        this.recoveryId = new byte[] {};
        this.r = new byte[] {};
        this.s = new byte[] {};
    }

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

    /**
     * RLP-encode the unsigned payload (9 fields through the access list).
     */
    byte[] toUnsignedRlp() {
        return RLPEncoder.list(chainId, nonce, maxPriorityGas, maxGas, gasLimit, to, value, callData, accessList);
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
    public byte[] sign(PrivateKey privateKey) {
        SignatureData signature = signTypedTransaction(toUnsignedRlp(), TYPE_BYTE, privateKey);
        this.r = signature.r;
        this.s = signature.s;
        this.recoveryId = EthereumEncoding.uint64ToEthBytes(signature.recoveryId);
        return toBytes();
    }

    /**
     * @return the chain id as a number
     */
    public long getChainId() {
        return EthereumEncoding.ethBytesToLong(chainId);
    }

    /**
     * @param chainId the chain id as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setChainId(long chainId) {
        this.chainId = EthereumEncoding.uint64ToEthBytes(chainId);
        return this;
    }

    /**
     * @return the raw chain id bytes
     */
    public byte[] getChainIdBytes() {
        return Arrays.copyOf(chainId, chainId.length);
    }

    /**
     * @param chainId the raw chain id bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setChainIdBytes(byte[] chainId) {
        this.chainId = Arrays.copyOf(chainId, chainId.length);
        return this;
    }

    /**
     * @return the nonce as a number
     */
    public long getNonce() {
        return EthereumEncoding.ethBytesToLong(nonce);
    }

    /**
     * @param nonce the nonce as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setNonce(long nonce) {
        this.nonce = EthereumEncoding.uint64ToEthBytes(nonce);
        return this;
    }

    /**
     * @return the raw nonce bytes
     */
    public byte[] getNonceBytes() {
        return Arrays.copyOf(nonce, nonce.length);
    }

    /**
     * @param nonce the raw nonce bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setNonceBytes(byte[] nonce) {
        this.nonce = Arrays.copyOf(nonce, nonce.length);
        return this;
    }

    /**
     * @return the max priority gas as a number
     */
    public long getMaxPriorityGas() {
        return EthereumEncoding.ethBytesToLong(maxPriorityGas);
    }

    /**
     * @param maxPriorityGas the max priority gas as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setMaxPriorityGas(long maxPriorityGas) {
        this.maxPriorityGas = EthereumEncoding.uint64ToEthBytes(maxPriorityGas);
        return this;
    }

    /**
     * @return the raw max priority gas bytes
     */
    public byte[] getMaxPriorityGasBytes() {
        return Arrays.copyOf(maxPriorityGas, maxPriorityGas.length);
    }

    /**
     * @param maxPriorityGas the raw max priority gas bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setMaxPriorityGasBytes(byte[] maxPriorityGas) {
        this.maxPriorityGas = Arrays.copyOf(maxPriorityGas, maxPriorityGas.length);
        return this;
    }

    /**
     * @return the max gas as a number
     */
    public long getMaxGas() {
        return EthereumEncoding.ethBytesToLong(maxGas);
    }

    /**
     * @param maxGas the max gas as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setMaxGas(long maxGas) {
        this.maxGas = EthereumEncoding.uint64ToEthBytes(maxGas);
        return this;
    }

    /**
     * @return the raw max gas bytes
     */
    public byte[] getMaxGasBytes() {
        return Arrays.copyOf(maxGas, maxGas.length);
    }

    /**
     * @param maxGas the raw max gas bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setMaxGasBytes(byte[] maxGas) {
        this.maxGas = Arrays.copyOf(maxGas, maxGas.length);
        return this;
    }

    /**
     * @return the gas limit as a number
     */
    public long getGasLimit() {
        return EthereumEncoding.ethBytesToLong(gasLimit);
    }

    /**
     * @param gasLimit the gas limit as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setGasLimit(long gasLimit) {
        this.gasLimit = EthereumEncoding.uint64ToEthBytes(gasLimit);
        return this;
    }

    /**
     * @return the raw gas limit bytes
     */
    public byte[] getGasLimitBytes() {
        return Arrays.copyOf(gasLimit, gasLimit.length);
    }

    /**
     * @param gasLimit the raw gas limit bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setGasLimitBytes(byte[] gasLimit) {
        this.gasLimit = Arrays.copyOf(gasLimit, gasLimit.length);
        return this;
    }

    /**
     * @return the receiver address
     */
    public byte[] getTo() {
        return Arrays.copyOf(to, to.length);
    }

    /**
     * @param to the receiver address
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setTo(byte[] to) {
        this.to = Arrays.copyOf(to, to.length);
        return this;
    }

    /**
     * @return the transaction value as a number
     */
    public BigInteger getValue() {
        return EthereumEncoding.ethBytesToBigInt(value);
    }

    /**
     * @param value the transaction value as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setValue(BigInteger value) {
        this.value = EthereumEncoding.bigIntToEthBytes(value);
        return this;
    }

    /**
     * @return the raw transaction value bytes
     */
    public byte[] getValueBytes() {
        return Arrays.copyOf(value, value.length);
    }

    /**
     * @param value the raw transaction value bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setValueBytes(byte[] value) {
        this.value = Arrays.copyOf(value, value.length);
        return this;
    }

    /**
     * @return the call data
     */
    public byte[] getCallData() {
        return Arrays.copyOf(callData, callData.length);
    }

    /**
     * @param callData the call data
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setCallData(byte[] callData) {
        this.callData = Arrays.copyOf(callData, callData.length);
        return this;
    }

    /**
     * @return the access list items decoded from the {@code accessList} bytes
     */
    public List<AccessListItem> getAccessListItems() {
        return AccessListItem.decodeAccessList(accessList);
    }

    /**
     * @param accessListItems the access list items (encoded into the {@code accessList} bytes)
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setAccessListItems(List<AccessListItem> accessListItems) {
        this.accessList = AccessListItem.encodeAccessList(accessListItems);
        return this;
    }

    /**
     * Append a single access list item.
     *
     * @param accessListItem the item to add
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 addAccessListItem(AccessListItem accessListItem) {
        List<AccessListItem> items = AccessListItem.decodeAccessList(accessList);
        items.add(accessListItem);
        this.accessList = AccessListItem.encodeAccessList(items);
        return this;
    }

    /**
     * @return the recovery id as a number
     */
    public long getRecoveryId() {
        return EthereumEncoding.ethBytesToLong(recoveryId);
    }

    /**
     * @param recoveryId the recovery id as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setRecoveryId(long recoveryId) {
        this.recoveryId = EthereumEncoding.uint64ToEthBytes(recoveryId);
        return this;
    }

    /**
     * @return the raw recovery id bytes
     */
    public byte[] getRecoveryIdBytes() {
        return Arrays.copyOf(recoveryId, recoveryId.length);
    }

    /**
     * @param recoveryId the raw recovery id bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setRecoveryIdBytes(byte[] recoveryId) {
        this.recoveryId = Arrays.copyOf(recoveryId, recoveryId.length);
        return this;
    }

    /**
     * @return the R value of the signature
     */
    public byte[] getR() {
        return Arrays.copyOf(r, r.length);
    }

    /**
     * @param r the R value of the signature
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setR(byte[] r) {
        this.r = Arrays.copyOf(r, r.length);
        return this;
    }

    /**
     * @return the S value of the signature
     */
    public byte[] getS() {
        return Arrays.copyOf(s, s.length);
    }

    /**
     * @param s the S value of the signature
     * @return {@code this}
     */
    public EthereumTransactionDataEip1559 setS(byte[] s) {
        this.s = Arrays.copyOf(s, s.length);
        return this;
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
