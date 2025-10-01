// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import javax.annotation.Nullable;

/**
 * Represents the ID of an entity using a hook.
 * <p>
 * Hooks can be attached to either accounts or contracts, and this class
 * provides a unified way to reference the owning entity.
 */
public class HookEntityId {
    @Nullable
    private final AccountId accountId;
    
    @Nullable
    private final ContractId contractId;

    private HookEntityId(@Nullable AccountId accountId, @Nullable ContractId contractId) {
        this.accountId = accountId;
        this.contractId = contractId;
    }

    /**
     * Create a HookEntityId for an account.
     *
     * @param accountId the account ID
     * @return a new HookEntityId instance
     */
    public static HookEntityId ofAccount(AccountId accountId) {
        return new HookEntityId(accountId, null);
    }

    /**
     * Create a HookEntityId for a contract.
     *
     * @param contractId the contract ID
     * @return a new HookEntityId instance
     */
    public static HookEntityId ofContract(ContractId contractId) {
        return new HookEntityId(null, contractId);
    }

    /**
     * Get the account ID if this entity is an account.
     *
     * @return the account ID, or null if this is a contract
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Get the contract ID if this entity is a contract.
     *
     * @return the contract ID, or null if this is an account
     */
    @Nullable
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * Check if this entity is an account.
     *
     * @return true if this is an account, false if it's a contract
     */
    public boolean isAccount() {
        return accountId != null;
    }

    /**
     * Check if this entity is a contract.
     *
     * @return true if this is a contract, false if it's an account
     */
    public boolean isContract() {
        return contractId != null;
    }

    /**
     * Convert this HookEntityId to a protobuf message.
     *
     * @return the protobuf HookEntityId
     */
    public com.hedera.hashgraph.sdk.proto.HookEntityId toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.HookEntityId.newBuilder();
        
        if (accountId != null) {
            builder.setAccountId(accountId.toProtobuf());
        } else if (contractId != null) {
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
    public static HookEntityId fromProtobuf(com.hedera.hashgraph.sdk.proto.HookEntityId proto) {
        return switch (proto.getEntityIdCase()) {
            case ACCOUNT_ID -> HookEntityId.ofAccount(AccountId.fromProtobuf(proto.getAccountId()));
            case CONTRACT_ID -> HookEntityId.ofContract(ContractId.fromProtobuf(proto.getContractId()));
            case ENTITYID_NOT_SET -> throw new IllegalArgumentException("HookEntityId must have either account_id or contract_id set");
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        HookEntityId that = (HookEntityId) o;
        
        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        return contractId != null ? contractId.equals(that.contractId) : that.contractId == null;
    }

    @Override
    public int hashCode() {
        int result = accountId != null ? accountId.hashCode() : 0;
        result = 31 * result + (contractId != null ? contractId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (accountId != null) {
            return "HookEntityId{accountId=" + accountId + "}";
        } else {
            return "HookEntityId{contractId=" + contractId + "}";
        }
    }
}
