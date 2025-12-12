// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in
 * <a href="https://eips.ethereum.org/EIPS/eip-7702">EIP-7702</a>
 */
public class EthereumTransactionDataEip7702 extends EthereumTransactionData {

    /**
     * ID of the chain.
     */
    public byte[] chainId;

    /**
     * Transaction's nonce.
     */
    public byte[] nonce;

    /**
     * An 'optional' additional fee in Ethereum that is paid directly to miners in order to incentivize them to include
     * your transaction in a block. Not used in Hedera.
     */
    public byte[] maxPriorityGas;

    /**
     * The maximum amount, in tinybars, that the payer of the hedera transaction is willing to pay to complete the
     * transaction.
     */
    public byte[] maxGas;

    /**
     * The amount of gas available for the transaction.
     */
    public byte[] gasLimit;

    /**
     * The receiver of the transaction.
     */
    public byte[] to;

    /**
     * The transaction value.
     */
    public byte[] value;

    /**
     * Specifies an array of addresses and storage keys that the transaction plans to access.
     */
    public List<byte[]> accessList;

    /**
     * The list of delegation authorizations.
     */
    public List<AuthorizationTuple> authorizationList;

    /**
     * Recovery parameter used to ease the signature verification.
     */
    public byte[] recoveryId;

    /**
     * The R value of the signature.
     */
    public byte[] r;

    /**
     * The S value of the signature.
     */
    public byte[] s;

    EthereumTransactionDataEip7702(
            HeaderData headerData,
            byte[] callData,
            List<byte[]> accessList,
            List<AuthorizationTuple> authorizationList,
            SignatureData signatureData) {
        super(callData);

        this.chainId = headerData.chainId;
        this.nonce = headerData.nonce;
        this.maxPriorityGas = headerData.maxPriorityGas;
        this.maxGas = headerData.maxGas;
        this.gasLimit = headerData.gasLimit;
        this.to = headerData.to;
        this.value = headerData.value;
        this.accessList = accessList;
        this.authorizationList = authorizationList;
        this.recoveryId = signatureData.recoveryId;
        this.r = signatureData.r;
        this.s = signatureData.s;
    }

    /**
     * Convert a byte array to an ethereum transaction data.
     *
     * @param bytes the byte array
     * @return the ethereum transaction data
     */
    public static EthereumTransactionDataEip7702 fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        // typed transaction?
        byte typeByte = rlpItem.asByte();
        if (typeByte != 4) {
            throw new IllegalArgumentException("rlp type byte " + typeByte + " is not supported");
        }
        rlpItem = decoder.next();
        if (!rlpItem.isList()) {
            throw new IllegalArgumentException("expected RLP element list");
        }
        List<RLPItem> rlpList = rlpItem.asRLPList().elements();
        if (rlpList.size() != 13) {
            throw new IllegalArgumentException("expected 13 RLP encoded elements, found " + rlpList.size());
        }

        var accessList = new ArrayList<byte[]>();
        for (var accessListItem : rlpList.get(8).asRLPList().elements()) {
            accessList.add(accessListItem.data());
        }

        var authorizationList = new ArrayList<AuthorizationTuple>();
        for (var authorizationTuple : rlpList.get(9).asRLPList().elements()) {
            var tupleElements = authorizationTuple.asRLPList().elements();
            if (tupleElements.size() != 6) {
                throw new IllegalArgumentException("invalid authorization list entry: must have 6 elements");
            }
            authorizationList.add(new AuthorizationTuple(
                    tupleElements.get(0).data(),
                    tupleElements.get(1).data(),
                    tupleElements.get(2).data(),
                    tupleElements.get(3).data(),
                    tupleElements.get(4).data(),
                    tupleElements.get(5).data()));
        }

        var headerData = new HeaderData(
                rlpList.get(0).data(),
                rlpList.get(1).data(),
                rlpList.get(2).data(),
                rlpList.get(3).data(),
                rlpList.get(4).data(),
                rlpList.get(5).data(),
                rlpList.get(6).data());

