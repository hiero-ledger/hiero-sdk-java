// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomFeeListTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private static List<CustomFee> spawnCustomFeeListExample() {
        var returnList = new ArrayList<CustomFee>();
        returnList.add(new CustomFixedFee()
                .setFeeCollectorAccountId(new AccountId(0, 0, 4322))
                .setDenominatingTokenId(new TokenId(0, 0, 483902))
                .setAmount(10));
        returnList.add(new CustomFractionalFee()
                .setFeeCollectorAccountId(new AccountId(0, 0, 389042))
                .setNumerator(3)
                .setDenominator(7)
                .setMin(3)
                .setMax(100));
        returnList.add(new CustomRoyaltyFee()
                .setFeeCollectorAccountId(new AccountId(0, 0, 23423))
                .setNumerator(5)
                .setDenominator(8)
                .setFallbackFee(new CustomFixedFee()
                        .setDenominatingTokenId(new TokenId(0, 0, 483902))
                        .setAmount(10)));
        return returnList;
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalCustomFeeList = spawnCustomFeeListExample();
        byte[] customFee0Bytes = originalCustomFeeList.get(0).toBytes();
        byte[] customFee1Bytes = originalCustomFeeList.get(1).toBytes();
        byte[] customFee2Bytes = originalCustomFeeList.get(2).toBytes();
        var copyCustomFeeList = new ArrayList<CustomFee>();
        copyCustomFeeList.add(CustomFee.fromBytes(customFee0Bytes));
        copyCustomFeeList.add(CustomFee.fromBytes(customFee1Bytes));
        copyCustomFeeList.add(CustomFee.fromBytes(customFee2Bytes));
        assertThat(originalCustomFeeList.toString()).isEqualTo(copyCustomFeeList.toString());
        SnapshotMatcher.expect(originalCustomFeeList.toString()).toMatchSnapshot();
    }

    @Test
    void deepClone() throws Exception {
        var originalCustomFeeList = spawnCustomFeeListExample();
        var copyCustomFeeList = new ArrayList<CustomFee>();
        for (var fee : originalCustomFeeList) {
            copyCustomFeeList.add(fee.deepClone());
        }
        var originalCustomFeeListString = originalCustomFeeList.toString();
        assertThat(originalCustomFeeListString).isEqualTo(copyCustomFeeList.toString());

        // modifying clone doesn't affect original
        ((CustomFixedFee) copyCustomFeeList.get(0)).setDenominatingTokenId(new TokenId(0, 0, 89803));
        assertThat(originalCustomFeeListString).isEqualTo(originalCustomFeeList.toString());

        SnapshotMatcher.expect(originalCustomFeeList.toString()).toMatchSnapshot();
    }
}
