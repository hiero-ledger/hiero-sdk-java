// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractLogInfo;
import com.hedera.hashgraph.sdk.Hbar;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class ContractCallResponse {

    /**
     * The ID of the contract that was invoked.
     */
    public final String contractId;

    /**
     * The contract's 20-byte EVM address
     */
    @Nullable
    public final ContractId evmAddress;

    /**
     * message in case there was an error during smart contract execution
     */
    @Nullable
    public final String errorMessage;

    /**
     * units of gas used to execute contract
     */
    public final long gasUsed;

    /**
     * the log info for events returned by the function
     */
    public final List<ContractLogInfo> logs;

    /**
     * The amount of gas available for the call, aka the gasLimit
     */
    public final long gas;

    /**
     * Number of tinybars sent (the function must be payable if this is nonzero).
     */
    public final Hbar hbarAmount;

    /**
     * The account that is the "sender." If not present it is the accountId from the transactionId.
     */
    @Nullable
    public final AccountId senderAccountId;

    /**
     * If not null this field specifies what the value of the signer account nonce is post transaction execution.
     * For transactions that don't update the signer nonce (like HAPI ContractCall and ContractCreate transactions) this field should be null.
     */
    public final long signerNonce;

    private final String rawResult;
}
