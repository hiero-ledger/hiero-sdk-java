// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.TransactionGetReceiptResponse;
import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.StatusRuntimeException;
import org.jspecify.annotations.Nullable;

public class TestResponse {
    public final @Nullable TransactionResponse transactionResponse;

    public final @Nullable Response queryResponse;

    public final @Nullable StatusRuntimeException errorResponse;

    private TestResponse(
            @Nullable TransactionResponse transactionResponse,
            @Nullable Response queryResponse,
            @Nullable StatusRuntimeException errorResponse) {
        this.transactionResponse = transactionResponse;
        this.queryResponse = queryResponse;
        this.errorResponse = errorResponse;
    }

    public static TestResponse transaction(Status status, Hbar cost) {
        return new TestResponse(buildTransactionResponse(status, cost), null, null);
    }

    public static TestResponse transaction(Status status) {
        return transaction(status, new Hbar(1));
    }

    public static TestResponse transactionOk(Hbar cost) {
        return transaction(Status.OK, cost);
    }

    public static TestResponse transactionOk() {
        return transactionOk(new Hbar(1));
    }

    public static TestResponse query(Response queryResponse) {
        return new TestResponse(null, queryResponse, null);
    }

    public static TestResponse receipt(Status status) {
        var response = Response.newBuilder()
                .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                        .setReceipt(TransactionReceipt.newBuilder()
                                .setStatus(status.code)
                                .build())
                        .build())
                .build();
        return new TestResponse(null, response, null);
    }

    public static TestResponse successfulReceipt() {
        return receipt(Status.SUCCESS);
    }

    public static TestResponse error(StatusRuntimeException exception) {
        return new TestResponse(null, null, exception);
    }

    public static TransactionResponse buildTransactionResponse(Status status, Hbar cost) {
        return TransactionResponse.newBuilder()
                .setNodeTransactionPrecheckCode(status.code)
                .setCost(cost.toTinybars())
                .build();
    }
}
