// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;

class CommonTransactionParamsTest {
    @Test
    void testCommonTransactionParamsParse() {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("transactionId", "txId");
        jrpcParams.put("maxTransactionFee", 100L);
        jrpcParams.put("validTransactionDuration", 120L);
        jrpcParams.put("memo", "commonMemo");
        jrpcParams.put("regenerateTransactionId", true);
        JSONArray signersArray = new JSONArray();
        signersArray.add(
                "302e020100300506032b657004220420c1ed50ed4b024f5df25992d1fc4b8c5b4e3c3db63a5ff5fa05857f5b4b90f3bc");
        jrpcParams.put("signers", signersArray);

        CommonTransactionParams params = CommonTransactionParams.parse(jrpcParams);

        assertEquals(Optional.of("txId"), params.transactionId());
        assertEquals(Optional.of(100L), params.maxTransactionFee());
        assertEquals(Optional.of(120L), params.validTransactionDuration());
        assertEquals(Optional.of("commonMemo"), params.memo());
        assertEquals(Optional.of(true), params.regenerateTransactionId());
        assertEquals(
                Optional.of(
                        List.of(
                                "302e020100300506032b657004220420c1ed50ed4b024f5df25992d1fc4b8c5b4e3c3db63a5ff5fa05857f5b4b90f3bc")),
                params.signers());
    }
}
