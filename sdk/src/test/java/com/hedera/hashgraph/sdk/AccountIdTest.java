// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AccountIdTest {

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
        SnapshotMatcher.expect(AccountId.fromString("0.0.5005").toString()).toMatchSnapshot();
    }

    @Test
    void fromStringWithChecksumOnMainnet() {
        SnapshotMatcher.expect(AccountId.fromString("0.0.123-vfmkw").toStringWithChecksum(mainnetClient))
                .toMatchSnapshot();
    }

    @Test
    void fromStringWithChecksumOnTestnet() {
        SnapshotMatcher.expect(AccountId.fromString("0.0.123-esxsf").toStringWithChecksum(testnetClient))
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddressWhereShardAndRealmDifferentFromNull() {
        var acId = AccountId.fromString("1.1.123");
        var string = acId.toSolidityAddress();
        var accountIdFromSolidityAddress = AccountId.fromSolidityAddress(string);
        assertThat(acId).isEqualTo(accountIdFromSolidityAddress);
    }

    @Test
    void fromStringWithChecksumOnPreviewnet() {
        SnapshotMatcher.expect(AccountId.fromString("0.0.123-ogizo").toStringWithChecksum(previewnetClient))
                .toMatchSnapshot();
    }

    @Test
    void goodChecksumOnMainnet() throws BadEntityIdException {
        AccountId.fromString("0.0.123-vfmkw").validateChecksum(mainnetClient);
    }

    @Test
    void goodChecksumOnTestnet() throws BadEntityIdException {
        AccountId.fromString("0.0.123-esxsf").validateChecksum(testnetClient);
    }

    @Test
    void goodChecksumOnPreviewnet() throws BadEntityIdException {
        AccountId.fromString("0.0.123-ogizo").validateChecksum(previewnetClient);
    }

    @Test
    void badChecksumOnPreviewnet() {
        assertThatExceptionOfType(BadEntityIdException.class).isThrownBy(() -> {
            AccountId.fromString("0.0.123-ntjli").validateChecksum(previewnetClient);
        });
    }

    @Test
    void malformedIdString() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString("0.0.");
        });
    }

    @Test
    void malformedIdChecksum() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString("0.0.123-ntjl");
        });
    }

    @Test
    void malformedIdChecksum2() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString("0.0.123-ntjl1");
        });
    }

    @Test
    void malformedAliasKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString(
                    "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf777");
        });
    }

    @Test
    void malformedAliasKey2() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString(
                    "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf777g");
        });
    }

    @Test
    void malformedAliasKey3() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            AccountId.fromString(
                    "0.0.303a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777");
        });
    }

    @Test
    void fromStringWithAliasKey() {
        SnapshotMatcher.expect(AccountId.fromString(
                                "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromStringWithEvmAddress() {
        SnapshotMatcher.expect(AccountId.fromString("0.0.302a300506032b6570032100114e6abc371b82da")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddress() {
        SnapshotMatcher.expect(AccountId.fromSolidityAddress("000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddressWith0x() {
        SnapshotMatcher.expect(AccountId.fromSolidityAddress("0x000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        Hex.toHexString(new AccountId(0, 0, 5005).toProtobuf().toByteArray()))
                .toMatchSnapshot();
    }

    @Test
    void toBytesAlias() {
        SnapshotMatcher.expect(Hex.toHexString(AccountId.fromString(
                                "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777")
                        .toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void toBytesEvmAddress() {
        SnapshotMatcher.expect(Hex.toHexString(AccountId.fromString("0.0.302a300506032b6570032100114e6abc371b82da")
                        .toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        AccountId.fromBytes(new AccountId(0, 0, 5005).toBytes()).toString())
                .toMatchSnapshot();
    }

    @Test
    void toFromProtobuf() {
        var id1 = new AccountId(0, 0, 5005);
        var id2 = AccountId.fromProtobuf(id1.toProtobuf());
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    void fromBytesAlias() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(AccountId.fromBytes(AccountId.fromString(
                                        "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777")
                                .toBytes())
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toFromProtobufAliasKey() {
        var id1 = AccountId.fromString(
                "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777");
        var id2 = AccountId.fromProtobuf(id1.toProtobuf());
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    void toFromProtobufEcdsaAliasKey() {
        var id1 = AccountId.fromString(
                "0.0.302d300706052b8104000a032200035d348292bbb8b511fdbe24e3217ec099944b4728999d337f9a025f4193324525");
        var id2 = AccountId.fromProtobuf(id1.toProtobuf());
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    void fromBytesEvmAddress() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(AccountId.fromBytes(AccountId.fromString("0.0.302a300506032b6570032100114e6abc371b82da")
                                .toBytes())
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toFromProtobufEvmAddress() {
        var id1 = AccountId.fromString("0.0.302a300506032b6570032100114e6abc371b82da");
        var id2 = AccountId.fromProtobuf(id1.toProtobuf());
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    void toFromProtobufRawEvmAddress() {
        var id1 = AccountId.fromString("302a300506032b6570032100114e6abc371b82da");
        var id2 = AccountId.fromProtobuf(id1.toProtobuf());
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    void toSolidityAddress() {
        SnapshotMatcher.expect(new AccountId(0, 0, 5005).toSolidityAddress()).toMatchSnapshot();
    }

    @Test
    void fromEvmAddress() {
        String evmAddress = "302a300506032b6570032100114e6abc371b82da";
        var id = AccountId.fromEvmAddress(evmAddress, 5, 9);

        assertThat(id.evmAddress).hasToString(evmAddress);
        assertThat(id.shard).isEqualTo(5);
        assertThat(id.realm).isEqualTo(9);
    }

    @Test
    void fromEvmAddressWithPrefix() {
        String evmAddressString = "302a300506032b6570032100114e6abc371b82da";
        EvmAddress evmAddress = EvmAddress.fromString(evmAddressString);
        var id1 = AccountId.fromEvmAddress(evmAddress, 0, 0);
        var id2 = AccountId.fromEvmAddress("0x" + evmAddressString, 0, 0);
        assertThat(id2).isEqualTo(id1);
    }
}
