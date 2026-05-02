// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.List;

/**
 * @deprecated - User mirror nodes for contract traceability instead
 *
 * The storage changes to a smart contract's storage as a side effect of the function call.
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/smart-contracts/contractcalllocal#contractstatechange">Hedera Documentation</a>
 */
@Deprecated
public class ContractStateChange {
    /**
     * The contract to which the storage changes apply to
     */
    public final ContractId contractId;

    /**
     * The list of storage changes
     */
    public final List<StorageChange> storageChanges;

    /**
     * Constructor.
     *
     * @param contractId                the contract id
     * @param storageChanges            the list of storage change objects
     */
    ContractStateChange(ContractId contractId, List<StorageChange> storageChanges) {
        this.contractId = contractId;
        this.storageChanges = storageChanges;
    }

    // /**
    //  * Create contract stage change object from protobuf.
    //  *
    //  * @param stateChangeProto          the protobuf
    //  * @return                          the contract stage change object
    //  */
    // static ContractStateChange fromProtobuf(org.hiero.sdk.proto.ContractStateChange stateChangeProto) {
    //     List<StorageChange> storageChanges = new ArrayList<>(stateChangeProto.getStorageChangesCount());
    //     for (var storageChangeProto : stateChangeProto.getStorageChangesList()) {
    //         storageChanges.add(StorageChange.fromProtobuf(storageChangeProto));
    //     }
    //     return new ContractStateChange(ContractId.fromProtobuf(stateChangeProto.getContractID()), storageChanges);
    // }
    //
    // /**
    //  * Create contract stage change object from byte array.
    //  *
    //  * @param bytes                     the byte array
    //  * @return                          the contract stage change object
    //  * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
    //  */
    // public static ContractStateChange fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
    //     return fromProtobuf(org.hiero.sdk.proto.ContractStateChange.parseFrom(bytes));
    // }
    //
    // /**
    //  * Create the protobuf.
    //  *
    //  * @return                          the protobuf representation
    //  */
    // org.hiero.sdk.proto.ContractStateChange toProtobuf() {
    //     var builder = org.hiero.sdk.proto.ContractStateChange.newBuilder()
    //         .setContractID(contractId.toProtobuf());
    //     for (var storageChange : storageChanges) {
    //         builder.addStorageChanges(storageChange.toProtobuf());
    //     }
    //     return builder.build();
    // }
    //
    // /**
    //  * Create the byte array.
    //  *
    //  * @return                          the byte array representation
    //  */
    // public byte[] toBytes() {
    //     return toProtobuf().toByteArray();
    // }
}
