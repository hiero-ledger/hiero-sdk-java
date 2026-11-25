// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.ClprSignatureScheme;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2RequestException;
import com.hedera.hashgraph.tck.methods.JSONRPC2ParamRegistry;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprCompleteChannelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprCompleteConnectorParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprRegisterChannelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprUpdateLedgerConfigurationParams;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClprServiceTest {
    private static final String CHANNEL_ID = "00".repeat(32);
    private final ClprService service = new ClprService(new SdkService());

    @Test
    void decodesHexWithoutPrefix() throws Exception {
        assertArrayEquals(new byte[] {0x0a, (byte) 0xff}, ClprService.decode("0aff"));
        assertArrayEquals(new byte[0], ClprService.decode(""));
        assertThrows(InvalidJSONRPC2RequestException.class, () -> ClprService.decode("0x0a"));
        assertThrows(InvalidJSONRPC2RequestException.class, () -> ClprService.decode("abc"));
    }

    @Test
    void parsesUint64DecimalStrings() throws Exception {
        assertEquals(0L, ClprService.uint64("0", "v"));
        assertEquals("18446744073709551615", Long.toUnsignedString(ClprService.uint64("18446744073709551615", "v")));
        var tooLarge = assertThrows(
                InvalidJSONRPC2RequestException.class, () -> ClprService.uint64("18446744073709551616", "v"));
        assertEquals("v must fit in 64 unsigned bits", tooLarge.getMessage());
        var negative = assertThrows(InvalidJSONRPC2RequestException.class, () -> ClprService.uint64("-1", "v"));
        assertEquals("v must be an unsigned decimal string", negative.getMessage());
    }

    @Test
    void encodesSchemeNames() {
        assertEquals("ECDSA_SECP256K1", ClprService.scheme(ClprSignatureScheme.ECDSA_SECP256K1));
        assertNull(ClprService.scheme(null));
        assertNull(ClprService.encode(null));
    }

    @Test
    void completeChannelRequiresOwnershipPrivateKey() {
        var params = new ClprCompleteChannelParams(
                Optional.of(CHANNEL_ID),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "session");
        var error = assertThrows(InvalidJSONRPC2RequestException.class, () -> service.completeClprChannel(params));
        assertEquals("ownershipPrivateKey must be a private key string", error.getMessage());
    }

    @Test
    void completeConnectorRejectsPublicOwnershipKey() {
        var publicKey = PrivateKey.generateED25519().getPublicKey().toStringDER();
        var params = new ClprCompleteConnectorParams(
                Optional.of(CHANNEL_ID),
                Optional.of(CHANNEL_ID),
                Optional.of(CHANNEL_ID),
                Optional.of(publicKey),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "session");
        assertThrows(InvalidJSONRPC2RequestException.class, () -> service.completeClprConnector(params));
    }

    @Test
    void completeConnectorRejectsStakeAboveSignedRange() {
        var params = new ClprCompleteConnectorParams(
                Optional.of(CHANNEL_ID),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("18446744073709551615"),
                Optional.empty(),
                "session");
        var error = assertThrows(InvalidJSONRPC2RequestException.class, () -> service.completeClprConnector(params));
        assertEquals("lockedStake must be a valid tinybar amount", error.getMessage());
    }

    @Test
    void parsesParamsThroughRegistry() throws Exception {
        var register = (ClprRegisterChannelParams) JSONRPC2ParamRegistry.parse(
                ClprRegisterChannelParams.class, Map.of("ownershipCommitment", CHANNEL_ID, "sessionId", "s"));
        assertEquals(Optional.of(CHANNEL_ID), register.ownershipCommitment());

        var query = (ClprQueryParams) JSONRPC2ParamRegistry.parse(
                ClprQueryParams.class, Map.of("queryPayment", "100", "maxQueryPayment", "200", "sessionId", "s"));
        assertEquals(Optional.of("100"), query.queryPayment());
        assertEquals(Optional.of("200"), query.maxQueryPayment());

        var update = (ClprUpdateLedgerConfigurationParams) JSONRPC2ParamRegistry.parse(
                ClprUpdateLedgerConfigurationParams.class,
                Map.of(
                        "throttles",
                        Map.of(
                                "maxMessagesPerBundle", 10,
                                "maxMessagePayloadBytes", 4096L,
                                "maxGasPerMessage", "18446744073709551615",
                                "maxQueueDepth", 100,
                                "maxSyncBytes", "1048576",
                                "maxLocalEndpoints", 0,
                                "maxPeerEndpoints", 5),
                        "sessionId",
                        "s"));
        var throttles = update.throttles().orElseThrow();
        assertEquals(4096L, throttles.maxMessagePayloadBytes());
        assertEquals("18446744073709551615", throttles.maxGasPerMessage());
    }

    @Test
    void rejectsNonStringUint64Throttle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JSONRPC2ParamRegistry.parse(
                        ClprUpdateLedgerConfigurationParams.class,
                        Map.of(
                                "throttles",
                                Map.of(
                                        "maxMessagesPerBundle", 10,
                                        "maxMessagePayloadBytes", 4096,
                                        "maxGasPerMessage", 5,
                                        "maxQueueDepth", 100,
                                        "maxSyncBytes", "1048576",
                                        "maxLocalEndpoints", 0,
                                        "maxPeerEndpoints", 5),
                                "sessionId",
                                "s")));
    }
}
