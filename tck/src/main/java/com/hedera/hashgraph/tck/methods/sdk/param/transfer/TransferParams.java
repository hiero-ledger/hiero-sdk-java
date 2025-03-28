// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.transfer;

import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contains the parameters of a transfer.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransferParams {
    private Optional<HbarTransferParams> hbar;
    private Optional<TokenTransferParams> token;
    private Optional<NftTransferParams> nft;
    private Optional<Boolean> approved;

    public static TransferParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedHbar = Optional.ofNullable(jrpcParams.get("hbar"))
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) obj;
                        return HbarTransferParams.parse(map);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var parsedToken = Optional.ofNullable(jrpcParams.get("token"))
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) obj;
                        return TokenTransferParams.parse(map);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var parsedNft = Optional.ofNullable(jrpcParams.get("nft"))
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) obj;
                        return NftTransferParams.parse(map);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var parsedApproved = Optional.ofNullable((Boolean) jrpcParams.get("approved"));

        // Only one allowance type should be allowed
        final boolean hasOnlyHbar = parsedHbar.isPresent() && parsedToken.isEmpty() && parsedNft.isEmpty();
        final boolean hasOnlyToken = parsedHbar.isEmpty() && parsedToken.isPresent() && parsedNft.isEmpty();
        final boolean hasOnlyNft = parsedHbar.isEmpty() && parsedToken.isEmpty() && parsedNft.isPresent();

        if (!hasOnlyHbar && !hasOnlyToken && !hasOnlyNft) {
            throw new IllegalArgumentException("invalid parameters: only one type of transfer SHALL be provided.");
        }

        return new TransferParams(parsedHbar, parsedToken, parsedNft, parsedApproved);
    }
}
