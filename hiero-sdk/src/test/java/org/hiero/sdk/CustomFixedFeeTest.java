// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.FixedFee;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomFixedFeeTest {
    private static final boolean allCollectorsAreExempt = true;
    private static final AccountId feeCollectorAccountId = new AccountId(1, 2, 3);
    private static final long amount = 4;
    private static final TokenId tokenId = new TokenId(5, 6, 7);

    private final FixedFee fee = FixedFee.newBuilder()
            .setAmount(amount)
            .setDenominatingTokenId(tokenId.toProtobuf())
            .build();

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromProtobuf() {
        SnapshotMatcher.expect(CustomFixedFee.fromProtobuf(fee).toString()).toMatchSnapshot();
    }

    @Test
    void deepCloneSubclass() {
        var customFixedFee = new CustomFixedFee()
                .setFeeCollectorAccountId(feeCollectorAccountId)
                .setAllCollectorsAreExempt(allCollectorsAreExempt);
        var clonedCustomFixedFee = customFixedFee.deepCloneSubclass();

        assertThat(clonedCustomFixedFee.getFeeCollectorAccountId()).isEqualTo(feeCollectorAccountId);
        assertThat(clonedCustomFixedFee.getAllCollectorsAreExempt()).isEqualTo(allCollectorsAreExempt);
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(CustomFixedFee.fromProtobuf(fee).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void toFixedFeeProtobuf() {
        SnapshotMatcher.expect(
                        CustomFixedFee.fromProtobuf(fee).toFixedFeeProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void getSetAmount() {
        final var customFixedFee1 = new CustomFixedFee().setAmount(amount);
        final var customFixedFee2 = new CustomFixedFee().setHbarAmount(Hbar.fromTinybars(amount));

        assertThat(customFixedFee1.getAmount()).isEqualTo(amount);
        assertThat(customFixedFee2.getHbarAmount().toTinybars()).isEqualTo(amount);
        assertThat(customFixedFee1.getHbarAmount().toTinybars()).isEqualTo(customFixedFee2.getAmount());
    }

    @Test
    void getSetDenominatingToken() {
        final var customFixedFee = new CustomFixedFee().setDenominatingTokenId(tokenId);
        assertThat(customFixedFee.getDenominatingTokenId()).isEqualTo(tokenId);
    }

    @Test
    void setSentinelValueToken() {
        final var customFixedFee = new CustomFixedFee().setDenominatingTokenToSameToken();
        assertThat(customFixedFee.getDenominatingTokenId()).isEqualTo(new TokenId(0, 0, 0));
    }
}
