// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Utility class used internally by the sdk.
 */
public class FeeComponents implements Cloneable {
    /**
     * A minimum, the calculated fee must be greater than this value
     */
    private long min;
    /**
     * A maximum, the calculated fee must be less than this value
     */
    private long max;
    /**
     * A constant contribution to the fee
     */
    private long constant;
    /**
     * The price of bandwidth consumed by a transaction, measured in bytes
     */
    private long transactionBandwidthByte;
    /**
     * The price per signature verification for a transaction
     */
    private long transactionVerification;
    /**
     * The price of RAM consumed by a transaction, measured in byte-hours
     */
    private long transactionRamByteHour;
    /**
     * The price of storage consumed by a transaction, measured in byte-hours
     */
    private long transactionStorageByteHour;
    /**
     * The price of computation for a smart contract transaction, measured in gas
     */
    private long contractTransactionGas;
    /**
     * The price per hbar transferred for a transfer
     */
    private long transferVolumeHbar;
    /**
     * The price of bandwidth for data retrieved from memory for a response, measured in bytes
     */
    private long responseMemoryByte;
    /**
     * The price of bandwidth for data retrieved from disk for a response, measured in bytes
     */
    private long responseDiskByte;

    /**
     * Constructor.
     */
    public FeeComponents() {}

    /**
     * Create a fee components object from a protobuf.
     *
     * @param feeComponents             the protobuf
     * @return                          the fee component object
     */
    static FeeComponents fromProtobuf(org.hiero.sdk.proto.FeeComponents feeComponents) {
        return new FeeComponents()
                .setMin(feeComponents.getMin())
                .setMax(feeComponents.getMax())
                .setConstant(feeComponents.getConstant())
                .setTransactionBandwidthByte(feeComponents.getBpt())
                .setTransactionVerification(feeComponents.getVpt())
                .setTransactionRamByteHour(feeComponents.getRbh())
                .setTransactionStorageByteHour(feeComponents.getSbh())
                .setContractTransactionGas(feeComponents.getGas())
                .setTransferVolumeHbar(feeComponents.getTv())
                .setResponseMemoryByte(feeComponents.getBpr())
                .setResponseDiskByte(feeComponents.getSbpr());
    }

    /**
     * Create a fee component object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the fee component object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static FeeComponents fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                org.hiero.sdk.proto.FeeComponents.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Extract the minimum component.
     *
     * @return                          the minimum component
     */
    public long getMin() {
        return min;
    }

    /**
     * Assign the minimum component.
     *
     * @param min                       the minimum component
     * @return {@code this}
     */
    public FeeComponents setMin(long min) {
        this.min = min;
        return this;
    }

    /**
     * Extract the maximum component.
     *
     * @return                          the maximum component
     */
    public long getMax() {
        return max;
    }

    /**
     * Assign the maximum component.
     *
     * @param max                       the maximum component
     * @return {@code this}
     */
    public FeeComponents setMax(long max) {
        this.max = max;
        return this;
    }

    /**
     * Extract the constant component.
     *
     * @return                          the constant component
     */
    public long getConstant() {
        return constant;
    }

    /**
     * Assign the constant component.
     *
     * @param constant                  the constant component
     * @return {@code this}
     */
    public FeeComponents setConstant(long constant) {
        this.constant = constant;
        return this;
    }

    /**
     * Extract the transaction bandwidth bytes.
     *
     * @return                          the transaction bandwidth bytes
     */
    public long getTransactionBandwidthByte() {
        return transactionBandwidthByte;
    }

    /**
     * Assign the transaction bandwidth bytes.
     *
     * @param transactionBandwidthByte  the transaction bandwidth bytes
     * @return {@code this}
     */
    public FeeComponents setTransactionBandwidthByte(long transactionBandwidthByte) {
        this.transactionBandwidthByte = transactionBandwidthByte;
        return this;
    }

    /**
     * Extract the transaction verification price per signature.
     *
     * @return                          the transaction verification price per signature
     */
    public long getTransactionVerification() {
        return transactionVerification;
    }

    /**
     * Assign the transaction verification price per signature.
     *
     * @param transactionVerification   the transaction verification price per signature
     * @return {@code this}
     */
    public FeeComponents setTransactionVerification(long transactionVerification) {
        this.transactionVerification = transactionVerification;
        return this;
    }

