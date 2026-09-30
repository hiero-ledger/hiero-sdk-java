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

        AccountCreateParams params = AccountCreateParams.parse(jrpcParams);

        assertEquals(Optional.of("someKey"), params.key());
        assertEquals(Optional.of("1000"), params.initialBalance());
        assertEquals(Optional.of(true), params.receiverSignatureRequired());
        assertEquals(Optional.of("7890000"), params.autoRenewPeriod());
        assertEquals(Optional.of("test memo"), params.memo());
        assertEquals(Optional.of(10L), params.maxAutoTokenAssociations());
        assertEquals(Optional.of("stakedAccountId"), params.stakedAccountId());
        assertEquals(Optional.of("5"), params.stakedNodeId());
        assertEquals(Optional.of(true), params.declineStakingReward());
        assertEquals(Optional.of("alias"), params.alias());

        assertTrue(params.commonTransactionParams().isPresent());
        CommonTransactionParams commonParams = params.commonTransactionParams().get();
        assertEquals(Optional.of("txId"), commonParams.transactionId());
        assertEquals(Optional.of(100L), commonParams.maxTransactionFee());
        assertEquals(Optional.of(120L), commonParams.validTransactionDuration());
        assertEquals(Optional.of("commonMemo"), commonParams.memo());
        assertEquals(Optional.of(true), commonParams.regenerateTransactionId());
        assertEquals(
                Optional.of(List.of(
                        "302e020100300506032b657004220420c1ed50ed4b024f5df25992d1fc4b8c5b4e3c3db63a5ff5fa05857f5b4b90f3bc",
                        "test")),
                commonParams.signers());
    }

    @Test
    void testParseWithOptionalFieldsAbsent() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("key", "someKey");
        jrpcParams.put("sessionId", "session-optional");

        AccountCreateParams params = AccountCreateParams.parse(jrpcParams);

        assertEquals(Optional.of("someKey"), params.key());
        assertEquals(Optional.empty(), params.initialBalance());
        assertEquals(Optional.empty(), params.receiverSignatureRequired());
        assertEquals(Optional.empty(), params.autoRenewPeriod());
        assertEquals(Optional.empty(), params.memo());
        assertEquals(Optional.empty(), params.maxAutoTokenAssociations());
        assertEquals(Optional.empty(), params.stakedAccountId());
        assertEquals(Optional.empty(), params.stakedNodeId());
        assertEquals(Optional.empty(), params.declineStakingReward());
        assertEquals(Optional.empty(), params.alias());
        assertEquals(Optional.empty(), params.commonTransactionParams());
    }

    @Test
    void testParseWithInvalidFieldTypes() {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("key", 123); // Invalid type
        jrpcParams.put("sessionId", "session-invalid");

        assertThrows(ClassCastException.class, () -> {
            AccountCreateParams.parse(jrpcParams);
        });
    }

    @Test
    void testParseWithEmptyParams() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("sessionId", "session-empty");

        AccountCreateParams params = AccountCreateParams.parse(jrpcParams);

        assertEquals(Optional.empty(), params.key());
        assertEquals(Optional.empty(), params.initialBalance());
        assertEquals(Optional.empty(), params.receiverSignatureRequired());
        assertEquals(Optional.empty(), params.autoRenewPeriod());
        assertEquals(Optional.empty(), params.memo());
        assertEquals(Optional.empty(), params.maxAutoTokenAssociations());
        assertEquals(Optional.empty(), params.stakedAccountId());
        assertEquals(Optional.empty(), params.stakedNodeId());
        assertEquals(Optional.empty(), params.declineStakingReward());
        assertEquals(Optional.empty(), params.alias());
        assertEquals(Optional.empty(), params.commonTransactionParams());
    }
}
