// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

/**
 * Represent the mirror node account balance response.
 *
 * @param hbars the hbar balance of the account in tinybars
 */
public record MirrorNodeAccountBalanceResponse(String hbars) {}
