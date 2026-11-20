// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprDeregisterConnectorTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Deregisters a CLPR Connector and returns its locked stake only to the named account.
 * <p>
 * The Connector admin key and the explicitly named stake recipient must both sign.
 * The SDK never defaults the recipient. The ownership commitment stays registered,
 * so registering another Connector requires a new salt and Connector ID.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprDeregisterConnectorTransaction extends ClprTransaction<ClprDeregisterConnectorTransaction> {
    private byte @Nullable [] channelId;
    private byte @Nullable [] connectorId;
    private @Nullable AccountId stakeRecipient;

    /**
     * Constructor.
     */
    public ClprDeregisterConnectorTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprDeregisterConnectorTransaction(
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
    ClprDeregisterConnectorTransaction(TransactionBody txBody) {
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
    public ClprDeregisterConnectorTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        this.channelId = ClprOwnership.requireBytes32(channelId, "channelId");
        return this;
    }

    /**
     * Returns a copy of the Connector ID.
     *
     * @return the Connector ID, or null when it has not been set
     */
    public byte @Nullable [] getConnectorId() {
        return connectorId == null ? null : connectorId.clone();
    }

    /**
     * Sets the 32-byte Connector ID and copies its bytes.
     *
     * @param connectorId the 32-byte Connector ID
     * @return {@code this}
     */
    public ClprDeregisterConnectorTransaction setConnectorId(byte[] connectorId) {
        requireNotFrozen();
        this.connectorId = ClprOwnership.requireBytes32(connectorId, "connectorId");
        return this;
    }

    /**
     * Returns the stake recipient.
     *
     * @return the stake recipient, or null when it has not been set
     */
    public @Nullable AccountId getStakeRecipient() {
        return stakeRecipient;
    }

    /**
     * Sets the account that receives the returned stake. This account must sign.
     *
     * @param stakeRecipient the stake recipient
     * @return {@code this}
     */
    public ClprDeregisterConnectorTransaction setStakeRecipient(AccountId stakeRecipient) {
        requireNotFrozen();
        this.stakeRecipient = Objects.requireNonNull(stakeRecipient, "stakeRecipient must not be null");
        return this;
    }

    ClprDeregisterConnectorTransactionBody.Builder build() {
        var builder = ClprDeregisterConnectorTransactionBody.newBuilder();
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        if (connectorId != null) {
            builder.setConnectorId(ByteString.copyFrom(connectorId));
        }
        if (stakeRecipient != null) {
            builder.setStakeRecipient(stakeRecipient.toProtobuf());
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprDeregisterConnector();
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
        if (!body.getConnectorId().isEmpty()) {
            connectorId = body.getConnectorId().toByteArray();
        }
        if (body.hasStakeRecipient()) {
            stakeRecipient = AccountId.fromProtobuf(body.getStakeRecipient());
        }
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        fields.put("connectorId", connectorId);
        fields.put("stakeRecipient", stakeRecipient);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (stakeRecipient != null) {
            stakeRecipient.validateChecksum(client);
        }
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprDeregisterConnector(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getDeregisterConnectorMethod();
    }
}
