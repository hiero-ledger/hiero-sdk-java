// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.TokenAirdropTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2ParamsException;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferParams;

// Utility class for handling airdrop parameters
public class AirdropUtils {

    /**
     * Handles a single airdrop transfer parameter and adds it to the transaction
     *
     * @param transaction The token airdrop transaction
     * @param transferParam The transfer parameter to handle
     * @throws Exception If an error occurs during processing
     */
    public static void handleAirdropParam(TokenAirdropTransaction transaction, TransferParams transferParam)
            throws Exception {
        if (transferParam.getToken().isPresent()) {
            handleAirdropTokenTransfer(transaction, transferParam);
        } else if (transferParam.getNft().isPresent()) {
            handleAirdropNftTransfer(transaction, transferParam);
        } else {
            throw new InvalidJSONRPC2ParamsException("Invalid transfer parameter");
        }
    }

    /**
     * Handles a token transfer parameter
     *
     * @param transaction The token airdrop transaction
     * @param transferParam The transfer parameter containing token details
     * @throws Exception If an error occurs during processing
     */
    private static void handleAirdropTokenTransfer(TokenAirdropTransaction transaction, TransferParams transferParam)
            throws Exception {
        var token = transferParam.getToken().get();

        AccountId accountId = AccountId.fromString(
                token.getAccountId().orElseThrow(() -> new InvalidJSONRPC2ParamsException("AccountId is required")));

        TokenId tokenId = TokenId.fromString(
                token.getTokenId().orElseThrow(() -> new InvalidJSONRPC2ParamsException("TokenId is required")));

        long amount;
        try {
            amount = Long.parseLong(
                    token.getAmount().orElseThrow(() -> new InvalidJSONRPC2ParamsException("Amount is required")));
        } catch (NumberFormatException e) {
            throw new InvalidJSONRPC2ParamsException("Invalid amount format");
        }

        boolean isApproved = transferParam.getApproved().isPresent()
                && transferParam.getApproved().get();

        if (token.getDecimals().isPresent()) {
            int decimals = token.getDecimals().get().intValue();
            if (isApproved) {
                transaction.addApprovedTokenTransferWithDecimals(tokenId, accountId, amount, decimals);
            } else {
                transaction.addTokenTransferWithDecimals(tokenId, accountId, amount, decimals);
            }
        } else {
            if (isApproved) {
                transaction.addApprovedTokenTransfer(tokenId, accountId, amount);
            } else {
                transaction.addTokenTransfer(tokenId, accountId, amount);
            }
        }
    }

    /**
     * Handles an NFT transfer parameter
     *
     * @param transaction The token airdrop transaction
     * @param transferParam The transfer parameter containing NFT details
     * @throws Exception If an error occurs during processing
     */
    private static void handleAirdropNftTransfer(TokenAirdropTransaction transaction, TransferParams transferParam)
            throws Exception {
        var nft = transferParam.getNft().get();

        AccountId senderAccountId = AccountId.fromString(nft.getSenderAccountId()
                .orElseThrow(() -> new InvalidJSONRPC2ParamsException("SenderAccountId is required")));

        AccountId receiverAccountId = AccountId.fromString(nft.getReceiverAccountId()
                .orElseThrow(() -> new InvalidJSONRPC2ParamsException("ReceiverAccountId is required")));

        long serialNumber;
        try {
            serialNumber = Long.parseLong(nft.getSerialNumber()
                    .orElseThrow(() -> new InvalidJSONRPC2ParamsException("SerialNumber is required")));
        } catch (NumberFormatException e) {
            throw new InvalidJSONRPC2ParamsException("Invalid serial number format");
        }

        TokenId tokenId = TokenId.fromString(
                nft.getTokenId().orElseThrow(() -> new InvalidJSONRPC2ParamsException("TokenId is required")));

        NftId nftId = new NftId(tokenId, serialNumber);

        boolean isApproved = transferParam.getApproved().isPresent()
                && transferParam.getApproved().get();

        if (isApproved) {
            transaction.addApprovedNftTransfer(nftId, senderAccountId, receiverAccountId);
        } else {
            transaction.addNftTransfer(nftId, senderAccountId, receiverAccountId);
        }
    }
}
