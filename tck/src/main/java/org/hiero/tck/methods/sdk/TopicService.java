// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk;

import java.time.Duration;
import java.util.List;
import org.hiero.sdk.Client;
import org.hiero.sdk.CustomFixedFee;
import org.hiero.sdk.Status;
import org.hiero.sdk.TopicCreateTransaction;
import org.hiero.sdk.TopicDeleteTransaction;
import org.hiero.sdk.TopicInfo;
import org.hiero.sdk.TopicInfoQuery;
import org.hiero.sdk.TopicMessageSubmitTransaction;
import org.hiero.sdk.TopicUpdateTransaction;
import org.hiero.sdk.TransactionReceipt;
import org.hiero.sdk.TransactionResponse;
import org.hiero.tck.annotation.JSONRPC2Method;
import org.hiero.tck.annotation.JSONRPC2Service;
import org.hiero.tck.methods.AbstractJSONRPC2Service;
import org.hiero.tck.methods.sdk.param.topic.CreateTopicParams;
import org.hiero.tck.methods.sdk.param.topic.DeleteTopicParams;
import org.hiero.tck.methods.sdk.param.topic.SubmitTopicMessageParams;
import org.hiero.tck.methods.sdk.param.topic.TopicInfoQueryParams;
import org.hiero.tck.methods.sdk.param.topic.UpdateTopicParams;
import org.hiero.tck.methods.sdk.response.TopicInfoResponse;
import org.hiero.tck.methods.sdk.response.TopicResponse;
import org.hiero.tck.util.QueryBuilders;
import org.hiero.tck.util.TransactionBuilders;

/**
 * TopicService for topic related methods
 */
@JSONRPC2Service
public class TopicService extends AbstractJSONRPC2Service {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(3L);
    private final SdkService sdkService;

    public TopicService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createTopic")
    public TopicResponse createTopic(final CreateTopicParams params) throws Exception {
        TopicCreateTransaction transaction = TransactionBuilders.TopicBuilder.buildCreate(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(client);

        String topicId = "";
        if (receipt.status == Status.SUCCESS && receipt.topicId != null) {
            topicId = receipt.topicId.toString();
        }

        return new TopicResponse(topicId, receipt.status);
    }

    @JSONRPC2Method("updateTopic")
    public TopicResponse updateTopic(final UpdateTopicParams params) throws Exception {
        TopicUpdateTransaction transaction = TransactionBuilders.TopicBuilder.buildUpdate(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(client);

        return new TopicResponse(null, receipt.status);
    }

    @JSONRPC2Method("deleteTopic")
    public TopicResponse deleteTopic(final DeleteTopicParams params) throws Exception {
        TopicDeleteTransaction transaction = TransactionBuilders.TopicBuilder.buildDelete(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(client);

        return new TopicResponse(null, receipt.status);
    }

    @JSONRPC2Method("submitTopicMessage")
    public TopicResponse submitTopicMessage(final SubmitTopicMessageParams params) throws Exception {
        TopicMessageSubmitTransaction transaction = TransactionBuilders.TopicBuilder.buildSubmitMessage(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(client);

        return new TopicResponse(null, receipt.status);
    }

    @JSONRPC2Method("getTopicInfo")
    public TopicInfoResponse getTopicInfo(final TopicInfoQueryParams params) throws Exception {
        TopicInfoQuery query = QueryBuilders.TopicBuilder.buildTopicInfoQuery(params);
        Client client = sdkService.getClient(params.getSessionId());

        TopicInfo result = query.execute(client);
        return mapTopicInfoResponse(result);
    }

    /**
     * Map TopicInfo from SDK to TopicInfoResponse for JSON-RPC
     */
    private TopicInfoResponse mapTopicInfoResponse(final TopicInfo topicInfo) {
        String adminKey = topicInfo.adminKey != null ? topicInfo.adminKey.toString() : null;
        String submitKey = topicInfo.submitKey != null ? topicInfo.submitKey.toString() : null;
        String autoRenewAccountId =
                topicInfo.autoRenewAccountId != null ? topicInfo.autoRenewAccountId.toString() : null;
        String feeScheduleKey = topicInfo.feeScheduleKey != null ? topicInfo.feeScheduleKey.toString() : null;

        List<String> feeExemptKeys = topicInfo.feeExemptKeys == null
                ? null
                : topicInfo.feeExemptKeys.stream().map(key -> key.toString()).toList();
        List<TopicInfoResponse.CustomFeeResponse> customFees = topicInfo.customFees == null
                ? null
                : topicInfo.customFees.stream()
                        .map(fee -> mapToCustomFeeResponse(fee))
                        .toList();

        return new TopicInfoResponse(
                topicInfo.topicId.toString(),
                topicInfo.topicMemo,
                String.valueOf(topicInfo.sequenceNumber),
                topicInfo.runningHash.toString(),
                adminKey,
                submitKey,
                autoRenewAccountId,
                String.valueOf(topicInfo.autoRenewPeriod.getSeconds()),
                String.valueOf(topicInfo.expirationTime.getEpochSecond()),
                feeScheduleKey,
                feeExemptKeys,
                customFees,
                topicInfo.ledgerId.toString());
    }

    /**
     * Map TopicInfo CustomFixedFee from SDK to TopicInfoResponse.CustomFeeResponse for JSON-RPC
     */
    private TopicInfoResponse.CustomFeeResponse mapToCustomFeeResponse(final CustomFixedFee fee) {
        TopicInfoResponse.FixedFeeResponse fixedFee = new TopicInfoResponse.FixedFeeResponse(
                String.valueOf(fee.getAmount()),
                fee.getDenominatingTokenId() != null
                        ? fee.getDenominatingTokenId().toString()
                        : null);

        return new TopicInfoResponse.CustomFeeResponse(
                fee.getFeeCollectorAccountId() != null
                        ? fee.getFeeCollectorAccountId().toString()
                        : null,
                fee.getAllCollectorsAreExempt(),
                fixedFee);
    }
}
