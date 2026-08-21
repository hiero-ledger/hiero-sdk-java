// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MirrorNodeAccountBalanceQueryTest {

    @Test
    void defaultsMatchTheOtherMirrorRestQueries() {
        var query = new MirrorNodeAccountBalanceQuery();

        assertThat(query.getAccountId()).isNull();
        assertThat(query.getMaxAttempts()).isEqualTo(10);
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void settersAreFluentAndRoundTrip() {
        var accountId = AccountId.fromString("0.0.5005");
        var query = new MirrorNodeAccountBalanceQuery()
                .setAccountId(accountId)
                .setMaxAttempts(3)
                .setMaxBackoff(Duration.ofMillis(500));

        assertThat(query.getAccountId()).isEqualTo(accountId);
        assertThat(query.getMaxAttempts()).isEqualTo(3);
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void setAccountIdRejectsNull() {
        assertThatThrownBy(() -> new MirrorNodeAccountBalanceQuery().setAccountId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setMaxBackoffRejectsSubHalfSecondValues() {
        assertThatThrownBy(() -> new MirrorNodeAccountBalanceQuery().setMaxBackoff(Duration.ofMillis(499)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 500 ms");
    }

    @Test
    void setMaxAttemptsRejectsNonPositiveValues() {
        assertThatThrownBy(() -> new MirrorNodeAccountBalanceQuery().setMaxAttempts(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void toStringIncludesTheAccountId() {
        var query = new MirrorNodeAccountBalanceQuery().setAccountId(AccountId.fromString("0.0.5005"));

        assertThat(query.toString()).contains("0.0.5005");
    }
}
