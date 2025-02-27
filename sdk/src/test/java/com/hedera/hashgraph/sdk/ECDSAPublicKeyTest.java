// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ECDSAPublicKeyTest {
    @Test
    void verifyTransaction() {
        var transaction = new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 3)))
                .setTransactionId(TransactionId.generate(new AccountId(0, 0, 4)))
                .freeze();

        var key = PrivateKey.fromStringECDSA("8776c6b831a1b61ac10dac0304a2843de4716f54b1919bb91a2685d0fe3f3048");
        key.signTransaction(transaction);

        assertThat(key.getPublicKey().verifyTransaction(transaction)).isTrue();
    }

    @Test
    @DisplayName("public key can be recovered from bytes")
    void keyByteSerialization() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        byte[] key1Bytes = key1.toBytes();
        PublicKey key2 = PublicKey.fromBytes(key1Bytes);
        byte[] key2Bytes = key2.toBytes();

        assertThat(key2Bytes).containsExactly(key1Bytes);
    }

    @Test
    @DisplayName("public key can be recovered from raw bytes")
    void keyByteSerialization2() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        byte[] key1Bytes = key1.toBytesRaw();
        PublicKey key2 = PublicKey.fromBytesECDSA(key1Bytes);
        byte[] key2Bytes = key2.toBytesRaw();
        // cannot use PrivateKey.fromBytes() to parse raw ECDSA bytes
        // because they're indistinguishable from ED25519 raw bytes

        assertThat(key2Bytes).containsExactly(key1Bytes);
    }

    @Test
    void keyByteValidation() {
        byte[] invalidKeyECDSA = new byte[33];
        Assertions.assertDoesNotThrow(() -> PublicKey.fromBytes(invalidKeyECDSA));
        Assertions.assertDoesNotThrow(() -> PublicKey.fromBytesECDSA(invalidKeyECDSA));

        byte[] invalidCompressedKey = new byte[] {
            0x00,
            (byte) 0xca,
            (byte) 0x35,
            0x4b,
            0x7c,
            (byte) 0xf4,
            (byte) 0x87,
            (byte) 0xd1,
            (byte) 0xbc,
            0x43,
            0x5a,
            0x25,
            0x66,
            0x77,
            0x09,
            (byte) 0xc1,
            (byte) 0xab,
            (byte) 0x98,
            0x0c,
            0x11,
            0x4d,
            0x35,
            (byte) 0x94,
            (byte) 0xe6,
            0x25,
            (byte) 0x9e,
            (byte) 0x81,
            0x2e,
            0x6a,
            0x70,
            0x3d,
            0x4f,
            0x51
        };
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> PublicKey.fromBytesECDSA(invalidCompressedKey));

        byte[] malformedKey = new byte[] {0x00, 0x01, 0x02};
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> PublicKey.fromBytesECDSA(malformedKey));

        byte[] validCompressedKey = new byte[] {
            0x02, // Prefix for compressed keys
            (byte) 0xca,
            (byte) 0x35,
            0x4b,
            0x7c,
            (byte) 0xf4,
            (byte) 0x87,
            (byte) 0xd1,
            (byte) 0xbc,
            0x43,
            0x5a,
            0x25,
            0x66,
            0x77,
            0x09,
            (byte) 0xc1,
            (byte) 0xab,
            (byte) 0x98,
            0x0c,
            0x1f,
            0x4d,
            0x35,
            (byte) 0x94,
            (byte) 0xe6,
            0x25,
            (byte) 0x9e,
            (byte) 0x81,
            0x2e,
            0x6a,
            0x75,
            0x3d,
            0x4f,
            0x59
        };
        Assertions.assertDoesNotThrow(() -> PublicKey.fromBytesECDSA(validCompressedKey));

        byte[] validDERKey = PrivateKey.generateECDSA().getPublicKey().toBytesDER();
        Assertions.assertDoesNotThrow(() -> PublicKey.fromBytesECDSA(validDERKey));
    }

    @Test
    @DisplayName("public key can be recovered from DER bytes")
    void keyByteSerialization3() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        byte[] key1Bytes = key1.toBytesDER();
        PublicKey key2 = PublicKey.fromBytesDER(key1Bytes);
        byte[] key2Bytes = key2.toBytesDER();
        PublicKey key3 = PublicKey.fromBytes(key1Bytes);
        byte[] key3Bytes = key3.toBytesDER();

        assertThat(key2Bytes).containsExactly(key1Bytes);
        assertThat(key3Bytes).isEqualTo(key1Bytes);
    }

    @Test
    @DisplayName("public key can be recovered after transaction serialization")
    void keyByteSerializationThroughTransaction() {
        var senderAccount = AccountId.fromString("0.0.1337");
        var receiverAccount = AccountId.fromString("0.0.3");
        var transferAmount = Hbar.from(new BigDecimal("0.0001"), HbarUnit.HBAR);
        var privateKey = PrivateKey.generateECDSA();
        var client = Client.forTestnet().setOperator(senderAccount, privateKey);
        var tx = new TransferTransaction()
                .addHbarTransfer(senderAccount, transferAmount.negated())
                .addHbarTransfer(receiverAccount, transferAmount);

        tx.freezeWith(client);
        tx.signWithOperator(client);

        var bytes = tx.toBytes();

        assertThatNoException().isThrownBy(() -> Transaction.fromBytes(bytes));
        assertThat(tx.getSignatures()).isNotEmpty();
    }

    @Test
    @DisplayName("public key can be recovered from string")
    void keyStringSerialization() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        String key1Str = key1.toString();
        PublicKey key2 = PublicKey.fromString(key1Str);
        String key2Str = key2.toString();
        PublicKey key3 = PublicKey.fromString(key1Str);
        String key3Str = key3.toString();

        assertThat(key3.getClass()).isEqualTo(PublicKeyECDSA.class);
        assertThat(key2Str).isEqualTo(key1Str);
        assertThat(key3Str).isEqualTo(key1Str);
    }

    @Test
    @DisplayName("public key can be recovered from raw string")
    void keyStringSerialization2() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        String key1Str = key1.toStringRaw();
        PublicKey key2 = PublicKey.fromStringECDSA(key1Str);
        String key2Str = key2.toStringRaw();
        PublicKey key3 = PublicKey.fromStringECDSA(key2Str);
        String key3Str = key3.toStringRaw();
        // cannot use PublicKey.fromString() to parse raw ECDSA string
        // because it's indistinguishable from ED25519 raw bytes

        assertThat(key3.getClass()).isEqualTo(PublicKeyECDSA.class);
        assertThat(key2Str).isEqualTo(key1Str);
        assertThat(key3Str).isEqualTo(key1Str);
    }

    @Test
    @DisplayName("public key can be recovered from DER string")
    void keyStringSerialization3() {
        PublicKey key1 = PrivateKey.generateECDSA().getPublicKey();
        String key1Str = key1.toStringDER();
        PublicKey key2 = PublicKey.fromStringDER(key1Str);
        String key2Str = key2.toStringDER();
        PublicKey key3 = PublicKey.fromString(key1Str);
        String key3Str = key3.toStringDER();

        assertThat(key3.getClass()).isEqualTo(PublicKeyECDSA.class);
        assertThat(key2Str).isEqualTo(key1Str);
        assertThat(key3Str).isEqualTo(key1Str);
    }

    @Test
    @DisplayName("public key is is ECDSA")
    void keyIsECDSA() {
        PublicKey key = PrivateKey.generateECDSA().getPublicKey();

        assertThat(key.isECDSA()).isTrue();
    }

    @Test
    @DisplayName("public key is is not Ed25519")
    void keyIsNotEd25519() {
        PublicKey key = PrivateKey.generateECDSA().getPublicKey();

        assertThat(key.isED25519()).isFalse();
    }

    @Test
    @DisplayName("to EVM address")
    void toEvmAddress() {
        // Generated by https://www.rfctools.com/ethereum-address-test-tool/
        String privateKeyString = "DEBAE3CA62AB3157110DBA79C8DE26540DC320EE9BE73A77D70BA175643A3500";
        String expectedEvmAddress = "d8eb8db03c699faa3f47adcdcd2ae91773b10f8b";

        PrivateKey privateKey = PrivateKey.fromStringECDSA(privateKeyString);
        PublicKey key = privateKey.getPublicKey();

        assertThat(key.toEvmAddress()).hasToString(expectedEvmAddress);
    }

    @Test
    @DisplayName("DER import test vectors")
    void DERImportTestVectors() {
        // https://github.com/hashgraph/hedera-sdk-reference/issues/93#issue-1665972122
        var PUBLIC_KEY_DER1 =
                "302d300706052b8104000a032200028173079d2e996ef6b2d064fc82d5fc7094367211e28422bec50a2f75c365f5fd";
        var PUBLIC_KEY1 = "028173079d2e996ef6b2d064fc82d5fc7094367211e28422bec50a2f75c365f5fd";

        var PUBLIC_KEY_DER2 =
                "3036301006072a8648ce3d020106052b8104000a032200036843f5cb338bbb4cdb21b0da4ea739d910951d6e8a5f703d313efe31afe788f4";
        var PUBLIC_KEY2 = "036843f5cb338bbb4cdb21b0da4ea739d910951d6e8a5f703d313efe31afe788f4";

        var PUBLIC_KEY_DER3 =
                "3056301006072a8648ce3d020106052b8104000a03420004aaac1c3ac1bea0245b8e00ce1e2018f9eab61b6331fbef7266f2287750a6597795f855ddcad2377e22259d1fcb4e0f1d35e8f2056300c15070bcbfce3759cc9d";
        var PUBLIC_KEY3 = "03aaac1c3ac1bea0245b8e00ce1e2018f9eab61b6331fbef7266f2287750a65977";

        var ecdsaPublicKey1 = PublicKey.fromStringDER(PUBLIC_KEY_DER1);
        assertThat(ecdsaPublicKey1.toStringRaw()).isEqualTo(PUBLIC_KEY1);

        var ecdsaPublicKey2 = PublicKey.fromStringDER(PUBLIC_KEY_DER2);
        assertThat(ecdsaPublicKey2.toStringRaw()).isEqualTo(PUBLIC_KEY2);

        var ecdsaPublicKey3 = PublicKey.fromStringDER(PUBLIC_KEY_DER3);
        assertThat(ecdsaPublicKey3.toStringRaw()).isEqualTo(PUBLIC_KEY3);
    }
}
