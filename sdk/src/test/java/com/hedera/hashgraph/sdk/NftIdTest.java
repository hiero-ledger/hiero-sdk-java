// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NftIdTest {

    static Client mainnetClient;
    static Client testnetClient;
    static Client previewnetClient;

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
        mainnetClient = Client.forMainnet();
        testnetClient = Client.forTestnet();
        previewnetClient = Client.forPreviewnet();
    }

    @AfterAll
    public static void afterAll() throws TimeoutException {
        mainnetClient.close();
        testnetClient.close();
        previewnetClient.close();
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromString() {
        SnapshotMatcher.expect(NftId.fromString("0.0.5005@1234").toString()).toMatchSnapshot();
    }

    @Test
    void fromString2() {
        SnapshotMatcher.expect(NftId.fromString("0.0.5005/1234").toString()).toMatchSnapshot();
    }

    @Test
    void toFromString() {
        var id1 = NftId.fromString("0.0.5005@1234");
        var id2 = NftId.fromString(id1.toString());
        assertThat(id2.toString()).isEqualTo(id1.toString());
    }

    @Test
    void fromStringWithChecksumOnMainnet() {
        SnapshotMatcher.expect(NftId.fromString("0.0.123-vfmkw/7584").toStringWithChecksum(mainnetClient))
                .toMatchSnapshot();
    }

    @Test
    void fromStringWithChecksumOnTestnet() {
        SnapshotMatcher.expect(NftId.fromString("0.0.123-esxsf@584903").toStringWithChecksum(testnetClient))
                .toMatchSnapshot();
    }

    @Test
    void fromStringWithChecksumOnPreviewnet() {
        SnapshotMatcher.expect(NftId.fromString("0.0.123-ogizo/487302").toStringWithChecksum(previewnetClient))
                .toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new TokenId(0, 0, 5005).nft(4920).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        NftId.fromBytes(new TokenId(0, 0, 5005).nft(574489).toBytes())
                                .toString())
                .toMatchSnapshot();
    }
}
