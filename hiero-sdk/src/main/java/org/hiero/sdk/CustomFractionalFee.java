// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.Objects;
import org.hiero.sdk.proto.Fraction;
import org.hiero.sdk.proto.FractionalFee;

/**
 * Custom fractional fee utility class.
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/custom-token-fees#fractional-fee">Hedera Documentation</a>
 */
public class CustomFractionalFee extends CustomFeeBase<CustomFractionalFee> {
    private long numerator = 0;
    private long denominator = 1;
    private long min = 0;
    private long max = 0;
    private FeeAssessmentMethod assessmentMethod = FeeAssessmentMethod.INCLUSIVE;

    /**
     * Constructor.
     */
    public CustomFractionalFee() {}

    /**
     * Create a custom fractional fee from a fee protobuf.
     *
     * @param fractionalFee the fractional fee protobuf
     * @return the new custom fractional fee object
     */
    static CustomFractionalFee fromProtobuf(FractionalFee fractionalFee) {
        var fraction = fractionalFee.getFractionalAmount();
        return new CustomFractionalFee()
                .setNumerator(fraction.getNumerator())
                .setDenominator(fraction.getDenominator())
                .setMin(fractionalFee.getMinimumAmount())
                .setMax(fractionalFee.getMaximumAmount())
                .setAssessmentMethod(FeeAssessmentMethod.valueOf(fractionalFee.getNetOfTransfers()));
    }

    @Override
    CustomFractionalFee deepCloneSubclass() {
        return new CustomFractionalFee()
                .setNumerator(numerator)
                .setDenominator(denominator)
                .setMin(min)
                .setMax(max)
                .setAssessmentMethod(assessmentMethod)
                .finishDeepClone(this);
    }

    /**
     * Extract the numerator.
     *
     * @return the numerator
     */
    public long getNumerator() {
        return numerator;
    }

    /**
     * Assign the numerator.
     *
     * @param numerator the numerator
     * @return {@code this}
     */
    public CustomFractionalFee setNumerator(long numerator) {
        this.numerator = numerator;
        return this;
    }

    /**
     * Extract the denominator.
     *
     * @return the denominator
     */
    public long getDenominator() {
        return denominator;
    }

    /**
     * Assign the denominator can not be zero (0).
     *
     * @param denominator the denominator
     * @return {@code this}
     */
    public CustomFractionalFee setDenominator(long denominator) {
        this.denominator = denominator;
        return this;
    }

    /**
     * Extract the minimum fee amount.
     *
     * @return the minimum fee amount
     */
    public long getMin() {
        return min;
    }

    /**
     * Assign the minimum fee amount.
     *
     * @param min the fee amount
     * @return {@code this}
     */
    public CustomFractionalFee setMin(long min) {
        this.min = min;
        return this;
    }

    /**
     * Extract the fee amount.
     *
     * @return the fee amount
     */
    public long getMax() {
        return max;
    }

    /**
     * Assign the maximum fee amount.
     *
     * @param max the fee amount
     * @return {@code this}
     */
    public CustomFractionalFee setMax(long max) {
        this.max = max;
        return this;
    }

    /**
     * Extract the assessment method inclusive / exclusive.
     *
     * @return the assessment method inclusive / exclusive
     */
    public FeeAssessmentMethod getAssessmentMethod() {
        return assessmentMethod;
    }

    /**
     * Assign the assessment method inclusive / exclusive.
     * <p>
     * If the assessment method field is set, the token's custom fee is charged
     * to the sending account and the receiving account receives the full token
     * transfer amount. If this field is set to false, the receiver pays for
     * the token custom fees and gets the remaining token balance.
     * INCLUSIVE(false)
     * EXCLUSIVE(true)
     * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/custom-token-fees#fractional-fee">Hedera Documentation</a>
     *
     * @param assessmentMethod inclusive / exclusive
     * @return {@code this}
     */
    public CustomFractionalFee setAssessmentMethod(FeeAssessmentMethod assessmentMethod) {
        Objects.requireNonNull(assessmentMethod);
        this.assessmentMethod = assessmentMethod;
        return this;
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("numerator", getNumerator())
                .add("denominator", getDenominator())
                .add("min", getMin())
                .add("max", getMax())
                .add("assessmentMethod", getAssessmentMethod())
                .toString();
    }

    /**
     * Convert the fractional fee object to a protobuf.
     *
     * @return the protobuf object
     */
    FractionalFee toFractionalFeeProtobuf() {
        return FractionalFee.newBuilder()
                .setMinimumAmount(getMin())
                .setMaximumAmount(getMax())
                .setFractionalAmount(
                        Fraction.newBuilder().setNumerator(getNumerator()).setDenominator(getDenominator()))
                .setNetOfTransfers(assessmentMethod.code)
                .build();
    }

    @Override
    org.hiero.sdk.proto.CustomFee toProtobuf() {
        var customFeeBuilder = org.hiero.sdk.proto.CustomFee.newBuilder().setFractionalFee(toFractionalFeeProtobuf());
        return finishToProtobuf(customFeeBuilder);
    }
}
