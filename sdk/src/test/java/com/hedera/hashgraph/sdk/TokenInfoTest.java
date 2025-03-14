// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// TODO: update this, test deepClone()

public class TokenInfoTest {
    /*
    if we will init PrivateKey using method `PrivateKey.fromSeedECDSAsecp256k1(byte[] seed)` (like in C++ SDK, for example)
    => we will get public key each time we run tests on different machines
    => io.github.jsonSnapshot.SnapshotMatcher will fail tests
    => we need to init PrivateKey fromString to get the same key each time
    => `toProtobuf()` tests uses getEd25519() method to assert equality
    */
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
    private static final TokenId testTokenId = TokenId.fromString("0.6.9");
    private static final AccountId testTreasuryAccountId = AccountId.fromString("7.7.7");
    private static final AccountId testAutoRenewAccountId = AccountId.fromString("8.9.0");
    private static final String testTokenName = "test token name";
    private static final String testTokenSymbol = "TTN";
    private static final String testTokenMemo = "memo";
    private static final int testTokenDecimals = 3;
    private static final long testTokenTotalSupply = 1000L;
    private static final Boolean testTokenFreezeStatus = true;
    private static final Boolean testTokenKycStatus = true;
    private static final boolean testTokenIsDeleted = false;
    private static final List<CustomFee> testTokenCustomFees = Arrays.asList(
            new CustomFixedFee()
                    .setFeeCollectorAccountId(new AccountId(0, 0, 4322))
                    .setDenominatingTokenId(new TokenId(0, 0, 483902))
                    .setAmount(10),
            new CustomFractionalFee()
                    .setFeeCollectorAccountId(new AccountId(0, 0, 389042))
                    .setNumerator(3)
                    .setDenominator(7)
                    .setMin(3)
                    .setMax(100));
    private static final TokenType testTokenType = TokenType.FUNGIBLE_COMMON;
    private static final TokenSupplyType testTokenSupplyType = TokenSupplyType.FINITE;
    private static final long testTokenMaxSupply = 1000000L;
    private static final boolean testTokenPauseStatus = true;
    private static final LedgerId testTokenLedgerId = LedgerId.MAINNET;
    private static final Duration testAutoRenewPeriod = Duration.ofHours(10);
    private static final Instant testExpirationTime = Instant.ofEpochSecond(1554158542);
    private static final byte[] testMetadata = new byte[] {1, 2, 3, 4, 5};

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private static TokenInfo spawnTokenInfoExample() {
        return new TokenInfo(
                testTokenId,
                testTokenName,
                testTokenSymbol,
                testTokenDecimals,
                testTokenTotalSupply,
                testTreasuryAccountId,
                testAdminKey,
                testKycKey,
                testFreezeKey,
                testWipeKey,
                testSupplyKey,
                testFeeScheduleKey,
                testTokenFreezeStatus,
                testTokenKycStatus,
                testTokenIsDeleted,
                testAutoRenewAccountId,
                testAutoRenewPeriod,
                testExpirationTime,
                testTokenMemo,
                testTokenCustomFees,
                testTokenType,
                testTokenSupplyType,
                testTokenMaxSupply,
                testPauseKey,
                testTokenPauseStatus,
                testMetadata,
                testMetadataKey,
                testTokenLedgerId);
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalTokenInfo = spawnTokenInfoExample();
        byte[] tokenInfoBytes = originalTokenInfo.toBytes();
        var copyTokenInfo = TokenInfo.fromBytes(tokenInfoBytes);
        assertThat(copyTokenInfo.toString()).isEqualTo(originalTokenInfo.toString());
        SnapshotMatcher.expect(originalTokenInfo.toString()).toMatchSnapshot();
    }

