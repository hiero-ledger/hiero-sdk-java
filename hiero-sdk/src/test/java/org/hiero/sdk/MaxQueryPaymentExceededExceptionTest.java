// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaxQueryPaymentExceededExceptionTest {
    @Test
    void shouldHaveMessage() {
        var e = new MaxQueryPaymentExceededException(new AccountBalanceQuery(), new Hbar(30), new Hbar(15));

        assertThat(e.getMessage())
                .isEqualTo(
                        "cost for AccountBalanceQuery, of 30 ℏ, without explicit payment is greater than the maximum allowed payment of 15 ℏ");
    }
}
