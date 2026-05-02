// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.account;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AllowanceParams {

    private Optional<String> ownerAccountId;
    private Optional<String> spenderAccountId;
    private Optional<String> tokenId;
    private Optional<Collection<String>> serialNumbers;
    private Optional<HbarAllowance> hbar;
    private Optional<TokenAllowance> token;
    private Optional<TokenNftAllowance> nft;

    public static AllowanceParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedOwnerAccountId = Optional.ofNullable((String) jrpcParams.get("ownerAccountId"));
        var parsedSpenderAccountId = Optional.ofNullable((String) jrpcParams.get("spenderAccountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var serialNumbers = Optional.ofNullable((Collection<String>) jrpcParams.get("serialNumbers"));

        Optional<HbarAllowance> parsedHbar = Optional.empty();
        if (jrpcParams.containsKey("hbar")) {
            Object hbarObject = jrpcParams.get("hbar");

            if (hbarObject instanceof Map) {
                Map<String, Object> hbarMap = (Map<String, Object>) hbarObject;
                var amount = (String) hbarMap.get("amount");
                parsedHbar = Optional.of(new HbarAllowance(amount));
            }
        }

        Optional<TokenAllowance> parsedToken = Optional.empty();
        if (jrpcParams.containsKey("token")) {
            Object tokenObject = jrpcParams.get("token");
            if (tokenObject instanceof Map) {
                var tokenMap = (Map<String, Object>) tokenObject;
                var amount = Long.parseLong((String) tokenMap.get("amount"));
                var tokenId = tokenMap.get("tokenId").toString();

                parsedToken = Optional.of(new TokenAllowance(
                        tokenId, parsedOwnerAccountId.orElseThrow(), parsedSpenderAccountId.orElseThrow(), amount));
            }
        }

        Optional<TokenNftAllowance> parsedNft = Optional.empty();
        if (jrpcParams.containsKey("nft")) {
            Object tokenObject = jrpcParams.get("nft");
            if (tokenObject instanceof Map) {
                var nftMap = (Map<String, Object>) tokenObject;
                var tokenId = nftMap.get("tokenId").toString();
                var delegateSpenderAccountId = (String) nftMap.get("delegateSpenderAccountId");
                var approvedForAll = (Boolean) nftMap.get("approvedForAll");
                var nftSerialNumbers = (List<String>) nftMap.get("serialNumbers");
                parsedNft = Optional.of(new TokenNftAllowance(
                        tokenId,
                        parsedOwnerAccountId.orElseThrow(),
                        parsedSpenderAccountId.orElseThrow(),
                        delegateSpenderAccountId,
                        nftSerialNumbers == null
                                ? new ArrayList<>()
                                : nftSerialNumbers.stream().map(Long::parseLong).toList(),
                        approvedForAll));
            }
        }

        return new AllowanceParams(
                parsedOwnerAccountId,
                parsedSpenderAccountId,
                parsedTokenId,
                serialNumbers,
                parsedHbar,
                parsedToken,
                parsedNft);
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HbarAllowance {
        private String amount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenAllowance {
        private String tokenId;
        private String ownerAccountId;
        private String spenderAccountId;
        private long amount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenNftAllowance {
        private String tokenId;
        private String ownerAccountId;
        private String spenderAccountId;
        private String delegatingSpender;
        private List<Long> serialNumbers;
        private Boolean allSerials;
    }
}
