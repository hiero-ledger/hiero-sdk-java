// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.hiero.sdk.utils.Bip32Utils;

/**
 * BIP-39 24-word mnemonic phrases compatible with the Android and iOS mobile wallets.
 */
public final class Mnemonic {
    // by storing our word list in a SoftReference, the GC is free to evict it at its discretion
    // but the implementation is meant to wait until free space is needed
    @Nullable
    private static SoftReference<List<String>> bip39WordList = null;

    @Nullable
    private static SoftReference<List<String>> legacyWordList = null;

    /**
     * The list of words in this mnemonic.
     */
    public final List<CharSequence> words;

    @Nullable
    private String asString;

    @SuppressWarnings("StaticAssignmentInConstructor")
    private Mnemonic(List<? extends CharSequence> words) {
        this.words = Collections.unmodifiableList(words);
    }

    /**
     * Construct a mnemonic from a 24-word list. {@link Mnemonic#validate()}
     * is called before returning, and it will throw an exception if it
     * does not pass validation. An invalid mnemonic can still create valid
     * Ed25519 private keys, so the exception will contain the mnemonic in case
     * the user wants to ignore the outcome of the validation.
     *
     * @param words the 24-word list that constitutes a mnemonic phrase.
     * @return {@code this}
     * @throws BadMnemonicException if the mnemonic does not pass validation.
     * @see #validate() the function that validates the mnemonic.
     */
    public static Mnemonic fromWords(List<? extends CharSequence> words) throws BadMnemonicException {
        Mnemonic mnemonic = new Mnemonic(words);

        if (words.size() != 22) {
            mnemonic.validate();
        }

        return mnemonic;
    }

    /**
     * Recover a mnemonic from a string, splitting on spaces.
     *
     * @param mnemonicString The string to recover the mnemonic from
     * @return {@code this}
     * @throws BadMnemonicException if the mnemonic does not pass validation.
     */
    public static Mnemonic fromString(String mnemonicString) throws BadMnemonicException {
        String toLowerCase = mnemonicString.toLowerCase();
        return Mnemonic.fromWords(Arrays.asList(toLowerCase.split(" ")));
    }

    /**
     * Returns a new random 24-word mnemonic from the BIP-39 standard English word list.
     *
     * @return {@code this}
     */
    public static Mnemonic generate24() {
        var entropy = new byte[32];
        ThreadLocalSecureRandom.current().nextBytes(entropy);

        return new Mnemonic(entropyToWords(entropy));
    }

    /**
     * Returns a new random 12-word mnemonic from the BIP-39 standard English word list.
     *
     * @return {@code this}
     */
    public static Mnemonic generate12() {
        var entropy = new byte[16];
        ThreadLocalSecureRandom.current().nextBytes(entropy);

        return new Mnemonic(entropyToWords(entropy));
    }

    private static List<String> entropyToWords(byte[] entropy) {
        if (entropy.length != 16 && entropy.length != 32) {
            throw new IllegalArgumentException("invalid entropy byte length: " + entropy.length);
        }

        // checksum for 256 bits is one byte
        List<String> wordList;
        ArrayList<String> words;
        byte[] bytes;
        if (entropy.length == 16) {
            wordList = getWordList(false);
            bytes = Arrays.copyOf(entropy, 17);
            bytes[16] = (byte) (checksum(entropy) & 0xF0);

            words = new ArrayList<>(12);
        } else {
            wordList = getWordList(false);
            bytes = Arrays.copyOf(entropy, 33);
            bytes[32] = checksum(entropy);

            words = new ArrayList<>(24);
        }
        var scratch = 0;
        var offset = 0;

        for (var b : bytes) {
            // shift `bytes` into `scratch`, popping off 11-bit indices when we can
            scratch <<= 8;
            // bitwise operations implicitly widen to `int` so mask off sign-extended bits
            scratch |= b & 0xFF;
            offset += 8;

            if (offset >= 11) {
                // pop 11 bits off the end of `scratch` and into `index`
                var index = (scratch >> (offset - 11)) & 0x7FF;
                offset -= 11;

                words.add(wordList.get(index));
            }
        }

        return words;
    }

