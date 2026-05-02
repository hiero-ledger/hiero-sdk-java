// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import javax.annotation.Nullable;
import org.hiero.sdk.proto.FixedCustomFee;
import org.hiero.sdk.proto.FixedFee;

/**
 * Custom fixed fee utility class.
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/custom-token-fees#fixed-fee">Hedera Documentation</a>
 */
public class CustomFixedFee extends CustomFeeBase<CustomFixedFee> {
    private long amount = 0;
    /**
     * The shard, realm, number of the tokens.
     */
    @Nullable
    private TokenId denominatingTokenId = null;

    /**
     * Constructor.
     */
    public CustomFixedFee() {}

    /**
     * Create a custom fixed fee from a fixed fee protobuf.
     *
     * @param fixedFee                  the fixed fee protobuf
     * @return                          the new custom fixed fee object
     */
    static CustomFixedFee fromProtobuf(FixedFee fixedFee) {
        var returnFee = new CustomFixedFee().setAmount(fixedFee.getAmount());
        if (fixedFee.hasDenominatingTokenId()) {
            returnFee.setDenominatingTokenId(TokenId.fromProtobuf(fixedFee.getDenominatingTokenId()));
        }
        return returnFee;
    }

    /**
     * Create a custom fixed fee from a fixed custom fee protobuf.
     * <p>
     * This fee definition is specific to an Hedera Consensus Service (HCS) topic and SHOULD NOT be used in any other context.
     *
     * @param fixedFee                  the fixed custom fee protobuf
     * @return                          the new custom fixed fee object
     */
    static CustomFixedFee fromProtobuf(FixedCustomFee fixedFee) {
        var returnFee = fromProtobuf(fixedFee.getFixedFee());
        if (fixedFee.hasFeeCollectorAccountId()) {
            returnFee.setFeeCollectorAccountId(AccountId.fromProtobuf(fixedFee.getFeeCollectorAccountId()));
        }
        return returnFee;
    }

    FixedCustomFee toTopicFeeProtobuf() {
        var builder = FixedCustomFee.newBuilder();
        var fixedFeeBuilder = FixedFee.newBuilder();

        if (feeCollectorAccountId != null) {
            builder.setFeeCollectorAccountId(feeCollectorAccountId.toProtobuf());
        }

        builder.setFixedFee(fixedFeeBuilder.setAmount(amount));

        if (denominatingTokenId != null) {
            builder.setFixedFee(fixedFeeBuilder.setDenominatingTokenId(denominatingTokenId.toProtobuf()));
        }

        return builder.build();
    }

    @Override
    CustomFixedFee deepCloneSubclass() {
        return new CustomFixedFee()
                .setAmount(amount)
                .setDenominatingTokenId(denominatingTokenId)
                .finishDeepClone(this);
    }

    /**
     * Extract the amount.
     *
     * @return                          the amount of the fee in tiny bar
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Assign the fee amount in tiny bar.
     *
     * @param amount                    the amount of the fee in tiny bar
     * @return {@code this}
     */
    public CustomFixedFee setAmount(long amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Extract the fee amount.
     *
     * @return                          the fee amount in hbar
     */
    public Hbar getHbarAmount() {
        return Hbar.fromTinybars(amount);
    }

    /**
     * Assign the fee amount in hbar.
     *
     * @param amount                    the fee amount in hbar
     * @return {@code this}
     */
    public CustomFixedFee setHbarAmount(Hbar amount) {
        denominatingTokenId = null;
        this.amount = amount.toTinybars();
        return this;
    }

    /**
     * Extract the token id.
     *
     * @return                          the token id object
     */
    @Nullable
    public TokenId getDenominatingTokenId() {
        return denominatingTokenId;
    }

    /**
     * Assign the desired token id.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public CustomFixedFee setDenominatingTokenId(@Nullable TokenId tokenId) {
        denominatingTokenId = tokenId;
        return this;
    }

    /**
     * Assign the default token 0.0.0.
     *
     * @return {@code this}
     */
    public CustomFixedFee setDenominatingTokenToSameToken() {
        denominatingTokenId = new TokenId(0, 0, 0);
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        super.validateChecksums(client);
        if (denominatingTokenId != null) {
            denominatingTokenId.validateChecksum(client);
        }
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("amount", getAmount())
                .add("demoninatingTokenId", getDenominatingTokenId())
                .toString();
    }

    /**
     * Convert to a protobuf.
     *
     * @return                          the protobuf converted object
     */
    FixedFee toFixedFeeProtobuf() {
        var fixedFeeBuilder = FixedFee.newBuilder().setAmount(getAmount());
        if (getDenominatingTokenId() != null) {
            fixedFeeBuilder.setDenominatingTokenId(getDenominatingTokenId().toProtobuf());
        }
        return fixedFeeBuilder.build();
    }

    @Override
    org.hiero.sdk.proto.CustomFee toProtobuf() {
        var customFeeBuilder = org.hiero.sdk.proto.CustomFee.newBuilder().setFixedFee(toFixedFeeProtobuf());
        return finishToProtobuf(customFeeBuilder);
    }
}
