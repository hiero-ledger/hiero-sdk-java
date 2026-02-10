// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.proto.HederaFunctionality;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RequestTypeTest {

    @Test
    void valueOf() {
        var codeValues = HederaFunctionality.values();
        // Exclude the last HederaFunctionality value (HookDispatch) as it's tested separately
        var codesToTest = IntStream.range(0, codeValues.length - 1)
                .mapToObj(i -> codeValues[i])
                .collect(Collectors.toList());

        codesToTest.forEach((code) -> {
            var mappedRequestType = RequestType.valueOf(code);
            // Verify the mapping is correct by checking the code field
            assertThat(mappedRequestType.code).isEqualTo(code);
            // Verify toString returns a non-empty string
            assertThat(mappedRequestType.toString()).isNotEmpty();
        });
    }

    @Test
    void valueOfMapsNewFunctions() {
        assertThat(RequestType.valueOf(HederaFunctionality.AtomicBatch)).isEqualTo(RequestType.ATOMIC_BATCH);
        assertThat(RequestType.valueOf(HederaFunctionality.LambdaSStore)).isEqualTo(RequestType.LAMBDA_S_STORE);
        assertThat(RequestType.valueOf(HederaFunctionality.HookStore)).isEqualTo(RequestType.HOOK_STORE);
        assertThat(RequestType.valueOf(HederaFunctionality.HookDispatch)).isEqualTo(RequestType.HOOK_DISPATCH);
    }

    @Test
    void toStringStableForNewEntries() {
        assertThat(RequestType.ATOMIC_BATCH.toString()).isEqualTo("ATOMIC_BATCH");
        assertThat(RequestType.LAMBDA_S_STORE.toString()).isEqualTo("LAMBDA_S_STORE");
        assertThat(RequestType.HOOK_STORE.toString()).isEqualTo("HOOK_STORE");
        assertThat(RequestType.HOOK_DISPATCH.toString()).isEqualTo("HOOK_DISPATCH");
    }

    @Test
    void roundTripNewEntries() {
        var pairs = new Object[][] {
            {HederaFunctionality.AtomicBatch, RequestType.ATOMIC_BATCH},
            {HederaFunctionality.LambdaSStore, RequestType.LAMBDA_S_STORE},
            {HederaFunctionality.HookStore, RequestType.HOOK_STORE},
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
