// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AllowancesTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    TokenAllowance spawnTokenAllowance() {
        return new TokenAllowance(
                TokenId.fromString("1.2.3"), AccountId.fromString("4.5.6"), AccountId.fromString("5.5.5"), 777);
    }

    TokenNftAllowance spawnNftAllowance() {
        List<Long> serials = new ArrayList<>();
        serials.add(123L);
        serials.add(456L);
        return new TokenNftAllowance(
                TokenId.fromString("1.1.1"),
                AccountId.fromString("2.2.2"),
                AccountId.fromString("3.3.3"),
                null,
                serials,
                null);
    }

    TokenNftAllowance spawnAllNftAllowance() {
        return new TokenNftAllowance(
                TokenId.fromString("1.1.1"),
                AccountId.fromString("2.2.2"),
                AccountId.fromString("3.3.3"),
                null,
                Collections.emptyList(),
                true);
    }

    HbarAllowance spawnHbarAllowance() {
        return new HbarAllowance(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2"), new Hbar(3));
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(
                        spawnHbarAllowance().toString(),
                        spawnTokenAllowance().toString(),
                        spawnNftAllowance().toString(),
                        spawnAllNftAllowance().toString())
                .toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws InvalidProtocolBufferException {
        var hbar1 = spawnHbarAllowance();
        var token1 = spawnTokenAllowance();
        var nft1 = spawnNftAllowance();
        var allNft1 = spawnAllNftAllowance();
        var hbar2 = HbarAllowance.fromBytes(hbar1.toBytes());
        var token2 = TokenAllowance.fromBytes(token1.toBytes());
        var nft2 = TokenNftAllowance.fromBytes(nft1.toBytes());
        var allNft2 = TokenNftAllowance.fromBytes(allNft1.toBytes());
        assertThat(hbar2.toString()).isEqualTo(hbar1.toString());
        assertThat(token2.toString()).isEqualTo(token1.toString());
        assertThat(nft2.toString()).isEqualTo(nft1.toString());
        assertThat(allNft2.toString()).isEqualTo(allNft1.toString());
    }
}
