// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Base class for the HIP-1535 CLPR transactions.
 * <p>
 * Checks required fields before freezing and before executing decoded frozen bytes.
 * CLPR transactions cannot be scheduled.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 *
 * @param <T> the concrete transaction type
 */
public abstract class ClprTransaction<T extends ClprTransaction<T>> extends Transaction<T> {
    ClprTransaction() {}

    ClprTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
    }

    ClprTransaction(TransactionBody txBody) {
        super(txBody);
    }

    /**
     * The required fields, by name. A null value is missing; an {@link OwnershipProofState} must be complete.
     */
    abstract Map<String, @Nullable Object> getRequiredFields();

    String getName() {
        return getClass().getSimpleName();
    }

    void requireFields(Map<String, @Nullable Object> fields, String action) {
        for (var entry : fields.entrySet()) {
            var value = entry.getValue();
            if (value instanceof OwnershipProofState proof) {
                proof.requireComplete(getName());
            } else if (value == null) {
                throw new IllegalStateException(
                        getName() + ": '" + entry.getKey() + "' must be explicitly set before " + action + ".");
            }
        }
    }

    @Override
    public T freezeWith(@Nullable Client client) {
        if (!isFrozen()) {
            requireFields(getRequiredFields(), "freezing or executing");
        }
        return super.freezeWith(client);
    }

    @Override
    void onExecute(Client client) {
        requireFields(getRequiredFields(), "freezing or executing");
        super.onExecute(client);
    }

    @Override
    final void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException(getName() + " cannot be scheduled");
    }
}
