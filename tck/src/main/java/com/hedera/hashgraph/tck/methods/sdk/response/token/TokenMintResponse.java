// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;
import java.util.List;

/**
 * Represent tokenMint transaction response.
 *
 * @param tokenId the ID of the token to burn
 * @param status the status of the submitted transaction
 * @param newTotalSupply the new total amount of tokens
 * @param serialNumbers list of serial numbers for the NFTs
 */
public record TokenMintResponse(String tokenId, Status status, String newTotalSupply, List<String> serialNumbers) {}
