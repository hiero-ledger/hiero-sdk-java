// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.Bytes;
import com.hedera.hashgraph.sdk.EthereumTransactionDataEip7702.AuthorizationTuple;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class EthereumTransactionDataEip7702Test {

    @Test
    public void eip7702ToFromBytes() {
        var authorizationTuple = new AuthorizationTuple(
                Hex.decode("012a"),
                Hex.decode("0102030405060708090a0b0c0d0e0f1011121314"),
                Hex.decode("01"),
                Hex.decode("00"),
                Hex.decode("11"),
                Hex.decode("12"));

        var headerData = new EthereumTransactionDataEip7702.HeaderData(
                Hex.decode("012a"),
                Hex.decode("02"),
                Hex.decode("2f"),
                Hex.decode("2f"),
                Hex.decode("018000"),
                Hex.decode("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181"),
                Hex.decode("0de0b6b3a7640000"));

        var signatureData = new EthereumTransactionDataEip7702.SignatureData(
                Hex.decode("01"),
                Hex.decode("df48f2efd10421811de2bfb125ab75b2d3c44139c4642837fb1fccce911fd479"),
                Hex.decode("1aaf7ae92bee896651dfc9d99ae422a296bf5d9f1ca49b2d96d82b79eb112d66"));

        var data = new EthereumTransactionDataEip7702(
                headerData, Hex.decode("123456"), List.of(), List.of(authorizationTuple), signatureData);

        var encodedHex = Hex.toHexString(data.toBytes());
        var decoded = (EthereumTransactionDataEip7702) EthereumTransactionData.fromBytes(Hex.decode(encodedHex));

        assertThat(encodedHex).isEqualTo(Hex.toHexString(decoded.toBytes()));
        assertThat(Hex.toHexString(decoded.chainId)).isEqualTo("012a");
        assertThat(Hex.toHexString(decoded.nonce)).isEqualTo("02");
        assertThat(Hex.toHexString(decoded.maxPriorityGas)).isEqualTo("2f");
        assertThat(Hex.toHexString(decoded.maxGas)).isEqualTo("2f");
        assertThat(Hex.toHexString(decoded.gasLimit)).isEqualTo("018000");
        assertThat(Hex.toHexString(decoded.to)).isEqualTo("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
        assertThat(Hex.toHexString(decoded.value)).isEqualTo("0de0b6b3a7640000");
        assertThat(Hex.toHexString(decoded.callData)).isEqualTo("123456");
        assertThat(decoded.accessList).isEmpty();
        assertThat(decoded.authorizationList).hasSize(1);

        var decodedAuth = decoded.authorizationList.get(0);
        assertThat(Hex.toHexString(decodedAuth.chainId)).isEqualTo("012a");
        assertThat(Hex.toHexString(decodedAuth.address)).isEqualTo("0102030405060708090a0b0c0d0e0f1011121314");
        assertThat(Hex.toHexString(decodedAuth.nonce)).isEqualTo("01");
        assertThat(Hex.toHexString(decodedAuth.yParity)).isEqualTo("00");
        assertThat(Hex.toHexString(decodedAuth.r)).isEqualTo("11");
        assertThat(Hex.toHexString(decodedAuth.s)).isEqualTo("12");

        assertThat(Hex.toHexString(decoded.recoveryId)).isEqualTo("01");
        assertThat(Hex.toHexString(decoded.r))
                .isEqualTo("df48f2efd10421811de2bfb125ab75b2d3c44139c4642837fb1fccce911fd479");
        assertThat(Hex.toHexString(decoded.s))
                .isEqualTo("1aaf7ae92bee896651dfc9d99ae422a296bf5d9f1ca49b2d96d82b79eb112d66");
    }

    @Test
    void manualEncodingMatchesHeadlongSequence() {
        var authorizationTuple = new AuthorizationTuple(
                Hex.decode("012a"),
                Hex.decode("0102030405060708090a0b0c0d0e0f1011121314"),
                Hex.decode("01"),
                Hex.decode("00"),
                Hex.decode("11"),
                Hex.decode("12"));

        var headerData = new EthereumTransactionDataEip7702.HeaderData(
                Hex.decode("012a"),
                Hex.decode("00"),
                Hex.decode("00"),
                Hex.decode("d1385c7bf0"),
                Hex.decode("07a120"),
                Hex.decode("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181"),
                Hex.decode("00"));

        var signatureData = new EthereumTransactionDataEip7702.SignatureData(
                Hex.decode("01"),
                Hex.decode("df48f2efd10421811de2bfb125ab75b2d3c44139c4642837fb1fccce911fd479"),
                Hex.decode("1aaf7ae92bee896651dfc9d99ae422a296bf5d9f1ca49b2d96d82b79eb112d66"));

        var data = new EthereumTransactionDataEip7702(
                headerData, Hex.decode("123456"), List.of(), List.of(authorizationTuple), signatureData);

        var manualPayload = com.esaulpaugh.headlong.rlp.RLPEncoder.list(
                data.chainId,
                data.nonce,
                data.maxPriorityGas,
                data.maxGas,
                data.gasLimit,
                data.to,
                data.value,
                data.callData,
                List.of(),
                List.of(List.of(
                        authorizationTuple.chainId,
                        authorizationTuple.address,
                        authorizationTuple.nonce,
                        authorizationTuple.yParity,
                        authorizationTuple.r,
                        authorizationTuple.s)),
                data.recoveryId,
                data.r,
                data.s);

        var manualBytes = Bytes.concat(new byte[] {0x04}, manualPayload);

        assertThat(data.toBytes()).containsExactly(manualBytes);
        assertThat(EthereumTransactionDataEip7702.fromBytes(manualBytes)).isNotNull();
        assertThat(EthereumTransactionDataEip7702.fromBytes(manualBytes).toBytes())
                .containsExactly(manualBytes);
    }

    @Test
    void toBytesAndFromBytesPreserveAllFieldValues() {
        var original = createEip7702Data();

        var bytes = original.toBytes();
        var decoded = EthereumTransactionDataEip7702.fromBytes(bytes);

        assertThat(decoded.chainId).containsExactly(original.chainId);
        assertThat(decoded.nonce).containsExactly(original.nonce);
        assertThat(decoded.maxPriorityGas).containsExactly(original.maxPriorityGas);
        assertThat(decoded.maxGas).containsExactly(original.maxGas);
        assertThat(decoded.gasLimit).containsExactly(original.gasLimit);
        assertThat(decoded.to).containsExactly(original.to);
        assertThat(decoded.value).containsExactly(original.value);
        assertThat(decoded.callData).containsExactly(original.callData);
        assertThat(decoded.recoveryId).containsExactly(original.recoveryId);
        assertThat(decoded.r).containsExactly(original.r);
        assertThat(decoded.s).containsExactly(original.s);

        assertThat(decoded.authorizationList).hasSize(1);

        var originalAuth = original.authorizationList.get(0);
        var decodedAuth = decoded.authorizationList.get(0);
        assertThat(decodedAuth.chainId).containsExactly(originalAuth.chainId);
        assertThat(decodedAuth.address).containsExactly(originalAuth.address);
        assertThat(decodedAuth.nonce).containsExactly(originalAuth.nonce);
        assertThat(decodedAuth.yParity).containsExactly(originalAuth.yParity);
        assertThat(decodedAuth.r).containsExactly(originalAuth.r);
        assertThat(decodedAuth.s).containsExactly(originalAuth.s);

        assertThat(decoded.toBytes()).containsExactly(bytes);
    }

    private EthereumTransactionDataEip7702 createEip7702Data() {
        var authData = new EthereumTransactionDataEip7702.AuthorizationTuple(
                Hex.decode("012a"),
                Hex.decode("00000000000000000000000000000000000003f9"),
                Hex.decode("00"),
                Hex.decode("01"),
                Hex.decode("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"),
                Hex.decode("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"));

        var headerData = new EthereumTransactionDataEip7702.HeaderData(
                Hex.decode("012a"),
                Hex.decode("00"),
                Hex.decode("01"),
                Hex.decode("d1385c7bf0"),
                Hex.decode("07A120"),
                Hex.decode("00000000000000000000000000000000000003f9"),
                new byte[0]);

        var signatureData = new EthereumTransactionDataEip7702.SignatureData(
                Hex.decode("01"),
                Hex.decode("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"),
                Hex.decode("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"));

        return new EthereumTransactionDataEip7702(
                headerData, Hex.decode("123456"), List.of(), List.of(authData), signatureData);
    }
}
