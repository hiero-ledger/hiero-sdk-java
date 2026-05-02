// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.Fraction;
import org.hiero.sdk.proto.FractionalFee;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomFractionalFeeTest {
    private static final boolean allCollectorsAreExempt = true;
    private static final AccountId feeCollectorAccountId = new AccountId(1, 2, 3);
    private static final int numerator = 4;
    private static final int denominator = 5;
    private static final int minAmount = 6;
    private static final int maxAmount = 7;
    private static final FeeAssessmentMethod feeAssessmentMethod = FeeAssessmentMethod.EXCLUSIVE;

    private final FractionalFee fee = FractionalFee.newBuilder()
            .setFractionalAmount(Fraction.newBuilder().setNumerator(numerator).setDenominator(denominator))
            .setMinimumAmount(minAmount)
            .setMaximumAmount(maxAmount)
            .setNetOfTransfers(true)
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
        SnapshotMatcher.expect(CustomFractionalFee.fromProtobuf(fee).toString()).toMatchSnapshot();
    }

    @Test
    void deepCloneSubclass() {
        var customFractionalFee = new CustomFractionalFee()
                .setFeeCollectorAccountId(feeCollectorAccountId)
                .setAllCollectorsAreExempt(allCollectorsAreExempt);
        var clonedCustomFractionalFee = customFractionalFee.deepCloneSubclass();

        assertThat(clonedCustomFractionalFee.getFeeCollectorAccountId()).isEqualTo(feeCollectorAccountId);
        assertThat(clonedCustomFractionalFee.getAllCollectorsAreExempt()).isEqualTo(allCollectorsAreExempt);
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(
                        CustomFractionalFee.fromProtobuf(fee).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void getSetNumerator() {
        final var customFractionalFee = new CustomFractionalFee().setNumerator(numerator);
        assertThat(customFractionalFee.getNumerator()).isEqualTo(numerator);
    }

    @Test
    void getSetDenominator() {
        final var customFractionalFee = new CustomFractionalFee().setDenominator(denominator);
        assertThat(customFractionalFee.getDenominator()).isEqualTo(denominator);
    }

    @Test
    void getSetMinimumAmount() {
        final var customFractionalFee = new CustomFractionalFee().setMin(minAmount);
        assertThat(customFractionalFee.getMin()).isEqualTo(minAmount);
    }

    @Test
    void getSetMaximumAmount() {
        final var customFractionalFee = new CustomFractionalFee().setMax(maxAmount);
        assertThat(customFractionalFee.getMax()).isEqualTo(maxAmount);
    }

    @Test
    void getSetAssessmentMethod() {
        final var customFractionalFee = new CustomFractionalFee().setAssessmentMethod(feeAssessmentMethod);
        assertThat(customFractionalFee.getAssessmentMethod()).isEqualTo(feeAssessmentMethod);
    }
}
