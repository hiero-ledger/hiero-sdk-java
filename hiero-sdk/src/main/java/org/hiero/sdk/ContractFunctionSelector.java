// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * Builder class for Solidity function selectors.
 */
public final class ContractFunctionSelector {
    @Nullable
    private Keccak.Digest256 digest;

    private boolean needsComma = false;

    @Nullable
    private byte[] finished = null;

    /**
     * Start building a selector for a function with a given name.
     *
     * @param funcName The name of the function
     */
    public ContractFunctionSelector(String funcName) {
        digest = new Keccak.Digest256();
        digest.update(funcName.getBytes(US_ASCII));
        digest.update((byte) '(');
    }

    /**
     * Add a string parameter.
     *
     * @return                          string
     */
    public ContractFunctionSelector addString() {
        return addParamType("string");
    }

    /**
     * Add a string array parameter.
     *
     * @return                          array string
     */
    public ContractFunctionSelector addStringArray() {
        return addParamType("string[]");
    }

    /**
     * Add a bytes parameter.
     *
     * @return                          bytes
     */
    public ContractFunctionSelector addBytes() {
        return addParamType("bytes");
    }

    /**
     * Add a bytes array parameter.
     *
     * @return                          bytes array
     */
    public ContractFunctionSelector addBytesArray() {
        return addParamType("bytes[]");
    }

    /**
     * Add a bytes 32 parameter.
     *
     * @return                          bytes 32
     */
    public ContractFunctionSelector addBytes32() {
        return addParamType("bytes32");
    }

    /**
     * Add a bytes 32 array parameter.
     *
     * @return                          bytes 32 array
     */
    public ContractFunctionSelector addBytes32Array() {
        return addParamType("bytes32[]");
    }

    /**
     * Add a bool parameter.
     *
     * @return                          bool
     */
    public ContractFunctionSelector addBool() {
        return addParamType("bool");
    }

    /**
     * Add an int 8 parameter.
     *
     * @return                          int 8
     */
    public ContractFunctionSelector addInt8() {
        return addParamType("int8");
    }

    /**
     * Add an int 32 parameter.
     *
     * @return                          int 32
     */
    public ContractFunctionSelector addInt32() {
        return addParamType("int32");
    }

    /**
     * Add an int 64 parameter.
     *
     * @return                          int 64
     */
    public ContractFunctionSelector addInt64() {
        return addParamType("int64");
    }

    /**
     * Add an int 256 parameter.
     *
     * @return                          int 256
     */
    public ContractFunctionSelector addInt256() {
        return addParamType("int256");
    }

    /**
     * Add an int 8 array parameter.
     *
     * @return                          int 8 array
     */
    public ContractFunctionSelector addInt8Array() {
        return addParamType("int8[]");
    }

    /**
     * Add an int 32 array parameter.
     *
     * @return                          int 32 array
     */
    public ContractFunctionSelector addInt32Array() {
        return addParamType("int32[]");
    }

    /**
     * Add an int 64 array parameter.
     *
     * @return                          int 64 array
     */
    public ContractFunctionSelector addInt64Array() {
        return addParamType("int64[]");
    }

    /**
     * Add an int 256 array parameter.
     *
     * @return                          int 256 array
     */
    public ContractFunctionSelector addInt256Array() {
        return addParamType("int256[]");
    }

    /**
     * Add an unsigned int 8 parameter.
     *
     * @return                          unsigned int 8
     */
    public ContractFunctionSelector addUint8() {
        return addParamType("uint8");
    }

    /**
     * Add an unsigned int 32 parameter.
     *
     * @return                          unsigned int 32
     */
    public ContractFunctionSelector addUint32() {
        return addParamType("uint32");
    }

    /**
     * Add an unsigned int 64 parameter.
     *
     * @return                          unsigned int 64
     */
    public ContractFunctionSelector addUint64() {
        return addParamType("uint64");
    }

    /**
     * Add an unsigned int 256 parameter.
     *
     * @return                          unsigned int 256
     */
    public ContractFunctionSelector addUint256() {
        return addParamType("uint256");
    }

    /**
     * Add an unsigned int 8 array parameter.
     *
     * @return                          unsigned int 8 array
     */
    public ContractFunctionSelector addUint8Array() {
        return addParamType("uint8[]");
    }

    /**
     * Add an unsigned int 32 array parameter.
     *
     * @return                          unsigned int 32 array
     */
    public ContractFunctionSelector addUint32Array() {
        return addParamType("uint32[]");
    }

    /**
     * Add an unsigned int 64 array parameter.
     *
     * @return                          unsigned int 64 array
     */
    public ContractFunctionSelector addUint64Array() {
        return addParamType("uint64[]");
    }

    /**
     * Add an unsigned int 256 array parameter.
     *
     * @return                          unsigned int 256 array
     */
    public ContractFunctionSelector addUint256Array() {
        return addParamType("uint256[]");
    }

    /**
     * Add an address parameter.
     *
     * @return                          address
     */
    public ContractFunctionSelector addAddress() {
        return addParamType("address");
    }

    /**
     * Add an address array parameter.
     *
     * @return                          address array
     */
    public ContractFunctionSelector addAddressArray() {
        return addParamType("address[]");
    }

    /**
     * Add a function parameter.
     *
     * @return                          function.
     */
    public ContractFunctionSelector addFunction() {
        return addParamType("function");
    }

    /**
     * Add a Solidity type name to this selector;
     * {@see https://solidity.readthedocs.io/en/v0.5.9/types.html}
     *
     * @param typeName the name of the Solidity type for a parameter.
     * @return {@code this}
     * @throws IllegalStateException if {@link #finish()} has already been called.
     */
    @SuppressWarnings({"NullableDereference"})
    ContractFunctionSelector addParamType(String typeName) {
        if (finished != null) {
            throw new IllegalStateException("FunctionSelector already finished");
        }

        Objects.requireNonNull(digest);

        if (needsComma) {
            digest.update((byte) ',');
        }

        digest.update(typeName.getBytes(US_ASCII));
        needsComma = true;

        return this;
    }

    /**
     * Complete the function selector after all parameters have been added and get the selector
     * bytes.
     * <p>
     * No more parameters may be added after this method call.
     * <p>
     * However, this can be called multiple times; it will always return the same result.
     *
     * @return the computed selector bytes.
     */
    @SuppressWarnings({"NullableDereference"})
    byte[] finish() {
        if (finished == null) {
            Objects.requireNonNull(digest);
            digest.update((byte) ')');
            finished = Arrays.copyOf(digest.digest(), 4);
            // release digest state
            digest = null;
        }

        return finished;
    }
}
