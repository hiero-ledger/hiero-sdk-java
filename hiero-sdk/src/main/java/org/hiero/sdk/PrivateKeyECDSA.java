// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.util.Arrays;
import org.hiero.sdk.utils.Bip32Utils;

/**
 * Encapsulate the ECDSA private key.
 */
public class PrivateKeyECDSA extends PrivateKey {

    private final BigInteger keyData;

    @Nullable
    private final KeyParameter chainCode;

    /**
     * Constructor.
     *
     * @param keyData                   the key data
     */
    PrivateKeyECDSA(BigInteger keyData, @Nullable KeyParameter chainCode) {
        this.keyData = keyData;
        this.chainCode = chainCode;
    }

    /**
     * Create a new private ECDSA key.
     *
     * @return                          the new key
     */
    static PrivateKeyECDSA generateInternal() {
        var generator = new ECKeyPairGenerator();
        var keygenParams = new ECKeyGenerationParameters(ECDSA_SECP256K1_DOMAIN, ThreadLocalSecureRandom.current());
        generator.init(keygenParams);
        var keypair = generator.generateKeyPair();
        var privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        return new PrivateKeyECDSA(privParams.getD(), null);
    }

    /**
     * Create a new private key from a private key ino object.
     *
     * @param privateKeyInfo            the private key info object
     * @return                          the new key
     */
    static PrivateKey fromPrivateKeyInfoInternal(PrivateKeyInfo privateKeyInfo) {
        try {
            var privateKey = ECPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
            return fromECPrivateKeyInternal(privateKey);
        } catch (IllegalArgumentException e) {
            // Try legacy import
            try {
                var privateKey = (ASN1OctetString) privateKeyInfo.parsePrivateKey();
                return new PrivateKeyECDSA(new BigInteger(1, privateKey.getOctets()), null);
            } catch (IOException ex) {
                throw new BadKeyException(ex);
            }
        } catch (IOException e) {
            throw new BadKeyException(e);
        }
    }

    /**
     * Create a new private key from a ECPrivateKey object.
     *
     * @param privateKey                the ECPrivateKey object
     * @return                          the new key
     */
    static PrivateKey fromECPrivateKeyInternal(ECPrivateKey privateKey) {
        return new PrivateKeyECDSA(privateKey.getKey(), null);
    }

    /**
     * Create a private key from a byte array.
     *
     * @param privateKey                the byte array
     * @return                          the new key
     */
    static PrivateKey fromBytesInternal(byte[] privateKey) {
        if (privateKey.length == 32) {
            return new PrivateKeyECDSA(new BigInteger(1, privateKey), null);
        }

        // Assume a DER-encoded private key descriptor
        return fromECPrivateKeyInternal(ECPrivateKey.getInstance(privateKey));
    }

    /**
     * Throws an exception when trying to derive a child key.
     *
     * @param entropy                   entropy byte array
     * @param index                     the child key index
     * @return                          the new key
     */
    static byte[] legacyDeriveChildKey(byte[] entropy, long index) {
        throw new IllegalStateException("ECDSA secp256k1 keys do not currently support derivation");
    }

    @Override
    public PrivateKey legacyDerive(long index) {
        throw new IllegalStateException("ECDSA secp256k1 keys do not currently support derivation");
    }

    @Override
    public boolean isDerivable() {
        return this.chainCode != null;
    }

    @Override
    public PrivateKey derive(int index) {
        if (!isDerivable()) {
            throw new IllegalStateException("this private key does not support derivation");
        }

        boolean isHardened = Bip32Utils.isHardenedIndex(index);
        ByteBuffer data = ByteBuffer.allocate(37);

        if (isHardened) {
            byte[] bytes33 = new byte[33];
            byte[] priv = toBytesRaw();
            System.arraycopy(priv, 0, bytes33, 33 - priv.length, priv.length);
            data.put(bytes33);
        } else {
            data.put(getPublicKey().toBytesRaw());
        }
        data.putInt(index);

        byte[] dataArray = data.array();
        HMac hmacSha512 = new HMac(new SHA512Digest());
        hmacSha512.init(new KeyParameter(chainCode.getKey()));
        hmacSha512.update(dataArray, 0, dataArray.length);

        byte[] i = new byte[64];
        hmacSha512.doFinal(i, 0);

        var il = java.util.Arrays.copyOfRange(i, 0, 32);
        var ir = java.util.Arrays.copyOfRange(i, 32, 64);

        var ki = keyData.add(new BigInteger(1, il)).mod(ECDSA_SECP256K1_CURVE.getN());

        return new PrivateKeyECDSA(ki, new KeyParameter(ir));
    }

