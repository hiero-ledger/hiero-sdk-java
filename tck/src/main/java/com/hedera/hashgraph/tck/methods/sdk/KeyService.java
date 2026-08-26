// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2RequestException;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.GenerateKeyParams;
import com.hedera.hashgraph.tck.methods.sdk.response.GenerateKeyResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import com.hedera.hashgraph.tck.util.KeyUtils.KeyType;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

@JSONRPC2Service
public class KeyService extends AbstractJSONRPC2Service {

    @JSONRPC2Method("generateKey")
    public GenerateKeyResponse generateKey(final GenerateKeyParams params) throws Exception {
        // Make sure getFromKey() is only provided for ED25519_PUBLIC_KEY, ECDSA_SECP256k1_PUBLIC_KEY, or
        // EVM_ADDRESS_KEY
        if (params.getFromKey().isPresent()
                && !params.getType().equals(KeyType.ED25519_PUBLIC_KEY)
                && !params.getType().equals(KeyType.ECDSA_SECP256K1_PUBLIC_KEY)
                && !params.getType().equals(KeyType.EVM_ADDRESS_KEY)) {
            throw new InvalidJSONRPC2RequestException(
                    "invalid parameters: fromKey should only be provided for ed25519PublicKey, ecdsaSecp256k1PublicKey, or evmAddress types.");
        }

        // Make sure threshold is only provided for THRESHOLD_KEY_TYPE.
        if (params.getThreshold().isPresent() && !params.getType().equals(KeyType.THRESHOLD_KEY)) {
            throw new InvalidJSONRPC2RequestException(
                    "invalid parameters: threshold should only be provided for thresholdKey types.");
        }

        // Make sure keys is only provided for LIST_KEY_TYPE or THRESHOLD_KEY_TYPE
        if (params.getKeys().isPresent()
                && !params.getType().equals(KeyType.LIST_KEY)
                && !params.getType().equals(KeyType.THRESHOLD_KEY)) {
            throw new InvalidJSONRPC2RequestException(
                    "invalid parameters: keys should only be provided for keyList or thresholdKey types.");
        }

        if ((params.getType().equals(KeyType.THRESHOLD_KEY) || params.getType().equals(KeyType.LIST_KEY))
                && params.getKeys().isEmpty()) {
            throw new InvalidJSONRPC2RequestException(
                    "invalid request: keys list is required for generating a KeyList type.");
        }

        if (params.getType().equals(KeyType.THRESHOLD_KEY)
                && params.getThreshold().isEmpty()) {
            throw new InvalidJSONRPC2RequestException(
                    "invalid request: threshold is required for generating a ThresholdKey type.");
        }

        List<String> privateKeys = new ArrayList<>();
        String key = processKeyRecursively(params, privateKeys, false);
        return new GenerateKeyResponse(key, privateKeys);
    }

    private String processKeyRecursively(final GenerateKeyParams params, final List<String> privateKeys, boolean isList)
            throws InvalidJSONRPC2RequestException, InvalidProtocolBufferException {
        String privateKeyString;
        PrivateKey privateKey;
        switch (params.getType()) {
            case ED25519_PRIVATE_KEY, ECDSA_SECP256K1_PRIVATE_KEY:
                privateKeyString = params.getType().equals(KeyType.ED25519_PRIVATE_KEY)
                        ? PrivateKey.generateED25519().toStringDER()
                        : PrivateKey.generateECDSA().toStringDER();
                if (isList) {
                    privateKeys.add(privateKeyString);
                }

                return privateKeyString;

            case ED25519_PUBLIC_KEY, ECDSA_SECP256K1_PUBLIC_KEY:
                if (params.getFromKey().isPresent()) {
                    return PrivateKey.fromString(params.getFromKey().get())
                            .getPublicKey()
                            .toStringDER();
                }
                privateKey = params.getType().equals(KeyType.ED25519_PUBLIC_KEY)
                        ? PrivateKey.generateED25519()
                        : PrivateKey.generateECDSA();
                if (isList) {
                    privateKeys.add(privateKey.toStringDER());
                }

                return privateKey.getPublicKey().toStringDER();

            case LIST_KEY, THRESHOLD_KEY:
                KeyList keyList = new KeyList();
                params.getKeys().get().forEach(keyParams -> {
                    try {
                        keyList.add(KeyUtils.getKeyFromString(processKeyRecursively(keyParams, privateKeys, true)));
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                });

                if (params.getType().equals(KeyType.THRESHOLD_KEY)) {
                    keyList.setThreshold(params.getThreshold().get().intValue());
                }

                return Hex.toHexString(keyList.toBytes());

            case EVM_ADDRESS_KEY:
                if (params.getFromKey().isPresent()) {
                    Key hederaKey =
                            KeyUtils.getKeyFromString(params.getFromKey().get());
                    if (hederaKey instanceof PrivateKey pk) {
                        return pk.getPublicKey().toEvmAddress().toString();
                    } else if (hederaKey instanceof PublicKey pk) {
                        return pk.toEvmAddress().toString();
                    } else {
                        throw new InvalidJSONRPC2RequestException(
                                "invalid parameters: fromKey for evmAddress is not ECDSAsecp256k1.");
                    }
                }
                return PrivateKey.generateECDSA().getPublicKey().toEvmAddress().toString();

            default:
                throw new InvalidJSONRPC2RequestException("invalid request: key type not recognized.");
        }
    }
}