    /**
     * Extract the of ram consumed in byte hours.
     *
     * @return                          price of ram consumed in byte hours
     */
    public long getTransactionRamByteHour() {
        return transactionRamByteHour;
    }

    /**
     * Assign the price of ram consumed in byte hours.
     *
     * @param transactionRamByteHour    price of ram consumed in byte hours
     * @return {@code this}
     */
    public FeeComponents setTransactionRamByteHour(long transactionRamByteHour) {
        this.transactionRamByteHour = transactionRamByteHour;
        return this;
    }

    /**
     * Extract the of storage in byte hours.
     *
     * @return                          price of storage in byte hours
     */
    public long getTransactionStorageByteHour() {
        return transactionStorageByteHour;
    }

    /**
     * Assign the price of storage consumed in byte hours.
     *
     * @param transactionStorageByteHour    price of storage in byte hours
     * @return {@code this}
     */
    public FeeComponents setTransactionStorageByteHour(long transactionStorageByteHour) {
        this.transactionStorageByteHour = transactionStorageByteHour;
        return this;
    }

    /**
     * Extract the of gas for computation.
     *
     * @return                          price of gas for computation
     */
    public long getContractTransactionGas() {
        return contractTransactionGas;
    }

    /**
     * Assign the price of computation in gas.
     *
     * @param contractTransactionGas    price of gas for computation
     * @return {@code this}
     */
    public FeeComponents setContractTransactionGas(long contractTransactionGas) {
        this.contractTransactionGas = contractTransactionGas;
        return this;
    }

    /**
     * Extract the per hbar transferred.
     *
     * @return                          price per hbar transferred
     */
    public long getTransferVolumeHbar() {
        return transferVolumeHbar;
    }

    /**
     * Assign the price per hbar transferred.
     *
     * @param transferVolumeHbar        price per hbar transferred
     * @return {@code this}
     */
    public FeeComponents setTransferVolumeHbar(long transferVolumeHbar) {
        this.transferVolumeHbar = transferVolumeHbar;
        return this;
    }

    /**
     * Extract the for data retrieved.
     *
     * @return                          price for data retrieved
     */
    public long getResponseMemoryByte() {
        return responseMemoryByte;
    }

    /**
     * Assign the price for data retrieved in bytes.
     *
     * @param responseMemoryByte        price for data retrieved
     * @return {@code this}
     */
    public FeeComponents setResponseMemoryByte(long responseMemoryByte) {
        this.responseMemoryByte = responseMemoryByte;
        return this;
    }

    /**
     * Extract the for data retrieved from disk.
     *
     * @return                          price for data retrieved from disk
     */
    public long getResponseDiskByte() {
        return responseDiskByte;
    }

    /**
     * Assign the price for data retrieved from disk.
     *
     * @param responseDiskByte          price for data retrieved from disk
     * @return {@code this}
     */
    public FeeComponents setResponseDiskByte(long responseDiskByte) {
        this.responseDiskByte = responseDiskByte;
        return this;
    }

    /**
     * Convert fee component object to protobuf.
     *
     * @return                          the protobuf
     */
    org.hiero.sdk.proto.FeeComponents toProtobuf() {
        return org.hiero.sdk.proto.FeeComponents.newBuilder()
                .setMin(getMin())
                .setMax(getMax())
                .setConstant(getConstant())
                .setBpt(getTransactionBandwidthByte())
                .setVpt(getTransactionVerification())
                .setRbh(getTransactionRamByteHour())
                .setSbh(getTransactionStorageByteHour())
                .setGas(getContractTransactionGas())
                .setTv(getTransferVolumeHbar())
                .setBpr(getResponseMemoryByte())
                .setSbpr(getResponseDiskByte())
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("min", getMin())
                .add("max", getMax())
                .add("constant", getConstant())
                .add("transactionBandwidthByte", getTransactionBandwidthByte())
                .add("transactionVerification", getTransactionVerification())
                .add("transactionRamByteHour", getTransactionRamByteHour())
                .add("transactionStorageByteHour", getTransactionStorageByteHour())
                .add("contractTransactionGas", getContractTransactionGas())
                .add("transferVolumeHbar", getTransferVolumeHbar())
                .add("responseMemoryByte", getResponseMemoryByte())
                .add("responseDiskByte", getResponseDiskByte())
                .toString();
    }

    /**
     * Convert fee component object to byte array.
     *
     * @return                          the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public FeeComponents clone() {
        try {
            return (FeeComponents) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
