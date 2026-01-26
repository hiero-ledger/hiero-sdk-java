// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.CreateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.DeleteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.ExecuteContractParams;
import com.hedera.hashgraph.tck.methods.sdk.param.contract.UpdateContractParams;
import com.hedera.hashgraph.tck.methods.sdk.response.ContractResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;
import org.bouncycastle.util.encoders.Hex;

@JSONRPC2Service
public class ContractService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public ContractService(SdkService sdkService) {
        this.sdkService = sdkService;
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
}
