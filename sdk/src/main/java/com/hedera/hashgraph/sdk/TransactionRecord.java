// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AccountAmount;
import com.hedera.hashgraph.sdk.proto.NftTransfer;
import com.hedera.hashgraph.sdk.proto.TokenTransferList;
import com.hedera.hashgraph.sdk.proto.TransferList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * The complete record for a transaction on Hedera that has reached consensus.
 * <p>
 * This is not-free to request and is available for 1 hour after a transaction reaches consensus.
 * <p>
 * A {@link TransactionReceipt} can be thought of as a light-weight record which is free to ask for if you just
 * need what it contains. A receipt however lasts for only 180 seconds.
 */
public final class TransactionRecord {
    /**
     * The status (reach consensus, or failed, or is unknown) and the ID of
     * any new account/file/instance created.
     */
    public final TransactionReceipt receipt;

    /**
     * The hash of the Transaction that executed (not the hash of any Transaction that failed for
     * having a duplicate TransactionID).
     */
    public final ByteString transactionHash;

    /**
     * The consensus timestamp (or null if didn't reach consensus yet).
     */
    public final Instant consensusTimestamp;

    /**
     * The ID of the transaction this record represents.
     */
    public final TransactionId transactionId;

    /**
     * The memo that was submitted as part of the transaction (max 100 bytes).
     */
    public final String transactionMemo;

    /**
     * The actual transaction fee charged, not the original
     * transactionFee value from TransactionBody.
     */
    public final Hbar transactionFee;

    /**
     * Record of the value returned by the smart contract
     * function or constructor.
     */
    @Nullable
    public final ContractFunctionResult contractFunctionResult;

    /**
     * All hbar transfers as a result of this transaction, such as fees, or
     * transfers performed by the transaction, or by a smart contract it calls,
     * or by the creation of threshold records that it triggers.
     */
    public final List<Transfer> transfers;

    /**
     * All fungible token transfers as a result of this transaction as a map
     */
    public final Map<TokenId, Map<AccountId, Long>> tokenTransfers;

    /**
     * All fungible token transfers as a result of this transaction as a list
     */
    public final List<TokenTransfer> tokenTransferList;

    /**
     * All NFT Token transfers as a result of this transaction
     */
    public final Map<TokenId, List<TokenNftTransfer>> tokenNftTransfers;

    /**
     * Reference to the scheduled transaction ID that this transaction record represents
     */
    @Nullable
    public final ScheduleId scheduleRef;

    /**
     * All custom fees that were assessed during a CryptoTransfer, and must be paid if the
     * transaction status resolved to SUCCESS
     */
    public final List<AssessedCustomFee> assessedCustomFees;

    /**
     * All token associations implicitly created while handling this transaction
     */
    public final List<TokenAssociation> automaticTokenAssociations;

    /**
     * In the record of an internal CryptoCreate transaction triggered by a user
     * transaction with a (previously unused) alias, the new account's alias.
     */
    @Nullable
    public final PublicKey aliasKey;

    /**
     * The records of processing all child transaction spawned by the transaction with the given
     * top-level id, in consensus order. Always empty if the top-level status is UNKNOWN.
     */
    public final List<TransactionRecord> children;

    /**
     * The records of processing all consensus transaction with the same id as the distinguished
     * record above, in chronological order.
     */
    public final List<TransactionRecord> duplicates;

    /**
     * In the record of an internal transaction, the consensus timestamp of the user
     * transaction that spawned it.
     */
    @Nullable
    public final Instant parentConsensusTimestamp;

    /**
     * The keccak256 hash of the ethereumData. This field will only be populated for
     * EthereumTransaction.
     */
    public final ByteString ethereumHash;

    /**
     * An approved allowance of hbar transfers for a spender
     */
    @Deprecated
    public final List<HbarAllowance> hbarAllowanceAdjustments;

    /**
     * An approved allowance of token transfers for a spender
     */
    @Deprecated
    public final List<TokenAllowance> tokenAllowanceAdjustments;

    /**
     * An approved allowance of NFT transfers for a spender
     */
    @Deprecated
    public final List<TokenNftAllowance> tokenNftAllowanceAdjustments;

    /**
     * List of accounts with the corresponding staking rewards paid as a result of a transaction.
     */
    public final List<Transfer> paidStakingRewards;

    /**
     * In the record of a UtilPrng transaction with no output range, a pseudorandom 384-bit string.
     */
    @Nullable
    public final ByteString prngBytes;

    /**
     * In the record of a PRNG transaction with an output range, the output of a PRNG whose input was a 384-bit string.
     */
    @Nullable
    public final Integer prngNumber;

