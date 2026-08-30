// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractLogInfo;
import java.util.List;
import javax.annotation.Nullable;

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