    // hash the first 32 bytes of `entropy` and return the first byte of the digest
    private static byte checksum(byte[] entropy) {
        SHA256Digest digest = new SHA256Digest();
        // hash the first

        if (entropy.length == 17 || entropy.length == 16) {
            digest.update(entropy, 0, 16);
        } else {
            digest.update(entropy, 0, 32);
        }

        byte[] checksum = new byte[digest.getDigestSize()];
        digest.doFinal(checksum, 0);

        return checksum[0];
    }

    private static int getWordIndex(CharSequence word, boolean isLegacy) {
        var wordList = getWordList(isLegacy);
        var found = -1;
        for (var i = 0; i < wordList.size(); i++) {
            if (word.toString().equals(wordList.get(i))) {
                found = i;
            }
        }
        return found;
    }

    private static List<String> getWordList(boolean isLegacy) {
        if (isLegacy) {
            return getSpecificWordList(
                    () -> legacyWordList, () -> readWordList(true), (newWordList) -> legacyWordList = newWordList);
        } else {
            return getSpecificWordList(
                    () -> bip39WordList, () -> readWordList(false), (newWordList) -> bip39WordList = newWordList);
        }
    }

    private static synchronized List<String> getSpecificWordList(
            Supplier<SoftReference<List<String>>> getCurrentWordList,
            Supplier<List<String>> getNewWordList,
            Consumer<SoftReference<List<String>>> setCurrentWordList) {
        var localWordList = getCurrentWordList.get();
        if (localWordList == null || localWordList.get() == null) {
            List<String> words = getNewWordList.get();
            setCurrentWordList.accept(new SoftReference<>(words));
            // immediately return the strong reference
            return words;
        }

        return localWordList.get();
    }