    /**
     * The new default EVM address of the account created by this transaction.
     * This field is populated only when the EVM address is not specified in the related transaction body.
     */
    public final ByteString evmAddress;

    /**
     * A list of pending token airdrops.
     * Each pending airdrop represents a single requested transfer from a
     * sending account to a recipient account. These pending transfers are
     * issued unilaterally by the sending account, and MUST be claimed by the
     * recipient account before the transfer MAY complete.
     * A sender MAY cancel a pending airdrop before it is claimed.
     * An airdrop transaction SHALL emit a pending airdrop when the recipient has no
     * available automatic association slots available or when the recipient
     * has set `receiver_sig_required`.
     */
    public final List<PendingAirdropRecord> pendingAirdropRecords;

    TransactionRecord(
            TransactionReceipt transactionReceipt,
            ByteString transactionHash,
            Instant consensusTimestamp,
            TransactionId transactionId,
            String transactionMemo,
            long transactionFee,
            @Nullable ContractFunctionResult contractFunctionResult,
            List<Transfer> transfers,
            Map<TokenId, Map<AccountId, Long>> tokenTransfers,
            List<TokenTransfer> tokenTransferList,
            Map<TokenId, List<TokenNftTransfer>> tokenNftTransfers,
            @Nullable ScheduleId scheduleRef,
            List<AssessedCustomFee> assessedCustomFees,
            List<TokenAssociation> automaticTokenAssociations,
            @Nullable PublicKey aliasKey,
            List<TransactionRecord> children,
            List<TransactionRecord> duplicates,
            @Nullable Instant parentConsensusTimestamp,
            ByteString ethereumHash,
            List<Transfer> paidStakingRewards,
            @Nullable ByteString prngBytes,
            @Nullable Integer prngNumber,
            ByteString evmAddress,
            List<PendingAirdropRecord> pendingAirdropRecords) {
        this.receipt = transactionReceipt;
        this.transactionHash = transactionHash;
        this.consensusTimestamp = consensusTimestamp;
        this.transactionMemo = transactionMemo;
        this.transactionId = transactionId;
        this.transfers = transfers;
        this.contractFunctionResult = contractFunctionResult;
        this.transactionFee = Hbar.fromTinybars(transactionFee);
        this.tokenTransfers = tokenTransfers;
        this.tokenTransferList = tokenTransferList;
        this.tokenNftTransfers = tokenNftTransfers;
        this.scheduleRef = scheduleRef;
        this.assessedCustomFees = assessedCustomFees;
        this.automaticTokenAssociations = automaticTokenAssociations;
        this.aliasKey = aliasKey;
        this.children = children;
        this.duplicates = duplicates;
        this.parentConsensusTimestamp = parentConsensusTimestamp;
        this.ethereumHash = ethereumHash;
        this.pendingAirdropRecords = pendingAirdropRecords;
        this.hbarAllowanceAdjustments = Collections.emptyList();
        this.tokenAllowanceAdjustments = Collections.emptyList();
        this.tokenNftAllowanceAdjustments = Collections.emptyList();
        this.paidStakingRewards = paidStakingRewards;
        this.prngBytes = prngBytes;
        this.prngNumber = prngNumber;
        this.evmAddress = evmAddress;
    }

