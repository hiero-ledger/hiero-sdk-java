// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

abstract class PendingAirdropLogic<T extends PendingAirdropLogic<T>> extends Transaction<T> {

    protected List<PendingAirdropId> pendingAirdropIds = new ArrayList<>();

    protected PendingAirdropLogic() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    PendingAirdropLogic(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    PendingAirdropLogic(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
    }

    /**
     * Extract the pending airdrop ids
     *
     * @return the pending airdrop ids
     */
    public List<PendingAirdropId> getPendingAirdropIds() {
        return this.pendingAirdropIds;
    }

    /**
     * Set the pending airdrop ids
     *
     * @param pendingAirdropIds
     * @return {@code this}
     */
    public T setPendingAirdropIds(List<PendingAirdropId> pendingAirdropIds) {
        Objects.requireNonNull(pendingAirdropIds);
        requireNotFrozen();
        this.pendingAirdropIds = pendingAirdropIds;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * clear the pending airdrop ids
     *
     * @return {@code this}
     */
    public T clearPendingAirdropIds() {
        requireNotFrozen();
        this.pendingAirdropIds = new ArrayList<>();
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Add pendingAirdropId
     *
     * @param pendingAirdropId
     * @return {@code this}
     */
    public T addPendingAirdrop(PendingAirdropId pendingAirdropId) {
        Objects.requireNonNull(pendingAirdropId);
        requireNotFrozen();
        this.pendingAirdropIds.add(pendingAirdropId);
        // noinspection unchecked
        return (T) this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (var pendingAirdropId : pendingAirdropIds) {
            if (pendingAirdropId.getTokenId() != null) {
                pendingAirdropId.getTokenId().validateChecksum(client);
            }

            if (pendingAirdropId.getReceiver() != null) {
                pendingAirdropId.getReceiver().validateChecksum(client);
            }

            if (pendingAirdropId.getSender() != null) {
                pendingAirdropId.getSender().validateChecksum(client);
            }

            if (pendingAirdropId.getNftId() != null) {
                pendingAirdropId.getNftId().tokenId.validateChecksum(client);
            }
        }
    }
}
