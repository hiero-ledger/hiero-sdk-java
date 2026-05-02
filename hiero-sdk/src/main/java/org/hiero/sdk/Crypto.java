// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;

/**
 * Utility class used internally by the sdk.
 */
final class Crypto {
    static final int IV_LEN = 16;
    static final int ITERATIONS = 262144;
    static final int SALT_LEN = 32;
    static final int DK_LEN = 32;

    // OpenSSL doesn't like longer derived keys
    static final int CBC_DK_LEN = 16;

    static final X9ECParameters ECDSA_SECP256K1_CURVE = SECNamedCurves.getByName("secp256k1");
    static final ECDomainParameters ECDSA_SECP256K1_DOMAIN = new ECDomainParameters(
            ECDSA_SECP256K1_CURVE.getCurve(),
            ECDSA_SECP256K1_CURVE.getG(),
            ECDSA_SECP256K1_CURVE.getN(),
            ECDSA_SECP256K1_CURVE.getH());

    /**
     * Constructor.
     */
    private Crypto() {}

    /**
     * Derive a sha 256 key.
     *
     * @param passphrase                the password will be converted into bytes
     * @param salt                      the salt to be mixed in
     * @param iterations                the iterations for mixing
     * @param dkLenBytes                the key length in bytes
     * @return                          the key parameter object
     */
    static KeyParameter deriveKeySha256(String passphrase, byte[] salt, int iterations, int dkLenBytes) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(passphrase.getBytes(StandardCharsets.UTF_8), salt, iterations);