    /**
     * Create a transaction record from a protobuf.
     *
     * @param transactionRecord the protobuf
     * @param children          the list of children
     * @param duplicates        the list of duplicates
     * @return the new transaction record
     */
    static TransactionRecord fromProtobuf(
            com.hedera.hashgraph.sdk.proto.TransactionRecord transactionRecord,
            List<TransactionRecord> children,
            List<TransactionRecord> duplicates,
            @Nullable TransactionId transactionId) {
        var transfers =
                new ArrayList<Transfer>(transactionRecord.getTransferList().getAccountAmountsCount());
        for (var accountAmount : transactionRecord.getTransferList().getAccountAmountsList()) {
            transfers.add(Transfer.fromProtobuf(accountAmount));
        }

        var tokenTransfers = new HashMap<TokenId, Map<AccountId, Long>>();
        var tokenNftTransfers = new HashMap<TokenId, List<TokenNftTransfer>>();
        var allTokenTransfers = new ArrayList<TokenTransfer>();

        for (var transferList : transactionRecord.getTokenTransferListsList()) {
            var tokenTransfersList = TokenTransfer.fromProtobuf(transferList);
            var nftTransfersList = TokenNftTransfer.fromProtobuf(transferList);
            for (var transfer : tokenTransfersList) {
                var current = tokenTransfers.containsKey(transfer.tokenId)
                        ? tokenTransfers.get(transfer.tokenId)
                        : new HashMap<AccountId, Long>();
                current.put(transfer.accountId, transfer.amount);
                tokenTransfers.put(transfer.tokenId, current);
            }
            allTokenTransfers.addAll(tokenTransfersList);

            for (var transfer : nftTransfersList) {
                var current = tokenNftTransfers.containsKey(transfer.tokenId)
                        ? tokenNftTransfers.get(transfer.tokenId)
                        : new ArrayList<TokenNftTransfer>();
                current.add(transfer);
                tokenNftTransfers.put(transfer.tokenId, current);
            }
        }

        var fees = new ArrayList<AssessedCustomFee>(transactionRecord.getAssessedCustomFeesCount());
        for (var fee : transactionRecord.getAssessedCustomFeesList()) {
            fees.add(AssessedCustomFee.fromProtobuf(fee));
        }

        // HACK: This is a bit bad, any takers to clean this up
        var contractFunctionResult = transactionRecord.hasContractCallResult()
                ? new ContractFunctionResult(transactionRecord.getContractCallResult())
                : transactionRecord.hasContractCreateResult()
                        ? new ContractFunctionResult(transactionRecord.getContractCreateResult())
                        : null;

        var automaticTokenAssociations =
                new ArrayList<TokenAssociation>(transactionRecord.getAutomaticTokenAssociationsCount());
        for (var tokenAssociation : transactionRecord.getAutomaticTokenAssociationsList()) {
            automaticTokenAssociations.add(TokenAssociation.fromProtobuf(tokenAssociation));
        }

        var aliasKey = PublicKey.fromAliasBytes(transactionRecord.getAlias());

        var paidStakingRewards = new ArrayList<Transfer>(transactionRecord.getPaidStakingRewardsCount());
        for (var reward : transactionRecord.getPaidStakingRewardsList()) {
            paidStakingRewards.add(Transfer.fromProtobuf(reward));
        }

        List<PendingAirdropRecord> pendingAirdropRecords = transactionRecord.getNewPendingAirdropsList().stream()
                .map(PendingAirdropRecord::fromProtobuf)
                .collect(Collectors.toList());

        return new TransactionRecord(
                TransactionReceipt.fromProtobuf(transactionRecord.getReceipt(), transactionId),
                transactionRecord.getTransactionHash(),
                InstantConverter.fromProtobuf(transactionRecord.getConsensusTimestamp()),
                TransactionId.fromProtobuf(transactionRecord.getTransactionID()),
                transactionRecord.getMemo(),
                transactionRecord.getTransactionFee(),
                contractFunctionResult,
                transfers,
                tokenTransfers,
                allTokenTransfers,
                tokenNftTransfers,
                transactionRecord.hasScheduleRef() ? ScheduleId.fromProtobuf(transactionRecord.getScheduleRef()) : null,
                fees,
                automaticTokenAssociations,
                aliasKey,
                children,
                duplicates,
                transactionRecord.hasParentConsensusTimestamp()
                        ? InstantConverter.fromProtobuf(transactionRecord.getParentConsensusTimestamp())
                        : null,
                transactionRecord.getEthereumHash(),
                paidStakingRewards,
                transactionRecord.hasPrngBytes() ? transactionRecord.getPrngBytes() : null,
                transactionRecord.hasPrngNumber() ? transactionRecord.getPrngNumber() : null,
                transactionRecord.getEvmAddress(),
                pendingAirdropRecords);
    }

    /**
     * Create a transaction record from a protobuf.
     *
     * @param transactionRecord the protobuf
     * @return the new transaction record
     */
    static TransactionRecord fromProtobuf(com.hedera.hashgraph.sdk.proto.TransactionRecord transactionRecord) {
        return fromProtobuf(transactionRecord, new ArrayList<>(), new ArrayList<>(), null);
    }

    /**
     * Create a transaction record from a byte array.
     *
     * @param bytes the byte array
     * @return the new transaction record
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static TransactionRecord fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.TransactionRecord.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Validate the transaction status in the receipt.
     *
     * @param shouldValidate Whether to perform transaction status validation
     * @return {@code this}
     * @throws ReceiptStatusException when shouldValidate is true and the transaction status is not SUCCESS
     */
    public TransactionRecord validateReceiptStatus(boolean shouldValidate) throws ReceiptStatusException {
        receipt.validateStatus(shouldValidate);
        return this;
    }

