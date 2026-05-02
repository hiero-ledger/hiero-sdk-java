// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.Client;
import org.hiero.sdk.ContractCreateFlow;
import org.hiero.sdk.ContractExecuteTransaction;
import org.hiero.sdk.ContractFunctionParameters;
import org.hiero.sdk.ContractFunctionResult;
import org.hiero.sdk.ContractId;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TransactionId;
import org.hiero.sdk.TransactionRecord;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/*
ContractHelper declutters PrecompileExample.java

When we instantiate a ContractHelper, we provide it with the JSON of a compiled solidity contract
which is assumed to have functions named "step0()" through "stepN()".

Each of these step functions is assumed to take no function parameters, and to return a Hedera ResponseCode
which ought to be SUCCESS -- in other words, an int32 with value 22.
See resources/org/hiero/sdk/java/examples/contracts/precompile/HederaResponseCodes.sol

If a step takes function parameters, or if its ContractFunctionResult should be validated with a different method,
the user can specify a supplier for a particular step with setParameterSupplier(stepIndex, parametersSupplier),
and can specify an alternative validation method with setResultValidator(stepIndex, validateFunction)

The contract is created on the Hedera network in the ContractHelper constructor, and when the user is ready to
execute the step functions in the contract, they should call executeSteps(firstStepToExecute, lastStepToExecute).
 */

public class ContractHelper {
    final ContractId contractId;
    final Map<Integer, Function<ContractFunctionResult, Boolean>> stepResultValidators = new HashMap<>();
    final Map<Integer, Supplier<ContractFunctionParameters>> stepParameterSuppliers = new HashMap<>();
    final Map<Integer, Hbar> stepPayableAmounts = new HashMap<>();
    final Map<Integer, List<PrivateKey>> stepSigners = new HashMap<>();
    final Map<Integer, AccountId> stepFeePayers = new HashMap<>();
    final Map<Integer, Consumer<String>> stepLogic = new HashMap<>();

    public static String getBytecodeHex(String filename) throws IOException {
        try (Reader reader = new InputStreamReader(
                Optional.ofNullable(ContractHelper.class.getResourceAsStream(filename))
                        .orElseThrow(() -> new RuntimeException("Failed to find: " + filename)),
                StandardCharsets.UTF_8)) {

            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            JsonElement bytecodeElement = Optional.ofNullable(
                            json.has("object") ? json.get("object") : json.get("bytecode"))
                    .orElseThrow(() -> new RuntimeException("No bytecode or object found in json."));

            if (bytecodeElement.isJsonObject()) {
                bytecodeElement = bytecodeElement.getAsJsonObject().get("object");
            }
            return bytecodeElement.getAsString();
        }
    }

