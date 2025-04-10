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
}
