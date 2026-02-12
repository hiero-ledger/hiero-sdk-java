// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.account.*;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.*;
import com.hedera.hashgraph.tck.methods.sdk.response.*;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AccountService for account related methods
 */
@JSONRPC2Service
public class AccountService extends AbstractJSONRPC2Service {
    private final SdkService sdkService;

    public AccountService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createAccount")
    public AccountResponse createAccount(final AccountCreateParams params) throws Exception {
        AccountCreateTransaction accountCreateTransaction = TransactionBuilders.AccountBuilder.buildCreate(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(accountCreateTransaction, client));

        TransactionReceipt transactionReceipt =
                accountCreateTransaction.execute(client).getReceipt(client);

        String stringAccountId = "";
        if (transactionReceipt.status == Status.SUCCESS) {
            stringAccountId = transactionReceipt.accountId.toString();
        }

        return new AccountResponse(stringAccountId, transactionReceipt.status);
    }

    @JSONRPC2Method("updateAccount")
    public AccountResponse updateAccount(final AccountUpdateParams params) throws Exception {
        AccountUpdateTransaction accountUpdateTransaction = TransactionBuilders.AccountBuilder.buildUpdate(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(accountUpdateTransaction, client));

        TransactionReceipt transactionReceipt =
                accountUpdateTransaction.execute(client).getReceipt(client);

        return new AccountResponse(null, transactionReceipt.status);
    }

    @JSONRPC2Method("deleteAccount")
    public AccountResponse deleteAccount(final AccountDeleteParams params) throws Exception {
        AccountDeleteTransaction accountDeleteTransaction = TransactionBuilders.AccountBuilder.buildDelete(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(accountDeleteTransaction, client));

        TransactionReceipt transactionReceipt =
                accountDeleteTransaction.execute(client).getReceipt(client);

        return new AccountResponse(null, transactionReceipt.status);
    }

