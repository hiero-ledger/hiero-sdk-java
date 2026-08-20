// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MirrorNodeTokenBalanceQueryTest {

    @Test
    void defaultsMatchTheOtherMirrorRestQueries() {
        var query = new MirrorNodeTokenBalanceQuery();

        assertThat(query.getAccountId()).isNull();
        assertThat(query.getTokenId()).isNull();
        assertThat(query.getMaxAttempts()).isEqualTo(10);
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void settersAreFluentAndRoundTrip() {
        var accountId = AccountId.fromString("0.0.5005");
        var tokenId = TokenId.fromString("0.0.1135");
        var query = new MirrorNodeTokenBalanceQuery()
                .setAccountId(accountId)
                .setTokenId(tokenId)
                .setMaxAttempts(3)
                .setMaxBackoff(Duration.ofMillis(500));

        assertThat(query.getAccountId()).isEqualTo(accountId);
        assertThat(query.getTokenId()).isEqualTo(tokenId);
        assertThat(query.getMaxAttempts()).isEqualTo(3);
        assertThat(query.getMaxBackoff()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void settersRejectNull() {
        assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery().setAccountId(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery().setTokenId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setMaxBackoffRejectsSubHalfSecondValues() {
        assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery().setMaxBackoff(Duration.ofMillis(499)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 500 ms");
    }

    @Test
    void setMaxAttemptsRejectsNonPositiveValues() {
        assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery().setMaxAttempts(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void toStringIncludesBothIds() {
        var query = new MirrorNodeTokenBalanceQuery()
                .setAccountId(AccountId.fromString("0.0.5005"))
                .setTokenId(TokenId.fromString("0.0.1135"));

        assertThat(query.toString()).contains("0.0.5005").contains("0.0.1135");
    }
}
