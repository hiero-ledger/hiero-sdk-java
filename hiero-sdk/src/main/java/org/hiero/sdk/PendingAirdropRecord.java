// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import org.hiero.sdk.proto.PendingAirdropValue;

public class PendingAirdropRecord {
    private final PendingAirdropId pendingAirdropId;
    private final long pendingAirdropAmount;

    PendingAirdropRecord(PendingAirdropId pendingAirdropId, long pendingAirdropAmount) {
        this.pendingAirdropId = pendingAirdropId;
        this.pendingAirdropAmount = pendingAirdropAmount;
    }

    public PendingAirdropId getPendingAirdropId() {
        return pendingAirdropId;
    }

    public long getPendingAirdropAmount() {
        return pendingAirdropAmount;
    }

    org.hiero.sdk.proto.PendingAirdropRecord toProtobuf() {
        return org.hiero.sdk.proto.PendingAirdropRecord.newBuilder()
                .setPendingAirdropId(this.pendingAirdropId.toProtobuf())
                .setPendingAirdropValue(PendingAirdropValue.newBuilder().setAmount(pendingAirdropAmount))
                .build();
    }

    static PendingAirdropRecord fromProtobuf(org.hiero.sdk.proto.PendingAirdropRecord pendingAirdropRecord) {
        return new PendingAirdropRecord(
                PendingAirdropId.fromProtobuf(pendingAirdropRecord.getPendingAirdropId()),
                pendingAirdropRecord.getPendingAirdropValue().getAmount());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pendingAirdropId", pendingAirdropId)
                .add("pendingAirdropAmount", pendingAirdropAmount)
                .toString();
    }
}
