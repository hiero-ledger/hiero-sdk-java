// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AddressBookQuery;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.AddressBookQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenInfoQueryParams;
import java.time.Duration;

public class QueryBuilders {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);

    /**
     * AddressBook-related query builders
     */
    public static class AddressBookBuilder {

        public static AddressBookQuery addressBookQuery(final AddressBookQueryParams params) {
            AddressBookQuery query = new AddressBookQuery();
            params.getFileId().ifPresent(fileId -> query.setFileId(FileId.fromString(fileId)));

            return query;
        }
    }

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
}