        return (KeyParameter) gen.generateDerivedParameters(dkLenBytes * 8);
    }

    /**
     * Initialize an advanced encryption standard counter mode cipher.
     *
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector byte array
     * @param forDecrypt                is this for decryption
     * @return                          the aes ctr cipher
     */
    static Cipher initAesCtr128(KeyParameter cipherKey, byte[] iv, boolean forDecrypt) {
        Cipher aesCipher;

        try {
            aesCipher = Cipher.getInstance("AES/CTR/NOPADDING");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new Error("platform does not support AES-CTR ciphers", e);
        }

        return initAesCipher(aesCipher, cipherKey, iv, forDecrypt);
    }

    /**
     * Initialize an advanced encryption standard cipher block chaining mode
     * cipher for encryption.
     *
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector byte array
     * @return                          the aes cbc cipher
     */
    static Cipher initAesCbc128Encrypt(KeyParameter cipherKey, byte[] iv) {
        Cipher aesCipher;

        try {
            aesCipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new Error("platform does not support AES-CBC ciphers", e);
        }

        return initAesCipher(aesCipher, cipherKey, iv, false);
    }

    /**
     * Initialize an advanced encryption standard cipher block chaining mode
     * cipher for decryption.
     *
     * @param cipherKey                 the cipher key
     * @param parameters                the algorithm parameters
     * @return                          the aes cbc cipher
     */
    static Cipher initAesCbc128Decrypt(KeyParameter cipherKey, AlgorithmParameters parameters) {
        Cipher aesCipher;

        try {
            aesCipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new Error("platform does not support AES-CBC ciphers", e);
        }

        try {
            aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cipherKey.getKey(), 0, 16, "AES"), parameters);
        } catch (InvalidKeyException e) {
            throw new Error("platform does not support AES-128 ciphers", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new Error(e);
        }

        return aesCipher;
    }

    /**
     * Create a new aes cipher.
     *
     * @param aesCipher                 the aes cipher
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector byte array
     * @param forDecrypt                is this for decryption True or encryption False
     * @return                          the new aes cipher
     */
    private static Cipher initAesCipher(Cipher aesCipher, KeyParameter cipherKey, byte[] iv, boolean forDecrypt) {
        int mode = forDecrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE;

        try {
            aesCipher.init(mode, new SecretKeySpec(cipherKey.getKey(), 0, 16, "AES"), new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new Error("platform does not support AES-128 ciphers", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new Error(e);
        }

        return aesCipher;
    }

    /**
     * Encrypt a byte array with the aes ctr cipher.
     *
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector
     * @param input                     the byte array to encrypt
     * @return                          the encrypted byte array
     */
    static byte[] encryptAesCtr128(KeyParameter cipherKey, byte[] iv, byte[] input) {
        Cipher aesCipher = initAesCtr128(cipherKey, iv, false);
        return runCipher(aesCipher, input);
    }

    /**
     * Decrypt a byte array with the aes ctr cipher.
     *
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector
     * @param input                     the byte array to decrypt
     * @return                          the decrypted byte array
     */
    static byte[] decryptAesCtr128(KeyParameter cipherKey, byte[] iv, byte[] input) {
        Cipher aesCipher = initAesCtr128(cipherKey, iv, true);
        return runCipher(aesCipher, input);
    }

    /**
     * Run the cipher on the given input.
     *
     * @param cipher                    the cipher
     * @param input                     the byte array
     * @return                          the output of running the cipher
     */
    static byte[] runCipher(Cipher cipher, byte[] input) {
        byte[] output = new byte[cipher.getOutputSize(input.length)];

        try {
            cipher.doFinal(input, 0, input.length, output);
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            throw new Error(e);
        }

        return output;
    }

    /**
     * Calculate a hash message authentication code using the secure hash
     * algorithm variant 384.
     *
     * @param cipherKey                 the cipher key
     * @param iv                        the initialization vector
     * @param input                     the byte array
     * @return                          the hmac using sha 384
     */
    static byte[] calcHmacSha384(KeyParameter cipherKey, @Nullable byte[] iv, byte[] input) {
        HMac hmacSha384 = new HMac(new SHA384Digest());
        byte[] output = new byte[hmacSha384.getMacSize()];

        hmacSha384.init(new KeyParameter(cipherKey.getKey(), 16, 16));
        if (iv != null) {
            hmacSha384.update(iv, 0, iv.length);
        }
        hmacSha384.update(input, 0, input.length);
        hmacSha384.doFinal(output, 0);

        return output;
    }

    /**
     * Calculate a keccak 256-bit hash.
     *
     * @param message                   the message to be hashed
     * @return                          the hash
     */
    static byte[] calcKeccak256(byte[] message) {
        var digest = new Keccak.Digest256();
        digest.update(message);
        return digest.digest();
    }

    /**
     * Generate some randomness.
     *
     * @param len                       the number of bytes requested
     * @return                          the byte array of randomness
     */
    static byte[] randomBytes(int len) {
        byte[] out = new byte[len];
        ThreadLocalSecureRandom.current().nextBytes(out);

        return out;
    }

    /**
     * Given the r and s components of a signature and the hash value of the message, recover and return the public key
     * according to the algorithm in <a href="https://www.secg.org/sec1-v2.pdf">SEC1v2 section 4.1.6.</a>
     * <p>
     * Calculations and explanations in this method were taken and adapted from
     * <a href="https://github.com/apache/incubator-tuweni/blob/0852e0b01ad126b47edae51b26e808cb73e294b1/crypto/src/main/java/org/apache/tuweni/crypto/SECP256K1.java#L199-L215">incubator-tuweni lib</a>
     *
     * @param recId Which possible key to recover.
     * @param r The R component of the signature.
     * @param s The S component of the signature.
     * @param messageHash Hash of the data that was signed.
     * @return A ECKey containing only the public part, or {@code null} if recovery wasn't possible.
     */
    static byte[] recoverPublicKeyECDSAFromSignature(int recId, BigInteger r, BigInteger s, byte[] messageHash) {
        if (!(recId == 0 || recId == 1)) {
            throw new IllegalArgumentException("Recovery Id must be 0 or 1 for secp256k1.");
        }
        if (r.signum() < 0 || s.signum() < 0) {
            throw new IllegalArgumentException("'r' and 's' shouldn't be negative.");
        }
        // 1.1 - 1.3 calculate point R
        ECPoint R = decompressKey(r, (recId & 1) == 1);
        // 1.4 nR should be a point at infinity
        if (R == null || !R.multiply(ECDSA_SECP256K1_DOMAIN.getN()).isInfinity()) {
            return null;
        }
        // 1.5 Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = new BigInteger(1, messageHash);

        // 1.6.1 Compute a candidate public key as:
        //   Q = mi(r) * (sR - eG)
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //   Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
        // In the above equation ** is point multiplication and + is point addition (the EC group
        // operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(ECDSA_SECP256K1_DOMAIN.getN());
        BigInteger rInv = r.modInverse(ECDSA_SECP256K1_DOMAIN.getN());
        BigInteger srInv = rInv.multiply(s).mod(ECDSA_SECP256K1_DOMAIN.getN());
        BigInteger eInvrInv = rInv.multiply(eInv).mod(ECDSA_SECP256K1_DOMAIN.getN());
        ECPoint q = ECAlgorithms.sumOfTwoMultiplies(ECDSA_SECP256K1_DOMAIN.getG(), eInvrInv, R, srInv);

        if (q.isInfinity()) {
            return null;
        }

        return q.getEncoded(true);
    }

    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        var X_9_INTEGER_CONVERTER = new X9IntegerConverter();
        byte[] compEnc = X_9_INTEGER_CONVERTER.integerToBytes(
                xBN, 1 + X_9_INTEGER_CONVERTER.getByteLength(ECDSA_SECP256K1_DOMAIN.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        try {
            return ECDSA_SECP256K1_DOMAIN.getCurve().decodePoint(compEnc);
        } catch (IllegalArgumentException e) {
            // the key was invalid
            return null;
        }
    }
}
