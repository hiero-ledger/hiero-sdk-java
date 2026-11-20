// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

final class ClprTestSupport {
    static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    static final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);
    static final List<AccountId> NODE_ACCOUNT_IDS =
            List.of(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006"));
    static final TransactionId TRANSACTION_ID =
            TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START);

    private ClprTestSupport() {}

    static byte[] bytes32(int fill) {
        var bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return bytes;
    }

    static <T extends Transaction<T>> T prepare(T transaction) {
        return transaction
                .setNodeAccountIds(NODE_ACCOUNT_IDS)
                .setTransactionId(TRANSACTION_ID)
                .setMaxTransactionFee(new Hbar(1));
    }
}
