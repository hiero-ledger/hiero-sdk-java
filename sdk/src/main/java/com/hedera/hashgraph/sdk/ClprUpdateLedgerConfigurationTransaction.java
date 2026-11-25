// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprLedgerConfiguration;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.ClprUpdateLedgerConfigurationTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Updates the local CLPR ledger's capacity limits.
 * <p>
 * Requires treasury or system admin authority. Send all seven limits: read the current
 * throttles with {@link ClprGetLedgerConfigurationQuery}, change the fields you want through
 * {@link ClprThrottles#of} while keeping the other limits, then submit the result with this
 * transaction.
 * <p>
 * Newly built bodies contain only {@code configuration.throttles}. Decoding exposes only the
 * throttles and keeps incoming signed bytes and signatures unchanged.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprUpdateLedgerConfigurationTransaction
        extends ClprTransaction<ClprUpdateLedgerConfigurationTransaction> {
    private @Nullable ClprThrottles throttles;

    /**
     * Constructor.
     */
    public ClprUpdateLedgerConfigurationTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprUpdateLedgerConfigurationTransaction(
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
    ClprUpdateLedgerConfigurationTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Returns the throttles.
     *
     * @return the throttles, or null when they have not been set
     */
    public @Nullable ClprThrottles getThrottles() {
        return throttles;
    }

    /**
     * Sets all seven capacity limits.
     *
     * @param throttles the throttles, built with {@link ClprThrottles#of}
     * @return {@code this}
     */
    public ClprUpdateLedgerConfigurationTransaction setThrottles(ClprThrottles throttles) {
        requireNotFrozen();
        this.throttles = Objects.requireNonNull(throttles, "throttles must not be null");
        return this;
    }

    ClprUpdateLedgerConfigurationTransactionBody.Builder build() {
        var configuration = ClprLedgerConfiguration.newBuilder();
        if (throttles != null) {
            configuration.setThrottles(throttles.toProtobuf());
        }
        return ClprUpdateLedgerConfigurationTransactionBody.newBuilder().setConfiguration(configuration);
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprUpdateLedgerConfiguration();
        if (body.getConfiguration().hasThrottles()) {
            throttles = ClprThrottles.fromProtobuf(body.getConfiguration().getThrottles());
        }
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("throttles", throttles);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprUpdateLedgerConfiguration(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getUpdateLedgerConfigurationMethod();
    }
}
