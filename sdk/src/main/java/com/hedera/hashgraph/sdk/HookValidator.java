// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for validating hook-related data structures and transactions.
 * <p>
 * This class provides static methods to validate hook configurations,
 * storage updates, and transaction parameters to ensure they meet
 * the requirements of the Hedera network.
 */
public final class HookValidator {

    private HookValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a list of hook creation details.
     * <p>
     * Ensures that:
     * - Hook IDs are unique within the list
     * - All required fields are present
     * - Extension points are valid for the context
     *
     * @param hookDetails the list of hook creation details to validate
     * @param context the context in which the hooks will be used
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateHookCreationDetails(List<HookCreationDetails> hookDetails, HookContext context) {
        if (hookDetails == null) {
            throw new IllegalArgumentException("Hook creation details cannot be null");
        }

        if (hookDetails.isEmpty()) {
            return; // Empty list is valid
        }

        // Validate hook IDs are unique
        Set<Long> hookIds = new HashSet<>();
        for (HookCreationDetails details : hookDetails) {
            if (details == null) {
                throw new IllegalArgumentException("Hook creation details cannot contain null elements");
            }

            if (!hookIds.add(details.getHookId())) {
                throw new IllegalArgumentException("Duplicate hook ID: " + details.getHookId());
            }

            // Validate extension point is appropriate for context
            validateExtensionPointForContext(details.getExtensionPoint(), context);
        }
    }

    /**
     * Validates a hook ID.
     *
     * @param hookId the hook ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateHookId(HookId hookId) {
        if (hookId == null) {
            throw new IllegalArgumentException("Hook ID cannot be null");
        }

        if (hookId.getEntityId() == null) {
            throw new IllegalArgumentException("Hook ID must have a valid entity ID");
        }

        if (hookId.getHookId() < 0) {
            throw new IllegalArgumentException("Hook ID must be non-negative");
        }
    }

    /**
     * Validates a list of storage updates.
     *
     * @param storageUpdates the storage updates to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateStorageUpdates(List<LambdaStorageUpdate> storageUpdates) {
        if (storageUpdates == null) {
            throw new IllegalArgumentException("Storage updates cannot be null");
        }

        if (storageUpdates.isEmpty()) {
            throw new IllegalArgumentException("At least one storage update must be provided");
        }

        // Check for reasonable limits (this would be enforced server-side)
        if (storageUpdates.size() > 1000) {
            throw new IllegalArgumentException("Too many storage updates: " + storageUpdates.size() + " (max 1000)");
        }

        for (LambdaStorageUpdate update : storageUpdates) {
            if (update == null) {
                throw new IllegalArgumentException("Storage updates cannot contain null elements");
            }
            validateStorageUpdate(update);
        }
    }

    /**
     * Validates a single storage update.
     *
     * @param storageUpdate the storage update to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateStorageUpdate(LambdaStorageUpdate storageUpdate) {
        if (storageUpdate == null) {
            throw new IllegalArgumentException("Storage update cannot be null");
        }

        if (storageUpdate instanceof LambdaStorageUpdate.LambdaStorageSlot) {
            validateStorageSlot((LambdaStorageUpdate.LambdaStorageSlot) storageUpdate);
        } else if (storageUpdate instanceof LambdaStorageUpdate.LambdaMappingEntries) {
            validateMappingEntries((LambdaStorageUpdate.LambdaMappingEntries) storageUpdate);
        }
    }

    /**
     * Validates a storage slot update.
     *
     * @param storageSlot the storage slot to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateStorageSlot(LambdaStorageUpdate.LambdaStorageSlot storageSlot) {
        if (storageSlot == null) {
            throw new IllegalArgumentException("Storage slot cannot be null");
        }

        validateStorageSlotBytes(storageSlot.getKey(), "Storage slot key");
        validateStorageSlotBytes(storageSlot.getValue(), "Storage slot value");
    }

    /**
     * Validates mapping entries update.
     *
     * @param mappingEntries the mapping entries to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateMappingEntries(LambdaStorageUpdate.LambdaMappingEntries mappingEntries) {
        if (mappingEntries == null) {
            throw new IllegalArgumentException("Mapping entries cannot be null");
        }

        validateStorageSlotBytes(mappingEntries.getMappingSlot(), "Mapping slot");

        List<LambdaMappingEntry> entries = mappingEntries.getEntries();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Mapping entries must contain at least one entry");
        }

        for (LambdaMappingEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("Mapping entries cannot contain null elements");
            }
            validateMappingEntry(entry);
        }
    }

    /**
     * Validates a mapping entry.
     *
     * @param mappingEntry the mapping entry to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateMappingEntry(LambdaMappingEntry mappingEntry) {
        if (mappingEntry == null) {
            throw new IllegalArgumentException("Mapping entry cannot be null");
        }

        if (mappingEntry.getKey() != null) {
            validateStorageSlotBytes(mappingEntry.getKey(), "Mapping entry key");
        }

        if (mappingEntry.getPreimage() != null) {
            // Preimage can be any length, but should not be empty
            if (mappingEntry.getPreimage().length == 0) {
                throw new IllegalArgumentException("Mapping entry preimage cannot be empty");
            }
        }

        validateStorageSlotBytes(mappingEntry.getValue(), "Mapping entry value");
    }

    /**
     * Validates storage slot bytes (key or value).
     *
     * @param bytes the bytes to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateStorageSlotBytes(byte[] bytes, String fieldName) {
        if (bytes == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }

        if (bytes.length > 32) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 32 bytes (got " + bytes.length + ")");
        }

        if (bytes.length > 0 && bytes[0] == 0) {
            throw new IllegalArgumentException(fieldName + " must use minimal byte representation (no leading zeros)");
        }
    }

    /**
     * Validates that an extension point is appropriate for a given context.
     *
     * @param extensionPoint the extension point to validate
     * @param context the context in which it will be used
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateExtensionPointForContext(HookExtensionPoint extensionPoint, HookContext context) {
        if (extensionPoint == null) {
            throw new IllegalArgumentException("Extension point cannot be null");
        }

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        switch (context) {
            case ACCOUNT_CREATION:
                if (extensionPoint != HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK) {
                    throw new IllegalArgumentException("Unsupported extension point for account creation: " + extensionPoint);
                }
                break;
            case CONTRACT_CREATION:
                // Future: Add contract creation extension points
                throw new IllegalArgumentException("Contract creation hooks not yet supported");
            case GENERAL:
                // All extension points are valid in general context
                break;
            default:
                throw new IllegalArgumentException("Unknown context: " + context);
        }
    }

    /**
     * Validates a hook creation details for a specific entity.
     *
     * @param hookDetails the hook creation details to validate
     * @param entityId the entity ID for which the hook is being created
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateHookCreationForEntity(HookCreationDetails hookDetails, HookEntityId entityId) {
        if (hookDetails == null) {
            throw new IllegalArgumentException("Hook creation details cannot be null");
        }

        if (entityId == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }

        // Validate the hook details themselves
        validateHookCreationDetails(List.of(hookDetails), HookContext.GENERAL);

        // Additional entity-specific validation could be added here
        // For example, checking if the entity has permission to create hooks
    }

    /**
     * Context in which hooks are being used.
     */
    public enum HookContext {
        /**
         * Hooks being created during account creation.
         */
        ACCOUNT_CREATION,

        /**
         * Hooks being created during contract creation.
         */
        CONTRACT_CREATION,

        /**
         * General hook usage context.
         */
        GENERAL
    }
}
