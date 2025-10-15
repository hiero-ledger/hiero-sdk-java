// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Hook type for NFT transfers, indicating side (sender/receiver) and timing (pre / pre-post).
 */
public enum NftHookType {
    PRE_HOOK_SENDER,
    PRE_POST_HOOK_SENDER,
    PRE_HOOK_RECEIVER,
    PRE_POST_HOOK_RECEIVER
}
