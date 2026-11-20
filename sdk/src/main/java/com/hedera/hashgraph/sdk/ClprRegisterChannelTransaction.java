// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprRegisterChannelTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reserves a CLPR Channel in the commit phase of the HIP-1535 commit-reveal flow.
 * <p>
 * Any payer can register a 32-byte ownership commitment, computed as
 * {@code keccak256(channelId || canonical public key)}. The Channel ID and public key
 * are revealed during Channel completion to prevent front-running.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprRegisterChannelTransaction extends ClprTransaction<ClprRegisterChannelTransaction> {
    private byte @Nullable [] ownershipCommitment;

    /**
     * Constructor.
     */
    public ClprRegisterChannelTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprRegisterChannelTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    ClprRegisterChannelTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Computes the 32-byte ownership commitment for a Channel registration,
     * {@code keccak256(channelId || canonical public key)}.
     * <p>
     * The canonical key is the 64-byte uncompressed {@code x || y} point for ECDSA secp256k1,
     * or the raw 32 bytes for Ed25519.
     *
     * @param channelId the 32-byte Channel ID
     * @param publicKey the ownership public key
     * @return the 32-byte commitment
     */
    public static byte[] computeOwnershipCommitment(byte[] channelId, PublicKey publicKey) {
        return ClprOwnership.computeChannelCommitment(channelId, publicKey);
    }

    /**
     * Returns a copy of the ownership commitment.
     *
     * @return the commitment, or null when it has not been set
     */
    public byte @Nullable [] getOwnershipCommitment() {
        return ownershipCommitment == null ? null : ownershipCommitment.clone();
    }

    /**
     * Sets the 32-byte ownership commitment and copies its bytes.
     *
     * @param ownershipCommitment the 32-byte commitment
     * @return {@code this}
     */
    public ClprRegisterChannelTransaction setOwnershipCommitment(byte[] ownershipCommitment) {
        requireNotFrozen();
        this.ownershipCommitment = ClprOwnership.requireBytes32(ownershipCommitment, "ownershipCommitment");
        return this;
    }

    ClprRegisterChannelTransactionBody.Builder build() {
        var builder = ClprRegisterChannelTransactionBody.newBuilder();
        if (ownershipCommitment != null) {
            builder.setOwnershipCommitment(ByteString.copyFrom(ownershipCommitment));
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprRegisterChannel();
        if (!body.getOwnershipCommitment().isEmpty()) {
            ownershipCommitment = body.getOwnershipCommitment().toByteArray();
        }
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("ownershipCommitment", ownershipCommitment);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprRegisterChannel(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getRegisterChannelMethod();
    }
}
