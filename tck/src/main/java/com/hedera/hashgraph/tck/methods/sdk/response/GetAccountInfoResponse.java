// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import java.util.Map;

/**
 * Response for getAccountInfo query
 */
public record GetAccountInfoResponse(
        String accountId,
        String contractAccountId,
        boolean isDeleted,
        String proxyAccountId,
        String proxyReceived,
        String key,
        String balance,
        String sendRecordThreshold,
        String receiveRecordThreshold,
        boolean isReceiverSignatureRequired,
        String expirationTime,
        String autoRenewPeriod,
        List<LiveHashResponse> liveHashes,
        Map<String, TokenRelationshipInfo> tokenRelationships,
        String accountMemo,
        String ownedNfts,
        String maxAutomaticTokenAssociations,
        String aliasKey,
        String ledgerId,
        List<HbarAllowanceResponse> hbarAllowances,
        List<TokenAllowanceResponse> tokenAllowances,
        List<TokenNftAllowanceResponse> nftAllowances,
        String ethereumNonce,
        com.hedera.hashgraph.tck.methods.sdk.response.GetAccountInfoResponse.StakingInfoResponse stakingInfo) {
    /**
     * LiveHashResponse for account info
     */
    public record LiveHashResponse(String accountId, String hash, List<String> keys, String duration) {}

    /**
     * TokenRelationshipInfo for account info
     */
    public record TokenRelationshipInfo(
            String tokenId,
            String symbol,
            String balance,
            Boolean isKycGranted,
            Boolean isFrozen,
            Boolean automaticAssociation) {}

    /**
     * HbarAllowanceResponse for account info
     */
    public record HbarAllowanceResponse(String ownerAccountId, String spenderAccountId, String amount) {}

    /**
     * TokenAllowanceResponse for account info
     */
    public record TokenAllowanceResponse(
            String tokenId, String ownerAccountId, String spenderAccountId, String amount) {}

    /**
     * TokenNftAllowanceResponse for account info
     */
    public record TokenNftAllowanceResponse(
            String tokenId,
            String ownerAccountId,
            String spenderAccountId,
            List<String> serialNumbers,
            Boolean allSerials,
            String delegatingSpender) {}

    /**
     * StakingInfoResponse for account info
     */
    public record StakingInfoResponse(
            boolean declineStakingReward,
            String stakePeriodStart,
            String pendingReward,
            String stakedToMe,
            String stakedAccountId,
            String stakedNodeId) {}
}
