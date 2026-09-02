// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent response for account related transactions.
 *
 * @param accountId the ID of the account
 * @param status the status of the submitted transaction
 * @param transactionId the ID of the transaction
 */
public record AccountResponse(String accountId, Status status, String transactionId) {}
