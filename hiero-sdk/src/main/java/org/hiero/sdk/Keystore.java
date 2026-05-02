// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Optional;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

/**
 * Internal utility class to serialize / deserialize between java object / json representation.
 */
final class Keystore {
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    private byte[] keyBytes;

    private Keystore(byte[] keyBytes) {
        this.keyBytes = keyBytes;
    }

    public Keystore(PrivateKey privateKey) {
        this.keyBytes = privateKey.toBytes();
    }

    public static Keystore fromStream(InputStream stream, String passphrase) throws IOException {
        try {
            JsonObject jsonObject = jsonParser
                    .parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return fromJson(jsonObject, passphrase);
        } catch (IllegalStateException e) {
            throw new BadKeyException(Optional.ofNullable(e.getMessage()).orElse("failed to parse Keystore"));
        } catch (JsonIOException e) {
            // RFC (@abonander): I'm all for keeping this as an unchecked exception
            // but I want consistency with export() so this may involve creating our own exception
            // because JsonIOException is kinda leaking implementation details.
            throw (IOException) Objects.requireNonNull(e.getCause());
        } catch (JsonSyntaxException e) {
            throw new BadKeyException(e);
        }
    }

    private static Keystore fromJson(JsonObject object, String passphrase) {
        int version = expectInt(object, "version");

        //noinspection SwitchStatementWithTooFewBranches
        switch (version) {
            case 1:
                return parseKeystoreV1(expectObject(object, "crypto"), passphrase);
            case 2:
                return parseKeystoreV2(expectObject(object, "crypto"), passphrase);
            default:
                throw new BadKeyException("unsupported keystore version: " + version);
        }
    }

    private static Keystore parseKeystoreV1(JsonObject crypto, String passphrase) {
        String ciphertext = expectString(crypto, "ciphertext");
        String ivString = expectString(expectObject(crypto, "cipherparams"), "iv");
        String cipher = expectString(crypto, "cipher");
        String kdf = expectString(crypto, "kdf");
        JsonObject kdfParams = expectObject(crypto, "kdfparams");
        String macString = expectString(crypto, "mac");

        if (!cipher.equals("aes-128-ctr")) {
            throw new BadKeyException("unsupported keystore cipher: " + cipher);
        }

        if (!kdf.equals("pbkdf2")) {
            throw new BadKeyException("unsuppported KDF: " + kdf);
        }

        int dkLen = expectInt(kdfParams, "dkLen");
        String saltStr = expectString(kdfParams, "salt");
        int count = expectInt(kdfParams, "c");
        String prf = expectString(kdfParams, "prf");

        if (!prf.equals("hmac-sha256")) {
            throw new BadKeyException("unsupported KDF hash function: " + prf);
        }

        byte[] cipherBytes = Hex.decode(ciphertext);
        byte[] iv = Hex.decode(ivString);
        byte[] mac = Hex.decode(macString);
        byte[] salt = Hex.decode(saltStr);

        KeyParameter cipherKey = Crypto.deriveKeySha256(passphrase, salt, count, dkLen);

        byte[] testHmac = Crypto.calcHmacSha384(cipherKey, null, cipherBytes);

        if (!MessageDigest.isEqual(mac, testHmac)) {
            throw new BadKeyException("HMAC mismatch; passphrase is incorrect");
        }

        return new Keystore(Crypto.decryptAesCtr128(cipherKey, iv, cipherBytes));
    }

