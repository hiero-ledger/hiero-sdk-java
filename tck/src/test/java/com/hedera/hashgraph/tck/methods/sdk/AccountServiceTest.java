// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.tck.methods.sdk.param.BaseParams;
import com.hedera.hashgraph.tck.methods.sdk.param.SetupParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.DeprecatedAccountBalanceQueryParams;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final SdkService sdkService = new SdkService();
    private final AccountService accountService = new AccountService(sdkService);

    @Test
    void testExecuteDeprecatedAccountBalanceQueryCapturesWarningAndError() throws Exception {
        // The deprecated query must fail locally, even when its only node is unreachable.
        var sessionId = "session-deprecated-balance";
        sdkService.setup(new SetupParams(
                "0.0.2",
                PrivateKey.generateED25519().toString(),
                Optional.of("127.0.0.1:1"),
                Optional.of("0.0.3"),
                Optional.of("127.0.0.1:5600"),
                sessionId));
        sdkService.getClient(sessionId).setMaxAttempts(1).setRequestTimeout(Duration.ofSeconds(1));

        try {
            for (var operation : List.of("execute", "getCost")) {
                // When
                var response = accountService.executeDeprecatedAccountBalanceQuery(
                        new DeprecatedAccountBalanceQueryParams("0.0.3", operation, sessionId));

                // Then every construction is captured, not just the first
                assertTrue(response.constructionWarning().contains("AccountBalanceQuery is no longer supported"));
                assertEquals(response.constructionWarning(), response.executionError());
            }
        } finally {
            sdkService.reset(new BaseParams(sessionId));
        }
    }

    @Test
    void testDeprecatedAccountBalanceQueryParamsRejectUnknownOperation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DeprecatedAccountBalanceQueryParams.parse(
                        Map.of("sessionId", "session", "accountId", "0.0.3", "operation", "getInfo")));
    }

    @Test
    void transactionReceiptTopicRunningHashIsHexEncoded() throws Exception {
        // A running hash is 48 raw SHA-384 bytes, not text.
        byte[] runningHash = new byte[48];
        for (int i = 0; i < runningHash.length; i++) {
            runningHash[i] = (byte) (0xd0 + i);
        }
        var receipt = TransactionReceipt.fromBytes(com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                .setStatus(ResponseCodeEnum.SUCCESS)
                .setTopicSequenceNumber(1)
                .setTopicRunningHash(ByteString.copyFrom(runningHash))
                .build()
                .toByteArray());

        var response = AccountService.mapTransactionReceiptResponse(receipt);

        assertEquals(
                "d0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
                response.topicRunningHash());
    }

    @Test
    void transactionReceiptWithoutTopicRunningHashReturnsNull() throws Exception {
        var receipt = TransactionReceipt.fromBytes(com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                .setStatus(ResponseCodeEnum.SUCCESS)
                .build()
                .toByteArray());

        assertNull(AccountService.mapTransactionReceiptResponse(receipt).topicRunningHash());
    }
}
