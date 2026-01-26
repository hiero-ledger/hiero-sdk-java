// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.account.*;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.*;
import com.hedera.hashgraph.tck.methods.sdk.response.AccountAllowanceResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.AccountResponse;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.util.Map;

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
}
