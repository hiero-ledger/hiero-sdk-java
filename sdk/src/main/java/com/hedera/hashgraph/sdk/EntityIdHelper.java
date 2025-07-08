// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

/**
 * Utility class used internally by the sdk.
 */
public class EntityIdHelper {
    /**
     * The length of a Solidity address in bytes.
     */
    static final int SOLIDITY_ADDRESS_LEN = 20;

    /**
     * The length of a hexadecimal-encoded Solidity address, in ASCII characters (bytes).
     */
    static final int SOLIDITY_ADDRESS_LEN_HEX = SOLIDITY_ADDRESS_LEN * 2;

    private static final Pattern ENTITY_ID_REGEX =
            Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([a-z]{5}))?$");

    static final Duration MIRROR_NODE_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Constructor.
     */
    private EntityIdHelper() {}

    /**
     * Generate an R object from a string.
     *
     * @param idString                  the id string
     * @param constructObjectWithIdNums the R object generator
     * @param <R>
     * @return the R type object
     */
    static <R> R fromString(String idString, WithIdNums<R> constructObjectWithIdNums) {
        var match = ENTITY_ID_REGEX.matcher(idString);
        if (!match.find()) {
            throw new IllegalArgumentException(
                    "Invalid ID \"" + idString + "\": format should look like 0.0.123 or 0.0.123-vfmkw");
        }
        return constructObjectWithIdNums.apply(
                Long.parseLong(match.group(1)),
                Long.parseLong(match.group(2)),
                Long.parseLong(match.group(3)),
                match.group(4));
    }

    /**
     * Generate an R object from a solidity address.
     *
     * @param address     the string representation
     * @param withAddress the R object generator
     * @param <R>
     * @return the R type object
     */
    static <R> R fromSolidityAddress(String address, WithIdNums<R> withAddress) {
        return fromSolidityAddress(decodeEvmAddress(address), withAddress);
    }

    private static <R> R fromSolidityAddress(byte[] address, WithIdNums<R> withAddress) {
        if (address.length != SOLIDITY_ADDRESS_LEN) {
            throw new IllegalArgumentException("Solidity addresses must be 20 bytes or 40 hex chars");
        }

        var buf = ByteBuffer.wrap(address);
        return withAddress.apply(buf.getInt(), buf.getLong(), buf.getLong(), null);
    }

    /**
     * Decode the solidity address from a string.
     *
     * @param address the string representation
     * @return the decoded address
     */
    static byte[] decodeEvmAddress(String address) {
        address = address.startsWith("0x") ? address.substring(2) : address;

        if (address.length() != SOLIDITY_ADDRESS_LEN_HEX) {
            throw new IllegalArgumentException("Solidity addresses must be 20 bytes or 40 hex chars");
        }

        try {
            return Hex.decode(address);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("failed to decode Solidity address as hex", e);
        }
    }

    /**
     * Generate a solidity address.
     *
     * @param shard the shard part
     * @param realm the realm part
     * @param num   the num part
     * @return the solidity address
     */
    static String toSolidityAddress(long shard, long realm, long num) {
        if (Long.highestOneBit(shard) > 32) {
            throw new IllegalStateException("shard out of 32-bit range " + shard);
        }

        return Hex.toHexString(ByteBuffer.allocate(20)
                .putInt((int) shard)
                .putLong(realm)
                .putLong(num)
                .array());
    }

    /**
     * Generate a checksum.
     *
     * @param ledgerId the ledger id
     * @param addr     the address
     * @return the checksum
     */
    static String checksum(LedgerId ledgerId, String addr) {
        StringBuilder answer = new StringBuilder();
        List<Integer> d =
                new ArrayList<>(); // Digits with 10 for ".", so if addr == "0.0.123" then d == [0, 10, 0, 10, 1, 2, 3]
        long s0 = 0; // Sum of even positions (mod 11)
        long s1 = 0; // Sum of odd positions (mod 11)
        long s = 0; // Weighted sum of all positions (mod p3)
        long sh = 0; // Hash of the ledger ID
        @SuppressWarnings("UnusedVariable")
        long c = 0; // The checksum, as a single number
        long p3 = 26 * 26 * 26; // 3 digits in base 26
        long p5 = 26 * 26 * 26 * 26 * 26; // 5 digits in base 26
        long asciiA = Character.codePointAt("a", 0); // 97
        long m = 1_000_003; // min prime greater than a million. Used for the final permutation.
        long w = 31; // Sum s of digit values weights them by powers of w. Should be coprime to p5.

        List<Byte> h = new ArrayList<>(ledgerId.toBytes().length + 6);
        for (byte b : ledgerId.toBytes()) {
            h.add(b);
        }
        for (int i = 0; i < 6; i++) {
            h.add((byte) 0);
        }
        for (var i = 0; i < addr.length(); i++) {
            d.add(addr.charAt(i) == '.' ? 10 : Integer.parseInt(String.valueOf(addr.charAt(i)), 10));
        }
        for (var i = 0; i < d.size(); i++) {
            s = (w * s + d.get(i)) % p3;
            if (i % 2 == 0) {
                s0 = (s0 + d.get(i)) % 11;
            } else {
                s1 = (s1 + d.get(i)) % 11;
            }
        }
        for (byte b : h) {
            // byte is signed in java, have to fake it to make bytes act like they're unsigned
            sh = (w * sh + (b < 0 ? 256 + b : b)) % p5;
        }
        c = ((((addr.length() % 5) * 11 + s0) * 11 + s1) * p3 + s + sh) % p5;
        c = (c * m) % p5;

        for (var i = 0; i < 5; i++) {
            answer.append((char) (asciiA + (c % 26)));
            c /= 26;
        }

        return answer.reverse().toString();
    }

    /**
     * Validate the configured client.
     *
     * @param shard    the shard part
     * @param realm    the realm part
     * @param num      the num part
     * @param client   the configured client
     * @param checksum the checksum
     * @throws BadEntityIdException
     */
    static void validate(long shard, long realm, long num, Client client, @Nullable String checksum)
            throws BadEntityIdException {
        if (client.getNetworkName() == null) {
            throw new IllegalStateException(
                    "Can't validate checksum without knowing which network the ID is for.  Ensure client's network name is set.");
        }
        if (checksum != null) {
            String expectedChecksum =
                    EntityIdHelper.checksum(client.getLedgerId(), EntityIdHelper.toString(shard, realm, num));
            if (!checksum.equals(expectedChecksum)) {
                throw new BadEntityIdException(shard, realm, num, checksum, expectedChecksum);
            }
        }
    }

    /**
     * Generate a string representation.
     *
     * @param shard the shard part
     * @param realm the realm part
     * @param num   the num part
     * @return the string representation
     */
    static String toString(long shard, long realm, long num) {
        return "" + shard + "." + realm + "." + num;
    }

    /**
     * Generate a string representation with a checksum.
     *
     * @param shard    the shard part
     * @param realm    the realm part
     * @param num      the num part
     * @param client   the configured client
     * @param checksum the checksum
     * @return the string representation with checksum
     */
    static String toStringWithChecksum(long shard, long realm, long num, Client client, @Nullable String checksum) {
        if (client.getLedgerId() != null) {
            return "" + shard + "." + realm + "." + num + "-"
                    + checksum(client.getLedgerId(), EntityIdHelper.toString(shard, realm, num));
        } else {
            throw new IllegalStateException(
                    "Can't derive checksum for ID without knowing which network the ID is for.  Ensure client's ledgerId is set.");
        }
    }

    /**
     * Takes an address as `byte[]` and returns whether this is a long-zero address
     * @param address
     * @return
     */
    static boolean isLongZeroAddress(byte[] address) {
        for (int i = 0; i < 12; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get AccountId num from mirror node using evm address.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 33
     * because it uses features introduced in API level 33 (Android 13).</p>*
     *
     * @param client
     * @param evmAddress
     */
    static CompletableFuture<Long> getAccountNumFromMirrorNodeAsync(Client client, String evmAddress) {
        String apiEndpoint = "/accounts/" + evmAddress;
        return performQueryToMirrorNodeAsync(client, apiEndpoint, null, false)
                .thenApply(response -> parseNumFromMirrorNodeResponse(response, "account"));
    }

    /**
     * Get EvmAddress from mirror node using account num.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 33
     * because it uses features introduced in API level 33 (Android 13).</p>*
     *
     * @param client
     * @param num
     */
    public static CompletableFuture<EvmAddress> getEvmAddressFromMirrorNodeAsync(Client client, long num) {
        String apiEndpoint = "/accounts/" + num;
        return performQueryToMirrorNodeAsync(client, apiEndpoint, null, false)
                .thenApply(response -> EvmAddress.fromString(parseStringMirrorNodeResponse(response, "evm_address")));
    }

    /**
     * Get ContractId num from mirror node using evm address.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 33
     * because it uses features introduced in API level 33 (Android 13).</p>*
     *
     * @param client
     * @param evmAddress
     */
    public static CompletableFuture<Long> getContractNumFromMirrorNodeAsync(Client client, String evmAddress) {
        String apiEndpoint = "/contracts/" + evmAddress;

        CompletableFuture<String> responseFuture = performQueryToMirrorNodeAsync(client, apiEndpoint, null, false);

        return responseFuture.thenApply(response -> parseNumFromMirrorNodeResponse(response, "contract_id"));
    }

    static CompletableFuture<String> performQueryToMirrorNodeAsync(
            Client client, String apiEndpoint, String jsonBody, boolean isContractCall) {
        Optional<String> mirrorUrl = client.getMirrorNetwork().stream()
                .map(url -> url.substring(0, url.indexOf(":")))
                .findFirst();

        if (mirrorUrl.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Mirror URL not found"));
        }

        String apiUrl = "https://" + mirrorUrl.get() + "/api/v1" + apiEndpoint;

        if (client.getLedgerId() == null) {
            if (isContractCall) {
                apiUrl = "http://" + mirrorUrl.get() + ":8545/api/v1" + apiEndpoint;
            } else {
                apiUrl = "http://" + mirrorUrl.get() + ":5551/api/v1" + apiEndpoint;
            }
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        var httpBuilder =
                HttpRequest.newBuilder().timeout(MIRROR_NODE_CONNECTION_TIMEOUT).uri(URI.create(apiUrl));

        if (jsonBody != null) {
            httpBuilder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        var httpRequest = httpBuilder.build();

        return httpClient
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .handle((response, ex) -> {
                    if (ex != null) {
                        if (ex instanceof HttpTimeoutException) {
                            throw new CompletionException(new RuntimeException("Request to Mirror Node timed out", ex));
                        } else {
                            throw new CompletionException(
                                    new RuntimeException("Failed to send request to Mirror Node", ex));
                        }
                    }

                    int statusCode = response.statusCode();
                    if (statusCode != 200) {
                        throw new CompletionException(
                                new RuntimeException("Received non-200 response from Mirror Node: " + response.body()));
                    }
                    return response.body();
                });
    }

    private static String parseStringMirrorNodeResponse(String responseBody, String memberName) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        String evmAddress = jsonObject.get(memberName).getAsString();
        return evmAddress.substring(evmAddress.lastIndexOf(".") + 1);
    }

    private static long parseNumFromMirrorNodeResponse(String responseBody, String memberName) {
        return Long.parseLong(parseStringMirrorNodeResponse(responseBody, memberName));
    }

    @FunctionalInterface
    interface WithIdNums<R> {
        R apply(long shard, long realm, long num, @Nullable String checksum);
    }
}
