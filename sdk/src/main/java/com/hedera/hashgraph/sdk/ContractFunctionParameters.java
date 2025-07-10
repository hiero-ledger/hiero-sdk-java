// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

// an implementation of function selector and parameter encoding as specified here:
// https://solidity.readthedocs.io/en/v0.5.7/abi-spec.html#

/**
 * Builder for encoding parameters for a Solidity contract constructor/function call.
 * <p>
 * If you require a type which is not supported here, please let us know on
 * <a href="https://github.com/hashgraph/hedera-sdk-java/issues/298">this Github issue</a>.
 */
public final class ContractFunctionParameters {
    /**
     * The length of a Solidity address in bytes.
     */
    public static final int ADDRESS_LEN = EntityIdHelper.SOLIDITY_ADDRESS_LEN;

    /**
     * The length of a hexadecimal-encoded Solidity address, in ASCII characters (bytes).
     */
    public static final int ADDRESS_LEN_HEX = EntityIdHelper.SOLIDITY_ADDRESS_LEN_HEX;

    /**
     * Function selector length in bytes
     */
    public static final int SELECTOR_LEN = 4;

    /**
     * Function selector length in hex characters
     */
    public static final int SELECTOR_LEN_HEX = 8;

    // padding that we can substring without new allocations
    private static final ByteString padding = ByteString.copyFrom(new byte[31]);
    private static final ByteString negativePadding;

    static {
        byte[] fill = new byte[31];
        Arrays.fill(fill, (byte) 0xFF);
        negativePadding = ByteString.copyFrom(fill);
    }

    private final ArrayList<Argument> args = new ArrayList<>();

    private static ByteString encodeString(String string) {
        ByteString strBytes = ByteString.copyFromUtf8(string);
        // prepend the size of the string in UTF-8 bytes
        return int256(strBytes.size(), 32).concat(rightPad32(strBytes));
    }

    private static ByteString encodeBytes(byte[] bytes) {
        return int256(bytes.length, 32).concat(rightPad32(ByteString.copyFrom(bytes)));
    }

    private static ByteString encodeBytes4(byte[] bytes) {
        if (bytes.length > 4) {
            throw new IllegalArgumentException("bytes4 encoding forbids byte array length greater than 4");
        }
        return rightPad32(ByteString.copyFrom(bytes));
    }

    private static ByteString encodeBytes32(byte[] bytes) {
        if (bytes.length > 32) {
            throw new IllegalArgumentException("byte32 encoding forbids byte array length greater than 32");
        }

        return rightPad32(ByteString.copyFrom(bytes));
    }

    private static ByteString encodeBool(boolean bool) {
        return int256(bool ? 1 : 0, 8);
    }

    private static ByteString encodeArray(Stream<ByteString> elements) {
        List<ByteString> list = elements.collect(Collectors.toList());

        return int256(list.size(), 32).concat(ByteString.copyFrom(list));
    }

    private static ByteString encodeDynArr(List<ByteString> elements) {
        int offsetsLen = elements.size();

        // [len, offset[0], offset[1], ... offset[len - 1]]
        ArrayList<ByteString> head = new ArrayList<>(offsetsLen + 1);

        head.add(uint256(elements.size(), 32));

        // points to start of dynamic segment, *not* including the length of the array
        long currOffset = offsetsLen * 32L;

        for (ByteString elem : elements) {
            head.add(uint256(currOffset, 64));
            currOffset += elem.size();
        }

        return ByteString.copyFrom(head).concat(ByteString.copyFrom(elements));
    }

    static ByteString int256(long val, int bitWidth) {
        return int256(val, bitWidth, true);
    }

    static ByteString int256(long val, int bitWidth, boolean signed) {
        // don't try to get wider than a `long` as it should just be filled with padding
        bitWidth = Math.min(bitWidth, 64);
        ByteString.Output output = ByteString.newOutput(bitWidth / 8);

        try {
            // write bytes in big-endian order
            for (int i = bitWidth - 8; i >= 0; i -= 8) {
                // widening conversion sign-extends so we don't have to do anything special when
                // truncating a previously widened value
                byte u8 = (byte) (val >> i);
                output.write(u8);
            }

            // byte padding will sign-extend appropriately
            return leftPad32(output.toByteString(), signed && val < 0);
        } finally {
            try {
                output.close();
            } catch (Throwable ignored) {
                // do nothing
            }
        }
    }

