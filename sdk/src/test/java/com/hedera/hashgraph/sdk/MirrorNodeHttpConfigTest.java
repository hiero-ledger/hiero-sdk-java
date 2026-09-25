// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeHttpConfigTest {

    @Test
    void defaultsMatchTheProposal() {
        var config = MirrorNodeHttpConfig.defaults();

        assertThat(config.getTransport()).isNull();
        assertThat(config.getRequestHeaders()).isEmpty();

        var transportConfiguration = config.getTransportConfiguration();
        assertThat(transportConfiguration.getConnectTimeout()).isEqualTo(Duration.ZERO);
        assertThat(transportConfiguration.getMaxRedirects()).isEqualTo(5);
        assertThat(transportConfiguration.getMaxResponseBytes()).isEqualTo(33554432L);

        var policy = config.getRetryPolicy();
        assertThat(policy.getMaxAttempts()).isEqualTo(5);
        assertThat(policy.getPerAttemptTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.getTotalDeadline()).isEqualTo(Duration.ZERO);
        assertThat(policy.getInitialBackoff()).isEqualTo(Duration.ofMillis(250));
        assertThat(policy.getMaxBackoff()).isEqualTo(Duration.ofSeconds(8));
        assertThat(policy.getRetryableStatusCodes()).containsExactly(408, 429, 500, 502, 503, 504);
    }

    @Test
    @DisplayName("Deriving leaves every field the caller did not name at its existing value")
    void derivationKeepsUnnamedFields() {
        var config = MirrorNodeHttpConfig.defaults()
                .withRequestHeader("authorization", "Bearer test")
                .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults().withMaxAttempts(2));

        assertThat(config.getRequestHeaders()).containsEntry("authorization", "Bearer test");
        assertThat(config.getRetryPolicy().getMaxAttempts()).isEqualTo(2);
        assertThat(config.getRetryPolicy().getPerAttemptTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void headerNamesAreLowercased() {
        var config = MirrorNodeHttpConfig.defaults().withRequestHeaders(Map.of("Authorization", "Bearer test"));

        assertThat(config.getRequestHeaders()).containsOnlyKeys("authorization");
    }

    @Test
    @DisplayName("The SDK owns the identity header, whatever case it is spelled in")
    void reservesTheIdentityHeader() {
        for (var name : List.of("User-Agent", "user-agent", "X-User-Agent", "x-USER-agent")) {
            assertThatThrownBy(() -> MirrorNodeHttpConfig.defaults().withRequestHeader(name, "nope"))
                    .as("header %s", name)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owned by the SDK");
        }

        assertThatThrownBy(() -> MirrorNodeHttpConfig.defaults().withRequestHeaders(Map.of("X-User-Agent", "nope")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retryPolicyRejectsInvalidValues() {
        assertThatThrownBy(() -> MirrorNodeHttpRetryPolicy.defaults().withMaxAttempts(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> MirrorNodeHttpRetryPolicy.defaults().withTotalDeadline(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("A zero total deadline inherits the client's request timeout rather than meaning no bound")
    void zeroTotalDeadlineInheritsTheRequestTimeout() {
        var policy = MirrorNodeHttpRetryPolicy.defaults();

        assertThat(MirrorNodeHttpClient.resolveTotalDeadline(policy, Duration.ofMinutes(2), null))
                .isEqualTo(Duration.ofMinutes(2));

        assertThat(MirrorNodeHttpClient.resolveTotalDeadline(
                        policy.withTotalDeadline(Duration.ofSeconds(15)), Duration.ofMinutes(2), null))
                .isEqualTo(Duration.ofSeconds(15));

        // An explicit per-call timeout argument maps to the total deadline.
        assertThat(MirrorNodeHttpClient.resolveTotalDeadline(
                        policy.withTotalDeadline(Duration.ofSeconds(15)), Duration.ofMinutes(2), Duration.ofSeconds(3)))
                .isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void transportConfigurationRejectsInvalidValues() {
        assertThatThrownBy(() -> HttpTransportConfiguration.defaults().withMaxResponseBytes(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> HttpTransportConfiguration.defaults().withConnectTimeout(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
