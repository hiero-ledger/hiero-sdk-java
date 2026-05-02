// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TokenAllowanceTest {

    private static final TokenId testTokenId = TokenId.fromString("0.6.9");
    private static final AccountId testOwnerAccountId = AccountId.fromString("8.8.8");
    private static final AccountId testSpenderAccountId = AccountId.fromString("7.7.7");
    private static final long testAmount = 4L;

    @Test
    void constructWithTokenIdOwnerSpenderAmount() {
        TokenAllowance tokenAllowance =
                new TokenAllowance(testTokenId, testOwnerAccountId, testSpenderAccountId, testAmount);

        assertThat(tokenAllowance.tokenId).isEqualTo(testTokenId);
        assertThat(tokenAllowance.ownerAccountId).isEqualTo(testOwnerAccountId);
        assertThat(tokenAllowance.spenderAccountId).isEqualTo(testSpenderAccountId);
        assertThat(tokenAllowance.amount).isEqualTo(testAmount);
    }

    @Test
    void fromProtobuf() {
        var tokenAllowanceProtobuf =
                new TokenAllowance(testTokenId, testOwnerAccountId, testSpenderAccountId, testAmount).toProtobuf();
        var tokenAllowance = TokenAllowance.fromProtobuf(tokenAllowanceProtobuf);

        assertThat(tokenAllowance.tokenId).isEqualTo(testTokenId);
        assertThat(tokenAllowance.ownerAccountId).isEqualTo(testOwnerAccountId);
        assertThat(tokenAllowance.spenderAccountId).isEqualTo(testSpenderAccountId);
        assertThat(tokenAllowance.amount).isEqualTo(testAmount);
    }

    @Test
    void toProtobuf() {
        var tokenAllowanceProtobuf =
                new TokenAllowance(testTokenId, testOwnerAccountId, testSpenderAccountId, testAmount).toProtobuf();

        assertTrue(tokenAllowanceProtobuf.hasTokenId());
        assertThat(TokenId.fromProtobuf(tokenAllowanceProtobuf.getTokenId())).isEqualTo(testTokenId);

        assertTrue(tokenAllowanceProtobuf.hasOwner());
        assertThat(AccountId.fromProtobuf(tokenAllowanceProtobuf.getOwner())).isEqualTo(testOwnerAccountId);

        assertTrue(tokenAllowanceProtobuf.hasSpender());
        assertThat(AccountId.fromProtobuf(tokenAllowanceProtobuf.getSpender())).isEqualTo(testSpenderAccountId);
    }
}
