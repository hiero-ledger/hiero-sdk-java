// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import java.util.Objects;

/**
 * Represents a unique identifier for a hook.
 * <p>
 * A HookId consists of the entity that owns the hook and a unique 64-bit
 * identifier within that entity's scope.
 */
public class HookId {
    private final HookEntityId entityId;
    private final long hookId;

    /**
     * Create a new HookId.
     *
     * @param entityId the entity that owns the hook
     * @param hookId the unique 64-bit identifier for the hook
     */
    public HookId(HookEntityId entityId, long hookId) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.hookId = hookId;
    }

    /**
     * Get the entity that owns this hook.
     *
     * @return the hook entity ID
     */
    public HookEntityId getEntityId() {
        return entityId;
    }

    /**
     * Get the unique 64-bit identifier for this hook.
     *
     * @return the hook ID
     */
    public long getHookId() {
        return hookId;
    }

    /**
     * Convert this HookId to a protobuf message.
     *
     * @return the protobuf HookId
     */
    public com.hedera.hapi.node.hooks.legacy.HookId toProtobuf() {
        return com.hedera.hapi.node.hooks.legacy.HookId.newBuilder()
                .setEntityId(entityId.toProtobuf())
                .setHookId(hookId)
                .build();
    }

    /**
     * Create a HookId from a protobuf message.
     *
     * @param proto the protobuf HookId
     * @return a new HookId instance
     */
    public static HookId fromProtobuf(com.hedera.hapi.node.hooks.legacy.HookId proto) {
        return new HookId(
                HookEntityId.fromProtobuf(proto.getEntityId()),
                proto.getHookId()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        HookId hookId1 = (HookId) o;
        return hookId == hookId1.hookId && entityId.equals(hookId1.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, hookId);
    }

    @Override
    public String toString() {
        return "HookId{entityId=" + entityId + ", hookId=" + hookId + "}";
    }
}
