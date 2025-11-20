// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Specifies the details of a hook's creation.
 * <p>
 * This class contains all the information needed to create a new hook,
 * including the extension point, hook ID, implementation, and optional admin key.
 */
public class HookCreationDetails {
    private final HookExtensionPoint extensionPoint;
    private final long hookId;
    private final LambdaEvmHook hook;

    @Nullable
    private final Key adminKey;

    /**
     * Create new hook creation details with an admin key.
     *
     * @param extensionPoint the extension point for the hook
     * @param hookId the ID to create the hook at
     * @param hook the hook implementation
     * @param adminKey the admin key for managing the hook
     */
    public HookCreationDetails(
            HookExtensionPoint extensionPoint, long hookId, LambdaEvmHook hook, @Nullable Key adminKey) {
        this.extensionPoint = Objects.requireNonNull(extensionPoint, "extensionPoint cannot be null");
        this.hookId = hookId;
        this.hook = Objects.requireNonNull(hook, "hook cannot be null");
        this.adminKey = adminKey;
    }

    /**
     * Create new hook creation details without an admin key.
     *
     * @param extensionPoint the extension point for the hook
     * @param hookId the ID to create the hook at
     * @param hook the hook implementation
     */
    public HookCreationDetails(HookExtensionPoint extensionPoint, long hookId, LambdaEvmHook hook) {
        this(extensionPoint, hookId, hook, null);
    }

    /**
     * Get the extension point for this hook.
     *
     * @return the extension point
     */
    public HookExtensionPoint getExtensionPoint() {
        return extensionPoint;
    }

    /**
     * Get the ID to create the hook at.
     *
     * @return the hook ID
     */
    public long getHookId() {
        return hookId;
    }

    /**
     * Get the hook implementation.
     *
     * @return the hook implementation
     */
    public LambdaEvmHook getHook() {
        return hook;
    }

    /**
     * Get the admin key for this hook.
     *
     * @return the admin key, or null if not set
     */
    @Nullable
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * Check if this hook has an admin key.
     *
     * @return true if an admin key is set, false otherwise
     */
    public boolean hasAdminKey() {
        return adminKey != null;
    }

    /**
     * Convert this HookCreationDetails to a protobuf message.
     *
     * @return the protobuf HookCreationDetails
     */
    com.hedera.hashgraph.sdk.proto.HookCreationDetails toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.HookCreationDetails.newBuilder()
                .setExtensionPoint(extensionPoint.getProtoValue())
                .setHookId(hookId)
                .setLambdaEvmHook(hook.toProtobuf());

        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }

        return builder.build();
    }

    /**
     * Create HookCreationDetails from a protobuf message.
     *
     * @param proto the protobuf HookCreationDetails
     * @return a new HookCreationDetails instance
     */
    public static HookCreationDetails fromProtobuf(com.hedera.hashgraph.sdk.proto.HookCreationDetails proto) {
        var adminKey = proto.hasAdminKey() ? Key.fromProtobufKey(proto.getAdminKey()) : null;

        return new HookCreationDetails(
                HookExtensionPoint.fromProtobuf(proto.getExtensionPoint()),
                proto.getHookId(),
                LambdaEvmHook.fromProtobuf(proto.getLambdaEvmHook()),
                adminKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HookCreationDetails that = (HookCreationDetails) o;
        return hookId == that.hookId
                && extensionPoint == that.extensionPoint
                && hook.equals(that.hook)
                && Objects.equals(adminKey, that.adminKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensionPoint, hookId, hook, adminKey);
    }

    @Override
    public String toString() {
        return "HookCreationDetails{" + "extensionPoint="
                + extensionPoint + ", hookId="
                + hookId + ", hook="
                + hook + ", adminKey="
                + adminKey + "}";
    }
}
