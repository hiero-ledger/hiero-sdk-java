// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.Query;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.Transaction;
import org.hiero.sdk.proto.TransactionResponse;

public interface TestService {

    private static <ResponseTypeT> void respond(
            StreamObserver<ResponseTypeT> streamObserver,
            @Nullable ResponseTypeT normalResponse,
            @Nullable StatusRuntimeException errorResponse,
            String exceptionString) {
        if (normalResponse != null) {
            streamObserver.onNext(normalResponse);
            streamObserver.onCompleted();
        } else if (errorResponse != null) {
            streamObserver.onError(errorResponse);
        } else {
            throw new IllegalStateException(exceptionString);
        }
    }

    Buffer getBuffer();

    default void respondToTransaction(
            Transaction request, StreamObserver<TransactionResponse> streamObserver, TestResponse response) {
        getBuffer().transactionRequestsReceived.add(request);

        var exceptionString = "TestService tried to respond to transaction with query response";
        respond(streamObserver, response.transactionResponse, response.errorResponse, exceptionString);
    }

    default void respondToQuery(Query request, StreamObserver<Response> streamObserver, TestResponse response) {
        getBuffer().queryRequestsReceived.add(request);

        var exceptionString = "TestService tried to respond to query with transaction response";
        respond(streamObserver, response.queryResponse, response.errorResponse, exceptionString);
    }

    default void respondToTransactionFromQueue(
            Transaction request, StreamObserver<TransactionResponse> streamObserver) {
        respondToTransaction(
                request, streamObserver, getBuffer().responsesToSend.remove());
    }

    default void respondToQueryFromQueue(Query request, StreamObserver<Response> streamObserver) {
        respondToQuery(request, streamObserver, getBuffer().responsesToSend.remove());
    }

    class Buffer {
        public final List<Transaction> transactionRequestsReceived = new ArrayList<>();
        public final List<Query> queryRequestsReceived = new ArrayList<>();
        public final Queue<TestResponse> responsesToSend = new ArrayDeque<>();

        public Buffer enqueueResponse(TestResponse response) {
            responsesToSend.add(response);
            return this;
        }
    }
}
