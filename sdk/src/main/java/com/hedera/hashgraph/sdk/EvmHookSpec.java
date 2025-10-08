// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import java.util.Objects;

/**
 * Shared specifications for an EVM hook.
 * <p>
 * This class defines the source of EVM bytecode for a hook implementation.
 * Currently, hooks can only be implemented by referencing an existing contract
 * that implements the extension point API.
 */
public abstract class EvmHookSpec {
    private final ContractId contractId;

    /**
     * Create a new EvmHookSpec that references a contract.
     *
     * @param contractId the ID of the contract that implements the hook
     */
    protected EvmHookSpec(ContractId contractId) {
        this.contractId = Objects.requireNonNull(contractId, "contractId cannot be null");
    }

    /**
     * Get the contract ID that implements this hook.
     *
     * @return the contract ID
     */
    public ContractId getContractId() {
        return contractId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EvmHookSpec that = (EvmHookSpec) o;
        return contractId.equals(that.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId);
    }

    @Override
    public String toString() {
        return "EvmHookSpec{contractId=" + contractId + "}";
    }
}
