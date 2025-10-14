// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * The ID of a hook.
 * <p>
 * This class represents the HookId protobuf message, which contains the hook's creating entity ID
 * and an arbitrary 64-bit identifier.
 */
public class HookId {
    private final HookEntityId entityId;
    private final long hookId;

    /**
     * Create a new HookId.
     *
     * @param entityId the hook's creating entity ID
     * @param hookId the arbitrary 64-bit identifier
     */
    public HookId(HookEntityId entityId, long hookId) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.hookId = hookId;
    }

    /**
     * Get the hook's creating entity ID.
     *
     * @return the entity ID
     */
    public HookEntityId getEntityId() {
        return entityId;
    }

    /**
     * Get the hook ID.
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
    com.hedera.hashgraph.sdk.proto.HookId toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.HookId.newBuilder()
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
    static HookId fromProtobuf(com.hedera.hashgraph.sdk.proto.HookId proto) {
        return new HookId(HookEntityId.fromProtobuf(proto.getEntityId()), proto.getHookId());
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
        int result = entityId.hashCode();
        result = 31 * result + Long.hashCode(hookId);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("entityId", entityId)
                .add("hookId", hookId)
                .toString();
    }
}