    /**
     * Create an ECDSA key from seed.
     * Implement the published algorithm as defined in BIP32 in order to derive the primary account key from the
     * original (and never stored) master key.
     * The original master key, which is a secure key generated according to the BIP39 specification, is input to this
     * operation, and provides the base cryptographic seed material required to ensure the output is sufficiently random
     * to maintain strong cryptographic assurances.
     * The fromSeed() method must be provided with cryptographically secure material; otherwise, it will produce
     * insecure output.
     *
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki">BIP-32 Definition</a>
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki">BIP-39 Definition</a>
     *
     * @param seed                      the seed bytes
     * @return                          the new key
     */
    public static PrivateKey fromSeed(byte[] seed) {
        var hmacSha512 = new HMac(new SHA512Digest());
        hmacSha512.init(new KeyParameter("Bitcoin seed".getBytes(StandardCharsets.UTF_8)));
        hmacSha512.update(seed, 0, seed.length);

        var derivedState = new byte[hmacSha512.getMacSize()];
        hmacSha512.doFinal(derivedState, 0);

        return derivableKeyECDSA(derivedState);
    }

    /**
     * Create a derived key.
     * The industry standard protocol for deriving an active ECDSA keypair from a BIP39 master key is described in
     * BIP32. By using this deterministic mechanism to derive cryptographically secure keypairs from a single original
     * secret, the user maintains secure access to their wallet, even if they lose access to a particular system or
     * wallet local data store.
     * The active keypair can always be re-derived from the original master key.
     * The use of the fixed "key" values in this code is defined by this deterministic protocol, and this data is mixed,
     * in a deterministic but cryptographically secure manner, with the original master key and/or other derived keys
     * "higher" in the tree to produce a cryptographically secure derived key.
     * This "Key Derivation Function" makes use of secure hash algorithm and a secure hash based message authentication
     * code to produce an initialization vector, and then produces the actual key from a portion of that vector.
     *
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki">BIP-32 Definition</a>
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki">BIP-39 Definition</a>
     *
     * @param deriveData                data to derive the key
     * @return                          the new key
     */
    static PrivateKeyECDSA derivableKeyECDSA(byte[] deriveData) {
        var keyData = java.util.Arrays.copyOfRange(deriveData, 0, 32);
        var chainCode = new KeyParameter(deriveData, 32, 32);

        return new PrivateKeyECDSA(new BigInteger(1, keyData), chainCode);
    }

    @Override
    public PublicKey getPublicKey() {
        if (publicKey != null) {
            return publicKey;
        }

        var q = ECDSA_SECP256K1_DOMAIN.getG().multiply(keyData);
        var publicParams = new ECPublicKeyParameters(q, ECDSA_SECP256K1_DOMAIN);
        publicKey = PublicKeyECDSA.fromBytesInternal(publicParams.getQ().getEncoded(true));
        return publicKey;
    }

    public KeyParameter getChainCode() {
        return chainCode;
    }

    @Override
    public byte[] sign(byte[] message) {
        var hash = Crypto.calcKeccak256(message);

        var signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(keyData, ECDSA_SECP256K1_DOMAIN));
        BigInteger[] bigSig = signer.generateSignature(hash);

        byte[] sigBytes = Arrays.copyOf(bigIntTo32Bytes(bigSig[0]), 64);
        System.arraycopy(bigIntTo32Bytes(bigSig[1]), 0, sigBytes, 32, 32);

        return sigBytes;
    }

    public int getRecoveryId(byte[] r, byte[] s, byte[] message) {
        int recId = -1;
        var hash = Crypto.calcKeccak256(message);
        var publicKey = getPublicKey().toBytesRaw();
        // On this curve, there are only two possible values for the recovery id.
        for (int i = 0; i < 2; i++) {
            byte[] k = Crypto.recoverPublicKeyECDSAFromSignature(i, new BigInteger(1, r), new BigInteger(1, s), hash);
            if (k != null && java.util.Arrays.equals(k, publicKey)) {
                recId = i;
                break;
            }
        }

        if (recId == -1) {
            // this should never happen
            throw new IllegalStateException("Unexpected error - could not construct a recoverable key.");
        }

        return recId;
    }

    @Override
    public byte[] toBytes() {
        return toBytesDER();
    }

    /**
     * Create a big int byte array.
     *
     * @param n                         the big integer
     * @return                          the 32 byte array
     */
    private static byte[] bigIntTo32Bytes(BigInteger n) {
        byte[] bytes = n.toByteArray();
        byte[] bytes32 = new byte[32];
        System.arraycopy(
                bytes,
                Math.max(0, bytes.length - 32),
                bytes32,
                Math.max(0, 32 - bytes.length),
                Math.min(32, bytes.length));
        return bytes32;
    }

    @Override
    public byte[] toBytesRaw() {
        return bigIntTo32Bytes(keyData);
    }

    @Override
    public byte[] toBytesDER() {
        try {
            return new ECPrivateKey(
                            256,
                            keyData,
                            new DERBitString(getPublicKey().toBytesRaw()),
                            new X962Parameters(ID_ECDSA_SECP256K1))
                    .getEncoded("DER");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isED25519() {
        return false;
    }

    @Override
    public boolean isECDSA() {
        return true;
    }
}
