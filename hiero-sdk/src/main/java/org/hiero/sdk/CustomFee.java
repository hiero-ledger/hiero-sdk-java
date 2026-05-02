// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Base class for custom fees.
 */
public abstract class CustomFee {
    /**
     * The account to receive the custom fee
     */
    @Nullable
    protected AccountId feeCollectorAccountId = null;

    /**
     * If true, exempts all the token's fee collection accounts from this fee
     */
    protected boolean allCollectorsAreExempt = false;

    /**
     * Constructor.
     */
    CustomFee() {}

    /**
     * Convert the protobuf object to a custom fee object.
     *
     * @param customFee                 protobuf response object
     * @return                          the converted custom fee object
     */
    static CustomFee fromProtobufInner(org.hiero.sdk.proto.CustomFee customFee) {
        switch (customFee.getFeeCase()) {
            case FIXED_FEE:
                return CustomFixedFee.fromProtobuf(customFee.getFixedFee());

            case FRACTIONAL_FEE:
                return CustomFractionalFee.fromProtobuf(customFee.getFractionalFee());

            case ROYALTY_FEE:
                return CustomRoyaltyFee.fromProtobuf(customFee.getRoyaltyFee());

            default:
                throw new IllegalStateException(
                        "CustomFee#fromProtobuf: unhandled fee case: " + customFee.getFeeCase());
        }
    }

    static CustomFee fromProtobuf(org.hiero.sdk.proto.CustomFee customFee) {
        var outFee = fromProtobufInner(customFee);
        if (customFee.hasFeeCollectorAccountId()) {
            outFee.feeCollectorAccountId = AccountId.fromProtobuf(customFee.getFeeCollectorAccountId());
        }
        outFee.allCollectorsAreExempt = customFee.getAllCollectorsAreExempt();

        return outFee;
    }

    /**
     * Convert byte array to a custom fee object.
     *
     * @param bytes                     the byte array
     * @return                          the converted custom fee object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static CustomFee fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                org.hiero.sdk.proto.CustomFee.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create a new copy of a custom fee list.
     *
     * @param customFees                existing custom fee list
     * @return                          new custom fee list
     */
    public static List<CustomFee> deepCloneList(List<CustomFee> customFees) {
        var returnCustomFees = new ArrayList<CustomFee>(customFees.size());
        for (var fee : customFees) {
            returnCustomFees.add(fee.deepClone());
        }
        return returnCustomFees;
    }

    /**
     * Extract the fee collector account id.
     *
     * @return                          the fee collector account id
     */
    @Nullable
    public AccountId getFeeCollectorAccountId() {
        return feeCollectorAccountId;
    }

    /**
     *
     * @return whether all fee collectors are exempt from fees
     */
    public boolean getAllCollectorsAreExempt() {
        return allCollectorsAreExempt;
    }

    /**
     * Create a deep clone.
     *
     * @return                          the correct cloned fee type
     */
    abstract CustomFee deepClone();

    /**
     * Verify the validity of the client object.
     *
     * @param client                    the configured client
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    void validateChecksums(Client client) throws BadEntityIdException {
        if (feeCollectorAccountId != null) {
            feeCollectorAccountId.validateChecksum(client);
        }
    }

    /**
     * Finalize the builder into the protobuf.
     *
     * @param customFeeBuilder              the builder object
     * @return                              the protobuf
     */
    protected org.hiero.sdk.proto.CustomFee finishToProtobuf(org.hiero.sdk.proto.CustomFee.Builder customFeeBuilder) {
        if (feeCollectorAccountId != null) {
            customFeeBuilder.setFeeCollectorAccountId(feeCollectorAccountId.toProtobuf());
        }
        customFeeBuilder.setAllCollectorsAreExempt(allCollectorsAreExempt);
        return customFeeBuilder.build();
    }

    /**
     * Create the protobuf.
     *
     * @return                              the protobuf for the custom fee object
     */
    abstract org.hiero.sdk.proto.CustomFee toProtobuf();

    /**
     * Create the byte array.
     *
     * @return                              the byte array representing the protobuf
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    /**
     * Serializes the class to ToStringHelper
     *
     * @return the {@link com.google.common.base.MoreObjects.ToStringHelper}
     */
    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("feeCollectorAccountId", feeCollectorAccountId)
                .add("allCollectorsAreExempt", allCollectorsAreExempt);
    }
}
