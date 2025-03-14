// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CustomFeeLimit;
import com.hedera.hashgraph.sdk.proto.FixedFee;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomFeeLimitTest {
    private static final AccountId TEST_PAYER_ID = new AccountId(0, 0, 1234);

    // Creating a sample FixedFee protobuf for testing
    private static final FixedFee TEST_FIXED_FEE_PROTO =
            FixedFee.newBuilder().setAmount(1000).build();

    // Using fromProtobuf() to properly initialize CustomFixedFee
    private static final CustomFixedFee TEST_CUSTOM_FIXED_FEE = CustomFixedFee.fromProtobuf(TEST_FIXED_FEE_PROTO);

    private static final List<CustomFixedFee> TEST_FEES = Collections.singletonList(TEST_CUSTOM_FIXED_FEE);

    // Instead of using a constructor, we initialize via fromProtobuf()
    private static final CustomFeeLimit TEST_CUSTOM_FEE_LIMIT;

    static {
        try {
            TEST_CUSTOM_FEE_LIMIT = createTestCustomFeeLimit();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private static CustomFeeLimit createTestCustomFeeLimit() throws InvalidProtocolBufferException {
        // Step 1: Build the Protobuf representation
        var proto = com.hedera.hashgraph.sdk.proto.CustomFeeLimit.newBuilder()
                .setAccountId(TEST_PAYER_ID.toProtobuf())
                .addAllFees(TEST_FEES.stream()
                        .map(CustomFixedFee::toFixedFeeProtobuf)
                        .toList())
                .build();

        // Step 2: Convert Protobuf to CustomFeeLimit instance
        return CustomFeeLimit.parseFrom(proto.toByteArray());
    }

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    public void testGetPayerId() {
        assertEquals(TEST_PAYER_ID, AccountId.fromProtobuf(TEST_CUSTOM_FEE_LIMIT.getAccountId()));
    }

    @Test
    public void testSetPayerId() {
        AccountId newPayerId = new AccountId(0, 0, 5678);

        // Create a new instance using the builder
        CustomFeeLimit updatedFeeLimit = CustomFeeLimit.newBuilder()
                .setAccountId(newPayerId.toProtobuf())
                .addAllFees(TEST_FEES.stream()
                        .map(CustomFixedFee::toFixedFeeProtobuf)
                        .toList())
                .build();

        assertEquals(newPayerId, AccountId.fromProtobuf(updatedFeeLimit.getAccountId()));
    }

    @Test
    public void testGetCustomFees() {
        assertEquals(
                TEST_FEES.stream().map(CustomFixedFee::toFixedFeeProtobuf).collect(Collectors.toList()),
                TEST_CUSTOM_FEE_LIMIT.getFeesList());
    }

    @Test
    public void testSetCustomFees() {
        List<CustomFixedFee> newFees = Collections.emptyList();

        // Create a new instance using the builder
        CustomFeeLimit updatedFeeLimit = CustomFeeLimit.newBuilder()
                .setAccountId(TEST_PAYER_ID.toProtobuf())
                .addAllFees(
                        newFees.stream().map(CustomFixedFee::toFixedFeeProtobuf).toList())
                .build();

        assertEquals(newFees, updatedFeeLimit.getFeesList());
    }

    @Test
    public void testToProtobuf() throws InvalidProtocolBufferException {
        // Create a protobuf representation manually
        var proto = createTestCustomFeeLimit();

        // Validate fields
        assertEquals(TEST_PAYER_ID.toProtobuf(), proto.getAccountId());
        assertFalse(proto.getFeesList().isEmpty());
    }

    @Test
    public void testFromProtobuf() {
        var proto = CustomFeeLimit.newBuilder()
                .setAccountId(TEST_PAYER_ID.toProtobuf())
                .addAllFees(TEST_FEES.stream()
                        .map(CustomFixedFee::toFixedFeeProtobuf)
                        .toList())
                .build();

        com.hedera.hashgraph.sdk.CustomFeeLimit converted = com.hedera.hashgraph.sdk.CustomFeeLimit.fromProtobuf(proto);

        assertEquals(TEST_PAYER_ID, converted.getPayerId());
        assertEquals(
                TEST_FEES.get(0).feeCollectorAccountId,
                converted.getCustomFees().get(0).feeCollectorAccountId);
    }
}