    public ContractHelper(String filename, ContractFunctionParameters constructorParameters, Client client)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        contractId = Objects.requireNonNull(new ContractCreateFlow()
                .setBytecode(getBytecodeHex(filename))
                .setMaxChunks(30)
                .setGas(8_000_000)
                .setConstructorParameters(constructorParameters)
                .execute(client)
                .getReceipt(client)
                .contractId);
    }

    public ContractHelper setResultValidatorForStep(
            int stepIndex, Function<ContractFunctionResult, Boolean> validator) {
        stepResultValidators.put(stepIndex, validator);
        return this;
    }

    public ContractHelper setParameterSupplierForStep(int stepIndex, Supplier<ContractFunctionParameters> supplier) {
        stepParameterSuppliers.put(stepIndex, supplier);
        return this;
    }

    public ContractHelper setPayableAmountForStep(int stepIndex, Hbar amount) {
        stepPayableAmounts.put(stepIndex, amount);
        return this;
    }

    public ContractHelper addSignerForStep(int stepIndex, PrivateKey signer) {
        if (stepSigners.containsKey(stepIndex)) {
            stepSigners.get(stepIndex).add(signer);
        } else {
            List<PrivateKey> signerList = new ArrayList<>(1);
            signerList.add(signer);
            stepSigners.put(stepIndex, signerList);
        }
        return this;
    }

    public ContractHelper setFeePayerForStep(int stepIndex, AccountId feePayerAccount, PrivateKey feePayerKey) {
        stepFeePayers.put(stepIndex, feePayerAccount);
        return addSignerForStep(stepIndex, feePayerKey);
    }

    public ContractHelper setStepLogic(int stepIndex, Consumer<String> stepLogic) {
        this.stepLogic.put(stepIndex, stepLogic);
        return this;
    }

    private Function<ContractFunctionResult, Boolean> getResultValidator(int stepIndex) {
        return stepResultValidators.getOrDefault(
                stepIndex,
                // if no custom validator is given, assume that the step returns a response code which ought to be
                // SUCCESS
                contractFunctionResult -> {
                    Status responseStatus = Status.fromResponseCode(contractFunctionResult.getInt32(0));
                    boolean isValid = responseStatus == Status.SUCCESS;
                    if (!isValid) {
                        System.out.println("Encountered invalid response status " + responseStatus);
                    }
                    return isValid;
                });
    }

    private Supplier<ContractFunctionParameters> getParameterSupplier(int stepIndex) {
        return stepParameterSuppliers.getOrDefault(stepIndex, () -> null);
    }

    private Hbar getPayableAmount(int stepIndex) {
        return stepPayableAmounts.get(stepIndex);
    }

    private List<PrivateKey> getSigners(int stepIndex) {
        return stepSigners.getOrDefault(stepIndex, Collections.emptyList());
    }

    public ContractHelper executeSteps(int firstStepToExecute, int lastStepToExecute, Client client) throws Exception {
        for (int stepIndex = firstStepToExecute; stepIndex <= lastStepToExecute; stepIndex++) {
            System.out.println("Attempting to execute step " + stepIndex);
            ContractExecuteTransaction tx =
                    new ContractExecuteTransaction().setContractId(contractId).setGas(10_000_000);

            Hbar payableAmount = getPayableAmount(stepIndex);
            if (payableAmount != null) {
                tx.setPayableAmount(payableAmount);
            }

            String functionName = "step" + stepIndex;
            ContractFunctionParameters parameters =
                    getParameterSupplier(stepIndex).get();
            if (parameters != null) {
                tx.setFunction(functionName, parameters);
            } else {
                tx.setFunction(functionName);
            }

            AccountId feePayerAccountId = stepFeePayers.get(stepIndex);
            if (feePayerAccountId != null) {
                tx.setTransactionId(TransactionId.generate(feePayerAccountId));
            }

            tx.freezeWith(client);
            for (PrivateKey signer : getSigners(stepIndex)) {
                tx.sign(signer);
            }

            TransactionRecord record =
                    tx.execute(client).setValidateStatus(false).getRecord(client);

            try {
                if (record.receipt.status != Status.SUCCESS) {
                    throw new Exception(
                            "transaction receipt yielded unsuccessful response code " + record.receipt.status);
                }

                ContractFunctionResult functionResult = Objects.requireNonNull(record.contractFunctionResult);
                System.out.println("gas used: " + functionResult.gasUsed);

                var currentStepLogic = stepLogic.get(stepIndex);
                if (currentStepLogic != null) {
                    currentStepLogic.accept(functionResult.getAddress(1));
                }

                if (getResultValidator(stepIndex).apply(functionResult)) {
                    System.out.println("step " + stepIndex + " completed, and returned valid result. (TransactionId \""
                            + record.transactionId + "\")");
                } else {
                    throw new Exception("returned invalid result");
                }
            } catch (Throwable error) {
                throw new Exception("Error occurred in step " + stepIndex + ": " + error.getMessage() + "\n"
                        + "Transaction record: " + record);
            }

            // otherwise will meet local-node throttle
            Thread.sleep(500L);
        }
        return this;
    }
}

