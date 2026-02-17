// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileContentsParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.NftInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenInfoQueryParams;
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
     * File-related query builders
     */
    public static class FileBuilder {

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
