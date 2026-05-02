// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

/**
 * A common base for the signing authority or key that entities in Hedera may have.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/key">Hedera Documentation</a>
 * @see KeyList
 * @see PublicKey
 */
public abstract class Key {
    static final ASN1ObjectIdentifier ID_ED25519 = new ASN1ObjectIdentifier("1.3.101.112");
    static final ASN1ObjectIdentifier ID_ECDSA_SECP256K1 = new ASN1ObjectIdentifier("1.3.132.0.10");
    static final ASN1ObjectIdentifier ID_EC_PUBLIC_KEY = new ASN1ObjectIdentifier("1.2.840.10045.2.1");

    static final X9ECParameters ECDSA_SECP256K1_CURVE = SECNamedCurves.getByName("secp256k1");
    static final ECDomainParameters ECDSA_SECP256K1_DOMAIN = new ECDomainParameters(
            ECDSA_SECP256K1_CURVE.getCurve(),
            ECDSA_SECP256K1_CURVE.getG(),
            ECDSA_SECP256K1_CURVE.getN(),
            ECDSA_SECP256K1_CURVE.getH());

    /**
     * Create a specific key type from the protobuf.
     *
     * @param key                       the protobuf key of unknown type
     * @return                          the differentiated key
     */
    static Key fromProtobufKey(org.hiero.sdk.proto.Key key) {
        switch (key.getKeyCase()) {
            case ED25519 -> {
                return PublicKeyED25519.fromBytesInternal(key.getEd25519().toByteArray());
            }
            case ECDSA_SECP256K1 -> {
                if (key.getECDSASecp256K1().size() == 20) {
                    return new EvmAddress(key.getECDSASecp256K1().toByteArray());
                } else {
                    return PublicKeyECDSA.fromBytesInternal(
                            key.getECDSASecp256K1().toByteArray());
                }
            }
            case KEYLIST -> {
                return KeyList.fromProtobuf(key.getKeyList(), null);
            }
            case THRESHOLDKEY -> {
                return KeyList.fromProtobuf(
                        key.getThresholdKey().getKeys(), key.getThresholdKey().getThreshold());
            }
            case CONTRACTID -> {
                return ContractId.fromProtobuf(key.getContractID());
            }
            case DELEGATABLE_CONTRACT_ID -> {
                return DelegateContractId.fromProtobuf(key.getDelegatableContractId());
            }
            case KEY_NOT_SET -> {
                return null;
            }
            default -> throw new IllegalStateException("Key#fromProtobuf: unhandled key case: " + key.getKeyCase());
        }
    }

    /**
     * Serialize this key as a protobuf object
     */
    abstract org.hiero.sdk.proto.Key toProtobufKey();

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobufKey().toByteArray();
    }

    /**
     * Create Key from proto.Key byte array
     *
     * @param bytes
     * @return Key representation
     * @throws InvalidProtocolBufferException
     */
    public static Key fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobufKey(org.hiero.sdk.proto.Key.parseFrom(bytes));
    }
}
