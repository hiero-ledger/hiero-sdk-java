// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleInfoParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenInfoQueryParams;
import java.time.Duration;

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
}
