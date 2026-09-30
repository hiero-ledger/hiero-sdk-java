// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractByteCodeQuery;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.FileInfoQuery;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceiptQuery;
import com.hedera.hashgraph.tck.methods.sdk.param.TransactionReceiptQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractByteCodeQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileContentsParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleInfoParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.NftInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.TopicInfoQueryParams;
import java.time.Duration;
import org.bouncycastle.util.encoders.Hex;

public class QueryBuilders {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);

    /**
     * Token-related query builders
     */
    public static class TokenBuilder {

        public static TokenInfoQuery buildTokenInfo(TokenInfoQueryParams params) {
            TokenInfoQuery query = new TokenInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            params.tokenId().ifPresent(tokenId -> query.setTokenId(TokenId.fromString(tokenId)));

            return query;
        }

        public static TokenNftInfoQuery buildNftInfo(NftInfoQueryParams params) {
            TokenNftInfoQuery query = new TokenNftInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            query.setNftId(NftId.fromString(params.nftId()));

            return query;
        }
    }

    /**
     * Account-related query builders
     */
    public static class AccountBuilder {

        public static AccountBalanceQuery buildAccountBalanceQuery(AccountBalanceQueryParams params) {
            AccountBalanceQuery query = new AccountBalanceQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            params.accountId().ifPresent(accountId -> query.setAccountId(AccountId.fromString(accountId)));
            params.contractId().ifPresent(contractIdStr -> query.setContractId(ContractId.fromString(contractIdStr)));

            return query;
        }
    }

    /**
     * Schedule-related query builders
     */
    public static class ScheduleBuilder {

        public static ScheduleInfoQuery buildScheduleInfoQuery(ScheduleInfoParams params) {
            ScheduleInfoQuery query = new ScheduleInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));

            if (params.scheduleId() != null) {
                query.setScheduleId(ScheduleId.fromString(params.scheduleId()));
            }

            if (params.queryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.queryPayment())));
            }

            if (params.maxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.maxQueryPayment())));
            }

            return query;
        }
    }

    /**
     * File-related query builders
     */
    public static class FileBuilder {
        public static FileInfoQuery buildFileInfoQuery(FileInfoQueryParams params) {
            FileInfoQuery query = new FileInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));

            if (params.fileId() != null) {
                query.setFileId(FileId.fromString(params.fileId()));
            }

            if (params.queryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.queryPayment())));
            }

            if (params.maxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.maxQueryPayment())));
            }

            return query;
        }

        public static FileContentsQuery buildFileContents(FileContentsParams params) {
            FileContentsQuery query = new FileContentsQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            if (params.fileId() != null) {
                query.setFileId(FileId.fromString(params.fileId()));
            }

            params.queryPayment()
                    .ifPresent(queryPayment -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPayment))));

            params.maxQueryPayment()
                    .ifPresent(maxQueryPayment ->
                            query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(maxQueryPayment))));

            return query;
        }
    }

    /**
     * Topic-related query builder
     */
    public static class TopicBuilder {
        public static TopicInfoQuery buildTopicInfoQuery(TopicInfoQueryParams params) {
            TopicInfoQuery query = new TopicInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            if (params.topicId() != null) {
                query.setTopicId(TopicId.fromString(params.topicId()));
            }

            if (params.queryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.queryPayment())));
            }

            if (params.maxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.maxQueryPayment())));
            }

            return query;
        }
    }

    public static ContractByteCodeQuery buildContractBytecode(ContractByteCodeQueryParams params) {
        ContractByteCodeQuery query = new ContractByteCodeQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.contractId().ifPresent(contractId -> query.setContractId(ContractId.fromString(contractId)));

        params.queryPayment()
                .ifPresent(queryPayment -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPayment))));

        params.maxQueryPayment()
                .ifPresent(maxQueryPayment ->
                        query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(maxQueryPayment))));

        return query;
    }

    public static ContractCallQuery buildContractCall(ContractCallQueryParams params) {
        ContractCallQuery query = new ContractCallQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
        if (params.contractId() != null) {
            query.setContractId(ContractId.fromString(params.contractId()));
        }
        if (params.gas() != null) {
            query.setGas(Long.parseLong(params.gas()));
        }
        if (params.functionParameters() != null) {
            query.setFunctionParameters(Hex.decode(params.functionParameters()));
        }
        if (params.maxResultSize() != null) {
            query.setMaxResultSize(Long.parseLong(params.maxResultSize()));
        }
        if (params.senderAccountId() != null) {
            query.setSenderAccountId(AccountId.fromString(params.senderAccountId()));
        }

        return query;
    }

    public static TransactionReceiptQuery buildTransactionReceiptQuery(TransactionReceiptQueryParams params) {
        TransactionReceiptQuery query = new TransactionReceiptQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        if (params.transactionId() != null) {
            query.setTransactionId(TransactionId.fromString(params.transactionId()));
        }

        query.setIncludeChildren(params.includeChildren());
        query.setIncludeDuplicates(params.includeDuplicates());
        return query;
    }
}
