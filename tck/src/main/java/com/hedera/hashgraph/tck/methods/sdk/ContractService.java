// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ContractByteCodeQuery;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionResult;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractInfo;
import com.hedera.hashgraph.sdk.ContractInfoQuery;
import com.hedera.hashgraph.sdk.ContractUpdateTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.StakingInfo;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractByteCodeQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.CreateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.DeleteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ExecuteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.InfoQueryContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.UpdateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractByteCodeResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractCallResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractResponse.ContractInfoQueryResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractResponse.ContractInfoQueryResponse.StakingInfoResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import com.hedera.hashgraph.tck.util.QueryBuilders;
import java.time.Duration;
import org.bouncycastle.util.encoders.Hex;

@JSONRPC2Service
public class ContractService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public ContractService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("contractByteCodeQuery")
    public ContractByteCodeResponse contractByteCodeQuery(final ContractByteCodeQueryParams params) throws Exception {
        ContractByteCodeQuery query = QueryBuilders.buildContractBytecode(params);
        Client client = sdkService.getClient(params.sessionId());

        ByteString response = query.execute(client);

        return new ContractByteCodeResponse(query.getContractId().toString(), Hex.toHexString(response.toByteArray()));
    }

    @JSONRPC2Method("contractCallQuery")
    public ContractCallResponse contractCallQuery(final ContractCallQueryParams params) throws Exception {
        ContractCallQuery query = QueryBuilders.buildContractCall(params);
        Client client = sdkService.getClient(params.sessionId());

        ContractFunctionResult result = query.execute(client);

        return new ContractCallResponse(
                result.contractId.toString(),
                result.evmAddress,
                result.errorMessage,
                result.gasUsed,
                result.logs,
                result.gas,
                result.hbarAmount.toString(),
                result.senderAccountId != null ? result.senderAccountId.toString() : null,
                result.signerNonce,
                Hex.toHexString(result.asBytes()));
    }

    @JSONRPC2Method("createContract")
    public ContractResponse createContract(final CreateContractParams params) throws Exception {
        ContractCreateTransaction transaction = new ContractCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        params.adminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.autoRenewPeriod()
                .ifPresent(periodStr -> transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(periodStr))));

        params.gas().ifPresent(gasStr -> transaction.setGas(Long.parseLong(gasStr)));

        params.autoRenewAccountId()
                .ifPresent(accountIdStr -> transaction.setAutoRenewAccountId(AccountId.fromString(accountIdStr)));

        params.initialBalance()
                .ifPresent(balanceStr -> transaction.setInitialBalance(Hbar.fromTinybars(Long.parseLong(balanceStr))));

        params.initcode().ifPresent(hex -> transaction.setBytecode(Hex.decode(hex)));

        params.bytecodeFileId().ifPresent(fileIdStr -> transaction.setBytecodeFileId(FileId.fromString(fileIdStr)));

        params.stakedAccountId()
                .ifPresent(accountIdStr -> transaction.setStakedAccountId(AccountId.fromString(accountIdStr)));

        params.stakedNodeId().ifPresent(nodeIdStr -> transaction.setStakedNodeId(Long.parseLong(nodeIdStr)));

        params.declineStakingReward().ifPresent(transaction::setDeclineStakingReward);

        params.memo().ifPresent(transaction::setContractMemo);

        params.maxAutomaticTokenAssociations()
                .ifPresent(maxAuto -> transaction.setMaxAutomaticTokenAssociations(maxAuto.intValue()));

        params.constructorParameters().ifPresent(hex -> transaction.setConstructorParameters(Hex.decode(hex)));

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        String contractId = "";
        if (receipt.status == Status.SUCCESS && receipt.contractId != null) {
            contractId = receipt.contractId.toString();
        }

        return new ContractResponse(contractId, receipt.status);
    }

    @JSONRPC2Method("executeContract")
    public ContractResponse executeContract(final ExecuteContractParams params) throws Exception {
        ContractExecuteTransaction transaction =
                new ContractExecuteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        if (params.contractId() != null) {
            transaction.setContractId(ContractId.fromString(params.contractId()));
        }

        params.gas().ifPresent(gasStr -> transaction.setGas(Long.parseLong(gasStr)));

        params.amount()
                .ifPresent(amountStr -> transaction.setPayableAmount(Hbar.fromTinybars(Long.parseLong(amountStr))));

        params.functionParameters()
                .ifPresent(hex -> transaction.setFunctionParameters(ByteString.copyFrom(Hex.decode(hex))));

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse("", receipt.status);
    }

    @JSONRPC2Method("updateContract")
    public ContractResponse updateContract(final UpdateContractParams params) throws Exception {
        ContractUpdateTransaction transaction = new ContractUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        params.contractId().ifPresent(contractIdStr -> transaction.setContractId(ContractId.fromString(contractIdStr)));

        params.adminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.autoRenewPeriod()
                .ifPresent(periodStr -> transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(periodStr))));

        params.autoRenewAccountId()
                .ifPresent(accountIdStr -> transaction.setAutoRenewAccountId(AccountId.fromString(accountIdStr)));

        params.stakedAccountId()
                .ifPresent(accountIdStr -> transaction.setStakedAccountId(AccountId.fromString(accountIdStr)));

        params.stakedNodeId().ifPresent(nodeIdStr -> transaction.setStakedNodeId(Long.parseLong(nodeIdStr)));

        params.declineStakingReward().ifPresent(transaction::setDeclineStakingReward);

        params.memo().ifPresent(transaction::setContractMemo);

        params.maxAutomaticTokenAssociations()
                .ifPresent(maxAuto -> transaction.setMaxAutomaticTokenAssociations(maxAuto.intValue()));

        params.expirationTime().ifPresent(expirationTimeStr -> {
            try {
                long expirationTimeSeconds = Long.parseLong(expirationTimeStr);
                transaction.setExpirationTime(Duration.ofSeconds(expirationTimeSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid expiration time: " + expirationTimeStr, e);
            }
        });

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse(null, receipt.status);
    }

    @JSONRPC2Method("deleteContract")
    public ContractResponse deleteContract(final DeleteContractParams params) throws Exception {
        ContractDeleteTransaction transaction = new ContractDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        params.contractId().ifPresent(contractIdStr -> transaction.setContractId(ContractId.fromString(contractIdStr)));

        if (params.transferAccountId().isPresent()
                && params.transferContractId().isPresent()) {
            transaction.setTransferAccountId(
                    AccountId.fromString(params.transferAccountId().get()));
        } else {
            params.transferContractId()
                    .ifPresent(transferContractIdStr ->
                            transaction.setTransferContractId(ContractId.fromString(transferContractIdStr)));

            params.transferAccountId()
                    .ifPresent(transferAccountIdStr ->
                            transaction.setTransferAccountId(AccountId.fromString(transferAccountIdStr)));
        }

        params.permanentRemoval().ifPresent(transaction::setPermanentRemoval);

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse(null, receipt.status);
    }

    @JSONRPC2Method("contractInfoQuery")
    public ContractInfoQueryResponse contractInfoQuery(final InfoQueryContractParams params) throws Exception {
        ContractInfoQuery query = new ContractInfoQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        params.contractId().ifPresent(contractIdStr -> query.setContractId(ContractId.fromString(contractIdStr)));

        params.queryPayment()
                .ifPresent(
                        queryPaymentStr -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPaymentStr))));

        params.maxQueryPayment()
                .ifPresent(maxQueryPaymentStr ->
                        query.setMaxQueryPayment(Hbar.fromTinybars(Long.parseLong(maxQueryPaymentStr))));

        ContractInfo result = query.execute(client);
        return mapContractInfo(result);
    }

    private static ContractInfoQueryResponse mapContractInfo(ContractInfo result) {
        return new ContractInfoQueryResponse(
                toStringOrNull(result.contractId),
                toStringOrNull(result.accountId),
                emptyToNull(result.contractAccountId),
                toStringOrNull(result.adminKey),
                epochSecondsOrNull(result.expirationTime),
                durationSecondsOrNull(result.autoRenewPeriod),
                toStringOrNull(result.autoRenewAccountId),
                Long.toString(result.storage),
                emptyToNull(result.contractMemo),
                hbarToTinybarsOrNull(result.balance),
                result.isDeleted,
                "0",
                toStringOrNull(result.ledgerId),
                mapStakingInfo(result.stakingInfo));
    }

    private static StakingInfoResponse mapStakingInfo(StakingInfo stakingInfo) {
        if (stakingInfo == null) {
            return null;
        }
        return new StakingInfoResponse(
                stakingInfo.declineStakingReward,
                epochSecondsOrNull(stakingInfo.stakePeriodStart),
                hbarToTinybarsOrNull(stakingInfo.pendingReward),
                hbarToTinybarsOrNull(stakingInfo.stakedToMe),
                toStringOrNull(stakingInfo.stakedAccountId),
                stakingInfo.stakedNodeId != null ? stakingInfo.stakedNodeId.toString() : null);
    }

    private static String toStringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String epochSecondsOrNull(java.time.Instant instant) {
        return instant != null ? Long.toString(instant.getEpochSecond()) : null;
    }

    private static String durationSecondsOrNull(Duration duration) {
        return duration != null ? Long.toString(duration.getSeconds()) : null;
    }

    private static String hbarToTinybarsOrNull(Hbar hbar) {
        return hbar != null ? Long.toString(hbar.toTinybars()) : null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
