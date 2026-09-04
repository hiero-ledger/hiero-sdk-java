// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import static java.util.Map.entry;

import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2ParamsException;
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

/**
 * Registry of JSON-RPC parameter parsers.
 * <p>
 * Each registered parameter type is associated with a parser that converts the
 * JSON-RPC request parameters into an instance of that type. Parameter classes are
 * responsible for defining their own {@code parse} method, while this registry
 * provides the mapping between parameter types and their parsers.
 *
 */
public final class JSONRPC2ParamRegistry {
    private static final Map<Class<?>, Function<Map<String, Object>, ? extends JSONRPC2Param>> REGISTRY =
            initRegistry();

    private JSONRPC2ParamRegistry() {}

    /**
     * Initializes the JSON-RPC parameter parser registry.
     *
     * @return the initialized parser registry
     */
    private static Map<Class<?>, Function<Map<String, Object>, ? extends JSONRPC2Param>> initRegistry() {
        return Map.<Class<?>, Function<Map<String, Object>, ? extends JSONRPC2Param>>ofEntries(
                entry(BaseParams.class, toUncheckedFunction(BaseParams::parse)),
                entry(SetupParams.class, toUncheckedFunction(SetupParams::parse)),
                entry(GenerateKeyParams.class, toUncheckedFunction(GenerateKeyParams::parse)),
                entry(TransactionReceiptQueryParams.class, toUncheckedFunction(TransactionReceiptQueryParams::parse)),
                entry(CustomFee.class, toUncheckedFunction(CustomFee::parse)),
                entry(AccountAllowanceParams.class, toUncheckedFunction(AccountAllowanceParams::parse)),
                entry(AccountBalanceQueryParams.class, toUncheckedFunction(AccountBalanceQueryParams::parse)),
                entry(AccountCreateParams.class, toUncheckedFunction(AccountCreateParams::parse)),
                entry(AccountDeleteParams.class, toUncheckedFunction(AccountDeleteParams::parse)),
                entry(AccountUpdateParams.class, toUncheckedFunction(AccountUpdateParams::parse)),
                entry(GetAccountInfoParams.class, toUncheckedFunction(GetAccountInfoParams::parse)),
                entry(ContractByteCodeQueryParams.class, toUncheckedFunction(ContractByteCodeQueryParams::parse)),
                entry(ContractCallQueryParams.class, toUncheckedFunction(ContractCallQueryParams::parse)),
                entry(CreateContractParams.class, toUncheckedFunction(CreateContractParams::parse)),
                entry(DeleteContractParams.class, toUncheckedFunction(DeleteContractParams::parse)),
                entry(ExecuteContractParams.class, toUncheckedFunction(ExecuteContractParams::parse)),
                entry(InfoQueryContractParams.class, toUncheckedFunction(InfoQueryContractParams::parse)),
                entry(UpdateContractParams.class, toUncheckedFunction(UpdateContractParams::parse)),
                entry(EthereumTransactionParams.class, toUncheckedFunction(EthereumTransactionParams::parse)),
                entry(FileAppendParams.class, toUncheckedFunction(FileAppendParams::parse)),
                entry(FileContentsParams.class, toUncheckedFunction(FileContentsParams::parse)),
                entry(FileCreateParams.class, toUncheckedFunction(FileCreateParams::parse)),
                entry(FileDeleteParams.class, toUncheckedFunction(FileDeleteParams::parse)),
                entry(FileInfoQueryParams.class, toUncheckedFunction(FileInfoQueryParams::parse)),
                entry(FileUpdateParams.class, toUncheckedFunction(FileUpdateParams::parse)),
                entry(AddressBookQueryParams.class, toUncheckedFunction(AddressBookQueryParams::parse)),
                entry(NodeCreateParams.class, toUncheckedFunction(NodeCreateParams::parse)),
                entry(NodeDeleteParams.class, toUncheckedFunction(NodeDeleteParams::parse)),
                entry(NodeUpdateParams.class, toUncheckedFunction(NodeUpdateParams::parse)),
                entry(ScheduleCreateParams.class, toUncheckedFunction(ScheduleCreateParams::parse)),
                entry(ScheduleDeleteParams.class, toUncheckedFunction(ScheduleDeleteParams::parse)),
                entry(ScheduleInfoParams.class, toUncheckedFunction(ScheduleInfoParams::parse)),
                entry(ScheduleSignParams.class, toUncheckedFunction(ScheduleSignParams::parse)),
                entry(
                        AssociateDisassociateTokenParams.class,
                        toUncheckedFunction(AssociateDisassociateTokenParams::parse)),
                entry(BurnTokenParams.class, toUncheckedFunction(BurnTokenParams::parse)),
                entry(FreezeUnfreezeTokenParams.class, toUncheckedFunction(FreezeUnfreezeTokenParams::parse)),
                entry(GrantRevokeTokenKycParams.class, toUncheckedFunction(GrantRevokeTokenKycParams::parse)),
                entry(MintTokenParams.class, toUncheckedFunction(MintTokenParams::parse)),
                entry(NftInfoQueryParams.class, toUncheckedFunction(NftInfoQueryParams::parse)),
                entry(PauseUnpauseTokenParams.class, toUncheckedFunction(PauseUnpauseTokenParams::parse)),
                entry(TokenAirdropCancelParams.class, toUncheckedFunction(TokenAirdropCancelParams::parse)),
                entry(TokenAirdropParams.class, toUncheckedFunction(TokenAirdropParams::parse)),
                entry(TokenClaimAirdropParams.class, toUncheckedFunction(TokenClaimAirdropParams::parse)),
                entry(TokenCreateParams.class, toUncheckedFunction(TokenCreateParams::parse)),
                entry(TokenDeleteParams.class, toUncheckedFunction(TokenDeleteParams::parse)),
                entry(TokenInfoQueryParams.class, toUncheckedFunction(TokenInfoQueryParams::parse)),
                entry(TokenRejectAirdropParams.class, toUncheckedFunction(TokenRejectAirdropParams::parse)),
                entry(TokenUpdateFeeScheduleParams.class, toUncheckedFunction(TokenUpdateFeeScheduleParams::parse)),
                entry(TokenUpdateParams.class, toUncheckedFunction(TokenUpdateParams::parse)),
                entry(TokenWipeParams.class, toUncheckedFunction(TokenWipeParams::parse)),
                entry(CreateTopicParams.class, toUncheckedFunction(CreateTopicParams::parse)),
                entry(CustomFeeLimit.class, toUncheckedFunction(CustomFeeLimit::parse)),
                entry(DeleteTopicParams.class, toUncheckedFunction(DeleteTopicParams::parse)),
                entry(SubmitTopicMessageParams.class, toUncheckedFunction(SubmitTopicMessageParams::parse)),
                entry(TopicInfoQueryParams.class, toUncheckedFunction(TopicInfoQueryParams::parse)),
                entry(UpdateTopicParams.class, toUncheckedFunction(UpdateTopicParams::parse)),
                entry(TransferCryptoParams.class, toUncheckedFunction(TransferCryptoParams::parse)));
    }

