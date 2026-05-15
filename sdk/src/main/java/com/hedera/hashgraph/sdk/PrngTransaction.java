// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.UtilPrngTransactionBody;
import com.hedera.hashgraph.sdk.proto.UtilServiceGrpc;
import io.grpc.MethodDescriptor;
import javax.annotation.Nullable;

/**
 * Random Number Generator Transaction.
 */
public class PrngTransaction extends Transaction<PrngTransaction> {

    /**
     * If provided and is positive, returns a 32-bit pseudorandom number from the given range in the transaction record.
     * If not set or set to zero, will return a 384-bit pseudorandom data in the record.
     */
    @Nullable
    private Integer range = null;

    /**
     * Constructor.
     */
    public PrngTransaction() {}

    /**
     * Assign the range.
     *
     * @param range                     if > 0 32 bit else 384 bit
     * @return {@code this}
     */
    public PrngTransaction setRange(Integer range) {
        this.range = range;
        return this;
    }

    /**
     * Retrieve the range.
     *
     * @return                          the range
     */
    public Integer getRange() {
        return range;
    }

    UtilPrngTransactionBody.Builder build() {
        var builder = UtilPrngTransactionBody.newBuilder();

        if (range != null) {
            builder.setRange(range);
        }

        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setUtilPrng(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("cannot schedule RngTransaction");
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {}

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return UtilServiceGrpc.getPrngMethod();
    }
}
