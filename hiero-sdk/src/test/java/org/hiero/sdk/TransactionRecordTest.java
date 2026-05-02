// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hiero.sdk.ContractFunctionResultTest.CALL_RESULT_HEX;
import static org.hiero.sdk.TransactionReceiptTest.spawnReceiptExample;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TransactionRecordTest {
    static final Instant time = Instant.ofEpochSecond(1554158542);
    private static final byte[] callResult = Hex.decode(CALL_RESULT_HEX);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private static TransactionRecord spawnRecordExample(@Nullable ByteString prngBytes, @Nullable Integer prngNumber) {
        return new TransactionRecord(
                spawnReceiptExample(),
                ByteString.copyFrom("hello", StandardCharsets.UTF_8),
                time,
                TransactionId.withValidStart(AccountId.fromString("3.3.3"), time),
                "memo",
                3000L,
                new ContractFunctionResult(org.hiero.sdk.proto.ContractFunctionResult.newBuilder()
                        .setContractID(ContractId.fromString("1.2.3").toProtobuf())
                        .setContractCallResult(ByteString.copyFrom(callResult))
                        .setEvmAddress(BytesValue.newBuilder()
                                .setValue(ByteString.copyFrom(Hex.decode("98329e006610472e6B372C080833f6D79ED833cf")))
                                .build())
                        .setSenderId(AccountId.fromString("1.2.3").toProtobuf())),
                List.of(new Transfer(AccountId.fromString("4.4.4"), Hbar.from(5))),
                Map.of(TokenId.fromString("6.6.6"), Map.of(AccountId.fromString("1.1.1"), 4L)),
                List.of(new TokenTransfer(TokenId.fromString("8.9.10"), AccountId.fromString("1.2.3"), 4L, 3, true)),
                Map.of(
                        TokenId.fromString("4.4.4"),
                        List.of(new TokenNftTransfer(
                                TokenId.fromString("4.4.4"),
                                AccountId.fromString("1.2.3"),
                                AccountId.fromString("3.2.1"),
                                4L,
                                true))),
                ScheduleId.fromString("3.3.3"),
                List.of(new AssessedCustomFee(
                        4L,
                        TokenId.fromString("4.5.6"),
                        AccountId.fromString("8.6.5"),
                        List.of(AccountId.fromString("3.3.3")))),
                List.of(new TokenAssociation(TokenId.fromString("5.4.3"), AccountId.fromString("8.7.6"))),
                PrivateKey.fromStringECDSA("8776c6b831a1b61ac10dac0304a2843de4716f54b1919bb91a2685d0fe3f3048")
                        .getPublicKey(),
                new ArrayList<>(),
                new ArrayList<>(),
                time,
                ByteString.copyFrom("Some hash", StandardCharsets.UTF_8),
                List.of(new Transfer(AccountId.fromString("1.2.3"), Hbar.from(8))),
                prngBytes,
                prngNumber,
                ByteString.copyFrom("0x00", StandardCharsets.UTF_8),
                List.of(
                        new PendingAirdropRecord(
                                new PendingAirdropId(
                                        AccountId.fromString("0.0.123"),
                                        AccountId.fromString("0.0.124"),
                                        NftId.fromString("0.0.5005/1234")),
                                123),
                        new PendingAirdropRecord(
                                new PendingAirdropId(
                                        AccountId.fromString("0.0.123"),
                                        AccountId.fromString("0.0.124"),
                                        TokenId.fromString("0.0.12345")),
                                123)),
                1000L);
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalRecord = spawnRecordExample(ByteString.copyFrom("very random bytes", StandardCharsets.UTF_8), null);
        byte[] recordBytes = originalRecord.toBytes();
        var copyRecord = TransactionRecord.fromBytes(recordBytes);
        assertThat(copyRecord.toString()).isEqualTo(originalRecord.toString());
        SnapshotMatcher.expect(originalRecord.toString()).toMatchSnapshot();
    }

    @Test
    void shouldSerialize2() throws Exception {
        var originalRecord = spawnRecordExample(null, 4 /* chosen by fair dice roll.  Guaranteed to be random */);
        byte[] recordBytes = originalRecord.toBytes();
        var copyRecord = TransactionRecord.fromBytes(recordBytes);
        assertThat(copyRecord.toString()).isEqualTo(originalRecord.toString());
        SnapshotMatcher.expect(originalRecord.toString()).toMatchSnapshot();
    }
}
