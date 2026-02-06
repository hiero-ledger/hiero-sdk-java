// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
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

    public static class ContractBuilder {

        public static ContractCallQuery buildContractCall(ContractCallQueryParams params) {
            ContractCallQuery query = new ContractCallQuery().setGrpcDeadline((DEFAULT_GRPC_DEADLINE));
            params.getContractId().ifPresent(accountId -> query.setContractId(ContractId.fromString(accountId)));
            params.getGas().ifPresent(gas -> query.setGas(Long.parseLong(gas)));
            params.getFunctionParameters().ifPresent(hex -> query.setFunctionParameters(Hex.decode(hex)));
            params.getMaxResultSize().ifPresent(maxResultSize -> query.setMaxResultSize(Long.parseLong(maxResultSize)));
            params.getSenderAccountId()
                    .ifPresent(accountId -> query.setSenderAccountId(AccountId.fromString(accountId)));

            return query;
        }
    }
}
