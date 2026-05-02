// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.List;
import java.util.stream.Collectors;
import org.hiero.sdk.proto.CustomFee;
import org.hiero.sdk.proto.FixedFee;

/**
 * A maximum custom fee that the user is willing to pay.
 * <p>
 * This message is used to specify the maximum custom fee that given user is
 * willing to pay.
 */
public class CustomFeeLimit {

    private AccountId payerId;

    private List<CustomFixedFee> customFees;

    /**
     * Constructor
     */
    public CustomFeeLimit() {}

    /**
     * Extracts the payer accountId
     * @return payerId
     */
    public AccountId getPayerId() {
        return payerId;
    }

    /**
     * A payer account identifier.
     */
    public CustomFeeLimit setPayerId(AccountId payerId) {
        this.payerId = payerId;
        return this;
    }

    /**
     * Extracts a list of CustomFixedFee
     * @return
     */
    public List<CustomFixedFee> getCustomFees() {
        return customFees;
    }

    /**
     * The maximum fees that the user is willing to pay for the message.
     */
    public CustomFeeLimit setCustomFees(List<CustomFixedFee> customFees) {
        this.customFees = customFees;
        return this;
    }

    static CustomFeeLimit fromProtobuf(org.hiero.sdk.proto.CustomFeeLimit customFeeLimit) {
        return new CustomFeeLimit()
                .setPayerId(AccountId.fromProtobuf(customFeeLimit.getAccountId()))
                .setCustomFees(customFeeLimit.getFeesList().stream()
                        .map(CustomFixedFee::fromProtobuf)
                        .collect(Collectors.toList()));
    }

    org.hiero.sdk.proto.CustomFeeLimit toProtobuf() {
        org.hiero.sdk.proto.CustomFeeLimit.Builder builder = org.hiero.sdk.proto.CustomFeeLimit.newBuilder();

        builder.setAccountId(payerId.toProtobuf());

        List<FixedFee> protoFixedFees = customFees.stream()
                .map(CustomFixedFee::toProtobuf)
                .map(CustomFee::getFixedFee)
                .collect(Collectors.toList());

        builder.addAllFees(protoFixedFees);

        return builder.build();
    }
}
