// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/**
 * Info about a contract account's nonce value.
 * A nonce of a contract is only incremented when that contract creates another contract.
 */
public final class ContractNonceInfo {
    /**
     * Id of the contract
     */
    public final ContractId contractId;

    /**
     * The current value of the contract account's nonce property
     */
    public final Long nonce;

    public ContractNonceInfo(ContractId contractId, Long nonce) {
        this.contractId = contractId;
        this.nonce = nonce;
    }

    /**
     * Extract the contractNonce from the protobuf.
     *
     * @param contractNonceInfo the protobuf
     * @return the contract object
     */
    static ContractNonceInfo fromProtobuf(org.hiero.sdk.proto.ContractNonceInfo contractNonceInfo) {
        return new ContractNonceInfo(
                ContractId.fromProtobuf(contractNonceInfo.getContractId()), contractNonceInfo.getNonce());
    }

    /**
     * Extract the contractNonce from a byte array.
     *
     * @param bytes the byte array
     * @return the extracted contract
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static ContractNonceInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.ContractNonceInfo.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Build the protobuf.
     *
     * @return the protobuf representation
     */
    org.hiero.sdk.proto.ContractNonceInfo toProtobuf() {
        return org.hiero.sdk.proto.ContractNonceInfo.newBuilder()
                .setContractId(contractId.toProtobuf())
                .setNonce(nonce)
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId, nonce);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContractNonceInfo otherInfo)) {
            return false;
        }

        return contractId.equals(otherInfo.contractId) && nonce.equals(otherInfo.nonce);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("contractId", contractId)
                .add("nonce", nonce)
                .toString();
    }

    /**
     * Create a byte array representation.
     *
     * @return the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
