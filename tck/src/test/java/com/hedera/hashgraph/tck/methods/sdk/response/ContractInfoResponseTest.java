// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContractInfoResponseTest {
    @Test
    void shouldSerializeContractInfoQueryResponseWithAllFields() {
        var stakingInfo = new ContractResponse.ContractInfoQueryResponse.StakingInfoResponse(
                true, "1787170588.538185104", "100", "200", "0.0.3", "7");
        var response = new ContractResponse.ContractInfoQueryResponse(
                "0.0.1",
                "0.0.2",
                "0.0.3",
                "adminKey",
                "1787170588.538185104",
                "7776000",
                "0.0.4",
                "1024",
                "contract memo",
                "1000",
                false,
                "10",
                "256",
                stakingInfo);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"accountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"contractAccountId\":\"0.0.3\""));
        Assertions.assertTrue(json.contains("\"adminKey\":\"adminKey\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":\"7776000\""));
        Assertions.assertTrue(json.contains("\"autoRenewAccountId\":\"0.0.4\""));
        Assertions.assertTrue(json.contains("\"storage\":\"1024\""));
        Assertions.assertTrue(json.contains("\"contractMemo\":\"contract memo\""));
        Assertions.assertTrue(json.contains("\"balance\":\"1000\""));
        Assertions.assertTrue(json.contains("\"isDeleted\":false"));
        Assertions.assertTrue(json.contains("\"maxAutomaticTokenAssociations\":\"10\""));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
        Assertions.assertTrue(
                json.contains(
                        "\"stakingInfo\":{\"declineStakingReward\":true,\"stakePeriodStart\":\"1787170588.538185104\",\"pendingReward\":\"100\",\"stakedToMe\":\"200\",\"stakedAccountId\":\"0.0.3\",\"stakedNodeId\":\"7\"}"));
    }

    @Test
    void shouldSerializeContractInfoQueryResponseNullFields() {
        var response = new ContractResponse.ContractInfoQueryResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":null"));
        Assertions.assertTrue(json.contains("\"accountId\":null"));
        Assertions.assertTrue(json.contains("\"contractAccountId\":null"));
        Assertions.assertTrue(json.contains("\"adminKey\":null"));
        Assertions.assertTrue(json.contains("\"expirationTime\":null"));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":null"));
        Assertions.assertTrue(json.contains("\"autoRenewAccountId\":null"));
        Assertions.assertTrue(json.contains("\"storage\":null"));
        Assertions.assertTrue(json.contains("\"contractMemo\":null"));
        Assertions.assertTrue(json.contains("\"balance\":null"));
        Assertions.assertTrue(json.contains("\"isDeleted\":null"));
        Assertions.assertTrue(json.contains("\"maxAutomaticTokenAssociations\":null"));
        Assertions.assertTrue(json.contains("\"ledgerId\":null"));
        Assertions.assertTrue(json.contains("\"stakingInfo\":null"));
    }
}
