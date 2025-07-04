// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ContractID;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ID for a smart contract instance on Hedera.
 */
public class ContractId extends Key implements Comparable<ContractId> {
    static final Pattern EVM_ADDRESS_REGEX = Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.([a-fA-F0-9]{40}$)");
    /**
     * The shard number
     */
    @Nonnegative
    public final long shard;

    /**
     * The realm number
     */
    @Nonnegative
    public final long realm;

    /**
     * The id number
     */
    @Nonnegative
    public final long num;

    @Nullable
    private final String checksum;

    /**
     * The 20-byte EVM address of the contract to call.
     */
    @Nullable
    public final byte[] evmAddress;

    /**
     * Assign the num part of the contract id.
     *
     * @param num                       the num part of the account id
     *
     * Constructor that uses shard, realm and num should be used instead
     * as shard and realm should not assume 0 value
     */
    @Deprecated
    public ContractId(@Nonnegative long num) {
        this(0, 0, num);
    }

    /**
     * Assign all parts of the contract id.
     *
     * @param shard                     the shard part of the contract id
     * @param realm                     the realm part of the contract id
     * @param num                       the num part of the contract id
     */
    @SuppressWarnings("InconsistentOverloads")
    public ContractId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num) {
        this(shard, realm, num, null);
    }

    /**
     * Assign all parts of the contract id.
     *
     * @param shard                     the shard part of the contract id
     * @param realm                     the realm part of the contract id
     * @param num                       the num part of the contract id
     */
    @SuppressWarnings("InconsistentOverloads")
    ContractId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num, @Nullable String checksum) {
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.checksum = checksum;
        this.evmAddress = null;
    }

    ContractId(@Nonnegative long shard, @Nonnegative long realm, byte[] evmAddress) {
        this.shard = shard;
        this.realm = realm;
        this.evmAddress = evmAddress;
        this.num = 0;
        this.checksum = null;
    }

    /**
     * Parse contract id from a string.
     *
     * @param id                        the string containing a contract id
     * @return                          the contract id object
     */
    public static ContractId fromString(String id) {
        var match = EVM_ADDRESS_REGEX.matcher(id);
        if (match.find()) {
            return new ContractId(
                    Long.parseLong(match.group(1)), Long.parseLong(match.group(2)), Hex.decode(match.group(3)));
        } else {
            return EntityIdHelper.fromString(id, ContractId::new);
        }
    }

    /**
     * Retrieve the contract id from a solidity address.
     *
     * @param address                   a string representing the address
     * @return                          the contract id object
     * @deprecated This method is deprecated. Use {@link #fromEvmAddress(long, long, String)} instead.
     */
    @Deprecated
    public static ContractId fromSolidityAddress(String address) {
        if (EntityIdHelper.isLongZeroAddress(EntityIdHelper.decodeEvmAddress(address))) {
            return EntityIdHelper.fromSolidityAddress(address, ContractId::new);
        } else {
            return fromEvmAddress(0, 0, address);
        }
    }

    /**
     * Parse contract id from an ethereum address.
     *
     * @param shard                     the desired shard
     * @param realm                     the desired realm
     * @param evmAddress                the evm address
     * @return                          the contract id object
     */
    public static ContractId fromEvmAddress(@Nonnegative long shard, @Nonnegative long realm, String evmAddress) {
        EntityIdHelper.decodeEvmAddress(evmAddress);
        return new ContractId(
                shard, realm, Hex.decode(evmAddress.startsWith("0x") ? evmAddress.substring(2) : evmAddress));
    }

    /**
     * Extract a contract id from a protobuf.
     *
     * @param contractId                the protobuf containing a contract id
     * @return                          the contract id object
     */
    static ContractId fromProtobuf(ContractID contractId) {
        Objects.requireNonNull(contractId);
        if (contractId.hasEvmAddress()) {
            return new ContractId(
                    contractId.getShardNum(),
                    contractId.getRealmNum(),
                    contractId.getEvmAddress().toByteArray());
        } else {
            return new ContractId(contractId.getShardNum(), contractId.getRealmNum(), contractId.getContractNum());
        }
    }

    /**
     * Convert a byte array to an account balance object.
     *
     * @param bytes                     the byte array
     * @return                          the converted contract id object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static ContractId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ContractID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Extract the solidity address.
     *
     * @return                          the solidity address as a string
     * @deprecated This method is deprecated. Use {@link #toEvmAddress()} instead.
     */
    @Deprecated
    public String toSolidityAddress() {
        if (evmAddress != null) {
            return Hex.toHexString(evmAddress);
        } else {
            return EntityIdHelper.toSolidityAddress(shard, realm, num);
        }
    }

    /**
     * toEvmAddress returns EVM-compatible address representation of the entity
     * @return
     */
    public String toEvmAddress() {
        if (evmAddress != null) {
            return Hex.toHexString(evmAddress);
        } else {
            return EntityIdHelper.toSolidityAddress(0, 0, num);
        }
    }

    /**
     * Convert contract id to protobuf.
     *
     * @return                          the protobuf object
     */
    ContractID toProtobuf() {
        var builder = ContractID.newBuilder().setShardNum(shard).setRealmNum(realm);
        if (evmAddress != null) {
            builder.setEvmAddress(ByteString.copyFrom(evmAddress));
        } else {
            builder.setContractNum(num);
        }
        return builder.build();
    }

    /**
     *  Gets the actual `num` field of the `ContractId` from the Mirror Node.
     * Should be used after generating `ContractId.fromEvmAddress()` because it sets the `num` field to `0`
     * automatically since there is no connection between the `num` and the `evmAddress`
     * Sync version
     *
     * @param client
     * @return populated ContractId instance
     */
    public ContractId populateContractNum(Client client) throws InterruptedException, ExecutionException {
        return populateContractNumAsync(client).get();
    }

    /**
     * Gets the actual `num` field of the `ContractId` from the Mirror Node.
     * Should be used after generating `ContractId.fromEvmAddress()` because it sets the `num` field to `0`
     * automatically since there is no connection between the `num` and the `evmAddress`
     * Async version
     *
     * @deprecated Use 'populateContractNum' instead due to its nearly identical operation.
     * @param client
     * @return populated ContractId instance
     */
    @Deprecated
    public CompletableFuture<ContractId> populateContractNumAsync(Client client) {
        EvmAddress address = new EvmAddress(this.evmAddress);

        return EntityIdHelper.getContractNumFromMirrorNodeAsync(client, address.toString())
                .thenApply(contractNumFromMirrorNode ->
                        new ContractId(this.shard, this.realm, contractNumFromMirrorNode, checksum));
    }

    /**
     * @param client to validate against
     * @throws BadEntityIdException if entity ID is formatted poorly
     * @deprecated Use {@link #validateChecksum(Client)} instead.
     */
    @Deprecated
    public void validate(Client client) throws BadEntityIdException {
        validateChecksum(client);
    }

    /**
     * Verify the checksum.
     *
     * @param client                    to validate against
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    public void validateChecksum(Client client) throws BadEntityIdException {
        EntityIdHelper.validate(shard, realm, num, client, checksum);
    }

    /**
     * Extract the checksum.
     *
     * @return                          the checksum
     */
    @Nullable
    public String getChecksum() {
        return checksum;
    }

    @Override
    com.hedera.hashgraph.sdk.proto.Key toProtobufKey() {
        return com.hedera.hashgraph.sdk.proto.Key.newBuilder()
                .setContractID(toProtobuf())
                .build();
    }

    @Override
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        if (evmAddress != null) {
            return "" + shard + "." + realm + "." + Hex.toHexString(evmAddress);
        } else {
            return EntityIdHelper.toString(shard, realm, num);
        }
    }

    /**
     * Create a string representation that includes the checksum.
     *
     * @param client                    the client
     * @return                          the string representation with the checksum
     */
    public String toStringWithChecksum(Client client) {
        if (evmAddress != null) {
            throw new IllegalStateException("toStringWithChecksum cannot be applied to ContractId with evmAddress");
        } else {
            return EntityIdHelper.toStringWithChecksum(shard, realm, num, client, checksum);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(shard, realm, num, Arrays.hashCode(evmAddress));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContractId)) {
            return false;
        }

        ContractId otherId = (ContractId) o;
        return shard == otherId.shard && realm == otherId.realm && num == otherId.num && evmAddressMatches(otherId);
    }

    private boolean evmAddressMatches(ContractId otherId) {
        if ((evmAddress == null) != (otherId.evmAddress == null)) {
            return false;
        }
        if (evmAddress != null) {
            return Arrays.equals(evmAddress, otherId.evmAddress);
        }
        // both are null
        return true;
    }

    @Override
    public int compareTo(ContractId o) {
        Objects.requireNonNull(o);
        int shardComparison = Long.compare(shard, o.shard);
        if (shardComparison != 0) {
            return shardComparison;
        }
        int realmComparison = Long.compare(realm, o.realm);
        if (realmComparison != 0) {
            return realmComparison;
        }
        int numComparison = Long.compare(num, o.num);
        if (numComparison != 0) {
            return numComparison;
        }
        return evmAddressCompare(o);
    }

    private int evmAddressCompare(ContractId o) {
        int nullCompare = (evmAddress == null ? 0 : 1) - (o.evmAddress == null ? 0 : 1);
        if (nullCompare != 0) {
            return nullCompare;
        }
        if (evmAddress != null) {
            return Hex.toHexString(evmAddress).compareTo(Hex.toHexString(o.evmAddress));
        }
        // both are null
        return 0;
    }
}