    @Test
    void fromProtobuf() {
        var tokenInfoProto = spawnTokenInfoExample().toProtobuf();

        var tokenInfo = TokenInfo.fromProtobuf(tokenInfoProto);

        assertThat(tokenInfo.tokenId).isEqualTo(testTokenId);
        assertThat(tokenInfo.name).isEqualTo(testTokenName);
        assertThat(tokenInfo.symbol).isEqualTo(testTokenSymbol);
        assertThat(tokenInfo.decimals).isEqualTo(testTokenDecimals);
        assertThat(tokenInfo.totalSupply).isEqualTo(testTokenTotalSupply);
        assertThat(tokenInfo.treasuryAccountId).isEqualTo(testTreasuryAccountId);
        assertThat(tokenInfo.adminKey.toBytes()).isEqualTo(testAdminKey.toBytes());
        assertThat(tokenInfo.kycKey.toBytes()).isEqualTo(testKycKey.toBytes());
        assertThat(tokenInfo.freezeKey.toBytes()).isEqualTo(testFreezeKey.toBytes());
        assertThat(tokenInfo.wipeKey.toBytes()).isEqualTo(testWipeKey.toBytes());
        assertThat(tokenInfo.supplyKey.toBytes()).isEqualTo(testSupplyKey.toBytes());
        assertThat(tokenInfo.defaultFreezeStatus).isEqualTo(testTokenFreezeStatus);
        assertThat(tokenInfo.defaultKycStatus).isEqualTo(testTokenKycStatus);
        assertThat(tokenInfo.isDeleted).isEqualTo(testTokenIsDeleted);
        assertThat(tokenInfo.autoRenewAccount).isEqualTo(testAutoRenewAccountId);
        assertThat(tokenInfo.autoRenewPeriod).isEqualTo(testAutoRenewPeriod);
        assertThat(tokenInfo.expirationTime).isEqualTo(testExpirationTime);
        assertThat(tokenInfo.tokenMemo).isEqualTo(testTokenMemo);
        assertThat(tokenInfo.tokenType).isEqualTo(testTokenType);
        assertThat(tokenInfo.supplyType).isEqualTo(testTokenSupplyType);
        assertThat(tokenInfo.maxSupply).isEqualTo(testTokenMaxSupply);
        assertThat(tokenInfo.feeScheduleKey.toBytes()).isEqualTo(testFeeScheduleKey.toBytes());
        assertThat(tokenInfo.customFees).hasSize(testTokenCustomFees.size());
        assertThat(tokenInfo.pauseKey.toBytes()).isEqualTo(testPauseKey.toBytes());
        assertThat(tokenInfo.pauseStatus).isEqualTo(testTokenPauseStatus);
        assertThat(tokenInfo.metadata).isEqualTo(testMetadata);
        assertThat(tokenInfo.metadataKey.toBytes()).isEqualTo(testMetadataKey.toBytes());
        assertThat(tokenInfo.ledgerId).isEqualTo(testTokenLedgerId);
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        var tokenInfoProto = spawnTokenInfoExample().toProtobuf();

        var tokenInfo = TokenInfo.fromBytes(tokenInfoProto.toByteArray());

        assertThat(tokenInfo.tokenId).isEqualTo(testTokenId);
        assertThat(tokenInfo.name).isEqualTo(testTokenName);
        assertThat(tokenInfo.symbol).isEqualTo(testTokenSymbol);
        assertThat(tokenInfo.decimals).isEqualTo(testTokenDecimals);
        assertThat(tokenInfo.totalSupply).isEqualTo(testTokenTotalSupply);
        assertThat(tokenInfo.treasuryAccountId).isEqualTo(testTreasuryAccountId);
        assertThat(tokenInfo.adminKey.toBytes()).isEqualTo(testAdminKey.toBytes());
        assertThat(tokenInfo.kycKey.toBytes()).isEqualTo(testKycKey.toBytes());
        assertThat(tokenInfo.freezeKey.toBytes()).isEqualTo(testFreezeKey.toBytes());
        assertThat(tokenInfo.wipeKey.toBytes()).isEqualTo(testWipeKey.toBytes());
        assertThat(tokenInfo.supplyKey.toBytes()).isEqualTo(testSupplyKey.toBytes());
        assertThat(tokenInfo.defaultFreezeStatus).isEqualTo(testTokenFreezeStatus);
        assertThat(tokenInfo.defaultKycStatus).isEqualTo(testTokenKycStatus);
        assertThat(tokenInfo.isDeleted).isEqualTo(testTokenIsDeleted);
        assertThat(tokenInfo.autoRenewAccount).isEqualTo(testAutoRenewAccountId);
        assertThat(tokenInfo.autoRenewPeriod).isEqualTo(testAutoRenewPeriod);
        assertThat(tokenInfo.expirationTime).isEqualTo(testExpirationTime);
        assertThat(tokenInfo.tokenMemo).isEqualTo(testTokenMemo);
        assertThat(tokenInfo.tokenType).isEqualTo(testTokenType);
        assertThat(tokenInfo.supplyType).isEqualTo(testTokenSupplyType);
        assertThat(tokenInfo.maxSupply).isEqualTo(testTokenMaxSupply);
        assertThat(tokenInfo.feeScheduleKey.toBytes()).isEqualTo(testFeeScheduleKey.toBytes());
        assertThat(tokenInfo.customFees).hasSize(testTokenCustomFees.size());
        assertThat(tokenInfo.pauseKey.toBytes()).isEqualTo(testPauseKey.toBytes());
        assertThat(tokenInfo.pauseStatus).isEqualTo(testTokenPauseStatus);
        assertThat(tokenInfo.metadata).isEqualTo(testMetadata);
        assertThat(tokenInfo.metadataKey.toBytes()).isEqualTo(testMetadataKey.toBytes());
        assertThat(tokenInfo.ledgerId).isEqualTo(testTokenLedgerId);
    }

