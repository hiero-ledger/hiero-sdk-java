// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractLogInfo;
import com.hedera.hashgraph.sdk.Hbar;
import java.util.List;
import javax.annotation.Nullable;

public record ContractCallResponse(
        String contractId,
        @Nullable ContractId evmAddress,
        @Nullable String errorMessage,
        long gasUsed,
        List<ContractLogInfo> logs,
        long gas,
        Hbar hbarAmount,
        @Nullable AccountId senderAccountId,
        long signerNonce,
        String rawResult) {}
