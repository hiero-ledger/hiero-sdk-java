// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import javax.annotation.Nullable;
import lombok.Data;

@Data
public class NftInfoResponse {

    /**
     * The ID of the NFT
     */
    public final String nftId;

    /**
     * The current owner of the NFT
     */
    public final String accountId;

    /**
     * The effective consensus timestamp at which the NFT was minted
     */
    public final String creationTime;

    /**
     * Represents the unique metadata of the NFT
     */
    public final String metadata;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    public final String ledgerId;

    /**
     * If an allowance is granted for the NFT, its corresponding spender account
     */
    @Nullable
    public final String spenderId;
}
