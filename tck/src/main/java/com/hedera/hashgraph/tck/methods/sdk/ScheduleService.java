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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@JSONRPC2Service
public class ScheduleService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private static final Map<String, Function<Map<String, Object>, Transaction<?>>> SCHEDULED_TRANSACTION_BUILDERS;
    private final SdkService sdkService;

    public ScheduleService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    static {
        Map<String, Function<Map<String, Object>, Transaction<?>>> builders = new HashMap<>();
        builders.put("transferCrypto", TransactionBuilders.TransferBuilder::buildTransfer);
        builders.put("submitMessage", TransactionBuilders.TopicBuilder::buildSubmitMessage);
        builders.put("burnToken", TransactionBuilders.TokenBuilder::buildBurn);
        builders.put("mintToken", TransactionBuilders.TokenBuilder::buildMint);
        builders.put("approveAllowance", TransactionBuilders.AccountBuilder::buildApproveAllowance);
        builders.put("createAccount", TransactionBuilders.AccountBuilder::buildCreate);
        builders.put("createToken", TransactionBuilders.TokenBuilder::buildCreate);
        builders.put("createTopic", TransactionBuilders.TopicBuilder::buildCreate);
        builders.put("createFile", TransactionBuilders.FileBuilder::buildCreate);
        builders.put("updateAccount", TransactionBuilders.AccountBuilder::buildUpdate);
        builders.put("updateToken", TransactionBuilders.TokenBuilder::buildUpdate);
        builders.put("updateTopic", TransactionBuilders.TopicBuilder::buildUpdate);
        builders.put("updateFile", TransactionBuilders.FileBuilder::buildUpdate);
        builders.put("deleteAccount", TransactionBuilders.AccountBuilder::buildDelete);
        builders.put("deleteToken", TransactionBuilders.TokenBuilder::buildDelete);
        builders.put("deleteTopic", TransactionBuilders.TopicBuilder::buildDelete);
        builders.put("deleteFile", TransactionBuilders.FileBuilder::buildDelete);
        builders.put("associateToken", TransactionBuilders.TokenBuilder::buildAssociate);
        builders.put("dissociateToken", TransactionBuilders.TokenBuilder::buildDissociate);
        builders.put("freezeToken", TransactionBuilders.TokenBuilder::buildFreeze);
        builders.put("unfreezeToken", TransactionBuilders.TokenBuilder::buildUnfreeze);
        builders.put("grantKyc", TransactionBuilders.TokenBuilder::buildGrantKyc);
        builders.put("revokeKyc", TransactionBuilders.TokenBuilder::buildRevokeKyc);
        builders.put("pauseToken", TransactionBuilders.TokenBuilder::buildPause);
        builders.put("unpauseToken", TransactionBuilders.TokenBuilder::buildUnpause);
        builders.put("wipeToken", TransactionBuilders.TokenBuilder::buildWipe);
        builders.put("updateTokenFeeSchedule", TransactionBuilders.TokenBuilder::buildUpdateFeeSchedule);
        builders.put("airdropToken", TransactionBuilders.TokenBuilder::buildAirdrop);
        builders.put("cancelAirdrop", TransactionBuilders.TokenBuilder::buildCancelAirdrop);
        builders.put("claimToken", TransactionBuilders.TokenBuilder::buildClaimAirdrop);
        builders.put("deleteAllowance", TransactionBuilders.AccountBuilder::buildDeleteAllowance);
        builders.put("appendFile", TransactionBuilders.FileBuilder::buildAppend);
        SCHEDULED_TRANSACTION_BUILDERS = Collections.unmodifiableMap(builders);
    }

    @JSONRPC2Method("createSchedule")
    public ScheduleResponse createSchedule(final ScheduleCreateParams params) throws Exception {
        ScheduleCreateTransaction transaction = new ScheduleCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.getSessionId());

        params.getScheduledTransaction().ifPresent(scheduledTx -> {
            try {
                Transaction<?> tx = buildScheduledTransaction(scheduledTx, params.getSessionId());
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

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

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
        Client client = sdkService.getClient(params.getSessionId());

        params.getScheduleId()
                .ifPresent(scheduleIdStr -> transaction.setScheduleId(ScheduleId.fromString(scheduleIdStr)));

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

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
        Client client = sdkService.getClient(params.getSessionId());

        params.getScheduleId()
                .ifPresent(scheduleIdStr -> transaction.setScheduleId(ScheduleId.fromString(scheduleIdStr)));

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

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
    private Transaction<?> buildScheduledTransaction(
            ScheduleCreateParams.ScheduledTransaction scheduledTx, String sessionId) {
        Map<String, Object> params = new HashMap<>(scheduledTx.getParams());
        params.put("sessionId", sessionId);

        Function<Map<String, Object>, Transaction<?>> builder =
                SCHEDULED_TRANSACTION_BUILDERS.get(scheduledTx.getMethod());
        if (builder == null) {
            throw new IllegalArgumentException("Unsupported scheduled transaction method: " + scheduledTx.getMethod());
        }
        return builder.apply(params);
    }
}
