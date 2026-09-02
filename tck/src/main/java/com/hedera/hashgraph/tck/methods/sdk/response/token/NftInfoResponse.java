// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import javax.annotation.Nullable;

/**
 * Represent nftInfo query responsee.
 *
 * @param nftId the ID of the NFT
 * @param accountId the current owner of the NFT
 * @param creationTime the effective consensus timestamp at which the NFT was minted
 * @param metadata the unique metadata of the NFT
 * @param ledgerId the ledger ID the response was returned from
 * @param spenderId if an allowance is granted for the NFT, its corresponding spender account
 */
public record NftInfoResponse(
        String nftId,
        String accountId,
        String creationTime,
        String metadata,
        String ledgerId,
        @Nullable String spenderId) {}
