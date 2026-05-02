// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A custom transfer fee that was assessed during the handling of a CryptoTransfer.
 */
public class AssessedCustomFee {
    /**
     * The number of units assessed for the fee
     */
    public final long amount;

    /**
     * The denomination of the fee; taken as hbar if left unset
     */
    @Nullable
    public final TokenId tokenId;

    /**
     * The account to receive the assessed fee
     */
    @Nullable
    public final AccountId feeCollectorAccountId;

    /**
     * The account(s) whose final balances would have been higher in the absence of this assessed fee
     */
    public final List<AccountId> payerAccountIdList;

    AssessedCustomFee(
            long amount,
            @Nullable TokenId tokenId,
            @Nullable AccountId feeCollectorAccountId,
            List<AccountId> payerAccountIdList) {
        this.amount = amount;
        this.tokenId = tokenId;
        this.feeCollectorAccountId = feeCollectorAccountId;
        this.payerAccountIdList = payerAccountIdList;
    }

    /**
     * Convert the protobuf object to an assessed custom fee object.
     *
     * @param assessedCustomFee         protobuf response object
     * @return                          the converted assessed custom fee object
     */
    static AssessedCustomFee fromProtobuf(org.hiero.sdk.proto.AssessedCustomFee assessedCustomFee) {
        var payerList = new ArrayList<AccountId>(assessedCustomFee.getEffectivePayerAccountIdCount());
        for (var payerId : assessedCustomFee.getEffectivePayerAccountIdList()) {
            payerList.add(AccountId.fromProtobuf(payerId));
        }
        return new AssessedCustomFee(
                assessedCustomFee.getAmount(),
                assessedCustomFee.hasTokenId() ? TokenId.fromProtobuf(assessedCustomFee.getTokenId()) : null,
                assessedCustomFee.hasFeeCollectorAccountId()
                        ? AccountId.fromProtobuf(assessedCustomFee.getFeeCollectorAccountId())
                        : null,
                payerList);
    }

    /**
     * Convert a byte array into an assessed custom fee object.
     *
     * @param bytes                     the byte array
     * @return                          the converted assessed custom fee object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static AssessedCustomFee fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.AssessedCustomFee.parseFrom(bytes).toBuilder()
                .build());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("amount", amount)
                .add("tokenId", tokenId)
                .add("feeCollectorAccountId", feeCollectorAccountId)
                .add("payerAccountIdList", payerAccountIdList)
                .toString();
    }

    /**
     * Create the protobuf representation.
     *
     * @return {@link org.hiero.sdk.proto.AssessedCustomFee}
     */
    org.hiero.sdk.proto.AssessedCustomFee toProtobuf() {
        var builder = org.hiero.sdk.proto.AssessedCustomFee.newBuilder().setAmount(amount);
        if (tokenId != null) {
            builder.setTokenId(tokenId.toProtobuf());
        }
        if (feeCollectorAccountId != null) {
            builder.setFeeCollectorAccountId(feeCollectorAccountId.toProtobuf());
        }
        for (var payerId : payerAccountIdList) {
            builder.addEffectivePayerAccountId(payerId.toProtobuf());
        }
        return builder.build();
    }

    /**
     * Create a byte array representation.
     *
     * @return                          the converted assessed custom fees
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
