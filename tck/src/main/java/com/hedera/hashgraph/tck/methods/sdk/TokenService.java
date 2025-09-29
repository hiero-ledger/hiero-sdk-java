// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.token.*;
import com.hedera.hashgraph.tck.methods.sdk.response.token.*;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.util.Map;

/**
 * TokenService for token related methods
 */
@JSONRPC2Service
public class TokenService extends AbstractJSONRPC2Service {

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
        TokenFeeScheduleUpdateTransaction transaction = TransactionBuilders.TokenBuilder.buildUpdateFeeSchedule(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("freezeToken")
    public TokenResponse tokenFreezeTransaction(FreezeUnfreezeTokenParams params) throws Exception {
        TokenFreezeTransaction transaction = TransactionBuilders.TokenBuilder.buildFreeze(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("unfreezeToken")
    public TokenResponse tokenUnfreezeTransaction(FreezeUnfreezeTokenParams params) throws Exception {
        TokenUnfreezeTransaction transaction = TransactionBuilders.TokenBuilder.buildUnfreeze(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("associateToken")
    public TokenResponse associateToken(AssociateDisassociateTokenParams params) throws Exception {
        TokenAssociateTransaction transaction = TransactionBuilders.TokenBuilder.buildAssociate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("dissociateToken")
    public TokenResponse dissociateToken(AssociateDisassociateTokenParams params) throws Exception {
        TokenDissociateTransaction transaction = TransactionBuilders.TokenBuilder.buildDissociate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("pauseToken")
    public TokenResponse pauseToken(PauseUnpauseTokenParams params) throws Exception {
        TokenPauseTransaction transaction = TransactionBuilders.TokenBuilder.buildPause(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("unpauseToken")
    public TokenResponse tokenUnpauseTransaction(PauseUnpauseTokenParams params) throws Exception {
        TokenUnpauseTransaction transaction = TransactionBuilders.TokenBuilder.buildUnpause(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("grantTokenKyc")
    public TokenResponse grantTokenKyc(GrantRevokeTokenKycParams params) throws Exception {
        TokenGrantKycTransaction transaction = TransactionBuilders.TokenBuilder.buildGrantKyc(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionReceipt receipt = transaction.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

        return new TokenResponse("", receipt.status);
    }

    @JSONRPC2Method("revokeTokenKyc")
    public TokenResponse revokeTokenKyc(GrantRevokeTokenKycParams params) throws Exception {
        TokenRevokeKycTransaction transaction = TransactionBuilders.TokenBuilder.buildRevokeKyc(params);

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
        TokenAirdropTransaction tokenAirdropTransaction = TransactionBuilders.TokenBuilder.buildAirdrop(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(tokenAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }

    @JSONRPC2Method("cancelAirdrop")
    public Map<String, String> cancelAirdrop(final TokenAirdropCancelParams params) throws Exception {
        TokenCancelAirdropTransaction tokenCancelAirdropTransaction =
                TransactionBuilders.TokenBuilder.buildCancelAirdrop(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(
                        tokenCancelAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenCancelAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }

    @JSONRPC2Method("claimToken")
    public Map<String, String> claimToken(final TokenClaimAirdropParams params) throws Exception {
        TokenClaimAirdropTransaction tokenClaimAirdropTransaction =
                TransactionBuilders.TokenBuilder.buildClaimAirdrop(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(
                        tokenClaimAirdropTransaction, sdkService.getClient()));

        TransactionResponse txResponse = tokenClaimAirdropTransaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return Map.of("status", receipt.status.toString());
    }
}
