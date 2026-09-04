// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import static java.util.Map.entry;

import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2ParamsException;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountCreateParams;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JSONRPC2ParamsRegistryTest {
    @Test
    void shouldParseValidJsonRpcRequestParams() {
        var requestParams = Map.<String, Object>ofEntries(
                entry(
                        "key",
                        "3030020100300706052b8104000a04220420f3b15203915311707e26650764f8bb654bd30783fb144011ec8ccd4444a63a15"),
                entry("initialBalance", "100"),
                entry("receiverSignatureRequired", true),
                entry("autoRenewPeriod", "1787170588.538185104"),
                entry("memo", "Test"),
                entry("maxAutoTokenAssociations", 1L),
                entry("stakedAccountId", "0.0.2"),
                entry("stakedNodeId", "1"),
                entry("declineStakingReward", true),
                entry("alias", "0x00000000000000000000000000"),
                entry("sessionId", "101"));

        var result = Assertions.assertDoesNotThrow(
                () -> JSONRPC2ParamRegistry.parse(AccountCreateParams.class, requestParams));
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(AccountCreateParams.class, result);
    }

    @Test
    void shouldThrowExceptionForInvalidJsonRpcRequestParams() {
        var requestParams = Map.<String, Object>ofEntries(
                entry("initialBalance", "100"),
                entry("receiverSignatureRequired", "true"),
                entry("autoRenewPeriod", "1787170588.538185104"),
                entry("memo", 9L),
                entry("maxAutoTokenAssociations", 1),
                entry("stakedAccountId", "0.0.2"));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> JSONRPC2ParamRegistry.parse(AccountCreateParams.class, requestParams));
    }

    @Test
    void shouldThrowExceptionIfJsonParamsClassNotRegister() {
        var requestParams = Map.<String, Object>of("id", "101");
        Assertions.assertThrows(
                InvalidJSONRPC2ParamsException.class,
                () -> JSONRPC2ParamRegistry.parse(MockParam.class, requestParams));
    }

    /**
     * Helper class for test
     */
    private static class MockParam {
        String id;

        public MockParam(String id) {
            this.id = id;
        }
    }
}
