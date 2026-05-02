// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * An Exception thrown on error status by {@link TransactionId#getReceipt(Client)}.
 * <p>
 * The receipt is included, though only the {@link TransactionReceipt#status} field will be
 * initialized; all the getters should throw.
 */
@Deprecated
public class HederaReceiptStatusException extends ReceiptStatusException {
    HederaReceiptStatusException(TransactionId transactionId, TransactionReceipt receipt) {
        super(transactionId, receipt);
    }
}