    private static Keystore parseKeystoreV2(JsonObject crypto, String passphrase) {
        String ciphertext = expectString(crypto, "ciphertext");
        String ivString = expectString(expectObject(crypto, "cipherparams"), "iv");
        String cipher = expectString(crypto, "cipher");
        String kdf = expectString(crypto, "kdf");
        JsonObject kdfParams = expectObject(crypto, "kdfparams");
        String macString = expectString(crypto, "mac");

        if (!cipher.equals("aes-128-ctr")) {
            throw new BadKeyException("unsupported keystore cipher: " + cipher);
        }

        if (!kdf.equals("pbkdf2")) {
            throw new BadKeyException("unsuppported KDF: " + kdf);
        }

        int dkLen = expectInt(kdfParams, "dkLen");
        String saltStr = expectString(kdfParams, "salt");
        int count = expectInt(kdfParams, "c");
        String prf = expectString(kdfParams, "prf");

        if (!prf.equals("hmac-sha256")) {
            throw new BadKeyException("unsupported KDF hash function: " + prf);
        }

        byte[] cipherBytes = Hex.decode(ciphertext);
        byte[] iv = Hex.decode(ivString);
        byte[] mac = Hex.decode(macString);
        byte[] salt = Hex.decode(saltStr);

        KeyParameter cipherKey = Crypto.deriveKeySha256(passphrase, salt, count, dkLen);

        byte[] testHmac = Crypto.calcHmacSha384(cipherKey, iv, cipherBytes);

        if (!MessageDigest.isEqual(mac, testHmac)) {
            throw new BadKeyException("HMAC mismatch; passphrase is incorrect");
        }

        return new Keystore(Crypto.decryptAesCtr128(cipherKey, iv, cipherBytes));
    }

    private static JsonObject expectObject(JsonObject object, String key) {
        try {
            return object.get(key).getAsJsonObject();
        } catch (ClassCastException | NullPointerException e) {
            throw new Error("expected key '" + key + "' to be an object", e);
        }
    }

    private static int expectInt(JsonObject object, String key) {
        try {
            return object.get(key).getAsInt();
        } catch (ClassCastException | NullPointerException e) {
            throw new Error("expected key '" + key + "' to be an integer", e);
        }
    }

    private static String expectString(JsonObject object, String key) {
        try {
            return object.get(key).getAsString();
        } catch (ClassCastException | NullPointerException e) {
            throw new Error("expected key '" + key + "' to be a string", e);
        }
    }

    /**
     * Get the decoded key from this keystore as an {@link PrivateKey}.
     *
     * @throws BadKeyException if the key bytes are of an incorrect length for a raw
     *                         private key or private key + public key, or do not represent a DER encoded Ed25519
     *                         private key.
     */
    public PrivateKey getEd25519() {
        return PrivateKey.fromBytes(keyBytes);
    }

    public void export(OutputStream outputStream, String passphrase) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        gson.toJson(exportJson(passphrase), writer);
        writer.flush();
    }

    private JsonObject exportJson(String passphrase) {
        JsonObject object = new JsonObject();
        object.addProperty("version", 2);

        JsonObject crypto = new JsonObject();
        crypto.addProperty("cipher", "aes-128-ctr");
        crypto.addProperty("kdf", "pbkdf2");

        byte[] salt = Crypto.randomBytes(Crypto.SALT_LEN);

        KeyParameter cipherKey = Crypto.deriveKeySha256(passphrase, salt, Crypto.ITERATIONS, Crypto.DK_LEN);

        byte[] iv = Crypto.randomBytes(Crypto.IV_LEN);

        byte[] cipherBytes = Crypto.encryptAesCtr128(cipherKey, iv, keyBytes);

        byte[] mac = Crypto.calcHmacSha384(cipherKey, iv, cipherBytes);

        JsonObject cipherParams = new JsonObject();
        cipherParams.addProperty("iv", Hex.toHexString(iv));

        JsonObject kdfParams = new JsonObject();
        kdfParams.addProperty("dkLen", Crypto.DK_LEN);
        kdfParams.addProperty("salt", Hex.toHexString(salt));
        kdfParams.addProperty("c", Crypto.ITERATIONS);
        kdfParams.addProperty("prf", "hmac-sha256");

        crypto.add("cipherparams", cipherParams);
        crypto.addProperty("ciphertext", Hex.toHexString(cipherBytes));
        crypto.add("kdfparams", kdfParams);
        crypto.addProperty("mac", Hex.toHexString(mac));

        object.add("crypto", crypto);

        return object;
    }
}
