// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.ClprSubmitBundleTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Submits an opaque peer bundle to a CLPR Channel under HIP-1535.
 * <p>
 * The SDK copies the payload without interpreting it. The Channel's verifier
 * contract interprets and verifies it. The deprecated {@code endpoint_node_id} and
 * {@code endpoint_signature} fields do not authenticate the caller and are not exposed
 * by this builder. Newly built bodies omit them; decoded signed transactions keep their
 * original body bytes and signatures.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprSubmitBundleTransaction extends ClprTransaction<ClprSubmitBundleTransaction> {
    private byte @Nullable [] channelId;
    private byte @Nullable [] bundlePayload;

    /**
     * Constructor.
     */
    public ClprSubmitBundleTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprSubmitBundleTransaction(
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
    ClprSubmitBundleTransaction(TransactionBody txBody) {
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
    public ClprSubmitBundleTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        this.channelId = ClprOwnership.requireBytes32(channelId, "channelId");
        return this;
    }

    /**
     * Returns a copy of the opaque bundle payload.
     *
     * @return the payload, or null when it has not been set
     */
    public byte @Nullable [] getBundlePayload() {
        return bundlePayload == null ? null : bundlePayload.clone();
    }

    /**
     * Sets the opaque bundle payload and copies its bytes without interpreting them.
     *
     * @param bundlePayload the bundle payload
     * @return {@code this}
     */
    public ClprSubmitBundleTransaction setBundlePayload(byte[] bundlePayload) {
        requireNotFrozen();
        this.bundlePayload = Objects.requireNonNull(bundlePayload, "bundlePayload must not be null")
                .clone();
        return this;
    }

    ClprSubmitBundleTransactionBody.Builder build() {
        var builder = ClprSubmitBundleTransactionBody.newBuilder();
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        if (bundlePayload != null) {
            builder.setBundlePayload(ByteString.copyFrom(bundlePayload));
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprSubmitBundle();
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
        bundlePayload = body.getBundlePayload().toByteArray();
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
        bodyBuilder.setClprSubmitBundle(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getSubmitBundleMethod();
    }
}