    static byte[] getTruncatedBytes(BigInteger bigInt, int bitWidth) {
        byte[] bytes = bigInt.toByteArray();
        int expectedBytes = bitWidth / 8;
        return bytes.length <= expectedBytes
                ? bytes
                : Arrays.copyOfRange(bytes, bytes.length - expectedBytes, bytes.length);
    }

    static ByteString int256(BigInteger bigInt, int bitWidth) {
        return leftPad32(getTruncatedBytes(bigInt, bitWidth), bigInt.signum() < 0);
    }

    static ByteString uint256(long val, int bitWidth) {
        return int256(val, bitWidth, false);
    }

    static ByteString uint256(BigInteger bigInt, int bitWidth) {
        if (bigInt.signum() < 0) {
            throw new IllegalArgumentException("negative BigInteger passed to unsigned function");
        }
        return leftPad32(getTruncatedBytes(bigInt, bitWidth), false);
    }

    static ByteString leftPad32(ByteString input) {
        return leftPad32(input, false);
    }

    // Solidity contracts require all parameters to be padded to 32 byte multiples but specifies
    // different requirements for padding for strings/byte arrays vs integers
    static ByteString leftPad32(ByteString input, boolean negative) {
        int rem = 32 - input.size() % 32;
        return rem == 32
                ? input
                : (negative ? negativePadding : padding).substring(0, rem).concat(input);
    }

    static ByteString leftPad32(byte[] input, boolean negative) {
        return leftPad32(ByteString.copyFrom(input), negative);
    }

    static ByteString rightPad32(ByteString input) {
        int rem = 32 - input.size() % 32;
        return rem == 32 ? input : input.concat(padding.substring(0, rem));
    }

    private static byte[] decodeAddress(String address) {
        address = address.startsWith("0x") ? address.substring(2) : address;

        if (address.length() != ADDRESS_LEN_HEX) {
            throw new IllegalArgumentException("Solidity addresses must be 40 hex chars");
        }

        try {
            return Hex.decode(address);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("failed to decode Solidity address as hex", e);
        }
    }

    /**
     * Add a parameter of type {@code string}.
     * <p>
     * For Solidity addresses, use {@link #addAddress(String)}.
     *
     * @param param The String to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addString(String param) {
        args.add(new Argument("string", encodeString(param), true));

        return this;
    }

    /**
     * Add a parameter of type {@code string[]}.
     *
     * @param strings The array of Strings to be added
     * @return {@code this}
     * @throws NullPointerException if any value in `strings` is null
     */
    public ContractFunctionParameters addStringArray(String[] strings) {
        List<ByteString> byteStrings = Arrays.stream(strings)
                .map(ContractFunctionParameters::encodeString)
                .collect(Collectors.toList());

        ByteString argBytes = encodeDynArr(byteStrings);

        args.add(new Argument("string[]", argBytes, true));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes}, a byte-string.
     *
     * @param param The byte-string to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addBytes(byte[] param) {
        args.add(new Argument("bytes", encodeBytes(param), true));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes[]}, an array of byte-strings.
     *
     * @param param The array of byte-strings to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addBytesArray(byte[][] param) {
        List<ByteString> byteArrays = Arrays.stream(param)
                .map(ContractFunctionParameters::encodeBytes)
                .collect(Collectors.toList());

        args.add(new Argument("bytes[]", encodeDynArr(byteArrays), true));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes4}, a 4-byte fixed-length byte-string.
     *
     * @param param The 4-byte array to be added
     * @return {@code this}
     * @throws IllegalArgumentException if the length of the byte array is not 4.
     */
    public ContractFunctionParameters addBytes4(byte[] param) {
        args.add(new Argument("bytes4", encodeBytes4(param), false));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes4[]}, an array of 4-byte fixed-length byte-strings.
     *
     * @param param The array of 4-byte arrays to be added
     * @return {@code this}
     * @throws IllegalArgumentException if the length of any byte array is not 4.
     */
    public ContractFunctionParameters addBytes4Array(byte[][] param) {
        Stream<ByteString> byteArrays = Arrays.stream(param).map(ContractFunctionParameters::encodeBytes4);

        args.add(new Argument("bytes4[]", encodeArray(byteArrays), true));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes32}, a 32-byte byte-string.
     * <p>
     * If applicable, the array will be right-padded with zero bytes to a length of 32 bytes.
     *
     * @param param The byte-string to be added
     * @return {@code this}
     * @throws IllegalArgumentException if the length of the byte array is greater than 32.
     */
    public ContractFunctionParameters addBytes32(byte[] param) {
        args.add(new Argument("bytes32", encodeBytes32(param), false));

        return this;
    }

