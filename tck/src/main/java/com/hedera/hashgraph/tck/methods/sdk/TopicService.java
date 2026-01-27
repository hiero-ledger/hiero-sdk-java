// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.*;
import com.hedera.hashgraph.tck.methods.sdk.response.TopicResponse;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;

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
}
