// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in <a
 * href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2930.md">EIP-2930</a> (access list transactions).
 */
public class EthereumTransactionDataEip2930 extends EthereumTransactionData {

    /**
     * The EIP-2718 transaction type prefix.
     */
    static final int TYPE_BYTE = 0x01;

    /**
     * ID of the chain
     */
    public byte[] chainId;

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
     * Constructor for building an unsigned EIP-2930 transaction. All fields are initialized to empty byte arrays and
     * should be populated via the setters before signing.
     */
    public EthereumTransactionDataEip2930() {
        super(new byte[] {});
        this.chainId = new byte[] {};
        this.nonce = new byte[] {};
        this.gasPrice = new byte[] {};
        this.gasLimit = new byte[] {};
        this.to = new byte[] {};
        this.value = new byte[] {};
        this.accessList = new byte[] {};
        this.recoveryId = new byte[] {};
        this.r = new byte[] {};
        this.s = new byte[] {};
    }

    EthereumTransactionDataEip2930(
            byte[] chainId,
            byte[] nonce,
            byte[] gasPrice,
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
        this.gasPrice = gasPrice;
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
    public static EthereumTransactionDataEip2930 fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        byte typeByte = rlpItem.asByte();
        if (typeByte != TYPE_BYTE) {
            throw new IllegalArgumentException("rlp type byte " + typeByte + " is not supported");
        }
        rlpItem = decoder.next();
        if (!rlpItem.isList()) {
            throw new IllegalArgumentException("expected RLP element list");
        }
        List<RLPItem> rlpList = rlpItem.asRLPList().elements();
        if (rlpList.size() != 11) {
            throw new IllegalArgumentException("expected 11 RLP encoded elements, found " + rlpList.size());
        }

        return new EthereumTransactionDataEip2930(
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
                rlpList.get(10).data());
    }

    /**
     * RLP-encode the unsigned payload (8 fields through the access list).
     */
    byte[] toUnsignedRlp() {
        return RLPEncoder.list(chainId, nonce, gasPrice, gasLimit, to, value, callData, accessList);
    }

    public byte[] toBytes() {
        return RLPEncoder.sequence(
                Integers.toBytes(TYPE_BYTE),
                List.of(chainId, nonce, gasPrice, gasLimit, to, value, callData, accessList, recoveryId, r, s));
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
    public EthereumTransactionDataEip2930 setChainId(long chainId) {
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
    public EthereumTransactionDataEip2930 setChainIdBytes(byte[] chainId) {
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
    public EthereumTransactionDataEip2930 setNonce(long nonce) {
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
    public EthereumTransactionDataEip2930 setNonceBytes(byte[] nonce) {
        this.nonce = Arrays.copyOf(nonce, nonce.length);
        return this;
    }

    /**
     * @return the gas price as a number
     */
    public long getGasPrice() {
        return EthereumEncoding.ethBytesToLong(gasPrice);
    }

    /**
     * @param gasPrice the gas price as a number
     * @return {@code this}
     */
    public EthereumTransactionDataEip2930 setGasPrice(long gasPrice) {
        this.gasPrice = EthereumEncoding.uint64ToEthBytes(gasPrice);
        return this;
    }

    /**
     * @return the raw gas price bytes
     */
    public byte[] getGasPriceBytes() {
        return Arrays.copyOf(gasPrice, gasPrice.length);
    }

    /**
     * @param gasPrice the raw gas price bytes
     * @return {@code this}
     */
    public EthereumTransactionDataEip2930 setGasPriceBytes(byte[] gasPrice) {
        this.gasPrice = Arrays.copyOf(gasPrice, gasPrice.length);
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
    public EthereumTransactionDataEip2930 setGasLimit(long gasLimit) {
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
    public EthereumTransactionDataEip2930 setGasLimitBytes(byte[] gasLimit) {
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
    public EthereumTransactionDataEip2930 setTo(byte[] to) {
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
    public EthereumTransactionDataEip2930 setValue(BigInteger value) {
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
    public EthereumTransactionDataEip2930 setValueBytes(byte[] value) {
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
    public EthereumTransactionDataEip2930 setCallData(byte[] callData) {
        this.callData = Arrays.copyOf(callData, callData.length);
        return this;
    }

    /**
     * @return the access list items
     */
    public List<AccessListItem> getAccessListItems() {
        return AccessListItem.decodeAccessList(accessList);
    }

    /**
     * @param accessListItems the access list items (encoded into the {@code accessList} bytes)
     * @return {@code this}
     */
    public EthereumTransactionDataEip2930 setAccessListItems(List<AccessListItem> accessListItems) {
        this.accessList = AccessListItem.encodeAccessList(accessListItems);
        return this;
    }

    /**
     * Append a single access list item.
     *
     * @param accessListItem the item to add
     * @return {@code this}
     */
    public EthereumTransactionDataEip2930 addAccessListItem(AccessListItem accessListItem) {
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
    public EthereumTransactionDataEip2930 setRecoveryId(long recoveryId) {
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
    public EthereumTransactionDataEip2930 setRecoveryIdBytes(byte[] recoveryId) {
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
    public EthereumTransactionDataEip2930 setR(byte[] r) {
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
    public EthereumTransactionDataEip2930 setS(byte[] s) {
        this.s = Arrays.copyOf(s, s.length);
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chainId", Hex.toHexString(chainId))
                .add("nonce", Hex.toHexString(nonce))
                .add("gasPrice", Hex.toHexString(gasPrice))
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
