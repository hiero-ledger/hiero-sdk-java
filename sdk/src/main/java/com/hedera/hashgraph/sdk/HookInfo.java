// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hapi.node.hooks.legacy.HookCreationDetails;
import com.hedera.hapi.node.hooks.legacy.HookExtensionPoint;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Information about a hook attached to an account.
 */
public final class HookInfo {
    /**
     * The unique ID of the hook.
     */
    public final long hookId;

    /**
     * The extension point where this hook is attached.
     */
    public final HookExtensionPoint extensionPoint;

    /**
     * The admin key for managing this hook, if any.
     */
    @Nullable
    public final Key adminKey;

    /**
     * The hook implementation details (as protobuf).
     */
    public final HookCreationDetails protobuf;

    /**
     * Create a new HookInfo from protobuf data.
     *
     * @param protobuf the protobuf HookCreationDetails
     * @return the HookInfo
     */
    public static HookInfo fromProtobuf(HookCreationDetails protobuf) {
        return new HookInfo(
            protobuf.getHookId(),
            protobuf.getExtensionPoint(),
            protobuf.hasAdminKey() ? Key.fromProtobufKey(protobuf.getAdminKey()) : null,
            protobuf
        );
    }

    private HookInfo(
            long hookId,
            HookExtensionPoint extensionPoint,
            @Nullable Key adminKey,
            HookCreationDetails protobuf
    ) {
        this.hookId = hookId;
        this.extensionPoint = extensionPoint;
        this.adminKey = adminKey;
        this.protobuf = protobuf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HookInfo hookInfo = (HookInfo) o;
        return hookId == hookInfo.hookId &&
            extensionPoint == hookInfo.extensionPoint &&
            Objects.equals(adminKey, hookInfo.adminKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hookId, extensionPoint, adminKey);
    }

    @Override
    public String toString() {
        return "HookInfo{" +
            "hookId=" + hookId +
            ", extensionPoint=" + extensionPoint +
            ", adminKey=" + adminKey +
            '}';
    }
}