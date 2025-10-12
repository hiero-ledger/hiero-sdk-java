// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Enum representing the types of hooks that can be attached to HBAR transfers.
 * <p>
 * These types correspond to the protobuf definitions for hook calls in the
 * AccountAmount message, specifically the hook_call oneof field.
 */
public enum HookType {
    /**
     * A single call made before attempting the CryptoTransfer, to a
     * method with logical signature allow(HookContext, ProposedTransfers).
     * <p>
     * This corresponds to the pre_tx_allowance_hook field in the protobuf.
     */
    PRE_TX_ALLOWANCE_HOOK,

    /**
     * Two calls, the first call before attempting the CryptoTransfer, to a
     * method with logical signature allowPre(HookContext, ProposedTransfers);
     * and the second call after attempting the CryptoTransfer, to a method
     * with logical signature allowPost(HookContext, ProposedTransfers).
     * <p>
     * This corresponds to the pre_post_tx_allowance_hook field in the protobuf.
     */
    PRE_POST_TX_ALLOWANCE_HOOK
}
