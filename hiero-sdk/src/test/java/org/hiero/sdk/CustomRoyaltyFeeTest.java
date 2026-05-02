// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.FixedFee;
import org.hiero.sdk.proto.Fraction;
import org.hiero.sdk.proto.RoyaltyFee;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomRoyaltyFeeTest {
    private static final boolean allCollectorsAreExempt = true;
    private static final AccountId feeCollectorAccountId = new AccountId(1, 2, 3);
    private static final int numerator = 4;
    private static final int denominator = 5;
    private static final CustomFixedFee fallbackFee = new CustomFixedFee().setAmount(6);

    private final RoyaltyFee fee = RoyaltyFee.newBuilder()
            .setExchangeValueFraction(
                    Fraction.newBuilder().setNumerator(numerator).setDenominator(denominator))
            .setFallbackFee(FixedFee.newBuilder().setAmount(6).build())
            .build();

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromProtobuf() {
        SnapshotMatcher.expect(CustomRoyaltyFee.fromProtobuf(fee).toString()).toMatchSnapshot();
    }

    @Test
    void deepCloneSubclass() {
        var customRoyaltyFee = new CustomRoyaltyFee()
                .setFeeCollectorAccountId(feeCollectorAccountId)
                .setAllCollectorsAreExempt(allCollectorsAreExempt);
        var clonedCustomRoyaltyFee = customRoyaltyFee.deepCloneSubclass();

        assertThat(clonedCustomRoyaltyFee.getFeeCollectorAccountId()).isEqualTo(feeCollectorAccountId);
        assertThat(clonedCustomRoyaltyFee.getAllCollectorsAreExempt()).isEqualTo(allCollectorsAreExempt);
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(CustomRoyaltyFee.fromProtobuf(fee).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void getSetNumerator() {
        final var customRoyaltyFee = new CustomRoyaltyFee().setNumerator(numerator);
        assertThat(customRoyaltyFee.getNumerator()).isEqualTo(numerator);
    }

    @Test
    void getSetDenominator() {
        final var customRoyaltyFee = new CustomRoyaltyFee().setDenominator(denominator);
        assertThat(customRoyaltyFee.getDenominator()).isEqualTo(denominator);
    }

    @Test
    void getSetFallbackFee() {
        final var customRoyaltyFee = new CustomRoyaltyFee().setFallbackFee(fallbackFee);
        assertThat(customRoyaltyFee.getFallbackFee()).isNotNull();
        assertThat(customRoyaltyFee.getFallbackFee().getAmount()).isEqualTo(fallbackFee.getAmount());
    }
}
