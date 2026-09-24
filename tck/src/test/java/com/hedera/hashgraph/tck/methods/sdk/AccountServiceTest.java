// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.sdk.PrivateKey;
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
        // Given a client whose only node refuses connections, so every operation fails fast
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
                // Only proves the error plumbing (the refused connection): Java has no Stage 2 error yet (#2851)
                assertNotNull(response.executionError());
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
}
