// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.clpr;

import com.hedera.hashgraph.sdk.Status;

/**
 * Response for the CLPR completion transactions, with the ownership proof they revealed.
 *
 * @param status          the receipt status
 * @param publicKey       hex canonical ownership public key, or null
 * @param signature       hex ownership signature, or null
 * @param signatureScheme {@code ECDSA_SECP256K1} or {@code ED25519}, or null
 */
public record ClprCompletionResponse(Status status, String publicKey, String signature, String signatureScheme) {}
