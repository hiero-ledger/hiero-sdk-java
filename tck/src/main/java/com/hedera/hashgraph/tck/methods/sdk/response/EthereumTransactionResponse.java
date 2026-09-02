// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

/**
 * Represent ethereumTransaction response.
 *
 * @param status the status of the submitted transaction
 * @param contractId the ID of the created contract
 */
public record EthereumTransactionResponse(String status, String contractId) {}
