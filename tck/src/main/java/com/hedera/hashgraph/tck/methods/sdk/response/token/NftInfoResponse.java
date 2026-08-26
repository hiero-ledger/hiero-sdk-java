// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import javax.annotation.Nullable;

public record NftInfoResponse(
        String nftId,
        String accountId,
        String creationTime,
        String metadata,
        String ledgerId,
        @Nullable String spenderId) {}
