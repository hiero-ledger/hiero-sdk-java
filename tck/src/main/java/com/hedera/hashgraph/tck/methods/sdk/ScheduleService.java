// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.response.ScheduleResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@JSONRPC2Service
public class ScheduleService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public ScheduleService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createSchedule")
    public ScheduleResponse createSchedule(final ScheduleCreateParams params) throws Exception {
        ScheduleCreateTransaction transaction = new ScheduleCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        // Set scheduled transaction if provided
        params.getScheduledTransaction().ifPresent(scheduledTx -> {
            try {
                Transaction<?> tx = buildScheduledTransaction(scheduledTx);
                transaction.setScheduledTransaction(tx);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to build scheduled transaction", e);
            }
        });

        // Set memo if provided
        params.getMemo().ifPresent(transaction::setScheduleMemo);

        // Set admin key if provided
        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid admin key format", e);
            }
        });

        // Set payer account ID if provided
        params.getPayerAccountId().ifPresent(accountIdStr -> 
            transaction.setPayerAccountId(AccountId.fromString(accountIdStr)));

        // Set expiration time if provided
        params.getExpirationTime().ifPresent(expirationTimeStr -> {
            try {
                long expirationTimeSeconds = Long.parseLong(expirationTimeStr);
                transaction.setExpirationTime(Instant.ofEpochSecond(expirationTimeSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid expiration time: " + expirationTimeStr, e);
            }
        });

        // Set wait for expiry if provided
        params.getWaitForExpiry().ifPresent(transaction::setWaitForExpiry);

        // Apply common transaction params
        params.getCommonTransactionParams()
                .ifPresent(common -> common.fillOutTransaction(transaction, sdkService.getClient()));

        // Execute the transaction
        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        String scheduleId = "";
        String transactionId = "";
        if (receipt.status == Status.SUCCESS) {
            if (receipt.scheduleId != null) {
                scheduleId = receipt.scheduleId.toString();
            }
            if (receipt.scheduledTransactionId != null) {
                transactionId = receipt.scheduledTransactionId.toString();
            }
        }

        return new ScheduleResponse(scheduleId, transactionId, receipt.status);
    }

    /**
     * Builds a scheduled transaction from method name and parameters
     */
    private Transaction<?> buildScheduledTransaction(ScheduleCreateParams.ScheduledTransaction scheduledTx) throws Exception {
        String method = scheduledTx.getMethod();
        Map<String, Object> params = scheduledTx.getParams();

        return switch (method) {
            case "transferCrypto" -> buildTransferTransaction(params);
            case "submitMessage" -> buildTopicMessageSubmitTransaction(params);
            case "burnToken" -> buildTokenBurnTransaction(params);
            case "mintToken" -> buildTokenMintTransaction(params);
            case "approveAllowance" -> buildAccountAllowanceApproveTransaction(params);
            case "createAccount" -> buildAccountCreateTransaction(params);
            case "createToken" -> buildTokenCreateTransaction(params);
            case "createTopic" -> buildTopicCreateTransaction(params);
            case "createFile" -> buildFileCreateTransaction(params);
            case "updateAccount" -> buildAccountUpdateTransaction(params);
            case "updateToken" -> buildTokenUpdateTransaction(params);
            case "updateTopic" -> buildTopicUpdateTransaction(params);
            case "updateFile" -> buildFileUpdateTransaction(params);
            case "deleteAccount" -> buildAccountDeleteTransaction(params);
            case "deleteToken" -> buildTokenDeleteTransaction(params);
            case "deleteTopic" -> buildTopicDeleteTransaction(params);
            case "deleteFile" -> buildFileDeleteTransaction(params);
            case "associateToken" -> buildTokenAssociateTransaction(params);
            case "dissociateToken" -> buildTokenDissociateTransaction(params);
            case "freezeToken" -> buildTokenFreezeTransaction(params);
            case "unfreezeToken" -> buildTokenUnfreezeTransaction(params);
            case "grantKyc" -> buildTokenGrantKycTransaction(params);
            case "revokeKyc" -> buildTokenRevokeKycTransaction(params);
            case "pauseToken" -> buildTokenPauseTransaction(params);
            case "unpauseToken" -> buildTokenUnpauseTransaction(params);
            case "wipeToken" -> buildTokenWipeTransaction(params);
            case "rejectToken" -> buildTokenRejectTransaction(params);
            case "deleteAllowance" -> buildAccountAllowanceDeleteTransaction(params);
            case "appendFile" -> buildFileAppendTransaction(params);
            default -> throw new IllegalArgumentException("Unsupported scheduled transaction method: " + method);
        };
    }

    // Helper methods to build specific transaction types
    private TransferTransaction buildTransferTransaction(Map<String, Object> params) throws Exception {
        TransferTransaction transaction = new TransferTransaction();
        
        if (params.containsKey("transfers")) {
            @SuppressWarnings("unchecked")
            var transfers = (java.util.List<Map<String, Object>>) params.get("transfers");
            for (var transfer : transfers) {
                if (transfer.containsKey("hbar")) {
                    @SuppressWarnings("unchecked")
                    var hbarTransfer = (Map<String, Object>) transfer.get("hbar");
                    if (hbarTransfer.containsKey("accountId") && hbarTransfer.containsKey("amount")) {
                        transaction.addHbarTransfer(
                            AccountId.fromString((String) hbarTransfer.get("accountId")),
                            Hbar.fromTinybars(Long.parseLong((String) hbarTransfer.get("amount")))
                        );
                    }
                }
                // Add other transfer types as needed
            }
        }
        
        return transaction;
    }

    private TopicMessageSubmitTransaction buildTopicMessageSubmitTransaction(Map<String, Object> params) throws Exception {
        TopicMessageSubmitTransaction transaction = new TopicMessageSubmitTransaction();
        if (params.containsKey("topicId")) {
            transaction.setTopicId(TopicId.fromString((String) params.get("topicId")));
        }
        if (params.containsKey("message")) {
            transaction.setMessage(((String) params.get("message")).getBytes());
        }
        return transaction;
    }

    private TokenBurnTransaction buildTokenBurnTransaction(Map<String, Object> params) throws Exception {
        TokenBurnTransaction transaction = new TokenBurnTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("amount")) {
            transaction.setAmount(Long.parseLong((String) params.get("amount")));
        }
        return transaction;
    }

    private TokenMintTransaction buildTokenMintTransaction(Map<String, Object> params) throws Exception {
        TokenMintTransaction transaction = new TokenMintTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("amount")) {
            transaction.setAmount(Long.parseLong((String) params.get("amount")));
        }
        return transaction;
    }

    private AccountAllowanceApproveTransaction buildAccountAllowanceApproveTransaction(Map<String, Object> params) throws Exception {
        AccountAllowanceApproveTransaction transaction = new AccountAllowanceApproveTransaction();
        
        if (params.containsKey("allowances")) {
            @SuppressWarnings("unchecked")
            var allowances = (java.util.List<Map<String, Object>>) params.get("allowances");
            for (var allowance : allowances) {
                if (allowance.containsKey("hbar")) {
                    @SuppressWarnings("unchecked")
                    var hbarAllowance = (Map<String, Object>) allowance.get("hbar");
                    if (hbarAllowance.containsKey("ownerAccountId") && hbarAllowance.containsKey("spenderAccountId") && hbarAllowance.containsKey("amount")) {
                        transaction.approveHbarAllowance(
                            AccountId.fromString((String) hbarAllowance.get("ownerAccountId")),
                            AccountId.fromString((String) hbarAllowance.get("spenderAccountId")),
                            Hbar.fromTinybars(Long.parseLong((String) hbarAllowance.get("amount")))
                        );
                    }
                }
                // Add other allowance types as needed
            }
        }
        
        return transaction;
    }

    private AccountCreateTransaction buildAccountCreateTransaction(Map<String, Object> params) throws Exception {
        AccountCreateTransaction transaction = new AccountCreateTransaction();
        if (params.containsKey("key")) {
            transaction.setKey(KeyUtils.getKeyFromString((String) params.get("key")));
        }
        if (params.containsKey("initialBalance")) {
            transaction.setInitialBalance(Hbar.fromTinybars(Long.parseLong((String) params.get("initialBalance"))));
        }
        return transaction;
    }

    private TokenCreateTransaction buildTokenCreateTransaction(Map<String, Object> params) throws Exception {
        TokenCreateTransaction transaction = new TokenCreateTransaction();
        if (params.containsKey("name")) {
            transaction.setTokenName((String) params.get("name"));
        }
        if (params.containsKey("symbol")) {
            transaction.setTokenSymbol((String) params.get("symbol"));
        }
        if (params.containsKey("decimals")) {
            transaction.setDecimals(((Number) params.get("decimals")).intValue());
        }
        if (params.containsKey("initialSupply")) {
            transaction.setInitialSupply(Long.parseLong((String) params.get("initialSupply")));
        }
        if (params.containsKey("treasuryAccountId")) {
            transaction.setTreasuryAccountId(AccountId.fromString((String) params.get("treasuryAccountId")));
        }
        return transaction;
    }

    private TopicCreateTransaction buildTopicCreateTransaction(Map<String, Object> params) throws Exception {
        TopicCreateTransaction transaction = new TopicCreateTransaction();
        if (params.containsKey("memo")) {
            transaction.setTopicMemo((String) params.get("memo"));
        }
        return transaction;
    }

    private FileCreateTransaction buildFileCreateTransaction(Map<String, Object> params) throws Exception {
        FileCreateTransaction transaction = new FileCreateTransaction();
        if (params.containsKey("contents")) {
            transaction.setContents(((String) params.get("contents")).getBytes());
        }
        return transaction;
    }

    private AccountUpdateTransaction buildAccountUpdateTransaction(Map<String, Object> params) throws Exception {
        AccountUpdateTransaction transaction = new AccountUpdateTransaction();
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        return transaction;
    }

    private TokenUpdateTransaction buildTokenUpdateTransaction(Map<String, Object> params) throws Exception {
        TokenUpdateTransaction transaction = new TokenUpdateTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        return transaction;
    }

    private TopicUpdateTransaction buildTopicUpdateTransaction(Map<String, Object> params) throws Exception {
        TopicUpdateTransaction transaction = new TopicUpdateTransaction();
        if (params.containsKey("topicId")) {
            transaction.setTopicId(TopicId.fromString((String) params.get("topicId")));
        }
        return transaction;
    }

    private FileUpdateTransaction buildFileUpdateTransaction(Map<String, Object> params) throws Exception {
        FileUpdateTransaction transaction = new FileUpdateTransaction();
        if (params.containsKey("fileId")) {
            transaction.setFileId(FileId.fromString((String) params.get("fileId")));
        }
        return transaction;
    }

    private AccountDeleteTransaction buildAccountDeleteTransaction(Map<String, Object> params) throws Exception {
        AccountDeleteTransaction transaction = new AccountDeleteTransaction();
        if (params.containsKey("deleteAccountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("deleteAccountId")));
        }
        if (params.containsKey("transferAccountId")) {
            transaction.setTransferAccountId(AccountId.fromString((String) params.get("transferAccountId")));
        }
        return transaction;
    }

    private TokenDeleteTransaction buildTokenDeleteTransaction(Map<String, Object> params) throws Exception {
        TokenDeleteTransaction transaction = new TokenDeleteTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        return transaction;
    }

    private TopicDeleteTransaction buildTopicDeleteTransaction(Map<String, Object> params) throws Exception {
        TopicDeleteTransaction transaction = new TopicDeleteTransaction();
        if (params.containsKey("topicId")) {
            transaction.setTopicId(TopicId.fromString((String) params.get("topicId")));
        }
        return transaction;
    }

    private FileDeleteTransaction buildFileDeleteTransaction(Map<String, Object> params) throws Exception {
        FileDeleteTransaction transaction = new FileDeleteTransaction();
        if (params.containsKey("fileId")) {
            transaction.setFileId(FileId.fromString((String) params.get("fileId")));
        }
        return transaction;
    }

    private TokenAssociateTransaction buildTokenAssociateTransaction(Map<String, Object> params) throws Exception {
        TokenAssociateTransaction transaction = new TokenAssociateTransaction();
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        if (params.containsKey("tokenIds")) {
            // Implementation would parse token IDs
        }
        return transaction;
    }

    private TokenDissociateTransaction buildTokenDissociateTransaction(Map<String, Object> params) throws Exception {
        TokenDissociateTransaction transaction = new TokenDissociateTransaction();
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        if (params.containsKey("tokenIds")) {
            // Implementation would parse token IDs
        }
        return transaction;
    }

    private TokenFreezeTransaction buildTokenFreezeTransaction(Map<String, Object> params) throws Exception {
        TokenFreezeTransaction transaction = new TokenFreezeTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        return transaction;
    }

    private TokenUnfreezeTransaction buildTokenUnfreezeTransaction(Map<String, Object> params) throws Exception {
        TokenUnfreezeTransaction transaction = new TokenUnfreezeTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        return transaction;
    }

    private TokenGrantKycTransaction buildTokenGrantKycTransaction(Map<String, Object> params) throws Exception {
        TokenGrantKycTransaction transaction = new TokenGrantKycTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        return transaction;
    }

    private TokenRevokeKycTransaction buildTokenRevokeKycTransaction(Map<String, Object> params) throws Exception {
        TokenRevokeKycTransaction transaction = new TokenRevokeKycTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        return transaction;
    }

    private TokenPauseTransaction buildTokenPauseTransaction(Map<String, Object> params) throws Exception {
        TokenPauseTransaction transaction = new TokenPauseTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        return transaction;
    }

    private TokenUnpauseTransaction buildTokenUnpauseTransaction(Map<String, Object> params) throws Exception {
        TokenUnpauseTransaction transaction = new TokenUnpauseTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        return transaction;
    }

    private TokenWipeTransaction buildTokenWipeTransaction(Map<String, Object> params) throws Exception {
        TokenWipeTransaction transaction = new TokenWipeTransaction();
        if (params.containsKey("tokenId")) {
            transaction.setTokenId(TokenId.fromString((String) params.get("tokenId")));
        }
        if (params.containsKey("accountId")) {
            transaction.setAccountId(AccountId.fromString((String) params.get("accountId")));
        }
        if (params.containsKey("amount")) {
            transaction.setAmount(Long.parseLong((String) params.get("amount")));
        }
        return transaction;
    }

    private TokenRejectTransaction buildTokenRejectTransaction(Map<String, Object> params) throws Exception {
        TokenRejectTransaction transaction = new TokenRejectTransaction();
        if (params.containsKey("ownerId")) {
            transaction.setOwnerId(AccountId.fromString((String) params.get("ownerId")));
        }
        if (params.containsKey("tokenIds")) {
            // Implementation would parse token IDs
        }
        return transaction;
    }

    private AccountAllowanceDeleteTransaction buildAccountAllowanceDeleteTransaction(Map<String, Object> params) throws Exception {
        AccountAllowanceDeleteTransaction transaction = new AccountAllowanceDeleteTransaction();
        // Implementation would parse allowance parameters
        return transaction;
    }

    private FileAppendTransaction buildFileAppendTransaction(Map<String, Object> params) throws Exception {
        FileAppendTransaction transaction = new FileAppendTransaction();
        if (params.containsKey("fileId")) {
            transaction.setFileId(FileId.fromString((String) params.get("fileId")));
        }
        if (params.containsKey("contents")) {
            transaction.setContents(((String) params.get("contents")).getBytes());
        }
        return transaction;
    }
}
