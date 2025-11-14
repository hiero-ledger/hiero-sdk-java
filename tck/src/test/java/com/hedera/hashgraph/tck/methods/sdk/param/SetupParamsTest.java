// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SetupParamsTest {

    @Test
    void testParse() {
        // Given
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("operatorAccountId", "testAccountId");
        jrpcParams.put("operatorPrivateKey", "testPrivateKey");
        jrpcParams.put("nodeIp", "testNodeIp");
        jrpcParams.put("nodeAccountId", "testNodeAccountId");
        jrpcParams.put("mirrorNetworkIp", "testMirrorNetworkIp");
        jrpcParams.put("sessionId", "session-setup");

        // When
        SetupParams result = new SetupParams().parse(jrpcParams);

        // Then
        assertEquals("testAccountId", result.getOperatorAccountId());
        assertEquals("testPrivateKey", result.getOperatorPrivateKey());
        assertEquals(Optional.of("testNodeIp"), result.getNodeIp());
        assertEquals(Optional.of("testNodeAccountId"), result.getNodeAccountId());
        assertEquals(Optional.of("testMirrorNetworkIp"), result.getMirrorNetworkIp());
    }
}
