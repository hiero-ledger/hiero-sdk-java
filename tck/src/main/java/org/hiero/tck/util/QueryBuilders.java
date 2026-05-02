// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.util;

import java.time.Duration;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.ContractByteCodeQuery;
import org.hiero.sdk.ContractCallQuery;
import org.hiero.sdk.ContractId;
import org.hiero.sdk.FileContentsQuery;
import org.hiero.sdk.FileId;
import org.hiero.sdk.FileInfoQuery;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.NftId;
import org.hiero.sdk.ScheduleId;
import org.hiero.sdk.ScheduleInfoQuery;
import org.hiero.sdk.TokenId;
import org.hiero.sdk.TokenInfoQuery;
import org.hiero.sdk.TokenNftInfoQuery;
import org.hiero.sdk.TopicId;
import org.hiero.sdk.TopicInfoQuery;
import org.hiero.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import org.hiero.tck.methods.sdk.param.contract.ContractByteCodeQueryParams;
import org.hiero.tck.methods.sdk.param.contract.ContractCallQueryParams;
import org.hiero.tck.methods.sdk.param.file.FileContentsParams;
import org.hiero.tck.methods.sdk.param.file.FileInfoQueryParams;
import org.hiero.tck.methods.sdk.param.schedule.ScheduleInfoParams;
import org.hiero.tck.methods.sdk.param.token.NftInfoQueryParams;
import org.hiero.tck.methods.sdk.param.token.TokenInfoQueryParams;
import org.hiero.tck.methods.sdk.param.topic.TopicInfoQueryParams;

public class QueryBuilders {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);

    /**
     * Token-related query builders
     */
    public static class TokenBuilder {

        public static TokenInfoQuery buildTokenInfo(TokenInfoQueryParams params) {
            TokenInfoQuery query = new TokenInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            params.getTokenId().ifPresent(tokenId -> query.setTokenId(TokenId.fromString(tokenId)));

            return query;
        }

        public static TokenNftInfoQuery buildNftInfo(NftInfoQueryParams params) {
            TokenNftInfoQuery query = new TokenNftInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            query.setNftId(NftId.fromString(params.getNftId()));

            return query;
        }
    }

    /**
     * Account-related query builders
     */
    public static class AccountBuilder {

        public static AccountBalanceQuery buildAccountBalanceQuery(AccountBalanceQueryParams params) {
            AccountBalanceQuery query = new AccountBalanceQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            params.getAccountId().ifPresent(accountId -> query.setAccountId(AccountId.fromString(accountId)));
            params.getContractId()
                    .ifPresent(contractIdStr -> query.setContractId(ContractId.fromString(contractIdStr)));

            return query;
        }
    }

    /**
     * Schedule-related query builders
     */
    public static class ScheduleBuilder {

        public static ScheduleInfoQuery buildScheduleInfoQuery(ScheduleInfoParams params) {
            ScheduleInfoQuery query = new ScheduleInfoQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));

            if (params.getScheduleId() != null) {
                query.setScheduleId(ScheduleId.fromString(params.getScheduleId()));
            }

            if (params.getQueryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getQueryPayment())));
            }

            if (params.getMaxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getMaxQueryPayment())));
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

            if (params.getFileId() != null) {
                query.setFileId(FileId.fromString(params.getFileId()));
            }

            if (params.getQueryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getQueryPayment())));
            }

            if (params.getMaxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getMaxQueryPayment())));
            }

            return query;
        }

        public static FileContentsQuery buildFileContents(FileContentsParams params) {
            FileContentsQuery query = new FileContentsQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            if (params.getFileId() != null) {
                query.setFileId(FileId.fromString(params.getFileId()));
            }

            params.getQueryPayment()
                    .ifPresent(queryPayment -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPayment))));

            params.getMaxQueryPayment()
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
            if (params.getTopicId() != null) {
                query.setTopicId(TopicId.fromString(params.getTopicId()));
            }

            if (params.getQueryPayment() != null) {
                query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getQueryPayment())));
            }

            if (params.getMaxQueryPayment() != null) {
                query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(params.getMaxQueryPayment())));
            }

            return query;
        }
    }

    public static ContractByteCodeQuery buildContractBytecode(ContractByteCodeQueryParams params) {
        ContractByteCodeQuery query = new ContractByteCodeQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getContractId().ifPresent(contractId -> query.setContractId(ContractId.fromString(contractId)));

        params.getQueryPayment()
                .ifPresent(queryPayment -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPayment))));

        params.getMaxQueryPayment()
                .ifPresent(maxQueryPayment ->
                        query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(maxQueryPayment))));

        return query;
    }

    public static ContractCallQuery buildContractCall(ContractCallQueryParams params) {
        ContractCallQuery query = new ContractCallQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
        if (params.getContractId() != null) {
            query.setContractId(ContractId.fromString(params.getContractId()));
        }
        if (params.getGas() != null) {
            query.setGas(Long.parseLong(params.getGas()));
        }
        if (params.getFunctionParameters() != null) {
            query.setFunctionParameters(Hex.decode(params.getFunctionParameters()));
        }
        if (params.getMaxResultSize() != null) {
            query.setMaxResultSize(Long.parseLong(params.getMaxResultSize()));
        }
        if (params.getSenderAccountId() != null) {
            query.setSenderAccountId(AccountId.fromString(params.getSenderAccountId()));
        }

        return query;
    }
}
