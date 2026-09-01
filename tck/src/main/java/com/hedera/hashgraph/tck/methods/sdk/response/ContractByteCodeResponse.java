// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

/**
 * Represent the contractByteCode response.
 *
 * @param contractId the ID of the contract
 * @param bytecode the contract bytecode hex string
 */
public record ContractByteCodeResponse(String contractId, String bytecode) {}
