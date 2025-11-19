// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.hashgraph.tck.methods.sdk.param.account.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;

class AccountCreateParamsTest {

    @Test
    void testParseWithAllFields() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("key", "someKey");
        jrpcParams.put("initialBalance", "1000");
        jrpcParams.put("receiverSignatureRequired", true);
        jrpcParams.put("autoRenewPeriod", "7890000");
        jrpcParams.put("memo", "test memo");
        jrpcParams.put("maxAutoTokenAssociations", 10L);
        jrpcParams.put("stakedAccountId", "stakedAccountId");
        jrpcParams.put("stakedNodeId", "5");
        jrpcParams.put("declineStakingReward", true);
        jrpcParams.put("alias", "alias");
        jrpcParams.put("sessionId", "session-all");

        JSONObject commonParamsJson = new JSONObject();
        commonParamsJson.put("transactionId", "txId");
        commonParamsJson.put("maxTransactionFee", 100L);
        commonParamsJson.put("validTransactionDuration", 120L);
        commonParamsJson.put("memo", "commonMemo");
        commonParamsJson.put("regenerateTransactionId", true);
        JSONArray signersArray = new JSONArray();
        signersArray.add(
                "302e020100300506032b657004220420c1ed50ed4b024f5df25992d1fc4b8c5b4e3c3db63a5ff5fa05857f5b4b90f3bc");
        signersArray.add("test");
        commonParamsJson.put("signers", signersArray);

        jrpcParams.put("commonTransactionParams", commonParamsJson);

        AccountCreateParams params = new AccountCreateParams().parse(jrpcParams);

        assertEquals(Optional.of("someKey"), params.getKey());
        assertEquals(Optional.of("1000"), params.getInitialBalance());
        assertEquals(Optional.of(true), params.getReceiverSignatureRequired());
        assertEquals(Optional.of("7890000"), params.getAutoRenewPeriod());
        assertEquals(Optional.of("test memo"), params.getMemo());
        assertEquals(Optional.of(10L), params.getMaxAutoTokenAssociations());
        assertEquals(Optional.of("stakedAccountId"), params.getStakedAccountId());
        assertEquals(Optional.of("5"), params.getStakedNodeId());
        assertEquals(Optional.of(true), params.getDeclineStakingReward());
        assertEquals(Optional.of("alias"), params.getAlias());

        assertTrue(params.getCommonTransactionParams().isPresent());
        CommonTransactionParams commonParams =
                params.getCommonTransactionParams().get();
        assertEquals(Optional.of("txId"), commonParams.getTransactionId());
        assertEquals(Optional.of(100L), commonParams.getMaxTransactionFee());
        assertEquals(Optional.of(120L), commonParams.getValidTransactionDuration());
        assertEquals(Optional.of("commonMemo"), commonParams.getMemo());
        assertEquals(Optional.of(true), commonParams.getRegenerateTransactionId());
        assertEquals(
                Optional.of(List.of(
                        "302e020100300506032b657004220420c1ed50ed4b024f5df25992d1fc4b8c5b4e3c3db63a5ff5fa05857f5b4b90f3bc",
                        "test")),
                commonParams.getSigners());
    }

    @Test
    void testParseWithOptionalFieldsAbsent() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("key", "someKey");
        jrpcParams.put("sessionId", "session-optional");

        AccountCreateParams params = new AccountCreateParams().parse(jrpcParams);

        assertEquals(Optional.of("someKey"), params.getKey());
        assertEquals(Optional.empty(), params.getInitialBalance());
        assertEquals(Optional.empty(), params.getReceiverSignatureRequired());
        assertEquals(Optional.empty(), params.getAutoRenewPeriod());
        assertEquals(Optional.empty(), params.getMemo());
        assertEquals(Optional.empty(), params.getMaxAutoTokenAssociations());
        assertEquals(Optional.empty(), params.getStakedAccountId());
        assertEquals(Optional.empty(), params.getStakedNodeId());
        assertEquals(Optional.empty(), params.getDeclineStakingReward());
        assertEquals(Optional.empty(), params.getAlias());
        assertEquals(Optional.empty(), params.getCommonTransactionParams());
    }

    @Test
    void testParseWithInvalidFieldTypes() {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("key", 123); // Invalid type
        jrpcParams.put("sessionId", "session-invalid");

        assertThrows(ClassCastException.class, () -> {
            new AccountCreateParams().parse(jrpcParams);
        });
    }

    @Test
    void testParseWithEmptyParams() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("sessionId", "session-empty");

        AccountCreateParams params = new AccountCreateParams().parse(jrpcParams);

        assertEquals(Optional.empty(), params.getKey());
        assertEquals(Optional.empty(), params.getInitialBalance());
        assertEquals(Optional.empty(), params.getReceiverSignatureRequired());
        assertEquals(Optional.empty(), params.getAutoRenewPeriod());
        assertEquals(Optional.empty(), params.getMemo());
        assertEquals(Optional.empty(), params.getMaxAutoTokenAssociations());
        assertEquals(Optional.empty(), params.getStakedAccountId());
        assertEquals(Optional.empty(), params.getStakedNodeId());
        assertEquals(Optional.empty(), params.getDeclineStakingReward());
        assertEquals(Optional.empty(), params.getAlias());
        assertEquals(Optional.empty(), params.getCommonTransactionParams());
    }
}
