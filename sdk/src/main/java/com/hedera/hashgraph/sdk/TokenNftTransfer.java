// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.TransferTransaction.toNftHook;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.NftTransfer;
import com.hedera.hashgraph.sdk.proto.TokenID;
import com.hedera.hashgraph.sdk.proto.TokenTransferList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

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

    // Optional typed hook calls for sender/receiver
    NftHookCall senderHookCall;
    NftHookCall receiverHookCall;

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
    }

    TokenNftTransfer(
            TokenId tokenId,
            AccountId sender,
            AccountId receiver,
            long serial,
            boolean isApproved,
            @Nullable NftHookCall senderHookCall,
            @Nullable NftHookCall receiverHookCall) {
        this.tokenId = tokenId;
        this.sender = sender;
        this.receiver = receiver;
        this.serial = serial;
        this.isApproved = isApproved;
        this.senderHookCall = senderHookCall;
        this.receiverHookCall = receiverHookCall;
    }

    static List<TokenNftTransfer> fromProtobuf(TokenTransferList tokenTransferList) {
        var token = TokenId.fromProtobuf(tokenTransferList.getToken());
        var nftTransfers = new ArrayList<TokenNftTransfer>();

        for (var transfer : tokenTransferList.getNftTransfersList()) {
            NftHookCall senderHookCall = null;
            NftHookCall receiverHookCall = null;

            if (transfer.hasPreTxSenderAllowanceHook()) {
                senderHookCall = toNftHook(transfer.getPreTxSenderAllowanceHook(), NftHookType.PRE_HOOK_SENDER);
            } else if (transfer.hasPrePostTxSenderAllowanceHook()) {
                senderHookCall =
                        toNftHook(transfer.getPrePostTxSenderAllowanceHook(), NftHookType.PRE_POST_HOOK_SENDER);
            }

            if (transfer.hasPreTxReceiverAllowanceHook()) {
                receiverHookCall = toNftHook(transfer.getPreTxReceiverAllowanceHook(), NftHookType.PRE_HOOK_RECEIVER);
            } else if (transfer.hasPrePostTxReceiverAllowanceHook()) {
                receiverHookCall =
                        toNftHook(transfer.getPrePostTxReceiverAllowanceHook(), NftHookType.PRE_POST_HOOK_RECEIVER);
            }

            var sender = AccountId.fromProtobuf(transfer.getSenderAccountID());
            var receiver = AccountId.fromProtobuf(transfer.getReceiverAccountID());

            nftTransfers.add(new TokenNftTransfer(
                    token,
                    sender,
                    receiver,
                    transfer.getSerialNumber(),
                    transfer.getIsApproval(),
                    senderHookCall,
                    receiverHookCall));
        }
        return nftTransfers;
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
        return fromProtobuf(TokenTransferList.newBuilder()
                        .setToken(TokenID.newBuilder().build())
                        .addNftTransfers(NftTransfer.parseFrom(bytes))
                        .build())
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

        if (senderHookCall != null) {
            switch (senderHookCall.getType()) {
                case PRE_HOOK_SENDER -> builder.setPreTxSenderAllowanceHook(senderHookCall.toProtobuf());
                case PRE_POST_HOOK_SENDER -> builder.setPrePostTxSenderAllowanceHook(senderHookCall.toProtobuf());
                default -> {}
            }
        }
        if (receiverHookCall != null) {
            switch (receiverHookCall.getType()) {
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
