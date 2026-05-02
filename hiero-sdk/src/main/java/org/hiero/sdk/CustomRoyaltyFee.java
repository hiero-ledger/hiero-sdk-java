// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.Fraction;
import org.hiero.sdk.proto.RoyaltyFee;

/**
 * Custom royalty fee utility class.
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/custom-token-fees#royalty-fee">Hedera Documentation</a>
 */
public class CustomRoyaltyFee extends CustomFeeBase<CustomRoyaltyFee> {
    private long numerator = 0;
    private long denominator = 1;

    @Nullable
    private CustomFixedFee fallbackFee = null;

    /**
     * Constructor.
     */
    public CustomRoyaltyFee() {}

    /**
     * Create a custom royalty fee from a royalty fee protobuf.
     *
     * @param royaltyFee                the royalty fee protobuf
     * @return                          the new royalty fee object
     */
    static CustomRoyaltyFee fromProtobuf(RoyaltyFee royaltyFee) {
        var fraction = royaltyFee.getExchangeValueFraction();
        var returnFee =
                new CustomRoyaltyFee().setNumerator(fraction.getNumerator()).setDenominator(fraction.getDenominator());
        if (royaltyFee.hasFallbackFee()) {
            returnFee.fallbackFee = CustomFixedFee.fromProtobuf(royaltyFee.getFallbackFee());
        }
        return returnFee;
    }

    @Override
    CustomRoyaltyFee deepCloneSubclass() {
        var returnFee = new CustomRoyaltyFee();
        returnFee.numerator = numerator;
        returnFee.denominator = denominator;
        returnFee.fallbackFee = fallbackFee != null ? fallbackFee.deepCloneSubclass() : null;
        returnFee.feeCollectorAccountId = feeCollectorAccountId;
        returnFee.allCollectorsAreExempt = allCollectorsAreExempt;
        return returnFee;
    }

    /**
     * Extract the numerator.
     *
     * @return                          the numerator
     */
    public long getNumerator() {
        return numerator;
    }

    /**
     * Assign the numerator.
     *
     * @param numerator                 the numerator
     * @return {@code this}
     */
    public CustomRoyaltyFee setNumerator(long numerator) {
        this.numerator = numerator;
        return this;
    }

    /**
     * Extract the denominator.
     *
     * @return                          the denominator
     */
    public long getDenominator() {
        return denominator;
    }

    /**
     * Assign the denominator can not be zero (0).
     *
     * @param denominator               the denominator
     * @return {@code this}
     */
    public CustomRoyaltyFee setDenominator(long denominator) {
        this.denominator = denominator;
        return this;
    }

    /**
     * The fallback fee is a fixed fee that is charged to the NFT receiver
     * when there is no fungible value exchanged with the sender of the NFT.
     *
     * @param fallbackFee               the fallback fee amount
     * @return {@code this}
     */
    public CustomRoyaltyFee setFallbackFee(CustomFixedFee fallbackFee) {
        Objects.requireNonNull(fallbackFee);
        this.fallbackFee = fallbackFee.deepCloneSubclass();
        return this;
    }

    /**
     * Get the fallback fixed fee.
     *
     * @return the fallback fixed fee
     */
    @Nullable
    public CustomFixedFee getFallbackFee() {
        return fallbackFee != null ? fallbackFee.deepCloneSubclass() : null;
    }

    /**
     * Convert the royalty fee object to a protobuf.
     *
     * @return                          the protobuf object
     */
    RoyaltyFee toRoyaltyFeeProtobuf() {
        var royaltyFeeBuilder = RoyaltyFee.newBuilder()
                .setExchangeValueFraction(
                        Fraction.newBuilder().setNumerator(numerator).setDenominator(denominator));
        if (fallbackFee != null) {
            royaltyFeeBuilder.setFallbackFee(fallbackFee.toFixedFeeProtobuf());
        }
        return royaltyFeeBuilder.build();
    }

    @Override
    org.hiero.sdk.proto.CustomFee toProtobuf() {
        var customFeeBuilder = org.hiero.sdk.proto.CustomFee.newBuilder().setRoyaltyFee(toRoyaltyFeeProtobuf());
        return finishToProtobuf(customFeeBuilder);
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("numerator", numerator)
                .add("denominator", denominator)
                .add("fallbackFee", fallbackFee)
                .toString();
    }
}
