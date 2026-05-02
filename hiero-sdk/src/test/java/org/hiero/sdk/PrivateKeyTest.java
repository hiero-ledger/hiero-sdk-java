// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class PrivateKeyTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @Test
    void signTransactionWorks() throws InvalidProtocolBufferException {
        byte[] bytes = new AccountCreateTransaction()
                .setNodeAccountIds(Collections.singletonList(AccountId.fromString("0.0.5005")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setKeyWithoutAlias(unusedPrivateKey)
                .setInitialBalance(Hbar.fromTinybars(450))
                .setProxyAccountId(AccountId.fromString("0.0.1001"))
                .setReceiverSignatureRequired(true)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .toBytes();

        AccountCreateTransaction transaction = (AccountCreateTransaction) Transaction.fromBytes(bytes);
        unusedPrivateKey.signTransaction(transaction);
    }

    @Test
    void ecdsa() {
        var message = "hello world".getBytes(StandardCharsets.UTF_8);
        var key = PrivateKey.fromStringECDSA("8776c6b831a1b61ac10dac0304a2843de4716f54b1919bb91a2685d0fe3f3048");
        var signature = key.sign(message);

        assertThat(Hex.toHexString(signature))
                .isEqualTo(
                        "f3a13a555f1f8cd6532716b8f388bd4e9d8ed0b252743e923114c0c6cbfe414cf791c8e859afd3c12009ecf2cb20dacf01636d80823bcdbd9ec1ce59afe008f0");
    }

    @Test
    void supports0xPrefix() {
        PrivateKey.fromString("0x8776c6b831a1b61ac10dac0304a2843de4716f54b1919bb91a2685d0fe3f3048");
    }
}