    /**
     * Create the protobuf.
     *
     * @return the protobuf representation
     */
    com.hedera.hashgraph.sdk.proto.TransactionRecord toProtobuf() {
        var transferList = TransferList.newBuilder();
        for (Transfer transfer : transfers) {
            transferList.addAccountAmounts(transfer.toProtobuf());
        }

        var transactionRecord = com.hedera.hashgraph.sdk.proto.TransactionRecord.newBuilder()
                .setReceipt(receipt.toProtobuf())
                .setTransactionHash(transactionHash)
                .setConsensusTimestamp(InstantConverter.toProtobuf(consensusTimestamp))
                .setTransactionID(transactionId.toProtobuf())
                .setMemo(transactionMemo)
                .setTransactionFee(transactionFee.toTinybars())
                .setTransferList(transferList)
                .setEthereumHash(ethereumHash)
                .setEvmAddress(evmAddress);

        for (var tokenEntry : tokenTransfers.entrySet()) {
            var tokenTransfersList =
                    TokenTransferList.newBuilder().setToken(tokenEntry.getKey().toProtobuf());
            for (var aaEntry : tokenEntry.getValue().entrySet()) {
                tokenTransfersList.addTransfers(AccountAmount.newBuilder()
                        .setAccountID(aaEntry.getKey().toProtobuf())
                        .setAmount(aaEntry.getValue())
                        .build());
            }

            transactionRecord.addTokenTransferLists(tokenTransfersList);
        }

        for (var nftEntry : tokenNftTransfers.entrySet()) {
            var nftTransferList =
                    TokenTransferList.newBuilder().setToken(nftEntry.getKey().toProtobuf());
            for (var aaEntry : nftEntry.getValue()) {
                nftTransferList.addNftTransfers(NftTransfer.newBuilder()
                        .setSenderAccountID(aaEntry.sender.toProtobuf())
                        .setReceiverAccountID(aaEntry.receiver.toProtobuf())
                        .setSerialNumber(aaEntry.serial)
                        .setIsApproval(aaEntry.isApproved)
                        .build());
            }

            transactionRecord.addTokenTransferLists(nftTransferList);
        }

        if (contractFunctionResult != null) {
            transactionRecord.setContractCallResult(contractFunctionResult.toProtobuf());
        }

        if (scheduleRef != null) {
            transactionRecord.setScheduleRef(scheduleRef.toProtobuf());
        }

        for (var fee : assessedCustomFees) {
            transactionRecord.addAssessedCustomFees(fee.toProtobuf());
        }

        for (var tokenAssociation : automaticTokenAssociations) {
            transactionRecord.addAutomaticTokenAssociations(tokenAssociation.toProtobuf());
        }

        if (aliasKey != null) {
            transactionRecord.setAlias(aliasKey.toProtobufKey().toByteString());
        }

        if (parentConsensusTimestamp != null) {
            transactionRecord.setParentConsensusTimestamp(InstantConverter.toProtobuf(parentConsensusTimestamp));
        }

        for (Transfer reward : paidStakingRewards) {
            transactionRecord.addPaidStakingRewards(reward.toProtobuf());
        }

        if (prngBytes != null) {
            transactionRecord.setPrngBytes(prngBytes);
        }

        if (prngNumber != null) {
            transactionRecord.setPrngNumber(prngNumber);
        }

        if (pendingAirdropRecords != null) {
            for (PendingAirdropRecord pendingAirdropRecord : pendingAirdropRecords) {
                transactionRecord.addNewPendingAirdrops(
                        pendingAirdropRecords.indexOf(pendingAirdropRecord), pendingAirdropRecord.toProtobuf());
            }
        }

        return transactionRecord.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("receipt", receipt)
                .add("transactionHash", Hex.toHexString(transactionHash.toByteArray()))
                .add("consensusTimestamp", consensusTimestamp)
                .add("transactionId", transactionId)
                .add("transactionMemo", transactionMemo)
                .add("transactionFee", transactionFee)
                .add("contractFunctionResult", contractFunctionResult)
                .add("transfers", transfers)
                .add("tokenTransfers", tokenTransfers)
                .add("tokenNftTransfers", tokenNftTransfers)
                .add("scheduleRef", scheduleRef)
                .add("assessedCustomFees", assessedCustomFees)
                .add("automaticTokenAssociations", automaticTokenAssociations)
                .add("aliasKey", aliasKey)
                .add("children", children)
                .add("duplicates", duplicates)
                .add("parentConsensusTimestamp", parentConsensusTimestamp)
                .add("ethereumHash", Hex.toHexString(ethereumHash.toByteArray()))
                .add("paidStakingRewards", paidStakingRewards)
                .add("prngBytes", prngBytes != null ? Hex.toHexString(prngBytes.toByteArray()) : null)
                .add("prngNumber", prngNumber)
                .add("evmAddress", Hex.toHexString(evmAddress.toByteArray()))
                .add("pendingAirdropRecords", pendingAirdropRecords.toString())
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
