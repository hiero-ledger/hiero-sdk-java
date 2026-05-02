// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.StatusRuntimeException;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.TransactionGetReceiptResponse;
import org.hiero.sdk.proto.TransactionReceipt;
import org.hiero.sdk.proto.TransactionResponse;

public class TestResponse {
    @Nullable
    public final TransactionResponse transactionResponse;

    @Nullable
    public final Response queryResponse;

    @Nullable
    public final StatusRuntimeException errorResponse;

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