    @Test
    void toProtobuf() {
        var tokenInfoProto = spawnTokenInfoExample().toProtobuf();

        assertThat(tokenInfoProto.getTokenInfo().getTokenId().getShardNum()).isEqualTo(testTokenId.shard);
        assertThat(tokenInfoProto.getTokenInfo().getTokenId().getRealmNum()).isEqualTo(testTokenId.realm);
        assertThat(tokenInfoProto.getTokenInfo().getTokenId().getTokenNum()).isEqualTo(testTokenId.num);
        assertThat(tokenInfoProto.getTokenInfo().getName()).isEqualTo(testTokenName);
        assertThat(tokenInfoProto.getTokenInfo().getSymbol()).isEqualTo(testTokenSymbol);
        assertThat(tokenInfoProto.getTokenInfo().getDecimals()).isEqualTo(testTokenDecimals);
        assertThat(tokenInfoProto.getTokenInfo().getTotalSupply()).isEqualTo(testTokenTotalSupply);
        assertThat(tokenInfoProto.getTokenInfo().getTreasury().getShardNum()).isEqualTo(testTreasuryAccountId.shard);
        assertThat(tokenInfoProto.getTokenInfo().getTreasury().getRealmNum()).isEqualTo(testTreasuryAccountId.realm);
        assertThat(tokenInfoProto.getTokenInfo().getTreasury().getAccountNum()).isEqualTo(testTreasuryAccountId.num);
        assertThat(tokenInfoProto.getTokenInfo().getAdminKey().getEd25519().toByteArray())
                .isEqualTo(testAdminKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getKycKey().getEd25519().toByteArray())
                .isEqualTo(testKycKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getFreezeKey().getEd25519().toByteArray())
                .isEqualTo(testFreezeKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getWipeKey().getEd25519().toByteArray())
                .isEqualTo(testWipeKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getSupplyKey().getEd25519().toByteArray())
                .isEqualTo(testSupplyKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getDefaultFreezeStatus())
                .isEqualTo(TokenInfo.freezeStatusToProtobuf(testTokenFreezeStatus));
        assertThat(tokenInfoProto.getTokenInfo().getDefaultKycStatus())
                .isEqualTo(TokenInfo.kycStatusToProtobuf(testTokenKycStatus));
        assertThat(tokenInfoProto.getTokenInfo().getDeleted()).isEqualTo(testTokenIsDeleted);
        assertThat(tokenInfoProto.getTokenInfo().getAutoRenewAccount().getShardNum())
                .isEqualTo(testAutoRenewAccountId.shard);
        assertThat(tokenInfoProto.getTokenInfo().getAutoRenewAccount().getRealmNum())
                .isEqualTo(testAutoRenewAccountId.realm);
        assertThat(tokenInfoProto.getTokenInfo().getAutoRenewAccount().getAccountNum())
                .isEqualTo(testAutoRenewAccountId.num);
        assertThat(tokenInfoProto.getTokenInfo().getAutoRenewPeriod().getSeconds())
                .isEqualTo(testAutoRenewPeriod.toSeconds());
        assertThat(tokenInfoProto.getTokenInfo().getExpiry().getSeconds())
                .isEqualTo(testExpirationTime.getEpochSecond());
        assertThat(tokenInfoProto.getTokenInfo().getMemo()).isEqualTo(testTokenMemo);
        assertThat(tokenInfoProto.getTokenInfo().getTokenType())
                .isEqualTo(com.hedera.hashgraph.sdk.proto.TokenType.valueOf(testTokenType.name()));
        assertThat(tokenInfoProto.getTokenInfo().getSupplyType())
                .isEqualTo(com.hedera.hashgraph.sdk.proto.TokenSupplyType.valueOf(testTokenSupplyType.name()));
        assertThat(tokenInfoProto.getTokenInfo().getMaxSupply()).isEqualTo(testTokenMaxSupply);
        assertThat(tokenInfoProto
                        .getTokenInfo()
                        .getFeeScheduleKey()
                        .getEd25519()
                        .toByteArray())
                .isEqualTo(testFeeScheduleKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getCustomFeesList()).hasSize(testTokenCustomFees.size());
        assertThat(tokenInfoProto.getTokenInfo().getPauseKey().getEd25519().toByteArray())
                .isEqualTo(testPauseKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getPauseStatus())
                .isEqualTo(TokenInfo.pauseStatusToProtobuf(testTokenPauseStatus));
        assertThat(tokenInfoProto.getTokenInfo().getMetadata().toByteArray()).isEqualTo(testMetadata);
        assertThat(tokenInfoProto.getTokenInfo().getMetadataKey().getEd25519().toByteArray())
                .isEqualTo(testMetadataKey.toBytesRaw());
        assertThat(tokenInfoProto.getTokenInfo().getLedgerId()).isEqualTo(testTokenLedgerId.toByteString());
    }

    @Test
    void toBytes() {
        var tokenInfo = spawnTokenInfoExample();
        var bytes = tokenInfo.toBytes();
        assertThat(bytes).isEqualTo(tokenInfo.toProtobuf().toByteArray());
    }
}
