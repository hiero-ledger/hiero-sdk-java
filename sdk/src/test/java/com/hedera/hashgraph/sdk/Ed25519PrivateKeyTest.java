// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Ed25519PrivateKeyTest {
    private static final String TEST_KEY_STR =
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10";
    private static final String TEST_KEY_STR_RAW = "db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10";
    private static final String TEST_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n"
            + "MC4CAQAwBQYDK2VwBCIEINtIS4KOZLLY8SzjwKDpOguMznrxu485yXcyOUSCU44Q\n"
            + "-----END PRIVATE KEY-----\n";

    // generated by hedera-sdk-js, not used anywhere
    private static final String MNEMONIC_STRING =
            "inmate flip alley wear offer often piece magnet surge toddler submit right radio absent pear floor belt raven price stove replace reduce plate home";
    private static final String MNEMONIC_PRIVATE_KEY =
            "302e020100300506032b657004220420853f15aecd22706b105da1d709b4ac05b4906170c2b9c7495dff9af49e1391da";

    private static final String MNEMONIC_LEGACY_STRING =
            "jolly kidnap tom lawn drunk chick optic lust mutter mole bride galley dense member sage neural widow decide curb aboard margin manure";
    private static final String MNEMONIC_LEGACY_PRIVATE_KEY =
            "302e020100300506032b657004220420882a565ad8cb45643892b5366c1ee1c1ef4a730c5ce821a219ff49b6bf173ddf";

    // backup phrase generated by the iOS wallet, not used anywhere
    private static final String IOS_MNEMONIC_STRING =
            "tiny denial casual grass skull spare awkward indoor ethics dash enough flavor good daughter early hard rug staff capable swallow raise flavor empty angle";

    // private key for "default account", should be index 0
    private static final String IOS_DEFAULT_PRIVATE_KEY =
            "5f66a51931e8c99089472e0d70516b6272b94dd772b967f8221e1077f966dbda2b60cf7ee8cf10ecd5a076bffad9a7c7b97df370ad758c0f1dd4ef738e04ceb6";

    // backup phrase generated by the Android wallet, also not used anywhere
    private static final String ANDROID_MNEMONIC_STRING =
            "ramp april job flavor surround pyramid fish sea good know blame gate village viable include mixed term draft among monitor swear swing novel track";
    // private key for "default account", should be index 0
    private static final String ANDROID_DEFAULT_PRIVATE_KEY =
            "c284c25b3a1458b59423bc289e83703b125c8eefec4d5aa1b393c2beb9f2bae66188a344ba75c43918ab12fa2ea4a92960eca029a2320d8c6a1c3b94e06c9985";

    private static final String PEM_PASSPHRASE = "this is a passphrase";

    private static final String TEST_VECTOR_PEM_PASSPHRASE = "asdasd123";

    /*
       # enter passphrase "this is a passphrase"
       echo '302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10' \
       | xxd -r -p \
       | openssl pkey -inform der -aes-128-cbc
    */
    private static final String ENCRYPTED_PEM = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
            + "MIGbMFcGCSqGSIb3DQEFDTBKMCkGCSqGSIb3DQEFDDAcBAi8WY7Gy2tThQICCAAw\n"
            + "DAYIKoZIhvcNAgkFADAdBglghkgBZQMEAQIEEOq46NPss58chbjUn20NoK0EQG1x\n"
            + "R88hIXcWDOECttPTNlMXWJt7Wufm1YwBibrxmCq1QykIyTYhy1TZMyxyPxlYW6aV\n"
            + "9hlo4YEh3uEaCmfJzWM=\n"
            + "-----END ENCRYPTED PRIVATE KEY-----\n";
    private static final String MESSAGE_STR = "This is a message about the world.";
    private static final byte[] MESSAGE_BYTES = MESSAGE_STR.getBytes(StandardCharsets.UTF_8);
    private static final String SIG_STR =
            "73bea53f31ca9c42a422ecb7516ec08d0bbd1a6bfd630ccf10ec1872454814d29f4a8011129cd007eab544af01a75f508285b591e5bed24b68f927751e49e30e";

    @SuppressWarnings("unused")
    private static Stream<String> privKeyStrings() {
        return Stream.of(
                TEST_KEY_STR,
                // raw hex (concatenated private + public key)
                TEST_KEY_STR_RAW + "e0c8ec2758a5879ffac226a13c0c516b799e72e35141a0dd828f94d37988a4b7",
                // raw hex (just private key)
                TEST_KEY_STR_RAW);
    }

    @Test
    @DisplayName("private key generates successfully")
    void keyGenerates() {
        PrivateKey key = PrivateKey.generateED25519();

        assertThat(key).isNotNull();
        assertThat(key.toBytes()).isNotNull();

        // we generate the chain code at the same time
        assertThat(key.isDerivable()).isTrue();
    }

    @Test
    @DisplayName("private key can be recovered from bytes")
    void keySerialization() {
        PrivateKey key1 = PrivateKey.generateED25519();
        byte[] key1Bytes = key1.toBytes();
        PrivateKey key2 = PrivateKey.fromBytes(key1Bytes);
        byte[] key2Bytes = key2.toBytes();

        assertThat(key2Bytes).containsExactly(key1Bytes);
    }

    @Test
    @DisplayName("private key can be recovered from raw bytes")
    void keySerialization2() {
        PrivateKey key1 = PrivateKey.generateED25519();
        byte[] key1Bytes = key1.toBytesRaw();
        PrivateKey key2 = PrivateKey.fromBytesED25519(key1Bytes);
        byte[] key2Bytes = key2.toBytesRaw();
        PrivateKey key3 = PrivateKey.fromBytes(key1Bytes);
        byte[] key3Bytes = key3.toBytesRaw();

        assertThat(key2Bytes).containsExactly(key1Bytes);
        assertThat(key3Bytes).containsExactly(key1Bytes);
    }

    @Test
    @DisplayName("private key can be recovered from DER bytes")
    void keySerialization3() {
        PrivateKey key1 = PrivateKey.generateED25519();
        byte[] key1Bytes = key1.toBytesDER();
        PrivateKey key2 = PrivateKey.fromBytesDER(key1Bytes);
        byte[] key2Bytes = key2.toBytesDER();
        PrivateKey key3 = PrivateKey.fromBytes(key1Bytes);
        byte[] key3Bytes = key3.toBytesDER();

        assertThat(key2Bytes).containsExactly(key1Bytes);
        assertThat(key3Bytes).containsExactly(key1Bytes);
    }

    @Test
    @DisplayName("private key can be recovered from string")
    void keyStringification() {
        PrivateKey key1 = PrivateKey.generateED25519();
        String key1String = key1.toString();
        PrivateKey key2 = PrivateKey.fromString(key1String);
        String key2String = key2.toString();

        assertThat(key2String).isEqualTo(key1String);
    }

    @Test
    @DisplayName("private key can be recovered from raw string")
    void keyStringification2() {
        PrivateKey key1 = PrivateKey.generateED25519();
        String key1String = key1.toStringRaw();
        PrivateKey key2 = PrivateKey.fromStringED25519(key1String);
        String key2String = key2.toStringRaw();
        PrivateKey key3 = PrivateKey.fromString(key1String);
        String key3String = key3.toStringRaw();

        assertThat(key2String).isEqualTo(key1String);
        assertThat(key3String).isEqualTo(key1String);
    }

    @Test
    @DisplayName("private key can be recovered from DER string")
    void keyStringification3() {
        PrivateKey key1 = PrivateKey.generateED25519();
        String key1String = key1.toStringDER();
        PrivateKey key2 = PrivateKey.fromStringDER(key1String);
        String key2String = key2.toStringDER();
        PrivateKey key3 = PrivateKey.fromString(key1String);
        String key3String = key3.toStringDER();

        assertThat(key2String).isEqualTo(key1String);
        assertThat(key3String).isEqualTo(key1String);
    }

    @ParameterizedTest
    @DisplayName("private key can be recovered from external string")
    @ValueSource(
            strings = {
                TEST_KEY_STR,
                // raw hex (concatenated private + public key)
                TEST_KEY_STR_RAW + "e0c8ec2758a5879ffac226a13c0c516b799e72e35141a0dd828f94d37988a4b7",
                // raw hex (just private key)
                TEST_KEY_STR_RAW
            })
    void externalKeyDeserialize(String keyStr) {
        PrivateKey key = PrivateKey.fromString(keyStr);
        assertThat(key).isNotNull();
        // the above are all the same key
        assertThat(key.toString()).isEqualTo(TEST_KEY_STR);
        assertThat(key.toStringDER()).isEqualTo(TEST_KEY_STR);
        assertThat(key.toStringRaw()).isEqualTo(TEST_KEY_STR_RAW);
    }

    @Test
    @DisplayName("private key can be encoded to a string")
    void keyToString() {
        PrivateKey key = PrivateKey.fromString(TEST_KEY_STR);

        assertThat(key).isNotNull();
        assertThat(key.toString()).isEqualTo(TEST_KEY_STR);
    }

    @Test
    @DisplayName("private key can be decoded from a PEM file")
    void keyFromPem() throws IOException {
        StringReader stringReader = new StringReader(TEST_KEY_PEM);
        PrivateKey privateKey = PrivateKey.readPem(stringReader);

        assertThat(privateKey.toString()).isEqualTo(TEST_KEY_STR);
    }

    @Test
    @DisplayName("private key can be recovered from a mnemonic")
    void keyFromMnemonic() throws Exception {
        Mnemonic mnemonic = Mnemonic.fromString(MNEMONIC_STRING);
        PrivateKey key = PrivateKey.fromMnemonic(mnemonic);
        PrivateKey key2 = PrivateKey.fromString(MNEMONIC_PRIVATE_KEY);
        assertThat(key2.toBytes()).containsExactly(key.toBytes());
    }

    @Test
    @DisplayName("validate 12 word generated mnemonic")
    void validateGenerated12() throws Exception {
        Mnemonic mnemonic = Mnemonic.generate12();
        Mnemonic.fromString(mnemonic.toString());
    }

    @Test
    @DisplayName("validate legacy mnemonic")
    void validateLegacyMnemonic() throws Exception {
        Mnemonic mnemonic = Mnemonic.fromString(MNEMONIC_LEGACY_STRING);
        PrivateKey key = mnemonic.toLegacyPrivateKey();
        assertThat(key.legacyDerive(-1).toString()).isEqualTo(MNEMONIC_LEGACY_PRIVATE_KEY);
    }

    @Test
    @DisplayName("validate 24 word generated mnemonic")
    void validateGenerated24() throws Exception {
        Mnemonic mnemonic = Mnemonic.generate24();
        Mnemonic.fromString(mnemonic.toString());
    }

    @Test
    @DisplayName("derived key matches that of the mobile wallets")
    void deriveKeyIndex0() throws Exception {
        Mnemonic iosMnemonic = Mnemonic.fromString(IOS_MNEMONIC_STRING);
        PrivateKey iosKey = PrivateKey.fromMnemonic(iosMnemonic);

        PrivateKey iosDerivedKey = iosKey.derive(0);
        PrivateKey iosExpectedKey = PrivateKey.fromString(IOS_DEFAULT_PRIVATE_KEY);

        assertThat(iosDerivedKey.toBytes()).containsExactly(iosExpectedKey.toBytes());

        Mnemonic androidMnemonic = Mnemonic.fromString(ANDROID_MNEMONIC_STRING);
        PrivateKey androidKey = PrivateKey.fromMnemonic(androidMnemonic);

        PrivateKey androidDerivedKey = androidKey.derive(0);
        PrivateKey androidExpectedKey = PrivateKey.fromString(ANDROID_DEFAULT_PRIVATE_KEY);

        assertThat(androidDerivedKey.toBytes()).containsExactly(androidExpectedKey.toBytes());
    }

    @Test
    @DisplayName("generated mnemonic24 can be turned into a working private key")
    void keyFromGeneratedMnemonic24() {
        Mnemonic mnemonic = Mnemonic.generate24();
        PrivateKey privateKey = PrivateKey.fromMnemonic(mnemonic);

        byte[] messageToSign = "this is a test message".getBytes(StandardCharsets.UTF_8);

        byte[] signature = privateKey.sign(messageToSign);

        Assertions.assertThat(Ed25519.verify(
                        signature, 0, privateKey.getPublicKey().toBytes(), 0, messageToSign, 0, messageToSign.length))
                .isTrue();
    }

    @Test
    @DisplayName("generated mnemonic12 can be turned into a working private key")
    void keyFromGeneratedMnemonic12() {
        Mnemonic mnemonic = Mnemonic.generate12();
        PrivateKey privateKey = PrivateKey.fromMnemonic(mnemonic);

        byte[] messageToSign = "this is a test message".getBytes(StandardCharsets.UTF_8);

        byte[] signature = privateKey.sign(messageToSign);

        Assertions.assertThat(Ed25519.verify(
                        signature, 0, privateKey.getPublicKey().toBytes(), 0, messageToSign, 0, messageToSign.length))
                .isTrue();
    }

    @Test
    @DisplayName("fromPem() with passphrase produces same key")
    void keyFromEncryptedPem() throws IOException {
        PrivateKey privateKey = PrivateKey.fromPem(ENCRYPTED_PEM, PEM_PASSPHRASE);
        assertThat(privateKey.toString()).isEqualTo(TEST_KEY_STR);
    }

    @Test
    @DisplayName("fromPem() with encrypted key without a passphrase throws useful error")
    void errorKeyFromEncryptedPemNoPassphrase() {
        assertThatExceptionOfType(BadKeyException.class)
                .isThrownBy(() -> PrivateKey.fromPem(ENCRYPTED_PEM))
                .satisfies(error -> assertThat(error.getMessage())
                        .isEqualTo("PEM file contained an encrypted private key but no passphrase was given"));
    }

    @ParameterizedTest
    @DisplayName("reproducible signature can be computed")
    @ValueSource(
            strings = {
                TEST_KEY_STR,
                // raw hex (concatenated private + public key)
                TEST_KEY_STR_RAW + "e0c8ec2758a5879ffac226a13c0c516b799e72e35141a0dd828f94d37988a4b7",
                // raw hex (just private key)
                TEST_KEY_STR_RAW
            })
    void reproducibleSignature(String keyStr) {
        PrivateKey key = PrivateKey.fromString(keyStr);
        byte[] signature = key.sign(MESSAGE_BYTES);

        assertThat(Hex.toHexString(signature)).isEqualTo(SIG_STR);
    }

    @Test
    @DisplayName("private key is is ECDSA")
    void keyIsECDSA() {
        PrivateKey key = PrivateKey.generateED25519();

        assertThat(key.isED25519()).isTrue();
    }

    @Test
    @DisplayName("private key is is not Ed25519")
    void keyIsNotEd25519() {
        PrivateKey key = PrivateKey.generateED25519();

        assertThat(key.isECDSA()).isFalse();
    }

    // TODO: replace with HexFormat.of().parseHex when the required Java version is 17
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    @DisplayName("SLIP10 test vector 1")
    void slip10TestVector1() {
        // https://github.com/satoshilabs/slips/blob/master/slip-0010.md#test-vector-1-for-ed25519
        final String CHAIN_CODE1 = "90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb";
        final String PRIVATE_KEY1 = "2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7";
        final String PUBLIC_KEY1 = "00a4b2856bfec510abab89753fac1ac0e1112364e7d250545963f135f2a33188ed";

        final String CHAIN_CODE2 = "8b59aa11380b624e81507a27fedda59fea6d0b779a778918a2fd3590e16e9c69";
        final String PRIVATE_KEY2 = "68e0fe46dfb67e368c75379acec591dad19df3cde26e63b93a8e704f1dade7a3";
        final String PUBLIC_KEY2 = "008c8a13df77a28f3445213a0f432fde644acaa215fc72dcdf300d5efaa85d350c";

        final String CHAIN_CODE3 = "a320425f77d1b5c2505a6b1b27382b37368ee640e3557c315416801243552f14";
        final String PRIVATE_KEY3 = "b1d0bad404bf35da785a64ca1ac54b2617211d2777696fbffaf208f746ae84f2";
        final String PUBLIC_KEY3 = "001932a5270f335bed617d5b935c80aedb1a35bd9fc1e31acafd5372c30f5c1187";

        final String CHAIN_CODE4 = "2e69929e00b5ab250f49c3fb1c12f252de4fed2c1db88387094a0f8c4c9ccd6c";
        final String PRIVATE_KEY4 = "92a5b23c0b8a99e37d07df3fb9966917f5d06e02ddbd909c7e184371463e9fc9";
        final String PUBLIC_KEY4 = "00ae98736566d30ed0e9d2f4486a64bc95740d89c7db33f52121f8ea8f76ff0fc1";

        final String CHAIN_CODE5 = "8f6d87f93d750e0efccda017d662a1b31a266e4a6f5993b15f5c1f07f74dd5cc";
        final String PRIVATE_KEY5 = "30d1dc7e5fc04c31219ab25a27ae00b50f6fd66622f6e9c913253d6511d1e662";
        final String PUBLIC_KEY5 = "008abae2d66361c879b900d204ad2cc4984fa2aa344dd7ddc46007329ac76c429c";

        final String CHAIN_CODE6 = "68789923a0cac2cd5a29172a475fe9e0fb14cd6adb5ad98a3fa70333e7afa230";
        final String PRIVATE_KEY6 = "8f94d394a8e8fd6b1bc2f3f49f5c47e385281d5c17e65324b0f62483e37e8793";
        final String PUBLIC_KEY6 = "003c24da049451555d51a7014a37337aa4e12d41e485abccfa46b47dfb2af54b7a";

        var seed = hexStringToByteArray("000102030405060708090a0b0c0d0e0f");

        // Chain m
        PrivateKey key1 = PrivateKey.fromSeedED25519(seed);
        assertThat(Hex.toHexString(key1.getChainCode().getKey())).isEqualTo(CHAIN_CODE1);
        assertThat(key1.toStringRaw()).isEqualTo(PRIVATE_KEY1);
        assertThat(key1.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY1);

        // Chain m/0'
        PrivateKey key2 = key1.derive(0);
        assertThat(Hex.toHexString(key2.getChainCode().getKey())).isEqualTo(CHAIN_CODE2);
        assertThat(key2.toStringRaw()).isEqualTo(PRIVATE_KEY2);
        assertThat(key2.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY2);

        // Chain m/0'/1'
        PrivateKey key3 = key2.derive(1);
        assertThat(Hex.toHexString(key3.getChainCode().getKey())).isEqualTo(CHAIN_CODE3);
        assertThat(key3.toStringRaw()).isEqualTo(PRIVATE_KEY3);
        assertThat(key3.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY3);

        // Chain m/0'/1'/2'
        PrivateKey key4 = key3.derive(2);
        assertThat(Hex.toHexString(key4.getChainCode().getKey())).isEqualTo(CHAIN_CODE4);
        assertThat(key4.toStringRaw()).isEqualTo(PRIVATE_KEY4);
        assertThat(key4.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY4);

        // Chain m/0'/1'/2'/2'
        PrivateKey key5 = key4.derive(2);
        assertThat(Hex.toHexString(key5.getChainCode().getKey())).isEqualTo(CHAIN_CODE5);
        assertThat(key5.toStringRaw()).isEqualTo(PRIVATE_KEY5);
        assertThat(key5.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY5);

        // Chain m/0'/1'/2'/2'/1000000000'
        PrivateKey key6 = key5.derive(1000000000);
        assertThat(Hex.toHexString(key6.getChainCode().getKey())).isEqualTo(CHAIN_CODE6);
        assertThat(key6.toStringRaw()).isEqualTo(PRIVATE_KEY6);
        assertThat(key6.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY6);
    }

    @Test
    @DisplayName("SLIP10 test vector 2")
    void slip10TestVector2() {
        // https://github.com/satoshilabs/slips/blob/master/slip-0010.md#test-vector-2-for-ed25519
        final String CHAIN_CODE1 = "ef70a74db9c3a5af931b5fe73ed8e1a53464133654fd55e7a66f8570b8e33c3b";
        final String PRIVATE_KEY1 = "171cb88b1b3c1db25add599712e36245d75bc65a1a5c9e18d76f9f2b1eab4012";
        final String PUBLIC_KEY1 = "008fe9693f8fa62a4305a140b9764c5ee01e455963744fe18204b4fb948249308a";

        final String CHAIN_CODE2 = "0b78a3226f915c082bf118f83618a618ab6dec793752624cbeb622acb562862d";
        final String PRIVATE_KEY2 = "1559eb2bbec5790b0c65d8693e4d0875b1747f4970ae8b650486ed7470845635";
        final String PUBLIC_KEY2 = "0086fab68dcb57aa196c77c5f264f215a112c22a912c10d123b0d03c3c28ef1037";

        final String CHAIN_CODE3 = "138f0b2551bcafeca6ff2aa88ba8ed0ed8de070841f0c4ef0165df8181eaad7f";
        final String PRIVATE_KEY3 = "ea4f5bfe8694d8bb74b7b59404632fd5968b774ed545e810de9c32a4fb4192f4";
        final String PUBLIC_KEY3 = "005ba3b9ac6e90e83effcd25ac4e58a1365a9e35a3d3ae5eb07b9e4d90bcf7506d";

        final String CHAIN_CODE4 = "73bd9fff1cfbde33a1b846c27085f711c0fe2d66fd32e139d3ebc28e5a4a6b90";
        final String PRIVATE_KEY4 = "3757c7577170179c7868353ada796c839135b3d30554bbb74a4b1e4a5a58505c";
        final String PUBLIC_KEY4 = "002e66aa57069c86cc18249aecf5cb5a9cebbfd6fadeab056254763874a9352b45";

        final String CHAIN_CODE5 = "0902fe8a29f9140480a00ef244bd183e8a13288e4412d8389d140aac1794825a";
        final String PRIVATE_KEY5 = "5837736c89570de861ebc173b1086da4f505d4adb387c6a1b1342d5e4ac9ec72";
        final String PUBLIC_KEY5 = "00e33c0f7d81d843c572275f287498e8d408654fdf0d1e065b84e2e6f157aab09b";

        final String CHAIN_CODE6 = "5d70af781f3a37b829f0d060924d5e960bdc02e85423494afc0b1a41bbe196d4";
        final String PRIVATE_KEY6 = "551d333177df541ad876a60ea71f00447931c0a9da16f227c11ea080d7391b8d";
        final String PUBLIC_KEY6 = "0047150c75db263559a70d5778bf36abbab30fb061ad69f69ece61a72b0cfa4fc0";

        var seed = hexStringToByteArray(
                "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542");

        // Chain m
        PrivateKey key1 = PrivateKey.fromSeedED25519(seed);
        assertThat(Hex.toHexString(key1.getChainCode().getKey())).isEqualTo(CHAIN_CODE1);
        assertThat(key1.toStringRaw()).isEqualTo(PRIVATE_KEY1);
        assertThat(key1.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY1);

        // Chain m/0'
        PrivateKey key2 = key1.derive(0);
        assertThat(Hex.toHexString(key2.getChainCode().getKey())).isEqualTo(CHAIN_CODE2);
        assertThat(key2.toStringRaw()).isEqualTo(PRIVATE_KEY2);
        assertThat(key2.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY2);

        // Chain m/0'/2147483647'
        PrivateKey key3 = key2.derive(2147483647);
        assertThat(Hex.toHexString(key3.getChainCode().getKey())).isEqualTo(CHAIN_CODE3);
        assertThat(key3.toStringRaw()).isEqualTo(PRIVATE_KEY3);
        assertThat(key3.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY3);

        // Chain m/0'/2147483647'/1'
        PrivateKey key4 = key3.derive(1);
        assertThat(Hex.toHexString(key4.getChainCode().getKey())).isEqualTo(CHAIN_CODE4);
        assertThat(key4.toStringRaw()).isEqualTo(PRIVATE_KEY4);
        assertThat(key4.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY4);

        // Chain m/0'/2147483647'/1'/2147483646'
        PrivateKey key5 = key4.derive(2147483646);
        assertThat(Hex.toHexString(key5.getChainCode().getKey())).isEqualTo(CHAIN_CODE5);
        assertThat(key5.toStringRaw()).isEqualTo(PRIVATE_KEY5);
        assertThat(key5.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY5);

        // Chain m/0'/2147483647'/1'/2147483646'/2'
        PrivateKey key6 = key5.derive(2);
        assertThat(Hex.toHexString(key6.getChainCode().getKey())).isEqualTo(CHAIN_CODE6);
        assertThat(key6.toStringRaw()).isEqualTo(PRIVATE_KEY6);
        assertThat(key6.getPublicKey().toStringRaw()).isSubstringOf(PUBLIC_KEY6);
    }

    @Test
    @DisplayName("PEM import test vectors")
    void PEMImportTestVectors() throws IOException {
        // https://github.com/hashgraph/hedera-sdk-reference/issues/93#issue-1665972122
        var PRIVATE_KEY_PEM1 = "-----BEGIN PRIVATE KEY-----\n"
                + "MC4CAQAwBQYDK2VwBCIEIOgbjaHgEqF7PY0t2dUf2VU0u1MRoKii/fywDlze4lvl\n" + "-----END PRIVATE KEY-----";
        var PRIVATE_KEY1 = "e81b8da1e012a17b3d8d2dd9d51fd95534bb5311a0a8a2fdfcb00e5cdee25be5";
        var PUBLIC_KEY1 = "f7b9aa4a8e4eee94e4277dfe757d8d7cde027e7cd5349b7d8e6ee21c9b9395be";

        var PRIVATE_KEY_PEM2 = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + "MIGbMFcGCSqGSIb3DQEFDTBKMCkGCSqGSIb3DQEFDDAcBAiho4GvPxvL6wICCAAw\n"
                + "DAYIKoZIhvcNAgkFADAdBglghkgBZQMEAQIEEIdsubXR0QvxXGSprqDuDXwEQJZl\n"
                + "OBtwm2p2P7WrWE0OnjGxUe24fWwdrvJUuguFtH3FVWc8C5Jbxgbyxsuzbf+utNL6\n"
                + "0ey+WdbGL06Bw0HGqs8=\n"
                + "-----END ENCRYPTED PRIVATE KEY-----";
        var PRIVATE_KEY2 = "fa0857e963946d5f5e035684c40354d3cd3dcc80c0fb77beac2ef7c4b5271599";
        var PUBLIC_KEY2 = "202af61e141465d4bf2c356d37d18bd026c246bde4eb73258722ad11f790be4e";

        var ed25519PrivateKey1 = PrivateKey.fromPem(PRIVATE_KEY_PEM1);
        assertThat(ed25519PrivateKey1.toStringRaw()).isEqualTo(PRIVATE_KEY1);
        assertThat(ed25519PrivateKey1.getPublicKey().toStringRaw()).isEqualTo(PUBLIC_KEY1);

        var ed25519PrivateKey2 = PrivateKey.fromPem(PRIVATE_KEY_PEM2, TEST_VECTOR_PEM_PASSPHRASE);
        assertThat(ed25519PrivateKey2.toStringRaw()).isEqualTo(PRIVATE_KEY2);
        assertThat(ed25519PrivateKey2.getPublicKey().toStringRaw()).isEqualTo(PUBLIC_KEY2);
    }

    @Test
    @DisplayName("DER import test vectors")
    void DERImportTestVectors() {
        // https://github.com/hashgraph/hedera-sdk-reference/issues/93#issue-1665972122
        var PRIVATE_KEY_DER1 =
                "302e020100300506032b657004220420feb858a4a69600a5eef2d9c76f7fb84fc0b6627f29e0ab17e160f640c267d404";
        var PRIVATE_KEY1 = "feb858a4a69600a5eef2d9c76f7fb84fc0b6627f29e0ab17e160f640c267d404";
        var PUBLIC_KEY1 = "8ccd31b53d1835b467aac795dab19b274dd3b37e3daf12fcec6bc02bac87b53d";

        var ed25519PrivateKey1 = PrivateKey.fromStringDER(PRIVATE_KEY_DER1);
        assertThat(ed25519PrivateKey1.toStringRaw()).isEqualTo(PRIVATE_KEY1);
        assertThat(ed25519PrivateKey1.getPublicKey().toStringRaw()).isEqualTo(PUBLIC_KEY1);
    }
}
