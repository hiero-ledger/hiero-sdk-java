// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.tck.methods.sdk.response.token.TokenInfoResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenInfoResponseTest {

    @Test
    void shouldSerializeTokenInfoResponseWithAllFields() {
        var response = new TokenInfoResponse(
                "0.0.1",
                "Token Name",
                "TKN",
                2,
                "1000",
                "0.0.2",
                "adminKey",
                "kycKey",
                "freezeKey",
                "wipeKey",
                "supplyKey",
                "feeScheduleKey",
                true,
                true,
                false,
                "0.0.3",
                "7776000",
                "1787170588.538185104",
                "token memo",
                List.of(),
                TokenType.FUNGIBLE_COMMON,
                TokenSupplyType.FINITE,
                "1000000",
                "pauseKey",
                true,
                "metadata",
                "metadataKey",
                "256");

        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"name\":\"Token Name\""));
        Assertions.assertTrue(json.contains("\"symbol\":\"TKN\""));
        Assertions.assertTrue(json.contains("\"decimals\":2"));
        Assertions.assertTrue(json.contains("\"totalSupply\":\"1000\""));
        Assertions.assertTrue(json.contains("\"treasuryAccountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"adminKey\":\"adminKey\""));
        Assertions.assertTrue(json.contains("\"kycKey\":\"kycKey\""));
        Assertions.assertTrue(json.contains("\"freezeKey\":\"freezeKey\""));
        Assertions.assertTrue(json.contains("\"wipeKey\":\"wipeKey\""));
        Assertions.assertTrue(json.contains("\"supplyKey\":\"supplyKey\""));
        Assertions.assertTrue(json.contains("\"feeScheduleKey\":\"feeScheduleKey\""));
        Assertions.assertTrue(json.contains("\"defaultFreezeStatus\":true"));
        Assertions.assertTrue(json.contains("\"defaultKycStatus\":true"));
        Assertions.assertTrue(json.contains("\"isDeleted\":false"));
        Assertions.assertTrue(json.contains("\"autoRenewAccountId\":\"0.0.3\""));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":\"7776000\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"tokenMemo\":\"token memo\""));
        Assertions.assertTrue(json.contains("\"customFees\":[]"));
        Assertions.assertTrue(json.contains("\"tokenType\":\"FUNGIBLE_COMMON\""));
        Assertions.assertTrue(json.contains("\"supplyType\":\"FINITE\""));
        Assertions.assertTrue(json.contains("\"maxSupply\":\"1000000\""));
        Assertions.assertTrue(json.contains("\"pauseKey\":\"pauseKey\""));
        Assertions.assertTrue(json.contains("\"pauseStatus\":true"));
        Assertions.assertTrue(json.contains("\"metadata\":\"metadata\""));
        Assertions.assertTrue(json.contains("\"metadataKey\":\"metadataKey\""));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
    }

    @Test
    void shouldSerializeTokenInfoResponseNullFields() {
        var response = new TokenInfoResponse(
                null, null, null, 0, null, null, null, null, null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null, null, null, null, null, null);

        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":null"));
        Assertions.assertTrue(json.contains("\"name\":null"));
        Assertions.assertTrue(json.contains("\"symbol\":null"));
        Assertions.assertTrue(json.contains("\"decimals\":0"));
        Assertions.assertTrue(json.contains("\"totalSupply\":null"));
        Assertions.assertTrue(json.contains("\"treasuryAccountId\":null"));
        Assertions.assertTrue(json.contains("\"adminKey\":null"));
        Assertions.assertTrue(json.contains("\"kycKey\":null"));
        Assertions.assertTrue(json.contains("\"freezeKey\":null"));
        Assertions.assertTrue(json.contains("\"wipeKey\":null"));
        Assertions.assertTrue(json.contains("\"supplyKey\":null"));
        Assertions.assertTrue(json.contains("\"feeScheduleKey\":null"));
        Assertions.assertTrue(json.contains("\"defaultFreezeStatus\":null"));
        Assertions.assertTrue(json.contains("\"defaultKycStatus\":null"));
        Assertions.assertTrue(json.contains("\"isDeleted\":false"));
        Assertions.assertTrue(json.contains("\"autoRenewAccountId\":null"));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":null"));
        Assertions.assertTrue(json.contains("\"expirationTime\":null"));
        Assertions.assertTrue(json.contains("\"tokenMemo\":null"));
        Assertions.assertTrue(json.contains("\"customFees\":null"));
        Assertions.assertTrue(json.contains("\"tokenType\":null"));
        Assertions.assertTrue(json.contains("\"supplyType\":null"));
        Assertions.assertTrue(json.contains("\"maxSupply\":null"));
        Assertions.assertTrue(json.contains("\"pauseKey\":null"));
        Assertions.assertTrue(json.contains("\"pauseStatus\":null"));
        Assertions.assertTrue(json.contains("\"metadata\":null"));
        Assertions.assertTrue(json.contains("\"metadataKey\":null"));
        Assertions.assertTrue(json.contains("\"ledgerId\":null"));
    }
}
