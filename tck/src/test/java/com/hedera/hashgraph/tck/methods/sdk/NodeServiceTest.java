// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.NodeAddressBook;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.NodeAddress;
import org.junit.jupiter.api.Test;

class NodeServiceTest {

    // SHA-384 hash of node 0's gRPC certificate in the testnet address book (file 0.0.102).
    private static final String TESTNET_NODE_0_CERT_HASH =
            "a171e3ba83476747aeb2e2ac4d0e115caaab918203b0dfe1cdeab443438fc289abc8ba8a6aff83db5f1b334046da88c8";

    private static com.hedera.hashgraph.sdk.NodeAddress parseNode(NodeAddress.Builder node) throws Exception {
        var book = com.hedera.hashgraph.sdk.proto.NodeAddressBook.newBuilder()
                .addNodeAddress(node.setNodeAccountId(AccountID.newBuilder().setAccountNum(3)))
                .build();
        return NodeAddressBook.fromBytes(book.toByteString()).getNodeAddresses().get(0);
    }

    @Test
    void nodeCertHashIsTheHashStoredInTheAddressBook() throws Exception {
        // The address book stores the hash as hex characters encoded as UTF-8 bytes.
        var address = parseNode(NodeAddress.newBuilder()
                .setNodeId(0)
                .setNodeCertHash(ByteString.copyFromUtf8(TESTNET_NODE_0_CERT_HASH)));
        assertEquals(96, address.getCertHash().size());

        var nodeCertHash = NodeService.toNodeAddress(address).nodeCertHash();

        assertEquals(TESTNET_NODE_0_CERT_HASH, nodeCertHash);
        assertEquals(96, nodeCertHash.length());
        assertTrue(nodeCertHash.matches("^[0-9a-fA-F]{96}$"));
    }

    @Test
    void nodeCertHashWithoutHashIsEmpty() throws Exception {
        var address = parseNode(NodeAddress.newBuilder().setNodeId(1));

        assertEquals("", NodeService.toNodeAddress(address).nodeCertHash());
    }

    @Test
    void nodeCertHashWithNullHashIsEmpty() throws Exception {
        var address = parseNode(NodeAddress.newBuilder().setNodeId(2));
        address.setCertHash(null);

        assertEquals("", NodeService.toNodeAddress(address).nodeCertHash());
    }
}
