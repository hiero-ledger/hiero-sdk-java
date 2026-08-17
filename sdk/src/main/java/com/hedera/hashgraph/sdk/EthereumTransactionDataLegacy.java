// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.Arrays;
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

    /**
     * Constructor for building an unsigned legacy transaction. All fields are initialized to empty byte arrays and
     * should be populated via the setters before signing.
     */
    public EthereumTransactionDataLegacy() {
        this(
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {});
    }

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

    /**
     * RLP-encode the unsigned payload: {@code [nonce, gasPrice, gasLimit, to, value, callData]}.
     */
    byte[] toUnsignedRlp() {
        return RLPEncoder.list(nonce, gasPrice, gasLimit, to, value, callData);
    }

    /**
     * RLP-encode the full signed transaction: the unsigned payload plus {@code v, r, s} (no type prefix).
     */
    byte[] encodeWithSignature() {
        return RLPEncoder.list(nonce, gasPrice, gasLimit, to, value, callData, v, r, s);
    }

    public byte[] toBytes() {
        return encodeWithSignature();
    }

    @Override
    public byte[] sign(PrivateKey privateKey) {
        byte[] message = toUnsignedRlp();
        SignatureData signature = signMessage(message, privateKey);
        this.r = signature.r;
        this.s = signature.s;
        this.recoveryId = signature.recoveryId;
        // Legacy (non-EIP-155) signatures encode v as 27 + recoveryId.
        this.v = new byte[] {(byte) (27 + signature.recoveryId)};
        return encodeWithSignature();
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
    public EthereumTransactionDataLegacy setNonce(long nonce) {
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
    public EthereumTransactionDataLegacy setNonceBytes(byte[] nonce) {
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
    public EthereumTransactionDataLegacy setGasPrice(long gasPrice) {
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
    public EthereumTransactionDataLegacy setGasPriceBytes(byte[] gasPrice) {
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
    public EthereumTransactionDataLegacy setGasLimit(long gasLimit) {
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
    public EthereumTransactionDataLegacy setGasLimitBytes(byte[] gasLimit) {
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
    public EthereumTransactionDataLegacy setTo(byte[] to) {
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
    public EthereumTransactionDataLegacy setValue(BigInteger value) {
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
    public EthereumTransactionDataLegacy setValueBytes(byte[] value) {
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
    public EthereumTransactionDataLegacy setCallData(byte[] callData) {
        this.callData = Arrays.copyOf(callData, callData.length);
        return this;
    }

    /**
     * @return the V value of the signature as a number
     */
    public long getV() {
        return EthereumEncoding.ethBytesToLong(v);
    }

    /**
     * @param v the V value of the signature as a number
     * @return {@code this}
     */
    public EthereumTransactionDataLegacy setV(long v) {
        this.v = EthereumEncoding.uint64ToEthBytes(v);
        return this;
    }

    /**
     * @return the raw V bytes of the signature
     */
    public byte[] getVBytes() {
        return Arrays.copyOf(v, v.length);
    }

    /**
     * @param v the raw V bytes of the signature
     * @return {@code this}
     */
    public EthereumTransactionDataLegacy setVBytes(byte[] v) {
        this.v = Arrays.copyOf(v, v.length);
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
    public EthereumTransactionDataLegacy setR(byte[] r) {
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
    public EthereumTransactionDataLegacy setS(byte[] s) {
        this.s = Arrays.copyOf(s, s.length);
        return this;
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
