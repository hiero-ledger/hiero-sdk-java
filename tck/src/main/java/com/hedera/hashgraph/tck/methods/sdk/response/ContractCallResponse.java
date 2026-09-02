// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractLogInfo;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represent contractCall response.
 *
 * @param contractId the ID of the contract that was invoked
 * @param evmAddress the contract's 20-byte EVM address
 * @param errorMessage the message in case there was an error during smart contract execution
 * @param gasUsed the units of gas used to execute contract
 * @param logs the log info for events returned by the function
 * @param gas the amount of gas available for the call, aka the gasLimit
 * @param hbarAmount the Number of tinybars sent (the function must be payable if this is nonzero)
 * @param senderAccountId the account that is the "sender" If not present it is the accountId from the transactionId
 * @param signerNonce if not null this field specifies what the value of the signer account nonce is post transaction execution
 * @param rawResult the raw return data on the function call
 */
public record ContractCallResponse(
        String contractId,
        @Nullable ContractId evmAddress,
        @Nullable String errorMessage,
        long gasUsed,
        List<ContractLogInfo> logs,
        long gas,
        String hbarAmount,
        @Nullable String senderAccountId,
        long signerNonce,
        String rawResult) {}
