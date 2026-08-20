// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GetAccountInfoResponseTest {
    @Test
    void shouldSerializeGetAccountInfoResponseWithAllFields() {
        var response = new GetAccountInfoResponse(
                "0.0.1",
                "0.0.2",
                true,
                "0.0.3",
                "10",
                "key",
                "100",
                "1",
                "1",
                false,
                "1787170588.538185104",
                "1787170588.538185105",
                List.of(new GetAccountInfoResponse.LiveHashResponse(
                        "0.0.1", "hash", List.of("key1", "key2"), "1787170588.538185104")),
                Map.of(
                        "0.0.1",
                        new GetAccountInfoResponse.TokenRelationshipInfo("0.0.1", "FT", "100", true, true, true)),
                "Test Account",
                "0",
                "-1",
                "aliasKey",
                "256",
                List.of(new GetAccountInfoResponse.HbarAllowanceResponse("0.0.1", "0.0.2", "10")),
                List.of(new GetAccountInfoResponse.TokenAllowanceResponse("0.0.1", "0.0.2", "0.0.3", "100")),
                List.of(new GetAccountInfoResponse.TokenNftAllowanceResponse(
                        "0.0.1", "0.0.2", "0.0.3", List.of("1", "2"), true, "0.0.100")),
                "0",
                new GetAccountInfoResponse.StakingInfoResponse(false, "1787170588.538185104", "0", "0", "0.0.1", "1"));
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"accountId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"contractAccountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"proxyAccountId\":\"0.0.3\""));
        Assertions.assertTrue(json.contains("\"proxyReceived\":\"10\""));
        Assertions.assertTrue(json.contains("\"key\":\"key\""));
        Assertions.assertTrue(json.contains("\"balance\":\"100\""));
        Assertions.assertTrue(json.contains("\"sendRecordThreshold\":\"1\""));
        Assertions.assertTrue(json.contains("\"receiveRecordThreshold\":\"1\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":\"1787170588.538185105\""));
        Assertions.assertTrue(
                json.contains(
                        "\"liveHashes\":[{\"accountId\":\"0.0.1\",\"hash\":\"hash\",\"keys\":[\"key1\",\"key2\"],\"duration\":\"1787170588.538185104\"}]"));
        Assertions.assertTrue(
                json.contains(
                        "\"tokenRelationships\":{\"0.0.1\":{\"tokenId\":\"0.0.1\",\"symbol\":\"FT\",\"balance\":\"100\",\"isKycGranted\":true,\"isFrozen\":true,\"automaticAssociation\":true}}"));
        Assertions.assertTrue(json.contains("\"accountMemo\":\"Test Account\""));
        Assertions.assertTrue(json.contains("\"ownedNfts\":\"0"));
        Assertions.assertTrue(json.contains("\"maxAutomaticTokenAssociations\":\"-1\""));
        Assertions.assertTrue(json.contains("aliasKey\":\"aliasKey\""));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
        Assertions.assertTrue(
                json.contains(
                        "\"hbarAllowances\":[{\"ownerAccountId\":\"0.0.1\",\"spenderAccountId\":\"0.0.2\",\"amount\":\"10\"}]"));
        Assertions.assertTrue(
                json.contains(
                        "\"tokenAllowances\":[{\"tokenId\":\"0.0.1\",\"ownerAccountId\":\"0.0.2\",\"spenderAccountId\":\"0.0.3\",\"amount\":\"100\"}]"));
        Assertions.assertTrue(
                json.contains(
                        "\"nftAllowances\":[{\"tokenId\":\"0.0.1\",\"ownerAccountId\":\"0.0.2\",\"spenderAccountId\":\"0.0.3\",\"serialNumbers\":[\"1\",\"2\"],\"allSerials\":true,\"delegatingSpender\":\"0.0.100\"}]"));
        Assertions.assertTrue(json.contains("\"ethereumNonce\":\"0\""));
        Assertions.assertTrue(
                json.contains(
                        "\"stakingInfo\":{\"declineStakingReward\":false,\"stakePeriodStart\":\"1787170588.538185104\",\"pendingReward\":\"0\",\"stakedToMe\":\"0\",\"stakedAccountId\":\"0.0.1\",\"stakedNodeId\":\"1\"}"));
        Assertions.assertTrue(json.contains("\"isDeleted\":true"));
        Assertions.assertTrue(json.contains("\"isReceiverSignatureRequired\":false"));
    }

    @Test
    void shouldSerializeGetAccountInfoResponseNullFields() {
        var response = new GetAccountInfoResponse(
                null, null, false, null, null, null, null, null, null, false, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"accountId\":null"));
        Assertions.assertTrue(json.contains("\"contractAccountId\":null"));
        Assertions.assertTrue(json.contains("\"proxyAccountId\":null"));
        Assertions.assertTrue(json.contains("\"proxyReceived\":"));
        Assertions.assertTrue(json.contains("\"key\":null"));
        Assertions.assertTrue(json.contains("\"balance\":null"));
        Assertions.assertTrue(json.contains("\"sendRecordThreshold\":null"));
        Assertions.assertTrue(json.contains("\"receiveRecordThreshold\":null"));
        Assertions.assertTrue(json.contains("\"expirationTime\":null"));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":null"));
        Assertions.assertTrue(json.contains("\"liveHashes\":null"));
        Assertions.assertTrue(json.contains("\"tokenRelationships\":null"));
        Assertions.assertTrue(json.contains("\"accountMemo\":null"));
        Assertions.assertTrue(json.contains("\"ownedNfts\":null"));
        Assertions.assertTrue(json.contains("\"maxAutomaticTokenAssociations\":null"));
        Assertions.assertTrue(json.contains("aliasKey\":null"));
        Assertions.assertTrue(json.contains("\"ledgerId\":null"));
        Assertions.assertTrue(json.contains("\"hbarAllowances\":null"));
        Assertions.assertTrue(json.contains("\"tokenAllowances\":null"));
        Assertions.assertTrue(json.contains("\"nftAllowances\":null"));
        Assertions.assertTrue(json.contains("\"ethereumNonce\":null"));
        Assertions.assertTrue(json.contains("\"stakingInfo\":null"));
        Assertions.assertTrue(json.contains("\"isDeleted\":false"));
        Assertions.assertTrue(json.contains("\"isReceiverSignatureRequired\":false"));
    }
}
