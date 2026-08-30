// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.ContractId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContractCallResponseTest {
    @Test
    void shouldSerializeContractCallResponseWithAllFields() {
        var response = new ContractCallResponse(
                "0.0.1",
                ContractId.fromString("0.0.101"),
                "error",
                1L,
                List.of(),
                10L,
                "1th",
                "0.0.2",
                1L,
                "resultData");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains(
                "\"evmAddress\":{\"shard\":0,\"realm\":0,\"num\":101,\"checksum\":null,\"evmAddress\":null}"));
        Assertions.assertTrue(json.contains("\"errorMessage\":\"error\""));
        Assertions.assertTrue(json.contains("\"gasUsed\":1"));
        Assertions.assertTrue(json.contains("\"logs\":[]"));
        Assertions.assertTrue(json.contains("\"gas\":10"));
        Assertions.assertTrue(json.contains("\"hbarAmount\":\"1th\""));
        Assertions.assertTrue(json.contains("\"senderAccountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"signerNonce\":1"));
        Assertions.assertTrue(json.contains("\"rawResult\":\"resultData\""));
    }

    @Test
    void shouldSerializeContractCallResponseNullFields() {
        var response = new ContractCallResponse(null, null, null, 0L, null, 0L, null, null, 0L, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":null"));
        Assertions.assertTrue(json.contains("\"evmAddress\":null"));
        Assertions.assertTrue(json.contains("\"errorMessage\":null"));
        Assertions.assertTrue(json.contains("\"gasUsed\":0"));
        Assertions.assertTrue(json.contains("\"logs\":null"));
        Assertions.assertTrue(json.contains("\"gas\":0"));
        Assertions.assertTrue(json.contains("\"hbarAmount\":null"));
        Assertions.assertTrue(json.contains("\"senderAccountId\":null"));
        Assertions.assertTrue(json.contains("\"signerNonce\":0"));
        Assertions.assertTrue(json.contains("\"rawResult\":null"));
    }
}