    /**
     * Parses JSON-RPC parameters using the parser registered for the given parameter type.
     *
     * @param parameterType the parameter type whose registered parser should be used
     * @param params the JSON-RPC request parameters to parse
     * @return the parsed JSON-RPC parameter
     */
    public static JSONRPC2Param parse(Class<?> parameterType, Map<String, Object> params)
            throws InvalidJSONRPC2ParamsException {
        Function<Map<String, Object>, ? extends JSONRPC2Param> parser = REGISTRY.get(parameterType);
        if (parser == null) {
            throw new InvalidJSONRPC2ParamsException(
                    "No parser registered for parameter type: " + parameterType.getName());
        }

        return parser.apply(params);
    }

    /**
     * Helper functional interface for functions that throw a checked exception.
     *
     * @param <InputType> the input type
     * @param <ReturnType> the return type
     */
    @FunctionalInterface
    private interface CheckedFunction<InputType, ReturnType> {
        ReturnType apply(InputType input) throws Exception;
    }

    /**
     * Converts a {@link CheckedFunction} into a standard {@link Function}.
     *
     * @param function the checked function to convert
     * @return a function that delegates to the supplied checked function
     * @throws IllegalArgumentException if the checked function fail to parse the params
     */
    private static <InputType, ReturnType extends JSONRPC2Param> Function<InputType, ReturnType> toUncheckedFunction(
            @NonNull CheckedFunction<InputType, ReturnType> function) {
        Objects.requireNonNull(function, "function must not be null");

        return args -> {
            try {
                return function.apply(args);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse JSON-RPC parameters");
            }
        };
    }
}
