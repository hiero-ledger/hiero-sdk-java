// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

// Using fully qualified names to avoid conflicts with generated classes

/**
 * Enum representing the Hiero extension points that accept a hook.
 * <p>
 * Extension points define where hooks can be attached to customize behavior
 * in the Hiero network.
 */
public enum HookExtensionPoint {
    /**
     * Used to customize an account's allowances during a CryptoTransfer transaction.
     * <p>
     * This hook allows accounts to define custom logic for approving or rejecting
     * token transfers, providing fine-grained control over allowance behavior.
     */
    ACCOUNT_ALLOWANCE_HOOK(org.hiero.sdk.proto.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK);

    private final org.hiero.sdk.proto.HookExtensionPoint protoValue;

    HookExtensionPoint(org.hiero.sdk.proto.HookExtensionPoint protoValue) {
        this.protoValue = protoValue;
    }

    /**
     * Get the protobuf value for this extension point.
     *
     * @return the protobuf enum value
     */
    public org.hiero.sdk.proto.HookExtensionPoint getProtoValue() {
        return protoValue;
    }

    /**
     * Create a HookExtensionPoint from a protobuf value.
     *
     * @param protoValue the protobuf enum value
     * @return the corresponding HookExtensionPoint
     * @throws IllegalArgumentException if the protobuf value is not recognized
     */
    public static HookExtensionPoint fromProtobuf(org.hiero.sdk.proto.HookExtensionPoint protoValue) {
        return switch (protoValue) {
            case ACCOUNT_ALLOWANCE_HOOK -> ACCOUNT_ALLOWANCE_HOOK;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized hook extension point: " + protoValue);
        };
    }
}
