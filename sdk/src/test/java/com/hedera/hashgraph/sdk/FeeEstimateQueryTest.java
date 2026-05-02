// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FeeEstimateQuery} covering default field values, validation,
 * response parsing, fee aggregation, and high-volume multiplier semantics.
 */
class FeeEstimateQueryTest {

    // -------------------------------------------------------------------------
    // Default values
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Default mode is null (resolved to INTRINSIC inside execute)")
    void defaultModeIsNull() {
        var query = new FeeEstimateQuery();
        assertThat(query.getMode()).isNull();
    }

    @Test
    @DisplayName("Default highVolumeThrottle is 0")
    void defaultHighVolumeThrottleIsZero() {
        var query = new FeeEstimateQuery();
        assertThat(query.getHighVolumeThrottle()).isEqualTo((short) 0);
    }

    @Test
    @DisplayName("Default maxAttempts is 10")
    void defaultMaxAttemptsIsTen() {
        var query = new FeeEstimateQuery();
        assertThat(query.getMaxAttempts()).isEqualTo(10);
    }

    @Test
    @DisplayName("Default maxBackoff is 8 seconds")
    void defaultMaxBackoffIsEightSeconds() {
        var query = new FeeEstimateQuery();
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofSeconds(8));
    }

    // -------------------------------------------------------------------------
    // Setter / getter round-trips
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("setMode stores and getMode returns the value")
    void setModeRoundTrip() {
        var query = new FeeEstimateQuery();
        query.setMode(FeeEstimateMode.STATE);
        assertThat(query.getMode()).isEqualTo(FeeEstimateMode.STATE);

        query.setMode(FeeEstimateMode.INTRINSIC);
        assertThat(query.getMode()).isEqualTo(FeeEstimateMode.INTRINSIC);
    }

    @Test
    @DisplayName("setMode rejects null")
    void setModeRejectsNull() {
        var query = new FeeEstimateQuery();
        assertThatThrownBy(() -> query.setMode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("setHighVolumeThrottle (short overload) stores and returns the value")
    void setHighVolumeThrottleShortRoundTrip() {
        var query = new FeeEstimateQuery();
        query.setHighVolumeThrottle((short) 5000);
        assertThat(query.getHighVolumeThrottle()).isEqualTo((short) 5000);
    }

    @Test
    @DisplayName("setHighVolumeThrottle (int overload) delegates to short overload")
    void setHighVolumeThrottleIntRoundTrip() {
        var query = new FeeEstimateQuery();
        query.setHighVolumeThrottle(10000);
        assertThat(query.getHighVolumeThrottle()).isEqualTo((short) 10000);
    }

    @Test
    @DisplayName("setHighVolumeThrottle rejects values below 0")
    void setHighVolumeThrottleRejectsNegative() {
        var query = new FeeEstimateQuery();
        assertThatThrownBy(() -> query.setHighVolumeThrottle((short) -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("highVolumeThrottle");
    }

    @Test
    @DisplayName("setHighVolumeThrottle rejects values above 10000")
    void setHighVolumeThrottleRejectsAboveMax() {
        var query = new FeeEstimateQuery();
        assertThatThrownBy(() -> query.setHighVolumeThrottle(10001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("highVolumeThrottle");
    }

    @Test
    @DisplayName("setMaxAttempts stores and getMaxAttempts returns the value")
    void setMaxAttemptsRoundTrip() {
        var query = new FeeEstimateQuery();
        query.setMaxAttempts(5);
        assertThat(query.getMaxAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("setMaxBackoff stores and getMaxBackoff returns the value")
    void setMaxBackoffRoundTrip() {
        var query = new FeeEstimateQuery();
        query.setMaxBackoff(Duration.ofSeconds(30));
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("setMaxBackoff rejects durations below 500 ms")
    void setMaxBackoffRejectsShortDuration() {
        var query = new FeeEstimateQuery();
        assertThatThrownBy(() -> query.setMaxBackoff(Duration.ofMillis(499)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("setMaxBackoff rejects null")
    void setMaxBackoffRejectsNull() {
        var query = new FeeEstimateQuery();
        assertThatThrownBy(() -> query.setMaxBackoff(null))
                .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // FeeEstimateResponse JSON parsing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FeeEstimateResponse.fromJson parses a complete response correctly")
    void feeEstimateResponseFromJsonComplete() {
        String json = """
                {
                  "mode": "INTRINSIC",
                  "network": {"multiplier": 3, "subtotal": 30},
                  "node": {"base": 10, "extras": []},
                  "service": {"base": 20, "extras": []},
                  "high_volume_multiplier": 2,
                  "total": 60
                }
                """;

        var response = FeeEstimateResponse.fromJson(json);

        assertThat(response.getTotal()).isEqualTo(60L);
        assertThat(response.getHighVolumeMultiplier()).isEqualTo(2L);
        assertThat(response.getNetwork()).isNotNull();
        assertThat(response.getNetwork().getMultiplier()).isEqualTo(3);
        assertThat(response.getNetwork().getSubtotal()).isEqualTo(30L);
        assertThat(response.getNode()).isNotNull();
        assertThat(response.getNode().getBase()).isEqualTo(10L);
        assertThat(response.getService()).isNotNull();
        assertThat(response.getService().getBase()).isEqualTo(20L);
    }

    @Test
    @DisplayName("FeeEstimateResponse.fromJson handles missing high_volume_multiplier (defaults to 0)")
    void feeEstimateResponseFromJsonMissingHighVolumeMultiplier() {
        String json = """
                {
                  "network": {"multiplier": 1, "subtotal": 10},
                  "node": {"base": 5, "extras": []},
                  "service": {"base": 5, "extras": []},
                  "total": 20
                }
                """;

        var response = FeeEstimateResponse.fromJson(json);

        assertThat(response.getHighVolumeMultiplier()).isEqualTo(0L);
        assertThat(response.getTotal()).isEqualTo(20L);
    }

    @Test
    @DisplayName("FeeEstimateResponse.fromJson handles missing network, node, and service (returns null components)")
    void feeEstimateResponseFromJsonMinimal() {
        String json = """
                {
                  "total": 42
                }
                """;

        var response = FeeEstimateResponse.fromJson(json);

        assertThat(response.getTotal()).isEqualTo(42L);
        assertThat(response.getNetwork()).isNull();
        assertThat(response.getNode()).isNull();
        assertThat(response.getService()).isNull();
        assertThat(response.getHighVolumeMultiplier()).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // FeeEstimateResponse equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FeeEstimateResponse equals and hashCode are consistent")
    void feeEstimateResponseEqualsAndHashCode() {
        String json = """
                {
                  "network": {"multiplier": 2, "subtotal": 20},
                  "node": {"base": 10, "extras": []},
                  "service": {"base": 10, "extras": []},
                  "high_volume_multiplier": 1,
                  "total": 40
                }
                """;

        var r1 = FeeEstimateResponse.fromJson(json);
        var r2 = FeeEstimateResponse.fromJson(json);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    // -------------------------------------------------------------------------
    // FeeEstimateResponse fee aggregation (simulates executeChunked logic)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Fee aggregation sums subtotals and bases, and takes max of multipliers per HIP-1261")
    void feeAggregationMaxMultiplierSemantics() {
        // Simulate two chunk responses that would be combined in executeChunked
        String chunk1Json = """
                {
                  "network": {"multiplier": 2, "subtotal": 20},
                  "node": {"base": 10, "extras": []},
                  "service": {"base": 5, "extras": []},
                  "high_volume_multiplier": 3,
                  "total": 35
                }
                """;
        String chunk2Json = """
                {
                  "network": {"multiplier": 4, "subtotal": 40},
                  "node": {"base": 10, "extras": []},
                  "service": {"base": 5, "extras": []},
                  "high_volume_multiplier": 5,
                  "total": 55
                }
                """;

        var r1 = FeeEstimateResponse.fromJson(chunk1Json);
        var r2 = FeeEstimateResponse.fromJson(chunk2Json);

        // Replicate aggregation logic from FeeEstimateQuery.executeChunked
        long totalNetworkSubtotal = r1.getNetwork().getSubtotal() + r2.getNetwork().getSubtotal();
        int maxNetworkMultiplier = Math.max(r1.getNetwork().getMultiplier(), r2.getNetwork().getMultiplier());
        long totalNodeBase = r1.getNode().getBase() + r2.getNode().getBase();
        long totalServiceBase = r1.getService().getBase() + r2.getService().getBase();
        long totalTotal = r1.getTotal() + r2.getTotal();
        long highVolumeMultiplier = Math.max(r1.getHighVolumeMultiplier(), r2.getHighVolumeMultiplier());

        // Assertions per HIP-1261 semantics
        assertThat(totalNetworkSubtotal).isEqualTo(60L);       // 20 + 40
        assertThat(maxNetworkMultiplier).isEqualTo(4);          // max(2, 4)
        assertThat(totalNodeBase).isEqualTo(20L);              // 10 + 10
        assertThat(totalServiceBase).isEqualTo(10L);           // 5 + 5
        assertThat(totalTotal).isEqualTo(90L);                 // 35 + 55
        assertThat(highVolumeMultiplier).isEqualTo(5L);        // max(3, 5) — per HIP-1261
    }

    @Test
    @DisplayName("Fee aggregation with single chunk produces unchanged values")
    void feeAggregationSingleChunkIsIdentity() {
        String json = """
                {
                  "network": {"multiplier": 3, "subtotal": 30},
                  "node": {"base": 10, "extras": []},
                  "service": {"base": 20, "extras": []},
                  "high_volume_multiplier": 2,
                  "total": 60
                }
                """;

        var r = FeeEstimateResponse.fromJson(json);

        long totalNetworkSubtotal = r.getNetwork().getSubtotal();
        int maxNetworkMultiplier = r.getNetwork().getMultiplier();
        long totalNodeBase = r.getNode().getBase();
        long totalServiceBase = r.getService().getBase();
        long totalTotal = r.getTotal();
        long highVolumeMultiplier = Math.max(1L, r.getHighVolumeMultiplier());

        assertThat(totalNetworkSubtotal).isEqualTo(30L);
        assertThat(maxNetworkMultiplier).isEqualTo(3);
        assertThat(totalNodeBase).isEqualTo(10L);
        assertThat(totalServiceBase).isEqualTo(20L);
        assertThat(totalTotal).isEqualTo(60L);
        assertThat(highVolumeMultiplier).isEqualTo(2L);
    }

    // -------------------------------------------------------------------------
    // FeeEstimateMode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FeeEstimateMode.fromString is case-insensitive and recognizes STATE and INTRINSIC")
    void feeEstimateModeFromString() {
        assertThat(FeeEstimateMode.fromString("state")).isEqualTo(FeeEstimateMode.STATE);
        assertThat(FeeEstimateMode.fromString("STATE")).isEqualTo(FeeEstimateMode.STATE);
        assertThat(FeeEstimateMode.fromString("intrinsic")).isEqualTo(FeeEstimateMode.INTRINSIC);
        assertThat(FeeEstimateMode.fromString("INTRINSIC")).isEqualTo(FeeEstimateMode.INTRINSIC);
    }

    @Test
    @DisplayName("FeeEstimateMode.fromString throws on unknown value")
    void feeEstimateModeFromStringThrowsOnUnknown() {
        assertThatThrownBy(() -> FeeEstimateMode.fromString("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("FeeEstimateMode.toString returns uppercase string")
    void feeEstimateModeToString() {
        assertThat(FeeEstimateMode.STATE.toString()).isEqualTo("STATE");
        assertThat(FeeEstimateMode.INTRINSIC.toString()).isEqualTo("INTRINSIC");
    }
}
