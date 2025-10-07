// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.proto.HederaFunctionality;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RequestTypeTest {

    @Test
    void valueOf() {
        var codeValues = HederaFunctionality.values();
        var requestTypeValues = RequestType.values();
        var pair = IntStream.range(0, codeValues.length - 1)
                .mapToObj(i -> Map.entry(codeValues[i], requestTypeValues[i]))
                .collect(Collectors.toList());

        pair.forEach((a) -> {
            var code = a.getKey();
            var requestType = a.getValue();
            assertThat(RequestType.valueOf(code)).hasToString(requestType.toString());
        });
    }

    @Test
    void valueOfMapsNewFunctions() {
        assertThat(RequestType.valueOf(HederaFunctionality.AtomicBatch)).isEqualTo(RequestType.ATOMIC_BATCH);
        assertThat(RequestType.valueOf(HederaFunctionality.LambdaSStore)).isEqualTo(RequestType.LAMBDA_S_STORE);
        assertThat(RequestType.valueOf(HederaFunctionality.HookDispatch)).isEqualTo(RequestType.HOOK_DISPATCH);
    }

    @Test
    void toStringStableForNewEntries() {
        assertThat(RequestType.ATOMIC_BATCH.toString()).isEqualTo("ATOMIC_BATCH");
        assertThat(RequestType.LAMBDA_S_STORE.toString()).isEqualTo("LAMBDA_S_STORE");
        assertThat(RequestType.HOOK_DISPATCH.toString()).isEqualTo("HOOK_DISPATCH");
    }

    @Test
    void roundTripNewEntries() {
        var pairs = new Object[][] {
            {HederaFunctionality.AtomicBatch, RequestType.ATOMIC_BATCH},
            {HederaFunctionality.LambdaSStore, RequestType.LAMBDA_S_STORE},
            {HederaFunctionality.HookDispatch, RequestType.HOOK_DISPATCH},
        };

        for (var pair : pairs) {
            var code = (HederaFunctionality) pair[0];
            var req = (RequestType) pair[1];
            assertThat(RequestType.valueOf(code)).isEqualTo(req);
            assertThat(req.code).isEqualTo(code);
        }
    }
}
