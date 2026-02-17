// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ContractCallQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.CreateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.DeleteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ExecuteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.InfoQueryContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.UpdateContractParams;
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

    @JSONRPC2Method("contractCallQuery")
    public ContractCallResponse contractCallQuery(final ContractCallQueryParams params) throws Exception {
        ContractCallQuery query = QueryBuilders.buildContractCall(params);
        Client client = sdkService.getClient(params.getSessionId());

        ContractFunctionResult result = query.execute(client);

        return new ContractCallResponse(
                result.contractId.toString(),
                result.evmAddress,
                result.errorMessage,
                result.gasUsed,
                result.logs,
                result.gas,
                result.hbarAmount,
                result.senderAccountId,
                result.signerNonce,
                Hex.toHexString(result.asBytes()));
    }

    @JSONRPC2Method("createContract")
    public ContractResponse createContract(final CreateContractParams params) throws Exception {
        ContractCreateTransaction transaction = new ContractCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.getSessionId());

        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.getAutoRenewPeriod()
                .ifPresent(periodStr -> transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(periodStr))));

        params.getGas().ifPresent(gasStr -> transaction.setGas(Long.parseLong(gasStr)));

        params.getAutoRenewAccountId()
                .ifPresent(accountIdStr -> transaction.setAutoRenewAccountId(AccountId.fromString(accountIdStr)));

        params.getInitialBalance()
                .ifPresent(balanceStr -> transaction.setInitialBalance(Hbar.fromTinybars(Long.parseLong(balanceStr))));

        params.getInitcode().ifPresent(hex -> transaction.setBytecode(Hex.decode(hex)));

        params.getBytecodeFileId().ifPresent(fileIdStr -> transaction.setBytecodeFileId(FileId.fromString(fileIdStr)));

        params.getStakedAccountId()
                .ifPresent(accountIdStr -> transaction.setStakedAccountId(AccountId.fromString(accountIdStr)));

        params.getStakedNodeId().ifPresent(nodeIdStr -> transaction.setStakedNodeId(Long.parseLong(nodeIdStr)));

        params.getDeclineStakingReward().ifPresent(transaction::setDeclineStakingReward);

        params.getMemo().ifPresent(transaction::setContractMemo);

        params.getMaxAutomaticTokenAssociations()
                .ifPresent(maxAuto -> transaction.setMaxAutomaticTokenAssociations(maxAuto.intValue()));

        params.getConstructorParameters().ifPresent(hex -> transaction.setConstructorParameters(Hex.decode(hex)));

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

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
        Client client = sdkService.getClient(params.getSessionId());

        if (params.getContractId() != null) {
            transaction.setContractId(ContractId.fromString(params.getContractId()));
        }

        params.getGas().ifPresent(gasStr -> transaction.setGas(Long.parseLong(gasStr)));

        params.getAmount()
                .ifPresent(amountStr -> transaction.setPayableAmount(Hbar.fromTinybars(Long.parseLong(amountStr))));

        params.getFunctionParameters()
                .ifPresent(hex -> transaction.setFunctionParameters(ByteString.copyFrom(Hex.decode(hex))));

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse("", receipt.status);
    }

    @JSONRPC2Method("updateContract")
    public ContractResponse updateContract(final UpdateContractParams params) throws Exception {
        ContractUpdateTransaction transaction = new ContractUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.getSessionId());

        params.getContractId()
                .ifPresent(contractIdStr -> transaction.setContractId(ContractId.fromString(contractIdStr)));

        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.getAutoRenewPeriod()
                .ifPresent(periodStr -> transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(periodStr))));

        params.getAutoRenewAccountId()
                .ifPresent(accountIdStr -> transaction.setAutoRenewAccountId(AccountId.fromString(accountIdStr)));

        params.getStakedAccountId()
                .ifPresent(accountIdStr -> transaction.setStakedAccountId(AccountId.fromString(accountIdStr)));

        params.getStakedNodeId().ifPresent(nodeIdStr -> transaction.setStakedNodeId(Long.parseLong(nodeIdStr)));

        params.getDeclineStakingReward().ifPresent(transaction::setDeclineStakingReward);

        params.getMemo().ifPresent(transaction::setContractMemo);

        params.getMaxAutomaticTokenAssociations()
                .ifPresent(maxAuto -> transaction.setMaxAutomaticTokenAssociations(maxAuto.intValue()));

        params.getExpirationTime().ifPresent(expirationTimeStr -> {
            try {
                long expirationTimeSeconds = Long.parseLong(expirationTimeStr);
                transaction.setExpirationTime(Duration.ofSeconds(expirationTimeSeconds));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid expiration time: " + expirationTimeStr, e);
            }
        });

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse(null, receipt.status);
    }

    @JSONRPC2Method("deleteContract")
    public ContractResponse deleteContract(final DeleteContractParams params) throws Exception {
        ContractDeleteTransaction transaction = new ContractDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.getSessionId());

        params.getContractId()
                .ifPresent(contractIdStr -> transaction.setContractId(ContractId.fromString(contractIdStr)));

        if (params.getTransferAccountId().isPresent()
                && params.getTransferContractId().isPresent()) {
            transaction.setTransferAccountId(
                    AccountId.fromString(params.getTransferAccountId().get()));
        } else {
            params.getTransferContractId()
                    .ifPresent(transferContractIdStr ->
                            transaction.setTransferContractId(ContractId.fromString(transferContractIdStr)));

            params.getTransferAccountId()
                    .ifPresent(transferAccountIdStr ->
                            transaction.setTransferAccountId(AccountId.fromString(transferAccountIdStr)));
        }

        params.getPermanentRemoval().ifPresent(transaction::setPermanentRemoval);

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(transaction, client));

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);

        return new ContractResponse(null, receipt.status);
    }

    @JSONRPC2Method("contractInfoQuery")
    public ContractInfoQueryResponse contractInfoQuery(final InfoQueryContractParams params) throws Exception {
        ContractInfoQuery query = new ContractInfoQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.getSessionId());

        params.getContractId().ifPresent(contractIdStr -> query.setContractId(ContractId.fromString(contractIdStr)));

        params.getQueryPayment()
                .ifPresent(
                        queryPaymentStr -> query.setQueryPayment(Hbar.fromTinybars(Long.parseLong(queryPaymentStr))));

        params.getMaxQueryPayment()
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
