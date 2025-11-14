// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.tck.methods.sdk.param.*;
import com.hedera.hashgraph.tck.methods.sdk.response.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SdkServiceTest {

    private final SdkService sdkService = new SdkService();

    @Test
    void testSetup() throws Exception {
        // Given
        var sessionId = "session-1";
        SetupParams params = new SetupParams(
                "0.0.2",
                "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137",
                Optional.of("127.0.0.1:50211"),
                Optional.of("0.0.3"),
                Optional.of("http://127.0.0.1:5551"),
                sessionId);

        // When
        SetupResponse response = sdkService.setup(params);

        // Then
        assertEquals("Successfully setup custom client.", response.getMessage());

        response = sdkService.reset(new BaseParams(sessionId));

        assertEquals("", response.getMessage());
        assertThrows(NullPointerException.class, () -> sdkService.getClient(sessionId));
    }

    @Test
    void testSetOperator() throws Exception {
        var initialKey = PrivateKey.generateED25519();
        var sessionId = "session-set-operator";
        SetupParams setupParams = new SetupParams(
                "0.0.2", initialKey.toString(), Optional.empty(), Optional.empty(), Optional.empty(), sessionId);

        sdkService.setup(setupParams);

        var newOperatorKey = PrivateKey.generateED25519();
        var newOperatorAccountId = AccountId.fromString("0.0.3");
        SetupParams setOperatorParams = new SetupParams(
                newOperatorAccountId.toString(),
                newOperatorKey.toString(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                sessionId);

        SetupResponse response = sdkService.setOperator(setOperatorParams);

        assertEquals("SUCCESS", response.getStatus());
        var client = sdkService.getClient(sessionId);
        assertEquals(newOperatorAccountId, client.getOperatorAccountId());
        assertEquals(newOperatorKey.getPublicKey(), client.getOperatorPublicKey());

        sdkService.reset(new BaseParams(sessionId));
    }

    @Test
    void testSetupFail() {
        // Given
        SetupParams params = new SetupParams(
                "operatorAccountId",
                "operatorPrivateKey",
                Optional.of("nodeIp"),
                Optional.of("3asdf"),
                Optional.of("127.0.0.1:50211"),
                "session-2");

        // then
        assertThrows(Exception.class, () -> sdkService.setup(params));
    }
}
