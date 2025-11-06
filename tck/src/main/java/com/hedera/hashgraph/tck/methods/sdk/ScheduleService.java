// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleSignParams;
import com.hedera.hashgraph.tck.methods.sdk.response.ScheduleResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;
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

        params.getScheduledTransaction().ifPresent(scheduledTx -> {
            try {
                Transaction<?> tx = buildScheduledTransaction(scheduledTx);
                transaction.setScheduledTransaction(tx);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to build scheduled transaction", e);
            }
        });

        params.getMemo().ifPresent(transaction::setScheduleMemo);

        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid admin key format", e);
            }
        });

        params.getPayerAccountId()
                .ifPresent(accountIdStr -> transaction.setPayerAccountId(AccountId.fromString(accountIdStr)));

        params.getExpirationTime().ifPresent(expirationTimeStr -> {
            try {
                long expirationTimeSeconds = Long.parseLong(expirationTimeStr);
                transaction.setExpirationTime(Duration.ofSeconds(expirationTimeSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid expiration time: " + expirationTimeStr, e);
            }
        });

        params.getWaitForExpiry().ifPresent(transaction::setWaitForExpiry);

        params.getCommonTransactionParams()
                .ifPresent(common -> common.fillOutTransaction(transaction, sdkService.getClient()));

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

    @JSONRPC2Method("signSchedule")
    public ScheduleResponse signSchedule(final ScheduleSignParams params) throws Exception {
        ScheduleSignTransaction transaction = new ScheduleSignTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getScheduleId()
                .ifPresent(scheduleIdStr -> transaction.setScheduleId(ScheduleId.fromString(scheduleIdStr)));

        params.getCommonTransactionParams()
                .ifPresent(common -> common.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        String scheduleId = "";
        String transactionId = "";
        if (receipt.status == Status.SUCCESS) {
            if (params.getScheduleId().isPresent()) {
                scheduleId = params.getScheduleId().get();
            }
            if (receipt.scheduledTransactionId != null) {
                transactionId = receipt.scheduledTransactionId.toString();
            }
        }

        return new ScheduleResponse(scheduleId, transactionId, receipt.status);
    }

    @JSONRPC2Method("deleteSchedule")
    public ScheduleResponse deleteSchedule(final ScheduleDeleteParams params) throws Exception {
        ScheduleDeleteTransaction transaction = new ScheduleDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getScheduleId()
                .ifPresent(scheduleIdStr -> transaction.setScheduleId(ScheduleId.fromString(scheduleIdStr)));

        params.getCommonTransactionParams()
                .ifPresent(common -> common.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        String scheduleId = "";
        String transactionId = "";
        if (receipt.status == Status.SUCCESS) {
            if (params.getScheduleId().isPresent()) {
                scheduleId = params.getScheduleId().get();
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
    private Transaction<?> buildScheduledTransaction(ScheduleCreateParams.ScheduledTransaction scheduledTx) {
        String method = scheduledTx.getMethod();
        Map<String, Object> params = scheduledTx.getParams();

        return switch (method) {
            case "transferCrypto" -> TransactionBuilders.TransferBuilder.buildTransfer(params);
            case "submitMessage" -> TransactionBuilders.TopicBuilder.buildSubmitMessage(params);
            case "burnToken" -> TransactionBuilders.TokenBuilder.buildBurn(params);
            case "mintToken" -> TransactionBuilders.TokenBuilder.buildMint(params);
            case "approveAllowance" -> TransactionBuilders.AccountBuilder.buildApproveAllowance(params);
            case "createAccount" -> TransactionBuilders.AccountBuilder.buildCreate(params);
            case "createToken" -> TransactionBuilders.TokenBuilder.buildCreate(params);
            case "createTopic" -> TransactionBuilders.TopicBuilder.buildCreate(params);
            case "createFile" -> TransactionBuilders.FileBuilder.buildCreate(params);
            case "updateAccount" -> TransactionBuilders.AccountBuilder.buildUpdate(params);
            case "updateToken" -> TransactionBuilders.TokenBuilder.buildUpdate(params);
            case "updateTopic" -> TransactionBuilders.TopicBuilder.buildUpdate(params);
            case "updateFile" -> TransactionBuilders.FileBuilder.buildUpdate(params);
            case "deleteAccount" -> TransactionBuilders.AccountBuilder.buildDelete(params);
            case "deleteToken" -> TransactionBuilders.TokenBuilder.buildDelete(params);
            case "deleteTopic" -> TransactionBuilders.TopicBuilder.buildDelete(params);
            case "deleteFile" -> TransactionBuilders.FileBuilder.buildDelete(params);
            case "associateToken" -> TransactionBuilders.TokenBuilder.buildAssociate(params);
            case "dissociateToken" -> TransactionBuilders.TokenBuilder.buildDissociate(params);
            case "freezeToken" -> TransactionBuilders.TokenBuilder.buildFreeze(params);
            case "unfreezeToken" -> TransactionBuilders.TokenBuilder.buildUnfreeze(params);
            case "grantKyc" -> TransactionBuilders.TokenBuilder.buildGrantKyc(params);
            case "revokeKyc" -> TransactionBuilders.TokenBuilder.buildRevokeKyc(params);
            case "pauseToken" -> TransactionBuilders.TokenBuilder.buildPause(params);
            case "unpauseToken" -> TransactionBuilders.TokenBuilder.buildUnpause(params);
            case "wipeToken" -> TransactionBuilders.TokenBuilder.buildWipe(params);
            case "updateTokenFeeSchedule" -> TransactionBuilders.TokenBuilder.buildUpdateFeeSchedule(params);
            case "airdropToken" -> TransactionBuilders.TokenBuilder.buildAirdrop(params);
            case "cancelAirdrop" -> TransactionBuilders.TokenBuilder.buildCancelAirdrop(params);
            case "claimToken" -> TransactionBuilders.TokenBuilder.buildClaimAirdrop(params);
            case "deleteAllowance" -> TransactionBuilders.AccountBuilder.buildDeleteAllowance(params);
            case "appendFile" -> TransactionBuilders.FileBuilder.buildAppend(params);
            default -> throw new IllegalArgumentException("Unsupported scheduled transaction method: " + method);
        };
    }
}
