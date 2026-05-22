// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import lombok.Data;

@Data
public class AccountResponse {
    private final String accountId;
    private final Status status;
    private final String transactionId;
}
