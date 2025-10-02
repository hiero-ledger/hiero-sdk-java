// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder class for creating complex hook configurations.
 * <p>
 * This class provides a fluent API for building hooks with multiple
 * storage updates, validation, and convenient factory methods.
 */
public final class HookBuilder {
    private HookExtensionPoint extensionPoint;
    private Long hookId;
    private ContractId contractId;
    private Key adminKey;
    private final List<LambdaStorageUpdate> storageUpdates = new ArrayList<>();

    private HookBuilder() {
        // Private constructor - use factory methods
    }

    /**
     * Create a new HookBuilder for an account allowance hook.
     *
     * @return a new HookBuilder instance
     */
    public static HookBuilder accountAllowanceHook() {
        return new HookBuilder()
                .setExtensionPoint(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK);
    }

    /**
     * Set the extension point for the hook.
     *
     * @param extensionPoint the extension point
     * @return {@code this}
     */
    public HookBuilder setExtensionPoint(HookExtensionPoint extensionPoint) {
        this.extensionPoint = Objects.requireNonNull(extensionPoint, "extensionPoint cannot be null");
        return this;
    }

    /**
     * Set the hook ID.
     *
     * @param hookId the hook ID
     * @return {@code this}
     */
    public HookBuilder setHookId(long hookId) {
        if (hookId < 0) {
            throw new IllegalArgumentException("Hook ID must be non-negative");
        }
        this.hookId = hookId;
        return this;
    }

    /**
     * Set the contract that implements the hook.
     *
     * @param contractId the contract ID
     * @return {@code this}
     */
    public HookBuilder setContract(ContractId contractId) {
        this.contractId = Objects.requireNonNull(contractId, "contractId cannot be null");
        return this;
    }

    /**
     * Set the admin key for the hook.
     *
     * @param adminKey the admin key
     * @return {@code this}
     */
    public HookBuilder setAdminKey(Key adminKey) {
        this.adminKey = adminKey; // Allow null
        return this;
    }

    /**
     * Add a storage slot update.
     *
     * @param key the storage slot key
     * @param value the storage slot value
     * @return {@code this}
     */
    public HookBuilder addStorageSlot(byte[] key, byte[] value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        storageUpdates.add(new LambdaStorageUpdate.LambdaStorageSlot(key, value));
        return this;
    }

    /**
     * Add a mapping entries update.
     *
     * @param mappingSlot the mapping slot
     * @param entries the mapping entries
     * @return {@code this}
     */
    public HookBuilder addMappingEntries(byte[] mappingSlot, List<LambdaMappingEntry> entries) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        storageUpdates.add(new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, entries));
        return this;
    }

    /**
     * Add a single mapping entry.
     *
     * @param mappingSlot the mapping slot
     * @param key the mapping key
     * @param value the mapping value
     * @return {@code this}
     */
    public HookBuilder addMappingEntry(byte[] mappingSlot, byte[] key, byte[] value) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.ofKey(key, value);
        List<LambdaMappingEntry> entries = List.of(entry);
        return addMappingEntries(mappingSlot, entries);
    }

    /**
     * Add a mapping entry with preimage.
     *
     * @param mappingSlot the mapping slot
     * @param preimage the preimage bytes
     * @param value the mapping value
     * @return {@code this}
     */
    public HookBuilder addMappingEntryWithPreimage(byte[] mappingSlot, byte[] preimage, byte[] value) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(preimage, "preimage cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.withPreimage(preimage, value);
        List<LambdaMappingEntry> entries = List.of(entry);
        return addMappingEntries(mappingSlot, entries);
    }

    /**
     * Add a storage update.
     *
     * @param storageUpdate the storage update to add
     * @return {@code this}
     */
    public HookBuilder addStorageUpdate(LambdaStorageUpdate storageUpdate) {
        Objects.requireNonNull(storageUpdate, "storageUpdate cannot be null");
        storageUpdates.add(storageUpdate);
        return this;
    }

    /**
     * Set all storage updates at once.
     *
     * @param storageUpdates the list of storage updates
     * @return {@code this}
     */
    public HookBuilder setStorageUpdates(List<LambdaStorageUpdate> storageUpdates) {
        Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null");
        this.storageUpdates.clear();
        this.storageUpdates.addAll(storageUpdates);
        return this;
    }

    /**
     * Build the HookCreationDetails.
     *
     * @return the built HookCreationDetails
     * @throws IllegalStateException if required fields are missing
     */
    public HookCreationDetails build() {
        validateRequiredFields();

        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec, new ArrayList<>(storageUpdates));

        return new HookCreationDetails(extensionPoint, hookId, lambdaEvmHook, adminKey);
    }

    /**
     * Build the HookCreationDetails and validate it.
     *
     * @param context the context in which the hook will be used
     * @return the built and validated HookCreationDetails
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if validation fails
     */
    public HookCreationDetails buildAndValidate(HookValidator.HookContext context) {
        HookCreationDetails details = build();
        HookValidator.validateHookCreationDetails(List.of(details), context);
        return details;
    }

    /**
     * Validate that all required fields are set.
     *
     * @throws IllegalStateException if required fields are missing
     */
    private void validateRequiredFields() {
        if (extensionPoint == null) {
            throw new IllegalStateException("Extension point must be set");
        }

        if (hookId == null) {
            throw new IllegalStateException("Hook ID must be set");
        }

        if (contractId == null) {
            throw new IllegalStateException("Contract ID must be set");
        }
    }

    /**
     * Get the current extension point.
     *
     * @return the extension point or null if not set
     */
    public HookExtensionPoint getExtensionPoint() {
        return extensionPoint;
    }

    /**
     * Get the current hook ID.
     *
     * @return the hook ID or null if not set
     */
    public Long getHookId() {
        return hookId;
    }

    /**
     * Get the current contract ID.
     *
     * @return the contract ID or null if not set
     */
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * Get the current admin key.
     *
     * @return the admin key or null if not set
     */
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * Get the current storage updates.
     *
     * @return a copy of the storage updates list
     */
    public List<LambdaStorageUpdate> getStorageUpdates() {
        return new ArrayList<>(storageUpdates);
    }

    /**
     * Check if the builder is ready to build.
     *
     * @return true if all required fields are set
     */
    public boolean isReady() {
        return extensionPoint != null && hookId != null && contractId != null;
    }

    /**
     * Reset the builder to its initial state.
     *
     * @return {@code this}
     */
    public HookBuilder reset() {
        extensionPoint = null;
        hookId = null;
        contractId = null;
        adminKey = null;
        storageUpdates.clear();
        return this;
    }
}
