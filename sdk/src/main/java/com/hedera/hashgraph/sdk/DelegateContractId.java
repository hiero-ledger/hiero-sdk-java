// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ContractID;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ID for a smart contract instance on Hedera.
 */
public final class DelegateContractId extends ContractId {
    /**
     * Constructor.
     *
     * @param num                       the num portion of the contract id
     *
     * Constructor that uses shard, realm and num should be used instead
     * as shard and realm should not assume 0 value
     */
    @Deprecated
    public DelegateContractId(long num) {
        super(num);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard portion of the contract id
     * @param realm                     the realm portion of the contract id
     * @param num                       the num portion of the contract id
     */
    public DelegateContractId(long shard, long realm, long num) {
        super(shard, realm, num);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard portion of the contract id
     * @param realm                     the realm portion of the contract id
     * @param num                       the num portion of the contract id
     * @param checksum                  the optional checksum
     */
    DelegateContractId(long shard, long realm, long num, @Nullable String checksum) {
        super(shard, realm, num, checksum);
    }

    public DelegateContractId(long shard, long realm, byte[] evmAddress) {
        super(shard, realm, evmAddress);
    }

    /**
     * Create a delegate contract id from a string.
     *
     * @param id                        the contract id
     * @return                          the delegate contract id object
     */
    public static DelegateContractId fromString(String id) {
        return EntityIdHelper.fromString(id, DelegateContractId::new);
    }

    /**
     * Create a delegate contract id from a string.
     *
     * @param address                   the contract id solidity address
     * @return                          the delegate contract id object
     */
    @Deprecated
    public static DelegateContractId fromSolidityAddress(String address) {
        return EntityIdHelper.fromSolidityAddress(address, DelegateContractId::new);
    }

    /**
     * Parse DelegateContract id from an ethereum address.
     *
     * @param shard                     the desired shard
     * @param realm                     the desired realm
     * @param evmAddress                the evm address
     * @return                          the contract id object
     */
    public static DelegateContractId fromEvmAddress(
            @Nonnegative long shard, @Nonnegative long realm, String evmAddress) {
        EntityIdHelper.decodeEvmAddress(evmAddress);
        return new DelegateContractId(
                shard, realm, Hex.decode(evmAddress.startsWith("0x") ? evmAddress.substring(2) : evmAddress));
    }

    /**
     * Create a delegate contract id from a string.
     *
     * @param contractId                the contract id protobuf
     * @return                          the delegate contract id object
     */
    static DelegateContractId fromProtobuf(ContractID contractId) {
        Objects.requireNonNull(contractId);
        return new DelegateContractId(contractId.getShardNum(), contractId.getRealmNum(), contractId.getContractNum());
    }

    /**
     * Create a delegate contract id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the delegate contract id object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static DelegateContractId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ContractID.parseFrom(bytes).toBuilder().build());
    }

    @Override
    com.hedera.hashgraph.sdk.proto.Key toProtobufKey() {
        return com.hedera.hashgraph.sdk.proto.Key.newBuilder()
                .setDelegatableContractId(toProtobuf())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DelegateContractId) {
            DelegateContractId otherId = (DelegateContractId) o;
            return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
        } else if (o instanceof ContractId) {
            ContractId otherId = (ContractId) o;
            return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
        } else {
            return false;
        }
    }
}
