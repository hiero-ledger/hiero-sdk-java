// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprRegisterConnectorTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reserves a CLPR Connector on an existing Channel in the HIP-1535 commit-reveal flow.
 * <p>
 * Derive the Connector ID with {@link #computeConnectorId}, then register the result of
 * {@link #computeOwnershipCommitment} alongside the Channel ID. Complete the registration
 * by revealing the bound Channel ID, Connector ID, salt and proof with
 * {@link ClprCompleteConnectorTransaction}. The reveal's admin key keeps deregistration
 * possible, and its payer funds the locked stake.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprRegisterConnectorTransaction extends ClprTransaction<ClprRegisterConnectorTransaction> {
    private byte @Nullable [] commitment;
    private byte @Nullable [] channelId;

    /**
     * Constructor.
     */
    public ClprRegisterConnectorTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprRegisterConnectorTransaction(
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
    ClprRegisterConnectorTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Computes the Connector ID, {@code keccak256(channelId || canonical public key || salt)}.
     * Supports ECDSA secp256k1 and Ed25519 ownership keys.
     *
     * @param channelId the 32-byte Channel ID
     * @param publicKey the ownership public key
     * @param salt      the 32-byte operator-chosen salt
     * @return the 32-byte Connector ID
     */
    public static byte[] computeConnectorId(byte[] channelId, PublicKey publicKey, byte[] salt) {
        return ClprOwnership.computeConnectorId(channelId, publicKey, salt);
    }

    /**
     * Computes the Connector ownership commitment, {@code keccak256(connectorId || canonical public key)}.
     *
     * @param connectorId the 32-byte Connector ID
     * @param publicKey   the ownership public key
     * @return the 32-byte commitment
     */
    public static byte[] computeOwnershipCommitment(byte[] connectorId, PublicKey publicKey) {
        return ClprOwnership.computeConnectorCommitment(connectorId, publicKey);
    }

    /**
     * Returns a copy of the ownership commitment.
     *
     * @return the commitment, or null when it has not been set
     */
    public byte @Nullable [] getCommitment() {
        return commitment == null ? null : commitment.clone();
    }

    /**
     * Sets the 32-byte ownership commitment and copies its bytes.
     *
     * @param commitment the 32-byte commitment
     * @return {@code this}
     */
    public ClprRegisterConnectorTransaction setCommitment(byte[] commitment) {
        requireNotFrozen();
        this.commitment = ClprOwnership.requireBytes32(commitment, "commitment");
        return this;
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
     * Sets the existing Channel's 32-byte ID and copies its bytes.
     *
     * @param channelId the 32-byte Channel ID
     * @return {@code this}
     */
    public ClprRegisterConnectorTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        this.channelId = ClprOwnership.requireBytes32(channelId, "channelId");
        return this;
    }

    ClprRegisterConnectorTransactionBody.Builder build() {
        var builder = ClprRegisterConnectorTransactionBody.newBuilder();
        if (commitment != null) {
            builder.setCommitment(ByteString.copyFrom(commitment));
        }
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprRegisterConnector();
        if (!body.getCommitment().isEmpty()) {
            commitment = body.getCommitment().toByteArray();
        }
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("commitment", commitment);
        fields.put("channelId", channelId);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprRegisterConnector(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getRegisterConnectorMethod();
    }
}
