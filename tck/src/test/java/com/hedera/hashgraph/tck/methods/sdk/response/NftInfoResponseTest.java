// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.tck.methods.sdk.response.token.NftInfoResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NftInfoResponseTest {
    @Test
    void shouldSerializeNftInfoResponseWithAllFields() {
        var response = new NftInfoResponse("0.0.1", "0.0.2", "1787170588.538185104", "nft metadata", "256", "0.0.3");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"nftId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"accountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"creationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"metadata\":\"nft metadata\""));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
        Assertions.assertTrue(json.contains("\"spenderId\":\"0.0.3\""));
    }

    @Test
    void shouldSerializeNftInfoResponseNullFields() {
        var response = new NftInfoResponse(null, null, null, null, null, null);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"nftId\":null"));
        Assertions.assertTrue(json.contains("\"accountId\":null"));
        Assertions.assertTrue(json.contains("\"creationTime\":null"));
        Assertions.assertTrue(json.contains("\"metadata\":null"));
        Assertions.assertTrue(json.contains("\"ledgerId\":null"));
        Assertions.assertTrue(json.contains("\"spenderId\":null"));
    }
}
