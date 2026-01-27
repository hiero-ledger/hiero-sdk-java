// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OfflineSignaturesExpiredRetryTest {
    @Test
    void doesNotRetryOnTransactionExpiredWhenNoSignersAvailable()
            throws TimeoutException, InterruptedException, com.google.protobuf.InvalidProtocolBufferException {
        var payer = new AccountId(0, 0, 1234);
        var offlineKey = PrivateKey.generateED25519();

        // Create a signed transaction whose payer is NOT the client's operator,
        // so the SDK has no signer available at execution time.
        var signed = new AccountCreateTransaction()
                .setTransactionId(TransactionId.generate(payer))
                .setNodeAccountIds(List.of(new AccountId(0, 0, 3)))
                .freeze()
                .sign(offlineKey);

        var tx = Transaction.fromBytes(signed.toBytes());

        var callCount = new AtomicInteger(0);
        var sigPairCounts = new ArrayList<Integer>();
        var txIds = new HashSet<TransactionId>();

        // First response is TRANSACTION_EXPIRED. If the SDK retries by regenerating the transaction ID,
        // it will invalidate existing offline signatures and (without signers) would submit an unsigned transaction.
        // We simulate that by returning INVALID_SIGNATURE on the second request.
        var handler = (Function<Object, Object>) o -> {
            try {
                var req = (com.hedera.hashgraph.sdk.proto.Transaction) o;
                var signedTx = SignedTransaction.parseFrom(req.getSignedTransactionBytes());
                var txBody = TransactionBody.parseFrom(signedTx.getBodyBytes());
                txIds.add(TransactionId.fromProtobuf(txBody.getTransactionID()));
                sigPairCounts.add(signedTx.getSigMap().getSigPairCount());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (callCount.getAndIncrement() == 0) {
                return TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.TRANSACTION_EXPIRED)
                        .build();
            }

            return TransactionResponse.newBuilder()
                    .setNodeTransactionPrecheckCode(ResponseCodeEnum.INVALID_SIGNATURE)
                    .build();
        };

        try (var mocker = Mocker.withResponses(List.of(List.of(handler, handler)))) {
            var ex = assertThrows(PrecheckStatusException.class, () -> tx.execute(mocker.client));
            assertEquals(Status.TRANSACTION_EXPIRED, ex.status);

            // No retry should have occurred.
            assertEquals(1, callCount.get());
            assertEquals(1, txIds.size());

            // And the one request that was sent should have contained the offline signature(s).
            assertEquals(1, sigPairCounts.size());
            assertTrue(sigPairCounts.get(0) > 0);
        }
    }
}
