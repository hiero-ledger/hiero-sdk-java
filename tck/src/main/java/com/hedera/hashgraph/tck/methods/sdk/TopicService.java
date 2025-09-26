// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.*;
import com.hedera.hashgraph.tck.methods.sdk.response.TopicResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(sdkService.getClient());

        String topicId = "";
        if (receipt.status == Status.SUCCESS && receipt.topicId != null) {
            topicId = receipt.topicId.toString();
        }

        return new TopicResponse(topicId, receipt.status);
    }

    @JSONRPC2Method("updateTopic")
    public TopicResponse updateTopic(final UpdateTopicParams params) throws Exception {
        TopicUpdateTransaction transaction = new TopicUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getTopicId().ifPresent(topicIdStr -> {
            try {
                transaction.setTopicId(TopicId.fromString(topicIdStr));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid topic ID: " + topicIdStr, e);
            }
        });

        params.getMemo().ifPresent(transaction::setTopicMemo);

        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Invalid admin key: " + key, e);
            }
        });

        params.getSubmitKey().ifPresent(key -> {
            try {
                transaction.setSubmitKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Invalid submit key: " + key, e);
            }
        });

        params.getFeeScheduleKey().ifPresent(key -> {
            try {
                transaction.setFeeScheduleKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Invalid fee schedule key: " + key, e);
            }
        });

        params.getFeeExemptKeys().ifPresent(keyStrings -> {
            if (keyStrings.isEmpty()) {
                transaction.clearFeeExemptKeys();
            } else {
                List<Key> keys = new ArrayList<>();
                for (String keyStr : keyStrings) {
                    try {
                        keys.add(KeyUtils.getKeyFromString(keyStr));
                    } catch (InvalidProtocolBufferException e) {
                        throw new IllegalArgumentException("Invalid fee exempt key: " + keyStr, e);
                    }
                }
                transaction.setFeeExemptKeys(keys);
            }
        });

        params.getAutoRenewPeriod().ifPresent(periodStr -> {
            try {
                long periodSeconds = Long.parseLong(periodStr);
                transaction.setAutoRenewPeriod(Duration.ofSeconds(periodSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid auto renew period: " + periodStr, e);
            }
        });

        params.getAutoRenewAccountId().ifPresent(accountIdStr -> {
            try {
                transaction.setAutoRenewAccountId(AccountId.fromString(accountIdStr));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid auto renew account ID: " + accountIdStr, e);
            }
        });

        params.getExpirationTime().ifPresent(expirationTimeStr -> {
            try {
                long expirationTimeSeconds = Long.parseLong(expirationTimeStr);
                transaction.setExpirationTime(Duration.ofSeconds(expirationTimeSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid expiration time: " + expirationTimeStr, e);
            }
        });

        params.getCustomFees().ifPresent(customFees -> {
            if (customFees.isEmpty()) {
                transaction.clearCustomFees();
            } else {
                List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                        customFees.get(0).fillOutCustomFees(customFees);

                // Filter for fixed fees only as topics don't support fractional/royalty fees
                List<CustomFixedFee> topicCustomFees = new ArrayList<>();
                for (com.hedera.hashgraph.sdk.CustomFee fee : sdkCustomFees) {
                    if (fee instanceof CustomFixedFee) {
                        topicCustomFees.add((CustomFixedFee) fee);
                    }
                }

                transaction.setCustomFees(topicCustomFees);
            }
        });

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(sdkService.getClient());

        return new TopicResponse(null, receipt.status);
    }

    @JSONRPC2Method("deleteTopic")
    public TopicResponse deleteTopic(final DeleteTopicParams params) throws Exception {
        TopicDeleteTransaction transaction = new TopicDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getTopicId().ifPresent(topicIdStr -> {
            try {
                transaction.setTopicId(TopicId.fromString(topicIdStr));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid topic ID: " + topicIdStr, e);
            }
        });

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(sdkService.getClient());

        return new TopicResponse(null, receipt.status);
    }

    @JSONRPC2Method("submitTopicMessage")
    public TopicResponse submitTopicMessage(final SubmitTopicMessageParams params) throws Exception {
        TopicMessageSubmitTransaction transaction = TransactionBuilders.TopicBuilder.buildSubmitMessage(params);

        params.getCommonTransactionParams()
                .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.setValidateStatus(true).getReceipt(sdkService.getClient());

        return new TopicResponse(null, receipt.status);
    }
}
