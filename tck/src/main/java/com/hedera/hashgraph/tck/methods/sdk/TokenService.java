// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2ParamsException;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.token.*;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferParams;
import com.hedera.hashgraph.tck.methods.sdk.response.token.*;
import com.hedera.hashgraph.tck.util.AirdropUtils;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TokenService for token related methods
 */
@JSONRPC2Service
public class TokenService extends AbstractJSONRPC2Service {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public TokenService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createToken")
    public TokenResponse createToken(final TokenCreateParams params) throws Exception {
        TokenCreateTransaction tokenCreateTransaction = TransactionBuilders.TokenBuilder.buildCreate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenCreateTransaction, sdkService.getClient()));

        TransactionReceipt transactionReceipt =
                tokenCreateTransaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        String tokenId = "";
        if (transactionReceipt.status == Status.SUCCESS) {
            tokenId = transactionReceipt.tokenId.toString();
        }

        return new TokenResponse(tokenId, transactionReceipt.status);
    }

    @JSONRPC2Method("updateToken")
    public TokenResponse updateToken(final TokenUpdateParams params) throws Exception {
        TokenUpdateTransaction tokenUpdateTransaction = TransactionBuilders.TokenBuilder.buildUpdate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenUpdateTransaction, sdkService.getClient()));

        TransactionReceipt transactionReceipt =
                tokenUpdateTransaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", transactionReceipt.status);
    }

    @JSONRPC2Method("deleteToken")
    public TokenResponse deleteToken(final TokenDeleteParams params) throws Exception {
        TokenDeleteTransaction tokenDeleteTransaction = TransactionBuilders.TokenBuilder.buildDelete(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenDeleteTransaction, sdkService.getClient()));

        TransactionReceipt transactionReceipt =
                tokenDeleteTransaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", transactionReceipt.status);
    }

    @JSONRPC2Method("updateTokenFeeSchedule")
    public TokenResponse updateTokenFeeSchedule(TokenUpdateFeeScheduleParams params) throws Exception {
        TokenFeeScheduleUpdateTransaction transaction =
                new TokenFeeScheduleUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

        params.getCustomFees()
                .ifPresent(customFees ->
                        transaction.setCustomFees(customFees.get(0).fillOutCustomFees(customFees)));

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("freezeToken")
    public TokenResponse tokenFreezeTransaction(FreezeUnfreezeTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));
        params.getAccountId().ifPresent(accountId -> mapParams.put("accountId", accountId));

        TokenFreezeTransaction transaction = TransactionBuilders.TokenBuilder.buildFreeze(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("unfreezeToken")
    public TokenResponse tokenUnfreezeTransaction(FreezeUnfreezeTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));
        params.getAccountId().ifPresent(accountId -> mapParams.put("accountId", accountId));

        TokenUnfreezeTransaction transaction = TransactionBuilders.TokenBuilder.buildUnfreeze(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("associateToken")
    public TokenResponse associateToken(AssociateDisassociateTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = Map.of(
            "accountId", params.getAccountId().orElse(""),
            "tokenIds", params.getTokenIds().orElse(List.of())
        );

        TokenAssociateTransaction transaction = TransactionBuilders.TokenBuilder.buildAssociate(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("dissociateToken")
    public TokenResponse dissociateToken(AssociateDisassociateTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = Map.of(
            "accountId", params.getAccountId().orElse(""),
            "tokenIds", params.getTokenIds().orElse(List.of())
        );

        TokenDissociateTransaction transaction = TransactionBuilders.TokenBuilder.buildDissociate(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("pauseToken")
    public TokenResponse pauseToken(PauseUnpauseTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));

        TokenPauseTransaction transaction = TransactionBuilders.TokenBuilder.buildPause(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("unpauseToken")
    public TokenResponse tokenUnpauseTransaction(PauseUnpauseTokenParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));

        TokenUnpauseTransaction transaction = TransactionBuilders.TokenBuilder.buildUnpause(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("grantTokenKyc")
    public TokenResponse grantTokenKyc(GrantRevokeTokenKycParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));
        params.getAccountId().ifPresent(accountId -> mapParams.put("accountId", accountId));

        TokenGrantKycTransaction transaction = TransactionBuilders.TokenBuilder.buildGrantKyc(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("revokeTokenKyc")
    public TokenResponse revokeTokenKyc(GrantRevokeTokenKycParams params) throws Exception {
        // Convert params to Map for TransactionBuilders
        Map<String, Object> mapParams = new HashMap<>();
        params.getTokenId().ifPresent(tokenId -> mapParams.put("tokenId", tokenId));
        params.getAccountId().ifPresent(accountId -> mapParams.put("accountId", accountId));

        TokenRevokeKycTransaction transaction = TransactionBuilders.TokenBuilder.buildRevokeKyc(mapParams);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("mintToken")
    public TokenMintResponse mintToken(MintTokenParams params) throws Exception {
        TokenMintTransaction transaction = TransactionBuilders.TokenBuilder.buildMint(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenMintResponse(
                "",
                receipt.status,
                receipt.totalSupply.toString(),
                receipt.serials.stream().map(String::valueOf).toList());
    }

    @JSONRPC2Method("burnToken")
    public TokenBurnResponse burnToken(BurnTokenParams params) throws Exception {
        TokenBurnTransaction transaction = TransactionBuilders.TokenBuilder.buildBurn(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenBurnResponse("", receipt.status, receipt.totalSupply.toString());
    }

    @JSONRPC2Method("wipeToken")
    public Map<String, String> wipeToken(final TokenWipeParams params) throws Exception {
        TokenWipeTransaction tokenWipeTransaction = TransactionBuilders.TokenBuilder.buildWipe(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenWipeTransaction, sdkService.getClient()));

        TransactionReceipt receipt =
                tokenWipeTransaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }

    @JSONRPC2Method("airdropToken")
    public Map<String, String> airdropToken(final TokenAirdropParams params) throws Exception {
        TokenAirdropTransaction tokenAirdropTransaction = new TokenAirdropTransaction();

        // Set a 3-second gRPC deadline
        Duration threeSecondsDuration = Duration.ofSeconds(3);
        tokenAirdropTransaction.setGrpcDeadline(threeSecondsDuration);

        if (params.getTokenTransfers().isEmpty()) {
            throw new InvalidJSONRPC2ParamsException("transferParams is required");
        }

        List<TransferParams> transferParams = params.getTokenTransfers().get();

        for (TransferParams transferParam : transferParams) {
            try {
                AirdropUtils.handleAirdropParam(tokenAirdropTransaction, transferParam);
            } catch (Exception e) {
                throw e;
            }
        }

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }

    @JSONRPC2Method("cancelAirdrop")
    public Map<String, String> cancelAirdrop(final TokenAirdropCancelParams params) throws Exception {
        TokenCancelAirdropTransaction tokenCancelAirdropTransaction = new TokenCancelAirdropTransaction();

        Duration threeSecondsDuration = Duration.ofSeconds(3);
        tokenCancelAirdropTransaction.setGrpcDeadline(threeSecondsDuration);

        if (params.getPendingAirdrops().isEmpty()) {
            throw new InvalidJSONRPC2ParamsException("pendingAirdrops is required");
        }

        List<PendingAirdropParams> pendingAirdrops = params.getPendingAirdrops().get();

        for (PendingAirdropParams pendingAirdrop : pendingAirdrops) {
            String tokenId = pendingAirdrop
                    .getTokenId()
                    .orElseThrow(() -> new InvalidJSONRPC2ParamsException("tokenId is required"));
            String senderAccountId = pendingAirdrop
                    .getSenderAccountId()
                    .orElseThrow(() -> new InvalidJSONRPC2ParamsException("senderAccountId is required"));
            String receiverAccountId = pendingAirdrop
                    .getReceiverAccountId()
                    .orElseThrow(() -> new InvalidJSONRPC2ParamsException("receiverAccountId is required"));

            if (pendingAirdrop.getSerialNumbers().isPresent()
                    && !pendingAirdrop.getSerialNumbers().get().isEmpty()) {
                List<String> serialNumbers = pendingAirdrop.getSerialNumbers().get();
                for (String serialNumber : serialNumbers) {
                    PendingAirdropId pendingAirdropId = new PendingAirdropId(
                            AccountId.fromString(senderAccountId),
                            AccountId.fromString(receiverAccountId),
                            new NftId(TokenId.fromString(tokenId), Long.parseLong(serialNumber)));
                    tokenCancelAirdropTransaction.addPendingAirdrop(pendingAirdropId);
                }
            } else {
                PendingAirdropId pendingAirdropId = new PendingAirdropId(
                        AccountId.fromString(senderAccountId),
                        AccountId.fromString(receiverAccountId),
                        TokenId.fromString(tokenId));
                tokenCancelAirdropTransaction.addPendingAirdrop(pendingAirdropId);
            }
        }

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(
                        tokenCancelAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenCancelAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }

    @JSONRPC2Method("claimToken")
    public Map<String, String> claimToken(final TokenClaimAirdropParams params) throws Exception {
        TokenClaimAirdropTransaction tokenClaimAirdropTransaction = new TokenClaimAirdropTransaction();

        Duration threeSecondsDuration = Duration.ofSeconds(3);
        tokenClaimAirdropTransaction.setGrpcDeadline(threeSecondsDuration);

        String senderAccountId = params.getSenderAccountId()
                .orElseThrow(() -> new InvalidJSONRPC2ParamsException("senderAccountId is required"));
        String receiverAccountId = params.getReceiverAccountId()
                .orElseThrow(() -> new InvalidJSONRPC2ParamsException("receiverAccountId is required"));
        String tokenId =
                params.getTokenId().orElseThrow(() -> new InvalidJSONRPC2ParamsException("tokenId is required"));

        // NFT token claiming
        if (params.getSerialNumbers().isPresent()
                && !params.getSerialNumbers().get().isEmpty()) {
            List<String> serialNumbers = params.getSerialNumbers().get();
            for (String serialNumber : serialNumbers) {
                PendingAirdropId pendingAirdropId = new PendingAirdropId(
                        AccountId.fromString(senderAccountId),
                        AccountId.fromString(receiverAccountId),
                        new NftId(TokenId.fromString(tokenId), Long.parseLong(serialNumber)));
                tokenClaimAirdropTransaction.addPendingAirdrop(pendingAirdropId);
            }
        } else {
            // Fungible token claiming
            PendingAirdropId pendingAirdropId = new PendingAirdropId(
                    AccountId.fromString(senderAccountId),
                    AccountId.fromString(receiverAccountId),
                    TokenId.fromString(tokenId));
            tokenClaimAirdropTransaction.addPendingAirdrop(pendingAirdropId);
        }

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(
                        tokenClaimAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenClaimAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }
}
