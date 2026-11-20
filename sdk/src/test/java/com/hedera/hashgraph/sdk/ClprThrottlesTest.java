// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClprThrottlesTest {
    private static final long MAX_UINT32 = 4_294_967_295L;

    @Test
    void acceptsValidLimits() {
        var throttles = ClprThrottles.of(1, 1, 1, 1, 65_537, 0, 0);
        assertThat(throttles.getMaxSyncBytes()).isEqualTo(65_537);
        assertThat(throttles.getMaxLocalEndpoints()).isZero();
    }

    @Test
    void acceptsUint32AndUint64Maximums() {
        var throttles = ClprThrottles.of(MAX_UINT32, MAX_UINT32, -1L, MAX_UINT32, -1L, MAX_UINT32, MAX_UINT32);
        var decoded = ClprThrottles.fromProtobuf(throttles.toProtobuf());
        assertThat(decoded).isEqualTo(throttles);
        assertThat(Long.toUnsignedString(decoded.getMaxGasPerMessage())).isEqualTo("18446744073709551615");
        assertThat(decoded.getMaxMessagesPerBundle()).isEqualTo(MAX_UINT32);
    }

    @Test
    void rejectsZeroForPositiveUint32Limits() {
        assertThatThrownBy(() -> ClprThrottles.of(0, 1, 1, 1, 65_537, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxMessagesPerBundle must be an integer from 1 to 4294967295");
        assertThatThrownBy(() -> ClprThrottles.of(1, 0, 1, 1, 65_537, 0, 0))
                .hasMessage("maxMessagePayloadBytes must be an integer from 1 to 4294967295");
        assertThatThrownBy(() -> ClprThrottles.of(1, 1, 1, 0, 65_537, 0, 0))
                .hasMessage("maxQueueDepth must be an integer from 1 to 4294967295");
    }

    @Test
    void rejectsOutOfRangeUint32Limits() {
        assertThatThrownBy(() -> ClprThrottles.of(MAX_UINT32 + 1, 1, 1, 1, 65_537, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClprThrottles.of(1, 1, 1, 1, 65_537, -1, 0))
                .hasMessage("maxLocalEndpoints must be an integer from 0 to 4294967295");
        assertThatThrownBy(() -> ClprThrottles.of(1, 1, 1, 1, 65_537, 0, MAX_UINT32 + 1))
                .hasMessage("maxPeerEndpoints must be an integer from 0 to 4294967295");
    }

    @Test
    void rejectsZeroUint64Limits() {
        assertThatThrownBy(() -> ClprThrottles.of(1, 1, 0, 1, 65_537, 0, 0))
                .hasMessage("maxGasPerMessage must be a positive uint64");
        assertThatThrownBy(() -> ClprThrottles.of(1, 1, 1, 1, 0, 0, 0))
                .hasMessage("maxSyncBytes must be a positive uint64");
    }

    @Test
    void requiresSyncCapacityForOneMessage() {
        assertThatThrownBy(() -> ClprThrottles.of(1, 1000, 1, 1, 66_535, 0, 0))
                .hasMessage("maxSyncBytes must be at least maxMessagePayloadBytes + 65536");
        assertThat(ClprThrottles.of(1, 1000, 1, 1, 66_536, 0, 0).getMaxSyncBytes())
                .isEqualTo(66_536);
    }

    @Test
    void decodingSkipsValidation() throws Exception {
        var decoded = ClprThrottles.fromBytes(new byte[0]);
        assertThat(decoded.getMaxMessagesPerBundle()).isZero();
        assertThat(decoded.getMaxSyncBytes()).isZero();
    }

    @Test
    void bytesRoundTrip() throws Exception {
        var throttles = ClprThrottles.of(10, 4096, 1_000_000, 100, 1 << 20, 0, 5);
        assertThat(ClprThrottles.fromBytes(throttles.toBytes())).isEqualTo(throttles);
        assertThat(throttles.toString()).contains("maxGasPerMessage=1000000");
    }
}
