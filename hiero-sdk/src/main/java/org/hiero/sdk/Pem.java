// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Internal utility class for handling PEM objects.
 * <br>
 * Privacy-Enhanced Mail (PEM) is a de facto file format for storing and
 * sending cryptographic keys, certificates, and other data, based on a set of
 * 1993 IETF standards defining "privacy-enhanced mail."
 */
final class Pem {
    private static final String TYPE_PRIVATE_KEY = "PRIVATE KEY";
    private static final String TYPE_ENCRYPTED_PRIVATE_KEY = "ENCRYPTED PRIVATE KEY";

    /**
     * Constructor.
     */
    private Pem() {}

    /**
     * For some reason, this generates PEM encodings that we ourselves can import, but OpenSSL
     * doesn't like. We decided to punt on generating encrypted PEMs for now but saving
     * the code for when we get back to it and/or any demand arises.
     */
    @SuppressWarnings("unused")
    static void writeEncryptedPrivateKey(PrivateKeyInfo pkInfo, Writer out, String passphrase) throws IOException {
        byte[] salt = Crypto.randomBytes(Crypto.SALT_LEN);

        KeyParameter derivedKey = Crypto.deriveKeySha256(passphrase, salt, Crypto.ITERATIONS, Crypto.CBC_DK_LEN);

        byte[] iv = Crypto.randomBytes(Crypto.IV_LEN);

        Cipher cipher = Crypto.initAesCbc128Encrypt(derivedKey, iv);

        byte[] encryptedKey = Crypto.runCipher(cipher, pkInfo.getEncoded());

        // I wanted to just do this with BC's PKCS8Generator and KcePKCSPBEOutputEncryptorBuilder
        // but it tries to init AES instance of `Cipher` with a `PBKDF2Key` and the former complains

        // So this is basically a reimplementation of that minus the excess OO
        PBES2Parameters parameters = new PBES2Parameters(
                new KeyDerivationFunc(
                        PKCSObjectIdentifiers.id_PBKDF2,
                        new PBKDF2Params(
                                salt,
                                Crypto.ITERATIONS,
                                Crypto.CBC_DK_LEN,
                                new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA256))),
                new EncryptionScheme(
                        NISTObjectIdentifiers.id_aes128_CBC,
                        ASN1Primitive.fromByteArray(cipher.getParameters().getEncoded())));

        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.id_PBES2, parameters), encryptedKey);

        PemWriter writer = new PemWriter(out);
        writer.writeObject(new PemObject(TYPE_ENCRYPTED_PRIVATE_KEY, encryptedPrivateKeyInfo.getEncoded()));
        writer.flush();
    }

    /**
     * Create a private key info object from a reader.
     *
     * @param input                     reader object
     * @param passphrase                passphrase
     * @return                          private key info object
     * @throws IOException              if IO operations fail
     */
    static PrivateKeyInfo readPrivateKey(Reader input, @Nullable String passphrase) throws IOException {
        try (final var parser = new PEMParser(input)) {
            Object parsedObject = parser.readObject();
            var password = (passphrase != null) ? passphrase.toCharArray() : "".toCharArray();
            if (parsedObject == null) {
                throw new BadKeyException("PEM file did not contain a private key");
            } else if (parsedObject instanceof PKCS8EncryptedPrivateKeyInfo) {
                var decryptProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .setProvider(new BouncyCastleProvider())
                        .build(password);
                var encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) parsedObject;
                return encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptProvider);
            } else if (parsedObject instanceof PrivateKeyInfo) {
                return (PrivateKeyInfo) parsedObject;
            } else if (parsedObject instanceof PEMEncryptedKeyPair) {
                var decryptProvider = new JcePEMDecryptorProviderBuilder()
                        .setProvider(new BouncyCastleProvider())
                        .build(password);
                var encryptedKeyPair = (PEMEncryptedKeyPair) parsedObject;
                return encryptedKeyPair.decryptKeyPair(decryptProvider).getPrivateKeyInfo();
            } else if (parsedObject instanceof PEMKeyPair) {
                var keyPair = (PEMKeyPair) parsedObject;
                return keyPair.getPrivateKeyInfo();
            } else {
                throw new BadKeyException("PEM file contained something the SDK didn't know what to do with: "
                        + parsedObject.getClass().getName());
            }
        } catch (PKCSException e) {
            if (e.getMessage().contains("password empty")) {
                throw new BadKeyException("PEM file contained an encrypted private key but no passphrase was given");
            }
            throw new RuntimeException(e);
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }
}
