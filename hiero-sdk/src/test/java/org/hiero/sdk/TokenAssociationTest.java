// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenAssociationTest {
    private static final AccountId testAccountId = AccountId.fromString("4.2.0");
    private static final TokenId testTokenId = TokenId.fromString("0.6.9");

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    TokenAssociation spawnTokenAssociationExample() {
        return new TokenAssociation(TokenId.fromString("1.2.3"), AccountId.fromString("4.5.6"));
    }

    @Test
    void shouldSerializeAccount() throws Exception {
        var originalTokenAssociation = spawnTokenAssociationExample();
        byte[] tokenAssociationBytes = originalTokenAssociation.toBytes();
        var copyTokenAssociation = TokenAssociation.fromBytes(tokenAssociationBytes);
        assertThat(copyTokenAssociation.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(originalTokenAssociation.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalTokenAssociation.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void fromProtobuf() {
        var tokenAssociationProtobuf = new TokenAssociation(testTokenId, testAccountId).toProtobuf();
        var tokenAssociation = TokenAssociation.fromProtobuf(tokenAssociationProtobuf);

        assertThat(tokenAssociation.accountId).isEqualTo(testAccountId);
        assertThat(tokenAssociation.tokenId).isEqualTo(testTokenId);
    }

    @Test
    void toProtobuf() {
        var tokenAssociationProtobuf = new TokenAssociation(testTokenId, testAccountId).toProtobuf();

        assertTrue(tokenAssociationProtobuf.hasAccountId());
        assertThat(tokenAssociationProtobuf.getAccountId().getShardNum()).isEqualTo(testAccountId.shard);
        assertThat(tokenAssociationProtobuf.getAccountId().getRealmNum()).isEqualTo(testAccountId.realm);
        assertThat(tokenAssociationProtobuf.getAccountId().getAccountNum()).isEqualTo(testAccountId.num);
        assertTrue(tokenAssociationProtobuf.hasTokenId());
        assertThat(tokenAssociationProtobuf.getTokenId().getShardNum()).isEqualTo(testTokenId.shard);
        assertThat(tokenAssociationProtobuf.getTokenId().getRealmNum()).isEqualTo(testTokenId.realm);
        assertThat(tokenAssociationProtobuf.getTokenId().getTokenNum()).isEqualTo(testTokenId.num);
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        var tokenAssociationProtobuf = new TokenAssociation(testTokenId, testAccountId).toProtobuf();

        var tokenAssociation = TokenAssociation.fromBytes(tokenAssociationProtobuf.toByteArray());

        assertThat(tokenAssociation.accountId).isEqualTo(testAccountId);
        assertThat(tokenAssociation.tokenId).isEqualTo(testTokenId);
    }

    @Test
    void toBytes() {
        var tokenAssociation = new TokenAssociation(testTokenId, testAccountId);
        var bytes = tokenAssociation.toBytes();
        assertThat(bytes).isEqualTo(tokenAssociation.toProtobuf().toByteArray());
    }
}
