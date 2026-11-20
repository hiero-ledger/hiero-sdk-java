// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprCloseChannelTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Initiates graceful closure of a CLPR Channel under HIP-1535.
 * <p>
 * This privileged operation requires the treasury or system admin authority.
 * An existing Channel stops accepting new messages and drains its queues before
 * closing. When no Channel exists, the ownership commitment identifies an abandoned
 * pending registration to sweep. The commitment is ignored for an existing Channel.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprCloseChannelTransaction extends ClprTransaction<ClprCloseChannelTransaction> {
    private byte @Nullable [] channelId;
    private byte @Nullable [] ownershipCommitment;

    /**
     * Constructor.
     */
    public ClprCloseChannelTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprCloseChannelTransaction(
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
    ClprCloseChannelTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Returns a copy of the Channel ID.
     *
     * @return the Channel ID, or null when it has not been set
     */
    public byte @Nullable [] getChannelId() {
        return channelId == null ? null : channelId.clone();
    }

    /**
     * Sets the 32-byte Channel ID and copies its bytes.
     *
     * @param channelId the 32-byte Channel ID
     * @return {@code this}
     */
    public ClprCloseChannelTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        this.channelId = ClprOwnership.requireBytes32(channelId, "channelId");
        return this;
    }

    /**
     * Returns a copy of the optional ownership commitment.
     *
     * @return the commitment, or null when it has not been set
     */
    public byte @Nullable [] getOwnershipCommitment() {
        return ownershipCommitment == null ? null : ownershipCommitment.clone();
    }

    /**
     * Sets the 32-byte commitment of an abandoned pending registration to sweep, and copies its bytes.
     * This field is ignored when the Channel already exists.
     *
     * @param ownershipCommitment the 32-byte commitment
     * @return {@code this}
     */
    public ClprCloseChannelTransaction setOwnershipCommitment(byte[] ownershipCommitment) {
        requireNotFrozen();
        this.ownershipCommitment = ClprOwnership.requireBytes32(ownershipCommitment, "ownershipCommitment");
        return this;
    }

    ClprCloseChannelTransactionBody.Builder build() {
        var builder = ClprCloseChannelTransactionBody.newBuilder();
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        if (ownershipCommitment != null) {
            builder.setOwnershipCommitment(ByteString.copyFrom(ownershipCommitment));
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprCloseChannel();
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
        if (!body.getOwnershipCommitment().isEmpty()) {
            ownershipCommitment = body.getOwnershipCommitment().toByteArray();
        }
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprCloseChannel(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getCloseChannelMethod();
    }
}