        var signatureData = new SignatureData(
                rlpList.get(10).data(), rlpList.get(11).data(), rlpList.get(12).data());

        return new EthereumTransactionDataEip7702(
                headerData, rlpList.get(7).data(), accessList, authorizationList, signatureData);
    }

    public byte[] toBytes() {
        List<Object> encodedAuthorizationList = new ArrayList<>();
        for (var tuple : authorizationList) {
            encodedAuthorizationList.add(
                    List.of(tuple.chainId, tuple.address, tuple.nonce, tuple.yParity, tuple.r, tuple.s));
        }

        List<Object> encodedAccessList = new ArrayList<>(accessList);

        return RLPEncoder.sequence(
                Integers.toBytes(0x04),
                List.of(
                        chainId,
                        nonce,
                        maxPriorityGas,
                        maxGas,
                        gasLimit,
                        to,
                        value,
                        callData,
                        encodedAccessList,
                        encodedAuthorizationList,
                        recoveryId,
                        r,
                        s));
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chainId", Hex.toHexString(chainId))
                .add("nonce", Hex.toHexString(nonce))
                .add("maxPriorityGas", Hex.toHexString(maxPriorityGas))
                .add("maxGas", Hex.toHexString(maxGas))
                .add("gasLimit", Hex.toHexString(gasLimit))
                .add("to", Hex.toHexString(to))
                .add("value", Hex.toHexString(value))
                .add("callData", Hex.toHexString(callData))
                .add("accessList", accessList.stream().map(Hex::toHexString).collect(Collectors.toList()))
                .add(
                        "authorizationList",
                        authorizationList.stream()
                                .map(AuthorizationTuple::toString)
                                .collect(Collectors.toList()))
                .add("recoveryId", Hex.toHexString(recoveryId))
                .add("r", Hex.toHexString(r))
                .add("s", Hex.toHexString(s))
                .toString();
    }

    /**
     * A helper class to hold core transaction fields for EIP-7702 transactions.
     */
    static class HeaderData {
        public byte[] chainId;
        public byte[] nonce;
        public byte[] maxPriorityGas;
        public byte[] maxGas;
        public byte[] gasLimit;
        public byte[] to;
        public byte[] value;

        public HeaderData(
                byte[] chainId,
                byte[] nonce,
                byte[] maxPriorityGas,
                byte[] maxGas,
                byte[] gasLimit,
                byte[] to,
                byte[] value) {
            this.chainId = chainId;
            this.nonce = nonce;
            this.maxPriorityGas = maxPriorityGas;
            this.maxGas = maxGas;
            this.gasLimit = gasLimit;
            this.to = to;
            this.value = value;
        }
    }

    /**
     * A helper class to hold signature data for EIP-7702 transactions.
     */
    static class SignatureData {
        public byte[] recoveryId;
        public byte[] r;
        public byte[] s;

        public SignatureData(byte[] recoveryId, byte[] r, byte[] s) {
            this.recoveryId = recoveryId;
            this.r = r;
            this.s = s;
        }
    }

    /**
     * A tuple describing an authorization entry for EIP-7702 transactions.
     */
    public static class AuthorizationTuple {
        public byte[] chainId;
        public byte[] address;
        public byte[] nonce;
        public byte[] yParity;
        public byte[] r;
        public byte[] s;

        public AuthorizationTuple(byte[] chainId, byte[] address, byte[] nonce, byte[] yParity, byte[] r, byte[] s) {
            this.chainId = chainId;
            this.address = address;
            this.nonce = nonce;
            this.yParity = yParity;
            this.r = r;
            this.s = s;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("chainId", Hex.toHexString(chainId))
                    .add("address", Hex.toHexString(address))
                    .add("nonce", Hex.toHexString(nonce))
                    .add("yParity", Hex.toHexString(yParity))
                    .add("r", Hex.toHexString(r))
                    .add("s", Hex.toHexString(s))
                    .toString();
        }
    }
}