    @JSONRPC2Method("approveAllowance")
    public AccountAllowanceResponse approveAllowance(final AccountAllowanceParams params) throws Exception {
        AccountAllowanceApproveTransaction tx = TransactionBuilders.AccountBuilder.buildApproveAllowance(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams().ifPresent(commonParams -> commonParams.fillOutTransaction(tx, client));

        TransactionReceipt transactionReceipt = tx.execute(client).getReceipt(client);
        return new AccountAllowanceResponse(transactionReceipt.status);
    }

    @JSONRPC2Method("deleteAllowance")
    public AccountAllowanceResponse deleteAllowance(final AccountAllowanceParams params) throws Exception {
        AccountAllowanceDeleteTransaction tx = TransactionBuilders.AccountBuilder.buildDeleteAllowance(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams().ifPresent(commonParams -> commonParams.fillOutTransaction(tx, client));

        TransactionReceipt transactionReceipt = tx.execute(client).getReceipt(client);
        return new AccountAllowanceResponse(transactionReceipt.status);
    }

    @JSONRPC2Method("getAccountInfo")
    public GetAccountInfoResponse getAccountInfo(final GetAccountInfoParams params) throws Exception {
        Client client = sdkService.getClient(params.getSessionId());
        AccountInfoQuery query = new AccountInfoQuery().setGrpcDeadline(Duration.ofSeconds(10L));

        if (params.getAccountId() != null) {
            query.setAccountId(AccountId.fromString(params.getAccountId()));
        }

        AccountInfo accountInfo = query.execute(client);
        return mapAccountInfoResponse(accountInfo);
    }

    /**
     * Transfers cryptocurrency between accounts
     *
     * @param params The transfer parameters
     * @return A response with the transaction status
     * @throws Exception If an error occurs during the transaction
     */
    @JSONRPC2Method("transferCrypto")
    public Map<String, String> transferCrypto(final TransferCryptoParams params) throws Exception {
        TransferTransaction transferTransaction = TransactionBuilders.TransferBuilder.buildTransfer(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transferTransaction, client));

        TransactionReceipt receipt = transferTransaction.execute(client).getReceipt(client);

        return Map.of("status", receipt.status.toString());
    }

    /**
     * Process an individual transfer based on its type (Hbar, Token, or NFT)
     */
    public static void processTransfer(TransferTransaction tx, TransferParams txParams) {
        boolean approved = txParams.getApproved().orElse(false);

        txParams.getHbar().ifPresent(hbarParams -> processHbarTransfer(tx, hbarParams, approved));
        txParams.getToken().ifPresent(tokenParams -> processTokenTransfer(tx, tokenParams, approved));
        txParams.getNft().ifPresent(nftParams -> processNftTransfer(tx, nftParams, approved));
    }

    /**
     * Process an Hbar transfer
     */
    private static void processHbarTransfer(TransferTransaction tx, HbarTransferParams hbarParams, boolean approved) {
        hbarParams.getAmount().ifPresent(amountStr -> {
            Hbar amount = Hbar.fromTinybars(Long.parseLong(amountStr));

            hbarParams.getAccountId().ifPresent(accountIdStr -> {
                AccountId accountId = AccountId.fromString(accountIdStr);
                if (approved) {
                    tx.addApprovedHbarTransfer(accountId, amount);
                } else {
                    tx.addHbarTransfer(accountId, amount);
                }
            });

            hbarParams.getEvmAddress().ifPresent(evmAddressStr -> {
                EvmAddress evmAddress = EvmAddress.fromString(evmAddressStr);
                if (approved) {
                    tx.addApprovedHbarTransfer(AccountId.fromEvmAddress(evmAddress, 0, 0), amount);
                } else {
                    tx.addHbarTransfer(evmAddress, amount);
                }
            });
        });
    }

    /**
     * Process a token transfer
     */
    private static void processTokenTransfer(
            TransferTransaction tx, TokenTransferParams tokenParams, boolean approved) {
        tokenParams.getAccountId().ifPresent(accountIdStr -> {
            tokenParams.getTokenId().ifPresent(tokenIdStr -> {
                tokenParams.getAmount().ifPresent(amountStr -> {
                    AccountId accountId = AccountId.fromString(accountIdStr);
                    TokenId tokenId = TokenId.fromString(tokenIdStr);
                    long amount = Long.parseLong(amountStr);

                    if (tokenParams.getDecimals().isPresent()) {
                        Long decimals = tokenParams.getDecimals().get();
                        if (approved) {
                            tx.addApprovedTokenTransferWithDecimals(tokenId, accountId, amount, decimals.intValue());
                        } else {
                            tx.addTokenTransferWithDecimals(tokenId, accountId, amount, decimals.intValue());
                        }
                    } else {
                        if (approved) {
                            tx.addApprovedTokenTransfer(tokenId, accountId, amount);
                        } else {
                            tx.addTokenTransfer(tokenId, accountId, amount);
                        }
                    }
                });
            });
        });
    }

    /**
     * Process an NFT transfer
     */
    private static void processNftTransfer(TransferTransaction tx, NftTransferParams nftParams, boolean approved) {
        nftParams.getSenderAccountId().ifPresent(senderIdStr -> {
            nftParams.getReceiverAccountId().ifPresent(receiverIdStr -> {
                nftParams.getTokenId().ifPresent(tokenIdStr -> {
                    nftParams.getSerialNumber().ifPresent(serialNumberStr -> {
                        AccountId senderAccountId = AccountId.fromString(senderIdStr);
                        AccountId receiverAccountId = AccountId.fromString(receiverIdStr);
                        TokenId tokenId = TokenId.fromString(tokenIdStr);
                        long serialNumber = Long.parseLong(serialNumberStr);
                        NftId nftId = new NftId(tokenId, serialNumber);

                        if (approved) {
                            tx.addApprovedNftTransfer(nftId, senderAccountId, receiverAccountId);
                        } else {
                            tx.addNftTransfer(nftId, senderAccountId, receiverAccountId);
                        }
                    });
                });
            });
        });
    }

    /**
     * Map AccountInfo from SDK to GetAccountInfoResponse for JSON-RPC
     */
    private static GetAccountInfoResponse mapAccountInfoResponse(AccountInfo info) {
        return new GetAccountInfoResponse(
                info.accountId.toString(),
                info.contractAccountId,
                info.isDeleted,
                info.proxyAccountId != null ? info.proxyAccountId.toString() : null,
                String.valueOf(info.proxyReceived.toTinybars()),
                info.key != null ? info.key.toString() : null,
                String.valueOf(info.balance.toTinybars()),
                String.valueOf(info.sendRecordThreshold.toTinybars()),
                String.valueOf(info.receiveRecordThreshold.toTinybars()),
                info.isReceiverSignatureRequired,
                info.expirationTime.toString(),
                String.valueOf(info.autoRenewPeriod.getSeconds()),
                mapLiveHashes(info.liveHashes),
                mapTokenRelationships(info.tokenRelationships),
                info.accountMemo,
                String.valueOf(info.ownedNfts),
                String.valueOf(info.maxAutomaticTokenAssociations),
                info.aliasKey != null ? info.aliasKey.toString() : null,
                info.ledgerId != null ? info.ledgerId.toString() : null,
                mapHbarAllowances(info.hbarAllowances),
                mapTokenAllowances(info.tokenAllowances),
                mapNftAllowances(info.tokenNftAllowances),
                String.valueOf(info.ethereumNonce),
                mapStakingInfo(info.stakingInfo));
    }

    private static List<GetAccountInfoResponse.LiveHashResponse> mapLiveHashes(List<LiveHash> liveHashes) {
        return liveHashes.stream()
                .map(lh -> new GetAccountInfoResponse.LiveHashResponse(
                        lh.accountId.toString(),
                        java.util.Base64.getEncoder().encodeToString(lh.hash.toByteArray()),
                        lh.keys.stream().map(key -> key.toString()).collect(Collectors.toList()),
                        String.valueOf(lh.duration.getSeconds())))
                .collect(Collectors.toList());
    }

    private static Map<String, GetAccountInfoResponse.TokenRelationshipInfo> mapTokenRelationships(
            Map<TokenId, TokenRelationship> rels) {
        Map<String, GetAccountInfoResponse.TokenRelationshipInfo> result = new HashMap<>();
        for (Map.Entry<TokenId, TokenRelationship> entry : rels.entrySet()) {
            TokenRelationship tr = entry.getValue();
            result.put(
                    entry.getKey().toString(),
                    new GetAccountInfoResponse.TokenRelationshipInfo(
                            tr.tokenId.toString(),
                            tr.symbol,
                            String.valueOf(tr.balance),
                            tr.kycStatus,
                            tr.freezeStatus,
                            tr.automaticAssociation));
        }
        return result;
    }

    private static List<GetAccountInfoResponse.HbarAllowanceResponse> mapHbarAllowances(
            List<HbarAllowance> allowances) {
        return allowances.stream()
                .map(a -> new GetAccountInfoResponse.HbarAllowanceResponse(
                        a.ownerAccountId != null ? a.ownerAccountId.toString() : null,
                        a.spenderAccountId != null ? a.spenderAccountId.toString() : null,
                        a.amount != null ? String.valueOf(a.amount.toTinybars()) : null))
                .collect(Collectors.toList());
    }

    private static List<GetAccountInfoResponse.TokenAllowanceResponse> mapTokenAllowances(
            List<TokenAllowance> allowances) {
        return allowances.stream()
                .map(a -> new GetAccountInfoResponse.TokenAllowanceResponse(
                        a.tokenId != null ? a.tokenId.toString() : null,
                        a.ownerAccountId != null ? a.ownerAccountId.toString() : null,
                        a.spenderAccountId != null ? a.spenderAccountId.toString() : null,
                        String.valueOf(a.amount)))
                .collect(Collectors.toList());
    }

    private static List<GetAccountInfoResponse.TokenNftAllowanceResponse> mapNftAllowances(
            List<TokenNftAllowance> allowances) {
        return allowances.stream()
                .map(a -> new GetAccountInfoResponse.TokenNftAllowanceResponse(
                        a.tokenId != null ? a.tokenId.toString() : null,
                        a.ownerAccountId != null ? a.ownerAccountId.toString() : null,
                        a.spenderAccountId != null ? a.spenderAccountId.toString() : null,
                        a.serialNumbers != null
                                ? a.serialNumbers.stream().map(String::valueOf).collect(Collectors.toList())
                                : null,
                        a.allSerials,
                        a.delegatingSpender != null ? a.delegatingSpender.toString() : null))
                .collect(Collectors.toList());
    }

    private static GetAccountInfoResponse.StakingInfoResponse mapStakingInfo(StakingInfo info) {
        if (info == null) {
            return null;
        }
        return new GetAccountInfoResponse.StakingInfoResponse(
                info.declineStakingReward,
                info.stakePeriodStart != null ? info.stakePeriodStart.toString() : null,
                info.pendingReward != null ? String.valueOf(info.pendingReward.toTinybars()) : null,
                info.stakedToMe != null ? String.valueOf(info.stakedToMe.toTinybars()) : null,
                info.stakedAccountId != null ? info.stakedAccountId.toString() : null,
                info.stakedNodeId != null ? String.valueOf(info.stakedNodeId) : null);
    }
}
