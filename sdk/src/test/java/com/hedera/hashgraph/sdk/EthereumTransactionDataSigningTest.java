// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class EthereumTransactionDataSigningTest {

    private static final byte[] TO = Hex.decode("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
    private static final byte[] CALL_DATA = Hex.decode("123456");

    @Test
    public void legacySignRoundTrip() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataLegacy()
                .setNonce(1)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(EthereumTransactionData.ethereumBodyIsSigned(tx)).isTrue();
        // legacy v is 27 or 28
        assertThat(tx.getV()).isIn(27L, 28L);

        var decoded = (EthereumTransactionDataLegacy) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getNonce()).isEqualTo(1L);
        assertThat(decoded.getGasPrice()).isEqualTo(47L);
        assertThat(decoded.getGasLimit()).isEqualTo(98304L);
        assertThat(Hex.toHexString(decoded.getTo())).isEqualTo(Hex.toHexString(TO));
        assertThat(decoded.getValue()).isEqualTo(new BigInteger("1000000000000000000"));
        assertThat(Hex.toHexString(decoded.getCallData())).isEqualTo(Hex.toHexString(CALL_DATA));
        assertThat(Hex.toHexString(decoded.getR())).isEqualTo(Hex.toHexString(tx.getR()));
        assertThat(Hex.toHexString(decoded.getS())).isEqualTo(Hex.toHexString(tx.getS()));
        assertThat(Hex.toHexString(signed)).isEqualTo(Hex.toHexString(decoded.toBytes()));
    }

    @Test
    public void eip2930SignRoundTrip() {
        var key = PrivateKey.generateECDSA();
        var item = new AccessListItem()
                .setAddress(TO)
                .addStorageKey(Hex.decode("0000000000000000000000000000000000000000000000000000000000000001"));
        var tx = new EthereumTransactionDataEip2930()
                .setChainId(298)
                .setNonce(2)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA)
                .addAccessListItem(item);

        byte[] signed = tx.sign(key);

        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(EthereumTransactionData.ethereumBodyIsSigned(tx)).isTrue();

        var decoded = (EthereumTransactionDataEip2930) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getChainId()).isEqualTo(298L);
        assertThat(decoded.getNonce()).isEqualTo(2L);
        assertThat(decoded.getGasPrice()).isEqualTo(47L);
        assertThat(decoded.getGasLimit()).isEqualTo(98304L);
        assertThat(Hex.toHexString(decoded.getTo())).isEqualTo(Hex.toHexString(TO));
        assertThat(decoded.getValue()).isEqualTo(new BigInteger("1000000000000000000"));
        assertThat(Hex.toHexString(decoded.getCallData())).isEqualTo(Hex.toHexString(CALL_DATA));
        assertThat(decoded.getAccessListItems()).hasSize(1);
        assertThat(Hex.toHexString(decoded.getR())).isEqualTo(Hex.toHexString(tx.getR()));
        assertThat(Hex.toHexString(decoded.getS())).isEqualTo(Hex.toHexString(tx.getS()));
        assertThat(Hex.toHexString(decoded.getRecoveryIdBytes())).isEqualTo(Hex.toHexString(tx.getRecoveryIdBytes()));
        assertThat(Hex.toHexString(signed)).isEqualTo(Hex.toHexString(decoded.toBytes()));
    }

    @Test
    public void eip1559SignRoundTrip() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(2)
                .setMaxPriorityGas(47)
                .setMaxGas(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(EthereumTransactionData.ethereumBodyIsSigned(tx)).isTrue();

        var decoded = (EthereumTransactionDataEip1559) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getChainId()).isEqualTo(298L);
        assertThat(decoded.getNonce()).isEqualTo(2L);
        assertThat(decoded.getMaxPriorityGas()).isEqualTo(47L);
        assertThat(decoded.getMaxGas()).isEqualTo(47L);
        assertThat(decoded.getGasLimit()).isEqualTo(98304L);
        assertThat(Hex.toHexString(decoded.getTo())).isEqualTo(Hex.toHexString(TO));
        assertThat(decoded.getValue()).isEqualTo(new BigInteger("1000000000000000000"));
        assertThat(Hex.toHexString(decoded.getCallData())).isEqualTo(Hex.toHexString(CALL_DATA));
        assertThat(decoded.getAccessListItems()).isEmpty();
        assertThat(Hex.toHexString(decoded.getR())).isEqualTo(Hex.toHexString(tx.getR()));
        assertThat(Hex.toHexString(decoded.getS())).isEqualTo(Hex.toHexString(tx.getS()));
        assertThat(Hex.toHexString(decoded.getRecoveryIdBytes())).isEqualTo(Hex.toHexString(tx.getRecoveryIdBytes()));
        assertThat(Hex.toHexString(signed)).isEqualTo(Hex.toHexString(decoded.toBytes()));
    }

    @Test
    public void legacyToBytesHasNoTypePrefix() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataLegacy()
                .setNonce(1)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        assertThat(tx.v).isNotEmpty();
        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(tx.getV()).isIn(27L, 28L);
        assertThat(signed[0] & 0xff).isGreaterThanOrEqualTo(0xc0);
        assertThat(EthereumTransactionData.fromBytes(signed)).isInstanceOf(EthereumTransactionDataLegacy.class);
    }

    @Test
    public void eip2930ToBytesPrefixedWith01() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip2930()
                .setChainId(298)
                .setNonce(2)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        assertThat(tx.recoveryId).isNotNull();
        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(signed[0]).isEqualTo((byte) 0x01);
        assertThat(EthereumTransactionData.fromBytes(signed)).isInstanceOf(EthereumTransactionDataEip2930.class);
    }

    @Test
    public void eip1559ToBytesPrefixedWith02() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(2)
                .setMaxPriorityGas(47)
                .setMaxGas(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        assertThat(tx.recoveryId).isNotNull();
        assertThat(tx.r).isNotEmpty();
        assertThat(tx.s).isNotEmpty();
        assertThat(signed[0]).isEqualTo((byte) 0x02);
        assertThat(EthereumTransactionData.fromBytes(signed)).isInstanceOf(EthereumTransactionDataEip1559.class);
    }

    @Test
    public void eip1559EmptyAccessListRoundTripsAsEmpty() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(2)
                .setMaxPriorityGas(47)
                .setMaxGas(47)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(new BigInteger("1000000000000000000"))
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        var decoded = (EthereumTransactionDataEip1559) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getAccessListItems()).isNotNull().isEmpty();
        assertThat(Hex.toHexString(signed)).isEqualTo(Hex.toHexString(decoded.toBytes()));
    }

    @Test
    public void largeValueAndMaxGasMinimalBigEndian() {
        var key = PrivateKey.generateECDSA();
        var value = BigInteger.ONE.shiftLeft(255).add(BigInteger.valueOf(123456789));
        byte[] maxGasBytes = Hex.decode("f00000000000000000000000000000000000000000000000000000000000abcd");
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(2)
                .setMaxPriorityGas(47)
                .setMaxGasBytes(maxGasBytes)
                .setGasLimit(98304)
                .setTo(TO)
                .setValue(value)
                .setCallData(CALL_DATA);

        byte[] signed = tx.sign(key);

        var decoded = (EthereumTransactionDataEip1559) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getValue()).isEqualTo(value);
        assertThat(Hex.toHexString(decoded.getMaxGasBytes())).isEqualTo(Hex.toHexString(maxGasBytes));
        assertThat(decoded.getValueBytes()[0]).isNotEqualTo((byte) 0);
        assertThat(decoded.getMaxGasBytes()[0]).isNotEqualTo((byte) 0);
    }

    @Test
    public void zeroValueAndGasLimitEncodeAsEmptyBytes() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(2)
                .setMaxPriorityGas(47)
                .setMaxGas(47)
                .setGasLimit(0)
                .setTo(TO)
                .setValue(BigInteger.ZERO)
                .setCallData(CALL_DATA);

        assertThat(tx.getValueBytes()).isEmpty();
        assertThat(tx.getGasLimitBytes()).isEmpty();

        byte[] signed = tx.sign(key);

        var decoded = (EthereumTransactionDataEip1559) EthereumTransactionData.fromBytes(signed);
        assertThat(decoded.getValueBytes()).isEmpty();
        assertThat(decoded.getGasLimitBytes()).isEmpty();
        assertThat(decoded.getValue()).isEqualTo(BigInteger.ZERO);
        assertThat(decoded.getGasLimit()).isEqualTo(0L);
    }

    @Test
    public void nonEcdsaKeyRejectedForAllEnvelopes() {
        var ed25519Key = PrivateKey.generateED25519();

        var legacy = new EthereumTransactionDataLegacy()
                .setNonce(1)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO);
        var eip2930 = new EthereumTransactionDataEip2930()
                .setChainId(298)
                .setNonce(1)
                .setGasPrice(47)
                .setGasLimit(98304)
                .setTo(TO);
        var eip1559 = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(1)
                .setMaxPriorityGas(47)
                .setMaxGas(47)
                .setGasLimit(98304)
                .setTo(TO);

        for (EthereumTransactionData body : new EthereumTransactionData[] {legacy, eip2930, eip1559}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> body.sign(ed25519Key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ECDSA");
        }
    }

    @Test
    public void unsignedBodyRejectedBySetEthereumDataFromBody() {
        var tx = new EthereumTransactionDataEip1559().setChainId(298).setNonce(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new EthereumTransaction().setEthereumDataFromBody(tx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not signed");
    }

    @Test
    public void signedBodyAcceptedBySetEthereumDataFromBody() {
        var key = PrivateKey.generateECDSA();
        var tx = new EthereumTransactionDataEip1559()
                .setChainId(298)
                .setNonce(1)
                .setMaxPriorityGas(1)
                .setMaxGas(1)
                .setGasLimit(98304)
                .setTo(TO);
        tx.sign(key);

        var ethTx = new EthereumTransaction().setEthereumDataFromBody(tx);
        assertThat(Hex.toHexString(ethTx.getEthereumData())).isEqualTo(Hex.toHexString(tx.toBytes()));
    }
}
