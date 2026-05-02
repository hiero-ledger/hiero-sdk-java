// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.math.BigInteger;
import javax.annotation.Nullable;

/**
 * @deprecated - User mirror nodes for contract traceability instead
 *
 * A storage slot change description.
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/smart-contracts/contractcalllocal#storagechange">Hedera Documentation</a>
 */
@Deprecated
public class StorageChange {
    /**
     * The storage slot changed. Up to 32 bytes, big-endian, zero bytes left trimmed
     */
    public final BigInteger slot;
    /**
     * The value read from the storage slot. Up to 32 bytes, big-endian, zero
     * bytes left trimmed. Because of the way SSTORE operations are charged
     * the slot is always read before being written to
     */
    public final BigInteger valueRead;
    /**
     * The new value written to the slot. Up to 32 bytes, big-endian, zero
     * bytes left trimmed. If a value of zero is written the valueWritten
     * will be present but the inner value will be absent. If a value was
     * read and not written this value will not be present.
     */
    @Nullable
    public final BigInteger valueWritten;

    /**
     * Constructor.
     *
     * @param slot                      the storage slot charged
     * @param valueRead                 the value read
     * @param valueWritten              the value written
     */
    StorageChange(BigInteger slot, BigInteger valueRead, @Nullable BigInteger valueWritten) {
        this.slot = slot;
        this.valueRead = valueRead;
        this.valueWritten = valueWritten;
    }

    // /**
    //  * Create a storage charge from a protobuf.
    //  *
    //  * @param storageChangeProto        the protobuf
    //  * @return                          the new storage charge object
    //  */
    // static StorageChange fromProtobuf(org.hiero.sdk.proto.StorageChange storageChangeProto) {
    //     return new StorageChange(
    //         new BigInteger(storageChangeProto.getSlot().toByteArray()),
    //         new BigInteger(storageChangeProto.getValueRead().toByteArray()),
    //         storageChangeProto.hasValueWritten() ? (
    //             storageChangeProto.getValueWritten().getValue().size() == 0 ?
    //                 BigInteger.ZERO :
    //                 new BigInteger(storageChangeProto.getValueWritten().getValue().toByteArray())
    //         ) : null
    //     );
    // }
    //
    // /**
    //  * Create a storage charge from a byte array.
    //  *
    //  * @param bytes                     the byte array
    //  * @return                          the new storage charge object
    //  * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
    //  */
    // public static StorageChange fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
    //     return fromProtobuf(org.hiero.sdk.proto.StorageChange.parseFrom(bytes));
    // }
    //
    // /**
    //  * Create the byte array.
    //  *
    //  * @return                          the byte array representation
    //  */
    // org.hiero.sdk.proto.StorageChange toProtobuf() {
    //     var builder = org.hiero.sdk.proto.StorageChange.newBuilder()
    //         .setSlot(ByteString.copyFrom(slot.toByteArray()))
    //         .setValueRead(ByteString.copyFrom(valueRead.toByteArray()));
    //     if (valueWritten != null) {
    //         if (valueWritten.equals(BigInteger.ZERO)) {
    //             builder.setValueWritten(BytesValue.newBuilder().setValue(ByteString.EMPTY).build());
    //         } else {
    //
    // builder.setValueWritten(BytesValue.newBuilder().setValue(ByteString.copyFrom(valueWritten.toByteArray())).build());
    //         }
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
