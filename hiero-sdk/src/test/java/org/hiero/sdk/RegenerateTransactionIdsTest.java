// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.Status;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.hiero.sdk.proto.ResponseCodeEnum;
import org.hiero.sdk.proto.SignedTransaction;
import org.hiero.sdk.proto.Transaction;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;
import org.junit.jupiter.api.Test;

public class RegenerateTransactionIdsTest {
    @Test
    void regeneratesTransactionIdsWhenTransactionExpiredIsReturned()
            throws PrecheckStatusException, TimeoutException, InterruptedException {
        var transactionIds = new HashSet<TransactionId>();
        AtomicInteger count = new AtomicInteger(0);

        var responses = List.of(
                TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.TRANSACTION_EXPIRED)
                        .build(),
                TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.TRANSACTION_EXPIRED)
                        .build(),
                TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.TRANSACTION_EXPIRED)
                        .build(),
                TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                        .build());

        var call = (Function<Object, Object>) o -> {
            try {
                var transaction = (Transaction) o;
                var signedTransaction = SignedTransaction.parseFrom(transaction.getSignedTransactionBytes());
                var transactionBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
                var transactionId = TransactionId.fromProtobuf(transactionBody.getTransactionID());

                if (transactionIds.contains(transactionId)) {
                    return Status.Code.ABORTED.toStatus().asRuntimeException();
                }

                transactionIds.add(transactionId);

                return responses.get(count.getAndIncrement());
            } catch (Throwable e) {
                return new RuntimeException(e);
            }
        };

        List<Object> responses1 = List.of(call, call, call, call);

        try (var mocker = Mocker.withResponses(List.of(responses1))) {
            new FileCreateTransaction().execute(mocker.client);
        }
    }
}
