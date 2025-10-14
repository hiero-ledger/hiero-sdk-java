// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * The ID of an entity using a hook.
 * <p>
 * This class represents the HookEntityId protobuf message, which can be either
 * an account ID or a contract ID.
 */
public class HookEntityId {
    private final AccountId accountId;
    private final ContractId contractId;

    /**
     * Create a HookEntityId with an account ID.
     *
     * @param accountId the account ID
     */
    public HookEntityId(AccountId accountId) {
        this.accountId = Objects.requireNonNull(accountId, "accountId cannot be null");
        this.contractId = null;
    }

    /**
     * Create a HookEntityId with a contract ID.
     *
     * @param contractId the contract ID
     */
    public HookEntityId(ContractId contractId) {
        this.accountId = null;
        this.contractId = Objects.requireNonNull(contractId, "contractId cannot be null");
    }

    /**
     * Get the account ID.
     *
     * @return the account ID, or null if this entity is a contract
     */
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Get the contract ID.
     *
     * @return the contract ID, or null if this entity is an account
     */
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * Check if this entity is an account.
     *
     * @return true if this entity is an account, false if it's a contract
     */
    public boolean isAccount() {
        return accountId != null;
    }

    /**
     * Check if this entity is a contract.
     *
     * @return true if this entity is a contract, false if it's an account
     */
    public boolean isContract() {
        return contractId != null;
    }

    /**
     * Convert this HookEntityId to a protobuf message.
     *
     * @return the protobuf HookEntityId
     */
    com.hedera.hashgraph.sdk.proto.HookEntityId toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.HookEntityId.newBuilder();

        if (accountId != null) {
            builder.setAccountId(accountId.toProtobuf());
        } else {
            builder.setContractId(contractId.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Create a HookEntityId from a protobuf message.
     *
     * @param proto the protobuf HookEntityId
     * @return a new HookEntityId instance
     */
    static HookEntityId fromProtobuf(com.hedera.hashgraph.sdk.proto.HookEntityId proto) {
        if (proto.hasAccountId()) {
            return new HookEntityId(AccountId.fromProtobuf(proto.getAccountId()));
        } else {
            return new HookEntityId(ContractId.fromProtobuf(proto.getContractId()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HookEntityId that = (HookEntityId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(contractId, that.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, contractId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("contractId", contractId)
                .toString();
    }
}
