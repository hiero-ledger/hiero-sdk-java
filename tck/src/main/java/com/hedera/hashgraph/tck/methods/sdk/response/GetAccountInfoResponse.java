// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import java.util.Map;

/**
 * Represent response for getAccountInfo query.
 *
 * @param accountId the account ID
 * @param contractAccountId the contract account ID comprising both the contract instance and the cryptocurrency account
 * @param isDeleted if true, then this account has been deleted
 * @param proxyAccountId the account ID of the account to which this account is proxy staked
 * @param proxyReceived the total number of tinybars proxy staked to this account
 * @param key the key for the account, which must sign
 * @param balance the current balance of the account in tinybars
 * @param sendRecordThreshold the threshold amount (in tinybars) for which an account record is created for any send/withdraw transaction
 * @param receiveRecordThreshold the threshold amount (in tinybars) for which an account record is created for any receive/deposit transaction
 * @param isReceiverSignatureRequired if true, no transaction can transfer to this account unless signed by this account's key
 * @param expirationTime the time at which this account is set to expire
 * @param autoRenewPeriod the duration for expiration time will extend every this many seconds
 * @param liveHashes all the livehashes attached to the account
 * @param tokenRelationships all tokens related to this account
 * @param accountMemo the memo associated with the account
 * @param ownedNfts the number of NFTs owned by this account
 * @param maxAutomaticTokenAssociations the maximum number of tokens that an account can be implicitly associated with
 * @param aliasKey the public key to be used as the account's alias
 * @param ledgerId the ID of the ledger from which the response was returned
 * @param hbarAllowances list of hbar allowances approved by this account
 * @param tokenAllowances list of fungible token allowances approved by this account
 * @param nftAllowances list of non-fungible token allowances approved by this account
 * @param ethereumNonce the ethereum transaction nonce associated with this account
 * @param stakingInfo the staking metadata for this account
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
        StakingInfoResponse stakingInfo) {

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
