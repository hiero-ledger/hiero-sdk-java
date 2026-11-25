// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.clpr;

import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprThrottlesParams;

/**
 * Response for {@code getClprLedgerConfiguration}. Byte fields are hex without {@code 0x};
 * a state proof is {@code ""} when no signed snapshot is available.
 *
 * @param protocolVersion         the protocol version
 * @param chainId                 the chain ID
 * @param serviceAddress          the CLPR service address
 * @param timestamp               the consensus timestamp, as {@code seconds.nanoseconds}
 * @param throttles               the throttles; uint64 values are decimal strings
 * @param initialTrustAnchor      the initial trust anchor
 * @param initialTrustAnchorId    the initial trust anchor ID
 * @param configurationStateProof the configuration state proof
 */
public record ClprLedgerConfigurationResponse(
        long protocolVersion,
        String chainId,
        String serviceAddress,
        String timestamp,
        ClprThrottlesParams throttles,
        String initialTrustAnchor,
        String initialTrustAnchorId,
        String configurationStateProof) {}
