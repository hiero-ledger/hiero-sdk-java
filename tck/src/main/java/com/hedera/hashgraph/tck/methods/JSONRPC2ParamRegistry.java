// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import static java.util.Map.entry;

import com.hedera.hashgraph.tck.methods.sdk.param.BaseParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import com.hedera.hashgraph.tck.methods.sdk.param.GenerateKeyParams;
import com.hedera.hashgraph.tck.methods.sdk.param.SetupParams;
import com.hedera.hashgraph.tck.methods.sdk.param.TransactionReceiptQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountAllowanceParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountBalanceQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.GetAccountInfoParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractByteCodeQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.CreateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.DeleteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ExecuteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.InfoQueryContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.UpdateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.ethereum.EthereumTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileAppendParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileContentsParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.AddressBookQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleInfoParams;
import com.hedera.hashgraph.tck.methods.sdk.param.schedule.ScheduleSignParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.AssociateDisassociateTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.BurnTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.FreezeUnfreezeTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.GrantRevokeTokenKycParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.MintTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.NftInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.PauseUnpauseTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenAirdropCancelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenClaimAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenRejectAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenUpdateFeeScheduleParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenWipeParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.CreateTopicParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.CustomFeeLimit;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.DeleteTopicParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.SubmitTopicMessageParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.TopicInfoQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.UpdateTopicParams;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferCryptoParams;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

public final class JSONRPC2ParamRegistry {
    private JSONRPC2ParamRegistry() {}

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws Exception;
    }

    public static <T, R> Function<T, R> unchecked(@NonNull CheckedFunction<T, R> function) {
        Objects.requireNonNull(function, "function must not be null");

        return args -> {
            try {
                return function.apply(args);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON-RPC parameters");
            }
        };
    }

    public static Map<Class<?>, Function<Map<String, Object>, ?>> create() {
        return Map.ofEntries(
                entry(BaseParams.class, unchecked(BaseParams::parse)),
                entry(SetupParams.class, unchecked(SetupParams::parse)),
                entry(GenerateKeyParams.class, unchecked(GenerateKeyParams::parse)),
                entry(TransactionReceiptQueryParams.class, unchecked(TransactionReceiptQueryParams::parse)),
                entry(CustomFee.class, unchecked(CustomFee::parse)),
                entry(AccountAllowanceParams.class, unchecked(AccountAllowanceParams::parse)),
                entry(AccountBalanceQueryParams.class, unchecked(AccountBalanceQueryParams::parse)),
                entry(AccountCreateParams.class, unchecked(AccountCreateParams::parse)),
                entry(AccountDeleteParams.class, unchecked(AccountDeleteParams::parse)),
                entry(AccountUpdateParams.class, unchecked(AccountUpdateParams::parse)),
                entry(GetAccountInfoParams.class, unchecked(GetAccountInfoParams::parse)),
                entry(ContractByteCodeQueryParams.class, unchecked(ContractByteCodeQueryParams::parse)),
                entry(ContractCallQueryParams.class, unchecked(ContractCallQueryParams::parse)),
                entry(CreateContractParams.class, unchecked(CreateContractParams::parse)),
                entry(DeleteContractParams.class, unchecked(DeleteContractParams::parse)),
                entry(ExecuteContractParams.class, unchecked(ExecuteContractParams::parse)),
                entry(InfoQueryContractParams.class, unchecked(InfoQueryContractParams::parse)),
                entry(UpdateContractParams.class, unchecked(UpdateContractParams::parse)),
                entry(EthereumTransactionParams.class, unchecked(EthereumTransactionParams::parse)),
                entry(FileAppendParams.class, unchecked(FileAppendParams::parse)),
                entry(FileContentsParams.class, unchecked(FileContentsParams::parse)),
                entry(FileCreateParams.class, unchecked(FileCreateParams::parse)),
                entry(FileDeleteParams.class, unchecked(FileDeleteParams::parse)),
                entry(FileInfoQueryParams.class, unchecked(FileInfoQueryParams::parse)),
                entry(FileUpdateParams.class, unchecked(FileUpdateParams::parse)),
                entry(AddressBookQueryParams.class, unchecked(AddressBookQueryParams::parse)),
                entry(NodeCreateParams.class, unchecked(NodeCreateParams::parse)),
                entry(NodeDeleteParams.class, unchecked(NodeDeleteParams::parse)),
                entry(NodeUpdateParams.class, unchecked(NodeUpdateParams::parse)),
                entry(ScheduleCreateParams.class, unchecked(ScheduleCreateParams::parse)),
                entry(ScheduleDeleteParams.class, unchecked(ScheduleDeleteParams::parse)),
                entry(ScheduleInfoParams.class, unchecked(ScheduleInfoParams::parse)),
                entry(ScheduleSignParams.class, unchecked(ScheduleSignParams::parse)),
                entry(AssociateDisassociateTokenParams.class, unchecked(AssociateDisassociateTokenParams::parse)),
                entry(BurnTokenParams.class, unchecked(BurnTokenParams::parse)),
                entry(FreezeUnfreezeTokenParams.class, unchecked(FreezeUnfreezeTokenParams::parse)),
                entry(GrantRevokeTokenKycParams.class, unchecked(GrantRevokeTokenKycParams::parse)),
                entry(MintTokenParams.class, unchecked(MintTokenParams::parse)),
                entry(NftInfoQueryParams.class, unchecked(NftInfoQueryParams::parse)),
                entry(PauseUnpauseTokenParams.class, unchecked(PauseUnpauseTokenParams::parse)),
                entry(TokenAirdropCancelParams.class, unchecked(TokenAirdropCancelParams::parse)),
                entry(TokenAirdropParams.class, unchecked(TokenAirdropParams::parse)),
                entry(TokenClaimAirdropParams.class, unchecked(TokenClaimAirdropParams::parse)),
                entry(TokenCreateParams.class, unchecked(TokenCreateParams::parse)),
                entry(TokenDeleteParams.class, unchecked(TokenDeleteParams::parse)),
                entry(TokenInfoQueryParams.class, unchecked(TokenInfoQueryParams::parse)),
                entry(TokenRejectAirdropParams.class, unchecked(TokenRejectAirdropParams::parse)),
                entry(TokenUpdateFeeScheduleParams.class, unchecked(TokenUpdateFeeScheduleParams::parse)),
                entry(TokenUpdateParams.class, unchecked(TokenUpdateParams::parse)),
                entry(TokenWipeParams.class, unchecked(TokenWipeParams::parse)),
                entry(CreateTopicParams.class, unchecked(CreateTopicParams::parse)),
                entry(CustomFeeLimit.class, unchecked(CustomFeeLimit::parse)),
                entry(DeleteTopicParams.class, unchecked(DeleteTopicParams::parse)),
                entry(SubmitTopicMessageParams.class, unchecked(SubmitTopicMessageParams::parse)),
                entry(TopicInfoQueryParams.class, unchecked(TopicInfoQueryParams::parse)),
                entry(UpdateTopicParams.class, unchecked(UpdateTopicParams::parse)),
                entry(TransferCryptoParams.class, unchecked(TransferCryptoParams::parse)));
    }
}