    /**
     * Add a parameter of type {@code bytes32[]}, an array of 32-byte byte-strings.
     * <p>
     * Each byte array will be right-padded with zero bytes to a length of 32 bytes.
     *
     * @param param The array of byte-strings to be added
     * @return {@code this}
     * @throws IllegalArgumentException if the length of any byte array is greater than 32.
     */
    public ContractFunctionParameters addBytes32Array(byte[][] param) {
        // array of fixed-size elements
        Stream<ByteString> byteArrays = Arrays.stream(param).map(ContractFunctionParameters::encodeBytes32);

        args.add(new Argument("bytes32[]", encodeArray(byteArrays), true));

        return this;
    }

    /**
     * Add a boolean parameter
     *
     * @param bool The bool to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addBool(boolean bool) {
        // boolean encodes to `uint8` of values [0, 1]
        args.add(new Argument("bool", encodeBool(bool), false));
        return this;
    }

    /**
     * Add a boolean array parameter
     *
     * @param param The array of booleans to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addBoolArray(boolean[] param) {
        Boolean[] boolWrapperArray = new Boolean[param.length];
        for (int i = 0; i < param.length; i++) {
            boolWrapperArray[i] = param[i];
        }

        Stream<ByteString> bools = Arrays.stream(boolWrapperArray).map(ContractFunctionParameters::encodeBool);

        args.add(new Argument("bool[]", encodeArray(bools), true));

        return this;
    }

    /**
     * Add an 8-bit integer.
     * <p>
     * The implementation is wasteful as we must pad to 32-bytes to store 1 byte.
     *
     * @param value The value to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt8(byte value) {
        args.add(new Argument("int8", int256(value, 8), false));

        return this;
    }

    /**
     * Add a 16-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt16(int value) {
        args.add(new Argument("int16", int256(value, 16), false));

        return this;
    }

    /**
     * Add a 24-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt24(int value) {
        args.add(new Argument("int24", int256(value, 24), false));

        return this;
    }

    /**
     * Add a 32-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt32(int value) {
        args.add(new Argument("int32", int256(value, 32), false));

        return this;
    }

    /**
     * Add a 40-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt40(long value) {
        args.add(new Argument("int40", int256(value, 40), false));

        return this;
    }

    /**
     * Add a 48-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt48(long value) {
        args.add(new Argument("int48", int256(value, 48), false));

        return this;
    }

    /**
     * Add a 56-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt56(long value) {
        args.add(new Argument("int56", int256(value, 56), false));

        return this;
    }

    /**
     * Add a 64-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt64(long value) {
        args.add(new Argument("int64", int256(value, 64), false));

        return this;
    }

    /**
     * Add a 72-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt72(BigInteger value) {
        args.add(new Argument("int72", int256(value, 72), false));

        return this;
    }

    /**
     * Add a 80-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt80(BigInteger value) {
        args.add(new Argument("int80", int256(value, 80), false));

        return this;
    }

    /**
     * Add a 88-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt88(BigInteger value) {
        args.add(new Argument("int88", int256(value, 88), false));

        return this;
    }

    /**
     * Add a 96-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt96(BigInteger value) {
        args.add(new Argument("int96", int256(value, 96), false));

        return this;
    }

    /**
     * Add a 104-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt104(BigInteger value) {
        args.add(new Argument("int104", int256(value, 104), false));

        return this;
    }

    /**
     * Add a 112-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt112(BigInteger value) {
        args.add(new Argument("int112", int256(value, 112), false));

        return this;
    }

    /**
     * Add a 120-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt120(BigInteger value) {
        args.add(new Argument("int120", int256(value, 120), false));

        return this;
    }

    /**
     * Add a 128-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt128(BigInteger value) {
        args.add(new Argument("int128", int256(value, 128), false));

        return this;
    }

    /**
     * Add a 136-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt136(BigInteger value) {
        args.add(new Argument("int136", int256(value, 136), false));

        return this;
    }

    /**
     * Add a 144-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt144(BigInteger value) {
        args.add(new Argument("int144", int256(value, 144), false));

        return this;
    }

    /**
     * Add a 152-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt152(BigInteger value) {
        args.add(new Argument("int152", int256(value, 152), false));

        return this;
    }

    /**
     * Add a 160-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt160(BigInteger value) {
        args.add(new Argument("int160", int256(value, 160), false));

        return this;
    }

    /**
     * Add a 168-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt168(BigInteger value) {
        args.add(new Argument("int168", int256(value, 168), false));

        return this;
    }

    /**
     * Add a 176-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt176(BigInteger value) {
        args.add(new Argument("int176", int256(value, 176), false));

        return this;
    }

    /**
     * Add a 184-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt184(BigInteger value) {
        args.add(new Argument("int184", int256(value, 184), false));

        return this;
    }

    /**
     * Add a 192-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt192(BigInteger value) {
        args.add(new Argument("int192", int256(value, 192), false));

        return this;
    }

    /**
     * Add a 200-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt200(BigInteger value) {
        args.add(new Argument("int200", int256(value, 200), false));

        return this;
    }

    /**
     * Add a 208-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt208(BigInteger value) {
        args.add(new Argument("int208", int256(value, 208), false));

        return this;
    }

    /**
     * Add a 216-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt216(BigInteger value) {
        args.add(new Argument("int216", int256(value, 216), false));

        return this;
    }

    /**
     * Add a 224-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt224(BigInteger value) {
        args.add(new Argument("int224", int256(value, 224), false));

        return this;
    }

    /**
     * Add a 232-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt232(BigInteger value) {
        args.add(new Argument("int232", int256(value, 232), false));

        return this;
    }

    /**
     * Add a 240-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt240(BigInteger value) {
        args.add(new Argument("int240", int256(value, 240), false));

        return this;
    }

    /**
     * Add a 248-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt248(BigInteger value) {
        args.add(new Argument("int248", int256(value, 248), false));

        return this;
    }

    /**
     * Add a 256-bit integer.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt256(BigInteger value) {
        args.add(new Argument("int256", int256(value, 256), false));

        return this;
    }

    /**
     * Add a dynamic array of 8-bit integers.
     * <p>
     * The implementation is wasteful as we must pad to 32-bytes to store 1 byte.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt8Array(byte[] intArray) {
        IntStream intStream = IntStream.range(0, intArray.length).map(idx -> intArray[idx]);

        ByteString arrayBytes =
                ByteString.copyFrom(intStream.mapToObj(i -> int256(i, 8)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int8[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 16-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt16Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 16)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int16[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 24-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt24Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 24)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int24[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 32-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt32Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 32)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int32[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 40-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt40Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 40)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int40[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 48-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt48Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 48)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int48[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 56-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt56Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 56)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int56[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 64-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt64Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> int256(i, 64)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int64[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 72-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt72Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 72)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int72[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 80-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt80Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 80)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int80[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 88-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt88Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 88)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int88[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 96-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt96Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 96)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int96[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 104-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt104Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 104)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int104[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 112-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt112Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 112)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int112[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 120-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt120Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 120)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int120[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 128-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt128Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 128)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int128[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 136-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt136Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 136)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int136[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 144-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt144Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 144)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int144[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 152-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt152Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 152)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int152[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 160-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt160Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 160)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int160[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 168-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt168Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 168)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int168[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 176-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt176Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 176)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int176[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 184-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt184Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 184)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int184[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 192-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt192Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 192)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int192[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 200-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt200Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 200)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int200[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 208-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt208Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 208)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int208[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 216-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt216Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 216)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int216[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 224-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt224Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 224)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int224[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 232-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt232Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 232)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int232[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 240-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt240Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 240)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int240[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 248-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt248Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 248)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int248[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 256-bit integers.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addInt256Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> int256(i, 256)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("int256[]", arrayBytes, true));

        return this;
    }

    /**
     * Add an unsigned 8-bit integer.
     * <p>
     * The implementation is wasteful as we must pad to 32-bytes to store 1 byte.
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint8(byte value) {
        args.add(new Argument("uint8", uint256(value, 8), false));

        return this;
    }

    /**
     * Add a 16-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint16(int value) {
        args.add(new Argument("uint16", uint256(value, 16), false));

        return this;
    }

    /**
     * Add a 24-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint24(int value) {
        args.add(new Argument("uint24", uint256(value, 24), false));

        return this;
    }

    /**
     * Add a 32-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint32(int value) {
        args.add(new Argument("uint32", uint256(value, 32), false));

        return this;
    }

    /**
     * Add a 40-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint40(long value) {
        args.add(new Argument("uint40", uint256(value, 40), false));

        return this;
    }

    /**
     * Add a 48-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint48(long value) {
        args.add(new Argument("uint48", uint256(value, 48), false));

        return this;
    }

    /**
     * Add a 56-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint56(long value) {
        args.add(new Argument("uint56", uint256(value, 56), false));

        return this;
    }

    /**
     * Add a 64-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint64(long value) {
        args.add(new Argument("uint64", uint256(value, 64), false));

        return this;
    }

    /**
     * Add a 72-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint72(BigInteger value) {
        args.add(new Argument("uint72", uint256(value, 72), false));

        return this;
    }

    /**
     * Add a 80-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint80(BigInteger value) {
        args.add(new Argument("uint80", uint256(value, 80), false));

        return this;
    }

    /**
     * Add a 88-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint88(BigInteger value) {
        args.add(new Argument("uint88", uint256(value, 88), false));

        return this;
    }

    /**
     * Add a 96-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint96(BigInteger value) {
        args.add(new Argument("uint96", uint256(value, 96), false));

        return this;
    }

    /**
     * Add a 104-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint104(BigInteger value) {
        args.add(new Argument("uint104", uint256(value, 104), false));

        return this;
    }

    /**
     * Add a 112-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint112(BigInteger value) {
        args.add(new Argument("uint112", uint256(value, 112), false));

        return this;
    }

    /**
     * Add a 120-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint120(BigInteger value) {
        args.add(new Argument("uint120", uint256(value, 120), false));

        return this;
    }

    /**
     * Add a 128-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint128(BigInteger value) {
        args.add(new Argument("uint128", uint256(value, 128), false));

        return this;
    }

    /**
     * Add a 136-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint136(BigInteger value) {
        args.add(new Argument("uint136", uint256(value, 136), false));

        return this;
    }

    /**
     * Add a 144-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint144(BigInteger value) {
        args.add(new Argument("uint144", uint256(value, 144), false));

        return this;
    }

    /**
     * Add a 152-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint152(BigInteger value) {
        args.add(new Argument("uint152", uint256(value, 152), false));

        return this;
    }

    /**
     * Add a 160-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint160(BigInteger value) {
        args.add(new Argument("uint160", uint256(value, 160), false));

        return this;
    }

    /**
     * Add a 168-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint168(BigInteger value) {
        args.add(new Argument("uint168", uint256(value, 168), false));

        return this;
    }

    /**
     * Add a 176-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint176(BigInteger value) {
        args.add(new Argument("uint176", uint256(value, 176), false));

        return this;
    }

    /**
     * Add a 184-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint184(BigInteger value) {
        args.add(new Argument("uint184", uint256(value, 184), false));

        return this;
    }

    /**
     * Add a 192-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint192(BigInteger value) {
        args.add(new Argument("uint192", uint256(value, 192), false));

        return this;
    }

    /**
     * Add a 200-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint200(BigInteger value) {
        args.add(new Argument("uint200", uint256(value, 200), false));

        return this;
    }

    /**
     * Add a 208-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint208(BigInteger value) {
        args.add(new Argument("uint208", uint256(value, 208), false));

        return this;
    }

    /**
     * Add a 216-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint216(BigInteger value) {
        args.add(new Argument("uint216", uint256(value, 216), false));

        return this;
    }

    /**
     * Add a 224-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint224(BigInteger value) {
        args.add(new Argument("uint224", uint256(value, 224), false));

        return this;
    }

    /**
     * Add a 232-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint232(BigInteger value) {
        args.add(new Argument("uint232", uint256(value, 232), false));

        return this;
    }

    /**
     * Add a 240-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint240(BigInteger value) {
        args.add(new Argument("uint240", uint256(value, 240), false));

        return this;
    }

    /**
     * Add a 248-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint248(BigInteger value) {
        args.add(new Argument("uint248", uint256(value, 248), false));

        return this;
    }

    /**
     * Add a 256-bit unsigned integer.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param value The integer to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint256(BigInteger value) {
        args.add(new Argument("uint256", uint256(value, 256), false));

        return this;
    }

    /**
     * Add a dynamic array of unsigned 8-bit integers.
     * <p>
     * The implementation is wasteful as we must pad to 32-bytes to store 1 byte.
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint8Array(byte[] intArray) {
        IntStream intStream = IntStream.range(0, intArray.length).map(idx -> intArray[idx]);

        ByteString arrayBytes =
                ByteString.copyFrom(intStream.mapToObj(i -> uint256(i, 8)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint8[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 16-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint16Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 16)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint16[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 24-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint24Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 24)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint24[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 32-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint32Array(int[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 32)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint32[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 40-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint40Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 40)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint40[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 48-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint48Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 48)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint48[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 56-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint56Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 56)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint56[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 64-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     */
    public ContractFunctionParameters addUint64Array(long[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).mapToObj(i -> uint256(i, 64)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint64[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 72-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint72Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 72)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint72[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 80-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint80Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 80)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint80[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 88-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint88Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 88)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint88[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 96-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint96Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 96)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint96[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 104-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint104Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 104)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint104[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 112-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint112Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 112)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint112[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 120-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint120Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 120)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint120[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 128-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint128Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 128)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint128[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 136-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint136Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 136)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint136[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 144-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint144Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 144)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint144[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 152-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint152Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 152)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint152[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 160-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint160Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 160)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint160[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 168-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint168Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 168)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint168[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 176-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint176Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 176)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint176[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 184-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint184Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 184)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint184[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 192-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint192Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 192)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint192[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 200-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint200Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 200)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint200[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 208-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint208Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 208)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint208[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 216-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint216Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 216)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint216[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 224-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint224Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 224)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint224[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 232-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint232Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 232)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint232[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 240-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint240Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 240)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint240[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 248-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint248Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 248)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint248[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a dynamic array of 256-bit unsigned integers.
     * <p>
     * The value will be treated as unsigned during encoding (it will be zero-padded instead of sign-extended to 32
     * bytes).
     *
     * @param intArray The array of integers to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code bigInt.signum() < 0}.
     */
    public ContractFunctionParameters addUint256Array(BigInteger[] intArray) {
        ByteString arrayBytes = ByteString.copyFrom(
                Arrays.stream(intArray).map(i -> uint256(i, 256)).collect(Collectors.toList()));

        arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);

        args.add(new Argument("uint256[]", arrayBytes, true));

        return this;
    }

    /**
     * Add a {@value ADDRESS_LEN_HEX}-character hex-encoded Solidity address parameter with the type {@code address}.
     * <p>
     * Note: adding a {@code address payable} or {@code contract} parameter must also use this function as the ABI does
     * not support those types directly.
     *
     * @param address The address to be added
     * @return {@code this}
     * @throws IllegalArgumentException if the address is not exactly {@value ADDRESS_LEN_HEX} characters long or fails
     *                                  to decode as hexadecimal.
     */
    public ContractFunctionParameters addAddress(String address) {
        byte[] addressBytes = decodeAddress(address);

        args.add(new Argument("address", leftPad32(ByteString.copyFrom(addressBytes)), false));

        return this;
    }

    /**
     * Add an array of {@value ADDRESS_LEN_HEX}-character hex-encoded Solidity addresses as a {@code address[]} param.
     *
     * @param addresses The array of addresses to be added
     * @return {@code this}
     * @throws IllegalArgumentException if any value is not exactly {@value ADDRESS_LEN_HEX} characters long or fails to
     *                                  decode as hexadecimal.
     * @throws NullPointerException     if any value in the array is null.
     */
    public ContractFunctionParameters addAddressArray(String[] addresses) {
        ByteString addressArray = encodeArray(Arrays.stream(addresses).map(a -> {
            byte[] address = decodeAddress(a);
            return leftPad32(ByteString.copyFrom(address));
        }));

        args.add(new Argument("address[]", addressArray, true));

        return this;
    }

    /**
     * Add a Solidity function reference as a {@value ADDRESS_LEN}-byte contract address and a
     * {@value SELECTOR_LEN}-byte function selector.
     *
     * @param address  a hex-encoded {@value ADDRESS_LEN_HEX}-character Solidity address.
     * @param selector a
     * @return {@code this}
     * @throws IllegalArgumentException if {@code address} is not {@value ADDRESS_LEN_HEX} characters or
     *                                  {@code selector} is not {@value SELECTOR_LEN} bytes.
     */
    public ContractFunctionParameters addFunction(String address, byte[] selector) {
        return addFunction(decodeAddress(address), selector);
    }

    /**
     * Add a Solidity function reference as a {@value ADDRESS_LEN}-byte contract address and a constructed
     * {@link ContractFunctionSelector}. The {@link ContractFunctionSelector} may not be modified after this call.
     *
     * @param address  The address used in the function to be added
     * @param selector The selector used in the function to be added
     * @return {@code this}
     * @throws IllegalArgumentException if {@code address} is not {@value ADDRESS_LEN_HEX} characters.
     */
    public ContractFunctionParameters addFunction(String address, ContractFunctionSelector selector) {
        // allow the `FunctionSelector` to be reused multiple times
        return addFunction(decodeAddress(address), selector.finish());
    }

    private ContractFunctionParameters addFunction(byte[] address, byte[] selector) {
        if (selector.length != SELECTOR_LEN) {
            throw new IllegalArgumentException("function selectors must be 4 bytes or 8 hex chars");
        }

        var output = ByteString.newOutput(ADDRESS_LEN + SELECTOR_LEN);
        try {
            output.write(address, 0, address.length);
            output.write(selector, 0, selector.length);

            // function reference encodes as `bytes24`
            args.add(new Argument("function", rightPad32(output.toByteString()), false));

            return this;
        } finally {
            try {
                output.close();
            } catch (Throwable ignored) {
                // do nothing
            }
        }
    }

    /**
     * Get the encoding of the currently added parameters as a {@link ByteString}.
     * <p>
     * You may continue to add parameters and call this again.
     *
     * @return the Solidity encoding of the call parameters in the order they were added.
     */
    public ByteString toBytes(@Nullable String funcName) {
        // offset for dynamic-length data, immediately after value arguments
        var dynamicOffset = args.size() * 32;

        var paramsBytes = new ArrayList<ByteString>(args.size() + 1);

        var dynamicArgs = new ArrayList<ByteString>();

        ContractFunctionSelector functionSelector = funcName != null ? new ContractFunctionSelector(funcName) : null;

        // iterate the arguments and determine whether they are dynamic or not
        for (Argument arg : args) {
            if (functionSelector != null) {
                functionSelector.addParamType(arg.type);
            }

            if (arg.isDynamic) {
                // dynamic arguments supply their offset in value position and append their data at
                // that offset
                paramsBytes.add(int256(dynamicOffset, 256));
                dynamicArgs.add(arg.value);
                dynamicOffset += arg.value.size();
            } else {
                // value arguments are dropped in the current arg position
                paramsBytes.add(arg.value);
            }
        }

        if (functionSelector != null) {
            paramsBytes.add(0, ByteString.copyFrom(functionSelector.finish()));
        }

        paramsBytes.addAll(dynamicArgs);

        return ByteString.copyFrom(paramsBytes);
    }

    private static final class Argument {
        private final String type;

        private final ByteString value;

        private final boolean isDynamic;

        private Argument(String type, ByteString value, boolean isDynamic) {
            this.type = type;
            if (!isDynamic && value.size() != 32) {
                throw new IllegalArgumentException("value argument that was not 32 bytes");
            }

            this.value = value;
            this.isDynamic = isDynamic;
        }
    }
}
