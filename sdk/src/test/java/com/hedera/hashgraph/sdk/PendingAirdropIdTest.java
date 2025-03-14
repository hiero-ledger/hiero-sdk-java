// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PendingAirdropIdTest {
    private AccountId sender;
    private AccountId receiver;
    private TokenId tokenId;
    private NftId nftId;

    @BeforeEach
    void setUp() {
        sender = new AccountId(0, 0, 1001);
        receiver = new AccountId(0, 0, 1002);
        tokenId = new TokenId(0, 0, 1003);
        nftId = new NftId(new TokenId(0, 0, 1004), 1);
    }

    @Test
    void testConstructorWithTokenId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(sender, receiver, tokenId);

        assertEquals(sender, pendingAirdropId.getSender());
        assertEquals(receiver, pendingAirdropId.getReceiver());
        assertEquals(tokenId, pendingAirdropId.getTokenId());
        assertNull(pendingAirdropId.getNftId());
    }

    @Test
    void testConstructorWithNftId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(sender, receiver, nftId);

        assertEquals(sender, pendingAirdropId.getSender());
        assertEquals(receiver, pendingAirdropId.getReceiver());
        assertEquals(nftId, pendingAirdropId.getNftId());
        assertNull(pendingAirdropId.getTokenId());
    }

    @Test
    void testSetSender() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId();
        pendingAirdropId.setSender(sender);

        assertEquals(sender, pendingAirdropId.getSender());
    }

    @Test
    void testSetReceiver() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId();
        pendingAirdropId.setReceiver(receiver);

        assertEquals(receiver, pendingAirdropId.getReceiver());
    }

    @Test
    void testSetTokenId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId();
        pendingAirdropId.setTokenId(tokenId);

        assertEquals(tokenId, pendingAirdropId.getTokenId());
    }

    @Test
    void testSetNftId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId();
        pendingAirdropId.setNftId(nftId);

        assertEquals(nftId, pendingAirdropId.getNftId());
    }

    @Test
    void testToProtobufWithTokenId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(sender, receiver, tokenId);
        com.hedera.hashgraph.sdk.proto.PendingAirdropId proto = pendingAirdropId.toProtobuf();

        assertNotNull(proto);
        assertEquals(sender.toProtobuf(), proto.getSenderId());
        assertEquals(receiver.toProtobuf(), proto.getReceiverId());
        assertEquals(tokenId.toProtobuf(), proto.getFungibleTokenType());
    }

    @Test
    void testToProtobufWithNftId() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(sender, receiver, nftId);
        com.hedera.hashgraph.sdk.proto.PendingAirdropId proto = pendingAirdropId.toProtobuf();

        assertNotNull(proto);
        assertEquals(sender.toProtobuf(), proto.getSenderId());
        assertEquals(receiver.toProtobuf(), proto.getReceiverId());
        assertEquals(nftId.toProtobuf(), proto.getNonFungibleToken());
    }

    @Test
    void testFromProtobufWithTokenId() {
        com.hedera.hashgraph.sdk.proto.PendingAirdropId proto =
                com.hedera.hashgraph.sdk.proto.PendingAirdropId.newBuilder()
                        .setSenderId(sender.toProtobuf())
                        .setReceiverId(receiver.toProtobuf())
                        .setFungibleTokenType(tokenId.toProtobuf())
                        .build();

        PendingAirdropId pendingAirdropId = PendingAirdropId.fromProtobuf(proto);

        assertNotNull(pendingAirdropId);
        assertEquals(sender, pendingAirdropId.getSender());
        assertEquals(receiver, pendingAirdropId.getReceiver());
        assertEquals(tokenId, pendingAirdropId.getTokenId());
        assertNull(pendingAirdropId.getNftId());
    }

    @Test
    void testFromProtobufWithNftId() {
        com.hedera.hashgraph.sdk.proto.PendingAirdropId proto =
                com.hedera.hashgraph.sdk.proto.PendingAirdropId.newBuilder()
                        .setSenderId(sender.toProtobuf())
                        .setReceiverId(receiver.toProtobuf())
                        .setNonFungibleToken(nftId.toProtobuf())
                        .build();

        PendingAirdropId pendingAirdropId = PendingAirdropId.fromProtobuf(proto);

        assertNotNull(pendingAirdropId);
        assertEquals(sender, pendingAirdropId.getSender());
        assertEquals(receiver, pendingAirdropId.getReceiver());
        assertEquals(nftId, pendingAirdropId.getNftId());
        assertNull(pendingAirdropId.getTokenId());
    }

    @Test
    void testToString() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(sender, receiver, tokenId);
        String result = pendingAirdropId.toString();

        assertTrue(result.contains("sender"));
        assertTrue(result.contains("receiver"));
        assertTrue(result.contains("tokenId"));
        assertTrue(result.contains("nftId"));
    }
}
