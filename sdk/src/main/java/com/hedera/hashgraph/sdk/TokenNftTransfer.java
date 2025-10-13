// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.NftTransfer;
import com.hedera.hashgraph.sdk.proto.TokenID;
import com.hedera.hashgraph.sdk.proto.TokenTransferList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal utility class.
 */
public class TokenNftTransfer implements Comparable<TokenNftTransfer> {
    /**
     * The ID of the token
     */
    public final TokenId tokenId;
    /**
     * The accountID of the sender
     */
    public final AccountId sender;
    /**
     * The accountID of the receiver
     */
    public final AccountId receiver;
    /**
     * The serial number of the NFT
     */
    public final long serial;
    /**
     * If true then the transfer is expected to be an approved allowance and the sender is expected to be the owner. The
     * default is false.
     */
    public boolean isApproved;

    // Optional hook calls for sender/receiver
    HookCall senderHookCall;
    HookCall receiverHookCall;
    NftHookType senderHookType;
    NftHookType receiverHookType;

    /**
     * Constructor.
     *
     * @param tokenId    the token id
     * @param sender     the sender account id
     * @param receiver   the receiver account id
     * @param serial     the serial number
     * @param isApproved is it approved
     */
    TokenNftTransfer(TokenId tokenId, AccountId sender, AccountId receiver, long serial, boolean isApproved) {
        this.tokenId = tokenId;
        this.sender = sender;
        this.receiver = receiver;
        this.serial = serial;
        this.isApproved = isApproved;
        this.senderHookCall = null;
        this.receiverHookCall = null;
        this.senderHookType = null;
        this.receiverHookType = null;
    }

    TokenNftTransfer(
            TokenId tokenId,
            AccountId sender,
            AccountId receiver,
            long serial,
            boolean isApproved,
            HookCall senderHookCall,
            NftHookType senderHookType,
            HookCall receiverHookCall,
            NftHookType receiverHookType) {
        this.tokenId = tokenId;
        this.sender = sender;
        this.receiver = receiver;
        this.serial = serial;
        this.isApproved = isApproved;
        this.senderHookCall = senderHookCall;
        this.senderHookType = senderHookType;
        this.receiverHookCall = receiverHookCall;
        this.receiverHookType = receiverHookType;
    }

    /**
     * Create a list of token nft transfer records from a protobuf.
     *
     * @param tokenTransferList the protobuf
     * @return the new list
     */
    static ArrayList<TokenNftTransfer> fromProtobuf(List<TokenTransferList> tokenTransferList) {
        var transfers = new ArrayList<TokenNftTransfer>();

        for (var tokenTransfer : tokenTransferList) {
            var tokenId = TokenId.fromProtobuf(tokenTransfer.getToken());

            for (var transfer : tokenTransfer.getNftTransfersList()) {
                transfers.add(new TokenNftTransfer(
                        tokenId,
                        AccountId.fromProtobuf(transfer.getSenderAccountID()),
                        AccountId.fromProtobuf(transfer.getReceiverAccountID()),
                        transfer.getSerialNumber(),
                        transfer.getIsApproval()));
            }
        }

        return transfers;
    }

    /**
     * Convert a byte array to a token NFT transfer object.
     *
     * @param bytes the byte array
     * @return the converted token nft transfer object
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    @Deprecated
    public static TokenNftTransfer fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(List.of(TokenTransferList.newBuilder()
                        .setToken(TokenID.newBuilder().build())
                        .addNftTransfers(NftTransfer.parseFrom(bytes))
                        .build()))
                .get(0);
    }

    /**
     * Create the protobuf.
     *
     * @return the protobuf representation
     */
    NftTransfer toProtobuf() {
        var builder = NftTransfer.newBuilder()
                .setSenderAccountID(sender.toProtobuf())
                .setReceiverAccountID(receiver.toProtobuf())
                .setSerialNumber(serial)
                .setIsApproval(isApproved);

        if (senderHookCall != null && senderHookType != null) {
            switch (senderHookType) {
                case PRE_HOOK_SENDER -> builder.setPreTxSenderAllowanceHook(senderHookCall.toProtobuf());
                case PRE_POST_HOOK_SENDER -> builder.setPrePostTxSenderAllowanceHook(senderHookCall.toProtobuf());
                default -> {}
            }
        }
        if (receiverHookCall != null && receiverHookType != null) {
            switch (receiverHookType) {
                case PRE_HOOK_RECEIVER -> builder.setPreTxReceiverAllowanceHook(receiverHookCall.toProtobuf());
                case PRE_POST_HOOK_RECEIVER -> builder.setPrePostTxReceiverAllowanceHook(receiverHookCall.toProtobuf());
                default -> {}
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("sender", sender)
                .add("receiver", receiver)
                .add("serial", serial)
                .add("isApproved", isApproved)
                .toString();
    }

    /**
     * Convert the token NFT transfer object to a byte array.
     *
     * @return the converted token NFT transfer object
     */
    @Deprecated
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public int compareTo(TokenNftTransfer o) {
        int senderComparison = sender.compareTo(o.sender);
        if (senderComparison != 0) {
            return senderComparison;
        }
        int receiverComparison = receiver.compareTo(o.receiver);
        if (receiverComparison != 0) {
            return receiverComparison;
        }
        return Long.compare(serial, o.serial);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TokenNftTransfer that = (TokenNftTransfer) o;
        return serial == that.serial
                && isApproved == that.isApproved
                && tokenId.equals(that.tokenId)
                && sender.equals(that.sender)
                && receiver.equals(that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenId, sender, receiver, serial, isApproved);
    }
}
