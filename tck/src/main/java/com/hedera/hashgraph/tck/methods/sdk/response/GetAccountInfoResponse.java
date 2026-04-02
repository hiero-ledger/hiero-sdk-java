// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for getAccountInfo query
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAccountInfoResponse {
    private String accountId;
    private String contractAccountId;
    private boolean isDeleted;
    private String proxyAccountId;
    private String proxyReceived;
    private String key;
    private String balance;
    private String sendRecordThreshold;
    private String receiveRecordThreshold;
    private boolean isReceiverSignatureRequired;
    private String expirationTime;

    // Explicit getters to ensure proper JSON serialization
    public boolean getIsDeleted() {
        return isDeleted;
    }

    public boolean getIsReceiverSignatureRequired() {
        return isReceiverSignatureRequired;
    }

    private String autoRenewPeriod;
    private List<LiveHashResponse> liveHashes;
    private Map<String, TokenRelationshipInfo> tokenRelationships;
    private String accountMemo;
    private String ownedNfts;
    private String maxAutomaticTokenAssociations;
    private String aliasKey;
    private String ledgerId;
    private List<HbarAllowanceResponse> hbarAllowances;
    private List<TokenAllowanceResponse> tokenAllowances;
    private List<TokenNftAllowanceResponse> nftAllowances;
    private String ethereumNonce;
    private StakingInfoResponse stakingInfo;

    /**
     * LiveHashResponse for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LiveHashResponse {
        private String accountId;
        private String hash;
        private List<String> keys;
        private String duration;
    }

    /**
     * TokenRelationshipInfo for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenRelationshipInfo {
        private String tokenId;
        private String symbol;
        private String balance;
        private Boolean isKycGranted;
        private Boolean isFrozen;
        private Boolean automaticAssociation;
    }

    /**
     * HbarAllowanceResponse for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HbarAllowanceResponse {
        private String ownerAccountId;
        private String spenderAccountId;
        private String amount;
    }

    /**
     * TokenAllowanceResponse for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenAllowanceResponse {
        private String tokenId;
        private String ownerAccountId;
        private String spenderAccountId;
        private String amount;
    }

    /**
     * TokenNftAllowanceResponse for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenNftAllowanceResponse {
        private String tokenId;
        private String ownerAccountId;
        private String spenderAccountId;
        private List<String> serialNumbers;
        private Boolean allSerials;
        private String delegatingSpender;
    }

    /**
     * StakingInfoResponse for account info
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StakingInfoResponse {
        private boolean declineStakingReward;
        private String stakePeriodStart;
        private String pendingReward;
        private String stakedToMe;
        private String stakedAccountId;
        private String stakedNodeId;
    }
}
