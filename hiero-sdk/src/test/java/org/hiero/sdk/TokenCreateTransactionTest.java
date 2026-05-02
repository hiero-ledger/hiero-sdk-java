// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.Timestamp;
import org.hiero.sdk.proto.TokenCreateTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenCreateTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final PublicKey testAdminKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e11")
            .getPublicKey();
    private static final PublicKey testKycKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e12")
            .getPublicKey();
    private static final PublicKey testFreezeKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e13")
            .getPublicKey();
    private static final PublicKey testWipeKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e14")
            .getPublicKey();
    private static final PublicKey testSupplyKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e15")
            .getPublicKey();
    private static final PublicKey testFeeScheduleKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e16")
            .getPublicKey();
    private static final PublicKey testPauseKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e17")
            .getPublicKey();
    private static final PublicKey testMetadataKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e18")
            .getPublicKey();
    private static final AccountId testTreasuryAccountId = AccountId.fromString("7.7.7");
    private static final AccountId testAutoRenewAccountId = AccountId.fromString("8.8.8");
    private static final long testInitialSupply = 30;
    private static final long testMaxSupply = 500;
    private static final int testDecimals = 3;
    private static final boolean testFreezeDefault = true;
    private static final String testTokenName = "test name";
    private static final String testTokenSymbol = "test symbol";
    private static final String testTokenMemo = "test memo";
    private static final Duration testAutoRenewPeriod = Duration.ofHours(10);
    private static final Instant testExpirationTime = Instant.now();
    private static final List<CustomFee> testCustomFees = Collections.singletonList(new CustomFixedFee()
            .setFeeCollectorAccountId(AccountId.fromString("0.0.543"))
            .setAmount(3)
            .setDenominatingTokenId(TokenId.fromString("4.3.2")));
    private static final byte[] testMetadata = new byte[] {1, 2, 3, 4, 5};
    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerializeFungible() {
        SnapshotMatcher.expect(spawnTestTransactionFungible().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSerializeNft() {
        SnapshotMatcher.expect(spawnTestTransactionNft().toString()).toMatchSnapshot();
    }

    private TokenCreateTransaction spawnTestTransactionFungible() {
        return new TokenCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setInitialSupply(testInitialSupply)
                .setFeeScheduleKey(testFeeScheduleKey)
                .setSupplyKey(testSupplyKey)
                .setAdminKey(testAdminKey)
                .setAutoRenewAccountId(testAutoRenewAccountId)
                .setAutoRenewPeriod(testAutoRenewPeriod)
                .setDecimals(testDecimals)
                .setFreezeDefault(testFreezeDefault)
                .setFreezeKey(testFreezeKey)
                .setWipeKey(testWipeKey)
                .setTokenSymbol(testTokenSymbol)
                .setKycKey(testKycKey)
                .setPauseKey(testPauseKey)
                .setMetadataKey(testMetadataKey)
                .setExpirationTime(validStart)
                .setTreasuryAccountId(testTreasuryAccountId)
                .setTokenName(testTokenName)
                .setTokenMemo(testTokenMemo)
                .setCustomFees(testCustomFees)
                .setMaxTransactionFee(new Hbar(1))
                .setTokenMetadata(testMetadata)
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesFungible() throws Exception {
        var tx = spawnTestTransactionFungible();
        var tx2 = TokenCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenCreateTransaction spawnTestTransactionNft() {
        return new TokenCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setFeeScheduleKey(testFeeScheduleKey)
                .setSupplyKey(testSupplyKey)
                .setMaxSupply(testMaxSupply)
                .setAdminKey(testAdminKey)
                .setAutoRenewAccountId(testAutoRenewAccountId)
                .setAutoRenewPeriod(testAutoRenewPeriod)
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setSupplyType(TokenSupplyType.FINITE)
                .setFreezeKey(testFreezeKey)
                .setWipeKey(testWipeKey)
                .setTokenSymbol(testTokenSymbol)
                .setKycKey(testKycKey)
                .setPauseKey(testPauseKey)
                .setMetadataKey(testMetadataKey)
                .setExpirationTime(validStart)
                .setTreasuryAccountId(testTreasuryAccountId)
                .setTokenName(testTokenName)
                .setTokenMemo(testTokenMemo)
                .setMaxTransactionFee(new Hbar(1))
                .setTokenMetadata(testMetadata)
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesNft() throws Exception {
        var tx = spawnTestTransactionNft();
        var tx2 = TokenCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenCreation(TokenCreateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenCreateTransaction.class);
    }

    @Test
    void constructTokenCreateTransactionFungibleFromTransactionBodyProtobuf() {
        var transactionBody = TokenCreateTransactionBody.newBuilder()
                .setInitialSupply(testInitialSupply)
                .setFeeScheduleKey(testFeeScheduleKey.toProtobufKey())
                .setSupplyKey(testSupplyKey.toProtobufKey())
                .setAdminKey(testAdminKey.toProtobufKey())
                .setAutoRenewAccount(testAutoRenewAccountId.toProtobuf())
                .setAutoRenewPeriod(org.hiero.sdk.proto.Duration.newBuilder()
                        .setSeconds(testAutoRenewPeriod.toSeconds())
                        .build())
                .setExpiry(Timestamp.newBuilder()
                        .setSeconds(testExpirationTime.getEpochSecond())
                        .build())
                .setDecimals(testDecimals)
                .setFreezeDefault(testFreezeDefault)
                .setFreezeKey(testFreezeKey.toProtobufKey())
                .setWipeKey(testWipeKey.toProtobufKey())
                .setSymbol(testTokenSymbol)
                .setKycKey(testKycKey.toProtobufKey())
                .setPauseKey(testPauseKey.toProtobufKey())
                .setMetadataKey(testMetadataKey.toProtobufKey())
                .setExpiry(Timestamp.newBuilder().setSeconds(testExpirationTime.getEpochSecond()))
                .setTreasury(testTreasuryAccountId.toProtobuf())
                .setName(testTokenName)
                .setMemo(testTokenMemo)
                .addCustomFees(Iterables.getLast(testCustomFees).toProtobuf())
                .setMetadata(ByteString.copyFrom(testMetadata))
                .build();

        var tx = TransactionBody.newBuilder().setTokenCreation(transactionBody).build();
        var tokenCreateTransaction = new TokenCreateTransaction(tx);

        assertThat(tokenCreateTransaction.getFeeScheduleKey()).isEqualTo(testFeeScheduleKey);
        assertThat(tokenCreateTransaction.getSupplyKey()).isEqualTo(testSupplyKey);
        assertThat(tokenCreateTransaction.getAdminKey()).isEqualTo(testAdminKey);
        assertThat(tokenCreateTransaction.getAutoRenewAccountId()).isEqualTo(testAutoRenewAccountId);
        assertThat(tokenCreateTransaction.getAutoRenewPeriod().toSeconds()).isEqualTo(testAutoRenewPeriod.toSeconds());
        assertThat(tokenCreateTransaction.getDecimals()).isEqualTo(testDecimals);
        assertThat(tokenCreateTransaction.getFreezeDefault()).isEqualTo(testFreezeDefault);
        assertThat(tokenCreateTransaction.getFreezeKey()).isEqualTo(testFreezeKey);
        assertThat(tokenCreateTransaction.getWipeKey()).isEqualTo(testWipeKey);
        assertThat(tokenCreateTransaction.getTokenSymbol()).isEqualTo(testTokenSymbol);
        assertThat(tokenCreateTransaction.getKycKey()).isEqualTo(testKycKey);
        assertThat(tokenCreateTransaction.getPauseKey()).isEqualTo(testPauseKey);
        assertThat(tokenCreateTransaction.getMetadataKey()).isEqualTo(testMetadataKey);
        assertThat(tokenCreateTransaction.getExpirationTime().getEpochSecond())
                .isEqualTo(testExpirationTime.getEpochSecond());
        assertThat(tokenCreateTransaction.getTreasuryAccountId()).isEqualTo(testTreasuryAccountId);
        assertThat(tokenCreateTransaction.getTokenName()).isEqualTo(testTokenName);
        assertThat(tokenCreateTransaction.getTokenMemo()).isEqualTo(testTokenMemo);
        assertThat(tokenCreateTransaction.getTokenType()).isEqualTo(TokenType.FUNGIBLE_COMMON);
        assertThat(Iterables.getLast(tokenCreateTransaction.getCustomFees()).toBytes())
                .isEqualTo(Iterables.getLast(testCustomFees).toBytes());
        assertThat(tokenCreateTransaction.getTokenMetadata()).isEqualTo(testMetadata);
    }

    @Test
    void constructTokenCreateTransactionNftFromTransactionBodyProtobuf() {
        var transactionBody = TokenCreateTransactionBody.newBuilder()
                .setFeeScheduleKey(testFeeScheduleKey.toProtobufKey())
                .setSupplyKey(testSupplyKey.toProtobufKey())
                .setMaxSupply(testMaxSupply)
                .setAdminKey(testAdminKey.toProtobufKey())
                .setAutoRenewAccount(testAutoRenewAccountId.toProtobuf())
                .setAutoRenewPeriod(org.hiero.sdk.proto.Duration.newBuilder()
                        .setSeconds(testAutoRenewPeriod.toSeconds())
                        .build())
                .setExpiry(Timestamp.newBuilder()
                        .setSeconds(testExpirationTime.getEpochSecond())
                        .build())
                .setTokenType(org.hiero.sdk.proto.TokenType.NON_FUNGIBLE_UNIQUE)
                .setSupplyType(org.hiero.sdk.proto.TokenSupplyType.FINITE)
                .setFreezeKey(testFreezeKey.toProtobufKey())
                .setWipeKey(testWipeKey.toProtobufKey())
                .setSymbol(testTokenSymbol)
                .setKycKey(testKycKey.toProtobufKey())
                .setPauseKey(testPauseKey.toProtobufKey())
                .setMetadataKey(testMetadataKey.toProtobufKey())
                .setExpiry(Timestamp.newBuilder().setSeconds(testExpirationTime.getEpochSecond()))
                .setTreasury(testTreasuryAccountId.toProtobuf())
                .setName(testTokenName)
                .setMemo(testTokenMemo)
                .build();

        var tx = TransactionBody.newBuilder().setTokenCreation(transactionBody).build();
        var tokenCreateTransaction = new TokenCreateTransaction(tx);

        assertThat(tokenCreateTransaction.getFeeScheduleKey()).isEqualTo(testFeeScheduleKey);
        assertThat(tokenCreateTransaction.getSupplyKey()).isEqualTo(testSupplyKey);
        assertThat(tokenCreateTransaction.getMaxSupply()).isEqualTo(testMaxSupply);
        assertThat(tokenCreateTransaction.getAdminKey()).isEqualTo(testAdminKey);
        assertThat(tokenCreateTransaction.getAutoRenewAccountId()).isEqualTo(testAutoRenewAccountId);
        assertThat(tokenCreateTransaction.getAutoRenewPeriod().toSeconds()).isEqualTo(testAutoRenewPeriod.toSeconds());
        assertThat(tokenCreateTransaction.getTokenType()).isEqualTo(TokenType.NON_FUNGIBLE_UNIQUE);
        assertThat(tokenCreateTransaction.getSupplyType()).isEqualTo(TokenSupplyType.FINITE);
        assertThat(tokenCreateTransaction.getFreezeKey()).isEqualTo(testFreezeKey);
        assertThat(tokenCreateTransaction.getWipeKey()).isEqualTo(testWipeKey);
        assertThat(tokenCreateTransaction.getTokenSymbol()).isEqualTo(testTokenSymbol);
        assertThat(tokenCreateTransaction.getKycKey()).isEqualTo(testKycKey);
        assertThat(tokenCreateTransaction.getPauseKey()).isEqualTo(testPauseKey);
        assertThat(tokenCreateTransaction.getMetadataKey()).isEqualTo(testMetadataKey);
        assertThat(tokenCreateTransaction.getExpirationTime().getEpochSecond())
                .isEqualTo(testExpirationTime.getEpochSecond());
        assertThat(tokenCreateTransaction.getTreasuryAccountId()).isEqualTo(testTreasuryAccountId);
        assertThat(tokenCreateTransaction.getTokenName()).isEqualTo(testTokenName);
        assertThat(tokenCreateTransaction.getTokenMemo()).isEqualTo(testTokenMemo);
    }

    @Test
    void getSetName() {
        var tokenCreateTransaction = new TokenCreateTransaction().setTokenName(testTokenName);
        assertThat(tokenCreateTransaction.getTokenName()).isEqualTo(testTokenName);
    }

    @Test
    void getSetNameFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTokenName(testTokenName));
    }

    @Test
    void getSetSymbol() {
        var tokenCreateTransaction = new TokenCreateTransaction().setTokenSymbol(testTokenSymbol);
        assertThat(tokenCreateTransaction.getTokenSymbol()).isEqualTo(testTokenSymbol);
    }

    @Test
    void getSetSymbolFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTokenSymbol(testTokenSymbol));
    }

    @Test
    void getSetDecimals() {
        var tokenCreateTransaction = new TokenCreateTransaction().setDecimals(testDecimals);
        assertThat(tokenCreateTransaction.getDecimals()).isEqualTo(testDecimals);
    }

    @Test
    void getSetDecimalsFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setDecimals(testDecimals));
    }

    @Test
    void getSetInitialSupply() {
        var tokenCreateTransaction = new TokenCreateTransaction().setInitialSupply(testInitialSupply);
        assertThat(tokenCreateTransaction.getInitialSupply()).isEqualTo(testInitialSupply);
    }

    @Test
    void getSetInitialSupplyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setInitialSupply(testInitialSupply));
    }

    @Test
    void getSetTreasuryAccountId() {
        var tokenCreateTransaction = new TokenCreateTransaction().setTreasuryAccountId(testTreasuryAccountId);
        assertThat(tokenCreateTransaction.getTreasuryAccountId()).isEqualTo(testTreasuryAccountId);
    }

    @Test
    void getSetTreasuryAccountIdFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTreasuryAccountId(testTreasuryAccountId));
    }

    @Test
    void getSetAdminKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setAdminKey(testAdminKey);
        assertThat(tokenCreateTransaction.getAdminKey()).isEqualTo(testAdminKey);
    }

    @Test
    void getSetAdminKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setAdminKey(testAdminKey));
    }

    @Test
    void getSetKycKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setKycKey(testKycKey);
        assertThat(tokenCreateTransaction.getKycKey()).isEqualTo(testKycKey);
    }

    @Test
    void getSetKycKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setKycKey(testKycKey));
    }

    @Test
    void getSetFreezeKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setFreezeKey(testFreezeKey);
        assertThat(tokenCreateTransaction.getFreezeKey()).isEqualTo(testFreezeKey);
    }

    @Test
    void getSetFreezeKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setFreezeKey(testFreezeKey));
    }

    @Test
    void getSetWipeKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setWipeKey(testWipeKey);
        assertThat(tokenCreateTransaction.getWipeKey()).isEqualTo(testWipeKey);
    }

    @Test
    void getSetWipeKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setWipeKey(testWipeKey));
    }

    @Test
    void getSetSupplyKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setSupplyKey(testSupplyKey);
        assertThat(tokenCreateTransaction.getSupplyKey()).isEqualTo(testSupplyKey);
    }

    @Test
    void getSetSupplyKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setSupplyKey(testSupplyKey));
    }

    @Test
    void getSetFeeScheduleKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setFeeScheduleKey(testFeeScheduleKey);
        assertThat(tokenCreateTransaction.getFeeScheduleKey()).isEqualTo(testFeeScheduleKey);
    }

    @Test
    void getSetFeeScheduleKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setFeeScheduleKey(testFeeScheduleKey));
    }

    @Test
    void getSetPauseKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setPauseKey(testPauseKey);
        assertThat(tokenCreateTransaction.getPauseKey()).isEqualTo(testPauseKey);
    }

    @Test
    void getSetPauseKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setPauseKey(testPauseKey));
    }

    @Test
    void getSetMetadataKey() {
        var tokenCreateTransaction = new TokenCreateTransaction().setMetadataKey(testMetadataKey);
        assertThat(tokenCreateTransaction.getMetadataKey()).isEqualTo(testMetadataKey);
    }

    @Test
    void getSetMetadataKeyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setMetadataKey(testMetadataKey));
    }

    @Test
    void getSetExpirationTime() {
        var tokenCreateTransaction = new TokenCreateTransaction().setExpirationTime(testExpirationTime);
        assertThat(tokenCreateTransaction.getExpirationTime()).isEqualTo(testExpirationTime);
    }

    @Test
    void getSetExpirationTimeFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setExpirationTime(testExpirationTime));
    }

    @Test
    void getSetAutoRenewAccountId() {
        var tokenCreateTransaction = new TokenCreateTransaction().setAutoRenewAccountId(testAutoRenewAccountId);
        assertThat(tokenCreateTransaction.getAutoRenewAccountId()).isEqualTo(testAutoRenewAccountId);
    }

    @Test
    void getSetAutoRenewAccountIdFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setAutoRenewAccountId(testAutoRenewAccountId));
    }

    @Test
    void getSetAutoRenewPeriod() {
        var tokenCreateTransaction = new TokenCreateTransaction().setAutoRenewPeriod(testAutoRenewPeriod);
        assertThat(tokenCreateTransaction.getAutoRenewPeriod()).isEqualTo(testAutoRenewPeriod);
    }

    @Test
    void getSetAutoRenewPeriodFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setAutoRenewPeriod(testAutoRenewPeriod));
    }

    @Test
    void getSetTokenMemo() {
        var tokenCreateTransaction = new TokenCreateTransaction().setTokenMemo(testTokenMemo);
        assertThat(tokenCreateTransaction.getTokenMemo()).isEqualTo(testTokenMemo);
    }

    @Test
    void getSetTokenMemoFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTokenMemo(testTokenMemo));
    }

    @Test
    void getSetTokenType() {
        final TokenType testTokenType = TokenType.FUNGIBLE_COMMON;
        var tokenCreateTransaction = new TokenCreateTransaction().setTokenType(testTokenType);
        assertThat(tokenCreateTransaction.getTokenType()).isEqualTo(testTokenType);
    }

    @Test
    void getSetTokenTypeFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTokenType(TokenType.FUNGIBLE_COMMON));
    }

    @Test
    void getSetSupplyType() {
        final TokenSupplyType testTokenType = TokenSupplyType.FINITE;
        var tokenCreateTransaction = new TokenCreateTransaction().setSupplyType(testTokenType);
        assertThat(tokenCreateTransaction.getSupplyType()).isEqualTo(testTokenType);
    }

    @Test
    void getSetSupplyTypeFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setSupplyType(TokenSupplyType.FINITE));
    }

    @Test
    void getSetMaxSupply() {
        var tokenCreateTransaction = new TokenCreateTransaction().setMaxSupply(testMaxSupply);
        assertThat(tokenCreateTransaction.getMaxSupply()).isEqualTo(testMaxSupply);
    }

    @Test
    void getSetMaxSupplyFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setMaxSupply(testMaxSupply));
    }

    @Test
    void getSetMetadata() {
        var tx = spawnTestTransactionFungible();
        assertThat(tx.getTokenMetadata()).isEqualTo(testMetadata);
    }

    @Test
    void getSetMetadataFrozen() {
        var tx = spawnTestTransactionFungible();
        assertThrows(IllegalStateException.class, () -> tx.setTokenMetadata(testMetadata));
    }
}