    private static List<String> readWordList(boolean isLegacy) {
        if (isLegacy) {
            InputStream wordStream = Mnemonic.class.getClassLoader().getResourceAsStream("legacy-english.txt");
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(Objects.requireNonNull(wordStream), UTF_8))) {
                ArrayList<String> words = new ArrayList<>(4096);

                for (String word = reader.readLine(); word != null; word = reader.readLine()) {
                    words.add(word);
                }
                return Collections.unmodifiableList(words);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            InputStream wordStream = Mnemonic.class.getClassLoader().getResourceAsStream("bip39-english.txt");
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(Objects.requireNonNull(wordStream), UTF_8))) {
                ArrayList<String> words = new ArrayList<>(2048);

                for (String word = reader.readLine(); word != null; word = reader.readLine()) {
                    words.add(word);
                }
                return Collections.unmodifiableList(words);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static int[] convertRadix(int[] nums, int fromRadix, int toRadix, int toLength) {
        BigInteger num = BigInteger.valueOf(0);
        for (int element : nums) {
            num = num.multiply(BigInteger.valueOf(fromRadix));
            num = num.add(BigInteger.valueOf(element));
        }

        var result = new int[toLength];
        for (var i = toLength - 1; i >= 0; i -= 1) {
            BigInteger tem = num.divide(BigInteger.valueOf(toRadix));
            BigInteger rem = num.mod(BigInteger.valueOf(toRadix));
            num = tem;
            result[i] = rem.intValue();
        }

        return result;
    }

    private static int crc8(int[] data) {
        var crc = 0xFF;

        for (var i = 0; i < data.length - 1; i += 1) {
            crc ^= data[i];
            for (var j = 0; j < 8; j += 1) {
                crc = (crc >>> 1) ^ (((crc & 1) == 0) ? 0 : 0xB2);
            }
        }

        return crc ^ 0xFF;
    }

    private static boolean[] bytesToBits(byte[] dat) {
        var bits = new boolean[dat.length * 8];
        Arrays.fill(bits, Boolean.FALSE);

        for (int i = 0; i < dat.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[(i * 8) + j] = (dat[i] & (1 << (7 - j))) != 0;
            }
        }

        return bits;
    }

    /**
     * @deprecated use {@link #toStandardEd25519PrivateKey(String, int)} ()} or {@link #toStandardECDSAsecp256k1PrivateKey(String, int)} (String, int)} instead
     * Recover a private key from this mnemonic phrase.
     * <p>
     * This is not compatible with the phrases generated by the Android and iOS wallets;
     * use the no-passphrase version instead.
     *
     * @param passphrase the passphrase used to protect the mnemonic
     * @return the recovered key; use {@link PrivateKey#derive(int)} to get a
     * key for an account index (0 for default account)
     * @see PrivateKey#fromMnemonic(Mnemonic, String)
     */
    @Deprecated
    public PrivateKey toPrivateKey(String passphrase) {
        return PrivateKey.fromMnemonic(this, passphrase);
    }

    /**
     * Extract the private key.
     *
     * @return the private key
     * @throws BadMnemonicException when there are issues with the mnemonic
     */
    public PrivateKey toLegacyPrivateKey() throws BadMnemonicException {
        if (this.words.size() == 22) {
            return PrivateKey.fromBytes(this.wordsToLegacyEntropy());
        }

        return PrivateKey.fromBytes(this.wordsToLegacyEntropy2());
    }

    /**
     * @deprecated use {@link #toStandardEd25519PrivateKey(String, int)} ()} or {@link #toStandardECDSAsecp256k1PrivateKey(String, int)} (String, int)} instead
     * Recover a private key from this mnemonic phrase.
     *
     * @return the recovered key; use
     * {@link PrivateKey#derive(int)} to get
     * a key for an account index (0 for
     * default account)
     * @see PrivateKey#fromMnemonic(Mnemonic)
     */
    @Deprecated
    public PrivateKey toPrivateKey() {
        return toPrivateKey("");
    }

    private void validate() throws BadMnemonicException {
        if (words.size() != 24 && words.size() != 12) {
            throw new BadMnemonicException(this, BadMnemonicReason.BadLength);
        }

        ArrayList<Integer> unknownIndices = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            if (getWordIndex(words.get(i), false) < 0) {
                unknownIndices.add(i);
            }
        }

        if (!unknownIndices.isEmpty()) {
            throw new BadMnemonicException(this, BadMnemonicReason.UnknownWords, unknownIndices);
        }

        if (words.size() != 22) {
            // test the checksum encoded in the mnemonic
            byte[] entropyAndChecksum = wordsToEntropyAndChecksum();
            // ignores the 33rd byte
            byte expectedChecksum;
            byte givenChecksum;

            if (words.size() == 12) {
                expectedChecksum = (byte) (checksum(entropyAndChecksum) & 0xF0);
                givenChecksum = entropyAndChecksum[16];
            } else {
                expectedChecksum = checksum(entropyAndChecksum);
                givenChecksum = entropyAndChecksum[32];
            }

            if (givenChecksum != expectedChecksum) {
                throw new BadMnemonicException(this, BadMnemonicReason.ChecksumMismatch);
            }
        }
    }

    @Override
    public String toString() {
        if (asString == null) {
            asString = Joiner.on(' ').join(words);
        }

        return asString;
    }

    /**
     * Convert passphrase to a byte array.
     *
     * @param passphrase the passphrase
     * @return the byte array
     */
    byte[] toSeed(String passphrase) {
        String salt = Normalizer.normalize("mnemonic" + passphrase, Normalizer.Form.NFKD);

        // BIP-39 seed generation
        PKCS5S2ParametersGenerator pbkdf2 = new PKCS5S2ParametersGenerator(new SHA512Digest());
        pbkdf2.init(toString().getBytes(UTF_8), salt.getBytes(UTF_8), 2048);

        KeyParameter key = (KeyParameter) pbkdf2.generateDerivedParameters(512);
        return key.getKey();
    }

    private byte[] wordsToEntropyAndChecksum() {
        if (words.size() != 24 && words.size() != 12) {
            // should be checked in `validate()`
            throw new IllegalStateException("(BUG) expected 24-word mnemonic, got " + words.size() + " words");
        }
        ByteBuffer buffer;
        if (words.size() == 12) {
            buffer = ByteBuffer.allocate(17);
        } else {
            buffer = ByteBuffer.allocate(33);
        }
        // reverse algorithm of `entropyToWords()` below
        int scratch = 0;
        int offset = 0;
        for (CharSequence word : words) {
            int index = getWordIndex(word, false);

            if (index < 0) {
                // should also be checked in `validate()`
                throw new IllegalStateException("(BUG) word not in word list: " + word);
            } else if (index > 0x7FF) {
                throw new IndexOutOfBoundsException("(BUG) index out of bounds: " + index);
            }

            scratch <<= 11;
            scratch |= index;
            offset += 11;

            while (offset >= 8) {
                // truncation is what we want here
                buffer.put((byte) (scratch >> (offset - 8)));
                offset -= 8;
            }
        }

        if (offset != 0) {
            buffer.put((byte) (scratch << offset));
        }

        return buffer.array();
    }

    private byte[] wordsToLegacyEntropy() throws BadMnemonicException {
        var indices = new int[words.size()];
        for (var i = 0; i < words.size(); i++) {
            indices[i] = getWordIndex(words.get(i), true);
        }
        var data = convertRadix(indices, 4096, 256, 33);
        var crc = data[data.length - 1];
        var result = new int[data.length - 1];
        for (var i = 0; i < data.length - 1; i += 1) {
            result[i] = data[i] ^ crc;
        }
        // int to byte conversion
        ByteBuffer byteBuffer = ByteBuffer.allocate(result.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(result);

        var crc2 = crc8(result);
        if (crc != crc2) {
            throw new BadMnemonicException(this, BadMnemonicReason.ChecksumMismatch);
        }

        byte[] array = byteBuffer.array();
        var i = 0;
        var j = 3;
        byte[] array2 = new byte[data.length - 1];
        // remove all the fill 0s
        while (j < array.length) {
            array2[i] = array[j];
            i++;
            j = j + 4;
        }

        return array2;
    }

    private byte[] wordsToLegacyEntropy2() throws BadMnemonicException {
        var concatBitsLen = this.words.size() * 11;
        var concatBits = new boolean[concatBitsLen];
        Arrays.fill(concatBits, Boolean.FALSE);

        for (int index = 0; index < this.words.size(); index++) {
            var nds = Collections.binarySearch(getWordList(false), this.words.get(index), null);

            for (int i = 0; i < 11; i++) {
                concatBits[(index * 11) + i] = (nds & (1 << (10 - i))) != 0;
            }
        }

        var checksumBitsLen = concatBitsLen / 33;
        var entropyBitsLen = concatBitsLen - checksumBitsLen;

        var entropy = new byte[entropyBitsLen / 8];

        for (int i = 0; i < entropy.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (concatBits[(i * 8) + j]) {
                    entropy[i] |= (byte) (1 << (7 - j));
                }
            }
        }

        var digest = new SHA256Digest();
        byte[] hash = new byte[entropy.length];
        digest.update(entropy, 0, entropy.length);
        digest.doFinal(hash, 0);
        var hashBits = bytesToBits(hash);

        for (int i = 0; i < checksumBitsLen; i++) {
            if (concatBits[entropyBitsLen + i] != hashBits[i]) {
                throw new BadMnemonicException(this, BadMnemonicReason.ChecksumMismatch);
            }
        }

        return entropy;
    }

    /**
     * Recover an Ed25519 private key from this mnemonic phrase, with an
     * optional passphrase.
     *
     * @param passphrase    the passphrase used to protect the mnemonic
     * @param index         the derivation index
     * @return the private key
     */
    public PrivateKey toStandardEd25519PrivateKey(String passphrase, int index) {
        var seed = this.toSeed(passphrase);
        PrivateKey derivedKey = PrivateKey.fromSeedED25519(seed);

        for (int i : new int[] {44, 3030, 0, 0, index}) {
            derivedKey = derivedKey.derive(i);
        }

        return derivedKey;
    }

    /**
     * Converts a derivation path from string to an array of integers.
     * Note that this expects precisely 5 components in the derivation path,
     * as per BIP-44:
     * `m / purpose' / coin_type' / account' / change / address_index`
     * Takes into account `'` for hardening as per BIP-32,
     * and does not prescribe which components should be hardened.
     *
     * @param derivationPath  the derivation path in BIP-44 format,
     *                        e.g. "m/44'/60'/0'/0/0"
     * @return an array of integers designed to be used with PrivateKey#derive
     */
    private int[] calculateDerivationPathValues(String derivationPath) throws IllegalArgumentException {
        if (derivationPath == null || derivationPath.isEmpty()) {
            throw new IllegalArgumentException("Derivation path cannot be null or empty");
        }

        // Parse the derivation path from string into values
        Pattern pattern = Pattern.compile("m/(\\d+'?)/(\\d+'?)/(\\d+'?)/(\\d+'?)/(\\d+'?)");
        Matcher matcher = pattern.matcher(derivationPath);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid derivation path format");
        }

        int[] numbers = new int[5];
        boolean[] isHardened = new boolean[5];
        try {
            // Extract numbers and use apostrophe to select if is hardened
            for (int i = 1; i <= 5; i++) {
                String value = matcher.group(i);
                if (value.endsWith("'")) {
                    isHardened[i - 1] = true;
                    value = value.substring(0, value.length() - 1);
                } else {
                    isHardened[i - 1] = false;
                }
                numbers[i - 1] = Integer.parseInt(value);
            }
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid number format in derivation path", nfe);
        }

        // Derive private key one index at a time
        int[] values = new int[5];
        for (int i = 0; i < numbers.length; i++) {
            values[i] = (isHardened[i] ? Bip32Utils.toHardenedIndex(numbers[i]) : numbers[i]);
        }

        return values;
    }

    /**
     * Common implementation for both `toStandardECDSAsecp256k1PrivateKey`
     * functions.
     *
     * @param passphrase            the passphrase used to protect the
     *                              mnemonic, use "" for none
     * @param derivationPathValues  derivation path as an integer array,
     *                              see: `calculateDerivationPathValues`
     * @return a private key
     */
    private PrivateKey toStandardECDSAsecp256k1PrivateKeyImpl(String passphrase, int[] derivationPathValues) {
        var seed = this.toSeed(passphrase);
        PrivateKey derivedKey = PrivateKey.fromSeedECDSAsecp256k1(seed);

        for (int derivationPathValue : derivationPathValues) {
            derivedKey = derivedKey.derive(derivationPathValue);
        }
        return derivedKey;
    }

    /**
     * Recover an ECDSAsecp256k1 private key from this mnemonic phrase, with an
     * optional passphrase.
     * Uses the default derivation path of `m/44'/3030'/0'/0/${index}`.
     *
     * @param passphrase    the passphrase used to protect the mnemonic,
     *                      use "" for none
     * @param index         the derivation index
     * @return the private key
     */
    public PrivateKey toStandardECDSAsecp256k1PrivateKey(String passphrase, int index) {
        // Harden the first 3 indexes
        final int[] derivationPathValues = new int[] {
            Bip32Utils.toHardenedIndex(44), Bip32Utils.toHardenedIndex(3030), Bip32Utils.toHardenedIndex(0), 0, index
        };
        return toStandardECDSAsecp256k1PrivateKeyImpl(passphrase, derivationPathValues);
    }

    /**
     * Recover an ECDSAsecp256k1 private key from this mnemonic phrase and
     * derivation path, with an optional passphrase.
     *
     * @param passphrase      the passphrase used to protect the mnemonic,
     *                        use "" for none
     * @param derivationPath  the derivation path in BIP-44 format,
     *                        e.g. "m/44'/60'/0'/0/0"
     * @return the private key
     */
    public PrivateKey toStandardECDSAsecp256k1PrivateKeyCustomDerivationPath(String passphrase, String derivationPath) {
        final int[] derivationPathValues = calculateDerivationPathValues(derivationPath);
        return toStandardECDSAsecp256k1PrivateKeyImpl(passphrase, derivationPathValues);
    }
}
