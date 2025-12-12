// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.util.Integers;
import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.EthereumTransaction;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PrivateKeyECDSA;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EthereumTransactionIntegrationTest {

    private static final String SMART_CONTRACT_BYTECODE =
        "608060405234801561001057600080fd5b506040516104d73803806104d78339818101604052602081101561003357600080fd5b810190808051604051939291908464010000000082111561005357600080fd5b90830190602082018581111561006857600080fd5b825164010000000081118282018810171561008257600080fd5b82525081516020918201929091019080838360005b838110156100af578181015183820152602001610097565b50505050905090810190601f1680156100dc5780820380516001836020036101000a031916815260200191505b506040525050600080546001600160a01b0319163317905550805161010890600190602084019061010f565b50506101aa565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061015057805160ff191683800117855561017d565b8280016001018555821561017d579182015b828111156102be578251825591602001919060010190610162565b5061018992915061018d565b5090565b6101a791905b808211156101895760008155600101610193565b90565b61031e806101b96000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c8063368b87721461004657806341c0e1b5146100ee578063ce6d41de146100f6575b600080fd5b6100ec6004803603602081101561005c57600080fd5b81019060208101813564010000000081111561007757600080fd5b82018360208201111561008957600080fd5b803590602001918460018302840111640100000000831117156100ab57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610173945050505050565b005b6100ec6101a2565b6100fe6101ba565b6040805160208082528351818301528351919283929083019185019080838360005b83811015610138578181015183820152602001610120565b50505050905090810190601f1680156101655780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000546001600160a01b0316331461018a5761019f565b805161019d906001906020840190610250565b505b50565b6000546001600160a01b03163314156101b85733ff5b565b60018054604080516020601f600260001961010087891615020190951694909404938401819004810282018101909252828152606093909290918301828280156102455780601f1061021a57610100808354040283529160200191610245565b820191906000526020600020905b81548152906001019060200180831161022857829003601f168201915b505050505090505b90565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061029157805160ff19168380011785556102be565b828001600101855582156102be579182015b828111156102be5782518255916020019190600101906102a3565b506102ca9291506102ce565b5090565b61024d91905b808211156102ca57600081556001016102d456fea264697066735822122084964d4c3f6bc912a9d20e14e449721012d625aa3c8a12de41ae5519752fc89064736f6c63430006000033";

    private static final String SMART_CONTRACT_BYTECODE_JUMBO =
        "6080604052348015600e575f5ffd5b506101828061001c5f395ff3fe608060405234801561000f575f5ffd5b5060043610610029575f3560e01c80631e0a3f051461002d575b5f5ffd5b610047600480360381019061004291906100d0565b61005d565b6040516100549190610133565b60405180910390f35b5f5f905092915050565b5f5ffd5b5f5ffd5b5f5ffd5b5f5ffd5b5f5ffd5b5f5f83601f8401126100905761008f61006f565b5b8235905067ffffffffffffffff8111156100ad576100ac610073565b5b6020830191508360018202830111156100c9576100c8610077565b5b9250929050565b5f5f602083850312156100e6576100e5610067565b5b5f83013567ffffffffffffffff8111156101035761010261006b565b5b61010f8582860161007b565b92509250509250929050565b5f819050919050565b61012d8161011b565b82525050565b5f6020820190506101465f830184610124565b9291505056fea26469706673582212202829ebd1cf38c443e4fd3770cd4306ac4c6bb9ac2828074ae2b9cd16121fcfea64736f6c634300081e0033";

    private static final String EMPTY_CONTRACT_BYTECODE =
        "608060405234801561001057600080fd5b5060b88061001f6000396000f3fe6080604052348015600f57600080fd5b506004361060285760003560e01c8063f8a8fd6d14602d575b600080fd5b60336047565b604051603e9190605d565b60405180910390f35b60006001905090565b6057816076565b82525050565b6000602082019050607060008301846050565b92915050565b6000811515905091905056fea2646970667358221220b4a7b9f1eedd2080ba6dc510555bb650f1ab8aa6ee958ba753ad2cd1665559bd64736f6c63430008000033";
    /**
     * @notice E2E-HIP-844
     * @url https://hips.hedera.com/hip/hip-844
     */
    @Test
    @DisplayName("Signer nonce changed on Ethereum transaction")
    void signerNonceChangedOnEthereumTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var privateKey = PrivateKey.generateECDSA();
            var newAccountAliasId = privateKey.toAccountId(0, 0);

            new TransferTransaction()
                .addHbarTransfer(testEnv.operatorId, new Hbar(1).negated())
                .addHbarTransfer(newAccountAliasId, new Hbar(1))
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            var fileCreateTransactionResponse = new FileCreateTransaction()
                .setKeys(testEnv.operatorKey)
                .setContents(SMART_CONTRACT_BYTECODE)
                .execute(testEnv.client);

            var fileId = Objects.requireNonNull(fileCreateTransactionResponse.getReceipt(testEnv.client).fileId);

            var contractCreateTransactionResponse = new ContractCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setGas(400000)
                .setConstructorParameters(new ContractFunctionParameters().addString("Hello from Hedera."))
                .setBytecodeFileId(fileId)
                .setContractMemo("[e2e::ContractCreateTransaction]")
                .execute(testEnv.client);

            var contractId =
                Objects.requireNonNull(contractCreateTransactionResponse.getReceipt(testEnv.client).contractId);

            int nonce = 0;
            byte[] chainId = Hex.decode("012a");
            byte[] maxPriorityGas = Hex.decode("00");
            byte[] maxGas = Hex.decode("d1385c7bf0");
            byte[] to = Hex.decode(contractId.toEvmAddress());
            byte[] callData = new ContractExecuteTransaction()
                .setFunction("setMessage", new ContractFunctionParameters().addString("new message"))
                .getFunctionParameters()
                .toByteArray();

            var sequence = RLPEncoder.sequence(Integers.toBytes(2), new Object[] {
                chainId,
                Integers.toBytes(nonce),
                maxPriorityGas,
                maxGas,
                Integers.toBytes(150000),
                to,
                Integers.toBytesUnsigned(BigInteger.ZERO),
                callData,
                new Object[0]
            });

            byte[] signedBytes = privateKey.sign(sequence);

            // wrap in signature object
            final byte[] r = new byte[32];
            System.arraycopy(signedBytes, 0, r, 0, 32);
            final byte[] s = new byte[32];
            System.arraycopy(signedBytes, 32, s, 0, 32);

            final int recId = ((PrivateKeyECDSA) privateKey).getRecoveryId(r, s, sequence);

            byte[] ethereumData = RLPEncoder.sequence(
                Integers.toBytes(0x02),
                List.of(
                    chainId,
                    Integers.toBytes(nonce), // nonce
                    maxPriorityGas,
                    maxGas,
                    Integers.toBytes(150000), // gasLimit
                    to,
                    Integers.toBytesUnsigned(BigInteger.ZERO), // value
                    callData,
                    List.of(/*accessList*/ ),
                    Integers.toBytes(recId), // recId
                    r,
                    s));

            EthereumTransaction ethereumTransaction = new EthereumTransaction().setEthereumData(ethereumData);
            var ethereumTransactionResponse = ethereumTransaction.execute(testEnv.client);
            var ethereumTransactionRecord = ethereumTransactionResponse.getRecord(testEnv.client);

            assertThat(ethereumTransactionRecord.contractFunctionResult.signerNonce)
                .isEqualTo(1);

            new ContractDeleteTransaction()
                .setTransferAccountId(testEnv.operatorId)
                .setContractId(contractId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            new FileDeleteTransaction()
                .setFileId(fileId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Jumbo Ethereum transaction with large calldata")
    void jumboEthereumTransactionWithLargeCalldata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var privateKey = PrivateKey.generateECDSA();
            var newAccountAliasId = privateKey.toAccountId(0, 0);

            new TransferTransaction()
                .addHbarTransfer(testEnv.operatorId, new Hbar(100).negated())
                .addHbarTransfer(newAccountAliasId, new Hbar(100))
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            var fileCreateTransactionResponse = new FileCreateTransaction()
                .setKeys(testEnv.operatorKey)
                .setContents(SMART_CONTRACT_BYTECODE_JUMBO)
                .execute(testEnv.client);

            var fileId = Objects.requireNonNull(fileCreateTransactionResponse.getReceipt(testEnv.client).fileId);

            var contractCreateTransactionResponse = new ContractCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setGas(300000)
                .setBytecodeFileId(fileId)
                .setContractMemo("[e2e::ContractCreateTransaction]")
                .execute(testEnv.client);

            var contractId =
                Objects.requireNonNull(contractCreateTransactionResponse.getReceipt(testEnv.client).contractId);

            byte[] largeCalldata = new byte[1024 * 120];
            var callData = new ContractExecuteTransaction()
                .setFunction("consumeLargeCalldata", new ContractFunctionParameters().addBytes(largeCalldata))
                .getFunctionParameters()
                .toByteArray();

            int nonce = 0;
            byte[] chainId = Hex.decode("012a");
            byte[] maxPriorityGas = Hex.decode("00");
            byte[] maxGas = Hex.decode("d1385c7bf0");
            byte[] gasLimitBytes = Hex.decode("3567E0");
            byte[] to = Hex.decode(contractId.toEvmAddress());
            byte[] value = Hex.decode("00");

            var sequence = RLPEncoder.sequence(Integers.toBytes(2), new Object[] {
                chainId,
                Integers.toBytes(nonce), // nonce
                maxPriorityGas,
                maxGas,
                gasLimitBytes, // gasLimit
                to,
                Integers.toBytesUnsigned(BigInteger.ZERO), // value
                callData,
                new Object[0]
            });

            byte[] signedBytes = privateKey.sign(sequence);

            // wrap in signature object
            final byte[] r = new byte[32];
            System.arraycopy(signedBytes, 0, r, 0, 32);
            final byte[] s = new byte[32];
            System.arraycopy(signedBytes, 32, s, 0, 32);

            final int recId = ((PrivateKeyECDSA) privateKey).getRecoveryId(r, s, sequence);

            byte[] ethereumData = RLPEncoder.sequence(
                Integers.toBytes(0x02),
                List.of(
                    chainId,
                    Integers.toBytes(nonce), // nonce
                    maxPriorityGas,
                    maxGas,
                    gasLimitBytes, // gasLimit
                    to,
                    Integers.toBytesUnsigned(BigInteger.ZERO), // value
                    callData,
                    List.of(/*accessList*/ ),
                    Integers.toBytes(recId), // recId
                    r,
                    s));

            EthereumTransaction ethereumTransaction = new EthereumTransaction().setEthereumData(ethereumData);
            var ethereumTransactionResponse = ethereumTransaction.execute(testEnv.client);
            var ethereumTransactionRecord = ethereumTransactionResponse.getRecord(testEnv.client);

            assertThat(ethereumTransactionRecord.contractFunctionResult.signerNonce)
                .isEqualTo(1);

            new ContractDeleteTransaction()
                .setTransferAccountId(testEnv.operatorId)
                .setContractId(contractId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            new FileDeleteTransaction()
                .setFileId(fileId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("EIP-7702 Ethereum transaction with authorization list")
    void eip7702EthereumTransactionWithAuthorizationList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var privateKey = PrivateKey.generateECDSA();
            var newAccountAliasId = privateKey.toAccountId(0, 0);

            new TransferTransaction()
                .addHbarTransfer(testEnv.operatorId, new Hbar(10).negated())
                .addHbarTransfer(newAccountAliasId, new Hbar(10))
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            var createdEntities = createContract(testEnv);
            var fileId = createdEntities.fileId;
            var contractId = createdEntities.contractId;

            int nonce = 0;
            byte[] chainId = Hex.decode("012a");
            byte[] maxPriorityGas = Hex.decode("00");
            byte[] maxGas = Hex.decode("d1385c7bf0");
            byte[] gasLimitBytes = Hex.decode("07A120");
            byte[] to = Hex.decode(contractId.toEvmAddress());
            byte[] value = Integers.toBytesUnsigned(BigInteger.ONE);
            byte[] callData = new ContractExecuteTransaction()
                .setFunction("test", new ContractFunctionParameters())
                .getFunctionParameters()
                .toByteArray();

            List<Object> encodedAuthorizationList = createSignedAuthorizationList(privateKey, chainId, to, nonce);

            var sequence = RLPEncoder.sequence(
                Integers.toBytes(4),
                List.of(
                    chainId,
                    Integers.toBytes(nonce),
                    maxPriorityGas,
                    maxGas,
                    gasLimitBytes,
                    to,
                    value,
                    callData,
                    List.of(),
                    encodedAuthorizationList));

            byte[] signedBytes = privateKey.sign(sequence);
            final byte[] r = Arrays.copyOfRange(signedBytes, 0, 32);
            final byte[] s = Arrays.copyOfRange(signedBytes, 32, 64);
            final int recId = ((PrivateKeyECDSA) privateKey).getRecoveryId(r, s, sequence);

            byte[] ethereumData = RLPEncoder.sequence(
                Integers.toBytes(0x04),
                List.of(
                    chainId,
                    Integers.toBytes(nonce),
                    maxPriorityGas,
                    maxGas,
                    gasLimitBytes,
                    to,
                    value,
                    callData,
                    List.of(),
                    encodedAuthorizationList,
                    Integers.toBytes(recId),
                    r,
                    s));

            EthereumTransaction ethereumTransaction = new EthereumTransaction().setEthereumData(ethereumData);
            var ethereumTransactionResponse = ethereumTransaction.execute(testEnv.client);
            var ethereumTransactionRecord = ethereumTransactionResponse.getRecord(testEnv.client);

            assertThat(ethereumTransactionRecord.contractFunctionResult.signerNonce)
                .isEqualTo(1);

            new ContractDeleteTransaction()
                .setTransferAccountId(testEnv.operatorId)
                .setContractId(contractId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            new FileDeleteTransaction()
                .setFileId(fileId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    private ContractCreationResult createContract(IntegrationTestEnv testEnv) throws Exception {
        var fileCreateTransactionResponse = new FileCreateTransaction()
            .setKeys(testEnv.operatorKey)
            .setContents(EMPTY_CONTRACT_BYTECODE)
            .setMaxTransactionFee(new Hbar(2))
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(fileCreateTransactionResponse.getReceipt(testEnv.client).fileId);

        var contractCreateTransactionResponse = new ContractCreateTransaction()
            .setAdminKey(testEnv.operatorKey)
            .setGas(300000)
            .setBytecodeFileId(fileId)
            .setContractMemo("[e2e::ContractCreateTransaction]")
            .execute(testEnv.client);

        var contractId =
            Objects.requireNonNull(contractCreateTransactionResponse.getReceipt(testEnv.client).contractId);

        return new ContractCreationResult(fileId, contractId);
    }

    private List<Object> createSignedAuthorizationList(
        PrivateKey privateKey, byte[] chainId, byte[] to, int nonce) {
        byte[] authorizationNonce = Integers.toBytesUnsigned(BigInteger.valueOf(nonce));
        byte[] authorizationPayload = RLPEncoder.sequence(chainId, to, authorizationNonce);
        byte[] authorizationMessage = new byte[1 + authorizationPayload.length];
        authorizationMessage[0] = 0x05;
        System.arraycopy(authorizationPayload, 0, authorizationMessage, 1, authorizationPayload.length);

        byte[] authorizationSignature = privateKey.sign(authorizationMessage);
        final byte[] authorizationR = Arrays.copyOfRange(authorizationSignature, 0, 32);
        final byte[] authorizationS = Arrays.copyOfRange(authorizationSignature, 32, 64);
        final int authorizationRecId =
            ((PrivateKeyECDSA) privateKey).getRecoveryId(authorizationR, authorizationS, authorizationMessage);

        return List.of(List.of(
            chainId,
            to,
            authorizationNonce,
            Integers.toBytes(authorizationRecId),
            authorizationR,
            authorizationS));
    }

    private static class ContractCreationResult {
        final FileId fileId;
        final ContractId contractId;

        ContractCreationResult(FileId fileId, ContractId contractId) {
            this.fileId = fileId;
            this.contractId = contractId;
        }
    }
}
