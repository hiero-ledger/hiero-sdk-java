// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import lombok.Data;

@Data
public class EthereumTransactionResponse {
    private final String status;
    private final String contractId;
}
