// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.*;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bouncycastle.crypto.digests.SHA384Digest;

/**
 * Base class for all transactions that may be built and submitted to Hedera.
 *
 * @param <T> The type of the transaction. Used to enable chaining.
 */
public abstract class Transaction<T extends Transaction<T>>
        extends Executable<
                T,
                com.hedera.hashgraph.sdk.proto.Transaction,
                com.hedera.hashgraph.sdk.proto.TransactionResponse,
                TransactionResponse> {

    /**
     * Default auto renew duration for accounts, contracts, topics, and files (entities)
     */
    static final Duration DEFAULT_AUTO_RENEW_PERIOD = Duration.ofDays(90);

    /**
     * Dummy account ID used to assist in deserializing incomplete Transactions.
     */
    protected static final AccountId DUMMY_ACCOUNT_ID = new AccountId(0L, 0L, 0L);

    /**
     * Dummy transaction ID used to assist in deserializing incomplete Transactions.
     */
    protected static final TransactionId DUMMY_TRANSACTION_ID =
            TransactionId.withValidStart(DUMMY_ACCOUNT_ID, Instant.EPOCH);

    /**
     * Default transaction duration
     */
    private static final Duration DEFAULT_TRANSACTION_VALID_DURATION = Duration.ofSeconds(120);

    private static final String ATOMIC_BATCH_NODE_ACCOUNT_ID = "0.0.0";

    /**
     * Transaction constructors end their work by setting sourceTransactionBody. The expectation is that the Transaction
     * subclass constructor will pick up where the Transaction superclass constructor left off, and will unpack the data
     * in the transaction body.
     */
    protected final TransactionBody sourceTransactionBody;
    /**
     * The builder that gets re-used to build each outer transaction. freezeWith() will create the frozenBodyBuilder.
     * The presence of frozenBodyBuilder indicates that this transaction is frozen.
     */
    @Nullable
    protected TransactionBody.Builder frozenBodyBuilder = null;

    /**
     * An SDK [Transaction] is composed of multiple, raw protobuf transactions. These should be functionally identical,
     * except pointing to different nodes. When retrying a transaction after a network error or retry-able status
     * response, we try a different transaction and thus a different node.
     */
    protected List<com.hedera.hashgraph.sdk.proto.Transaction> outerTransactions = Collections.emptyList();

    /**
     * An SDK [Transaction] is composed of multiple, raw protobuf transactions. These should be functionally identical,
     * except pointing to different nodes. When retrying a transaction after a network error or retry-able status
     * response, we try a different transaction and thus a different node.
     */
    protected List<com.hedera.hashgraph.sdk.proto.SignedTransaction.Builder> innerSignedTransactions =
            Collections.emptyList();

    /**
     * A set of signatures corresponding to every unique public key used to sign the transaction.
     */
    protected List<SignatureMap.Builder> sigPairLists = Collections.emptyList();

    /**
     * List of IDs for the transaction based on the operator because the transaction ID includes the operator's account
     */
    protected LockableList<TransactionId> transactionIds = new LockableList<>();

    /**
     * publicKeys and signers are parallel arrays. If the signer associated with a public key is null, that means that
     * the private key associated with that public key has already contributed a signature to sigPairListBuilders, but
     * the signer is not available (likely because this came from fromBytes())
     */
    protected List<PublicKey> publicKeys = new ArrayList<>();

    /**
     * publicKeys and signers are parallel arrays. If the signer associated with a public key is null, that means that
     * the private key associated with that public key has already contributed a signature to sigPairListBuilders, but
     * the signer is not available (likely because this came from fromBytes())
     */
    protected List<Function<byte[], byte[]>> signers = new ArrayList<>();

    /**
     * The maximum transaction fee the client is willing to pay
     */
    protected Hbar defaultMaxTransactionFee = new Hbar(2);
    /**
     * Should the transaction id be regenerated
     */
    protected Boolean regenerateTransactionId = null;

    private Duration transactionValidDuration;

    @Nullable
    private Hbar maxTransactionFee = null;

    private String memo = "";

    List<CustomFeeLimit> customFeeLimits = new ArrayList<>();

    private Key batchKey = null;

    /**
     * Constructor.
     */
    Transaction() {
        setTransactionValidDuration(DEFAULT_TRANSACTION_VALID_DURATION);

        sourceTransactionBody = TransactionBody.getDefaultInstance();
    }

    // This constructor is used to construct from a scheduled transaction body

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    Transaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        setTransactionValidDuration(DEFAULT_TRANSACTION_VALID_DURATION);
        setMaxTransactionFee(Hbar.fromTinybars(txBody.getTransactionFee()));
        setTransactionMemo(txBody.getMemo());

        sourceTransactionBody = txBody;
    }

    // This constructor is used to construct via fromBytes

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    Transaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction> transactionMap =
                txs.values().iterator().next();
        if (!transactionMap.isEmpty()
                && transactionMap.keySet().iterator().next().equals(DUMMY_ACCOUNT_ID)
                && this.batchKey != null) {
            // If the first account ID is a dummy account ID, then only the source TransactionBody needs to be copied.
            var signedTransaction = SignedTransaction.parseFrom(
                    transactionMap.values().iterator().next().getSignedTransactionBytes());
            sourceTransactionBody = parseTransactionBody(signedTransaction.getBodyBytes());
        } else {
            var txCount = txs.keySet().size();
            var nodeCount = txs.values().iterator().next().size();

            nodeAccountIds.ensureCapacity(nodeCount);
            sigPairLists = new ArrayList<>(nodeCount * txCount);
            outerTransactions = new ArrayList<>(nodeCount * txCount);
            innerSignedTransactions = new ArrayList<>(nodeCount * txCount);
            transactionIds.ensureCapacity(txCount);

            for (var transactionEntry : txs.entrySet()) {
                if (!transactionEntry.getKey().equals(DUMMY_TRANSACTION_ID)) {
                    transactionIds.add(transactionEntry.getKey());
                }
                for (var nodeEntry : transactionEntry.getValue().entrySet()) {
                    if (nodeAccountIds.size() != nodeCount) {
                        nodeAccountIds.add(nodeEntry.getKey());
                    }

                    var transaction =
                            SignedTransaction.parseFrom(nodeEntry.getValue().getSignedTransactionBytes());
                    outerTransactions.add(nodeEntry.getValue());
                    sigPairLists.add(transaction.getSigMap().toBuilder());
                    innerSignedTransactions.add(transaction.toBuilder());

                    if (publicKeys.isEmpty()) {
                        for (var sigPair : transaction.getSigMap().getSigPairList()) {
                            publicKeys.add(PublicKey.fromBytes(
                                    sigPair.getPubKeyPrefix().toByteArray()));
                            signers.add(null);
                        }
                    }
                }
            }

            nodeAccountIds.remove(new AccountId(0, 0, 0));

            // Verify that transaction bodies match
            for (int i = 0; i < txCount; i++) {
                TransactionBody firstTxBody = null;
                for (int j = 0; j < nodeCount; j++) {
                    int k = i * nodeCount + j;
                    var txBody =
                            parseTransactionBody(innerSignedTransactions.get(k).getBodyBytes());
                    if (firstTxBody == null) {
                        firstTxBody = txBody;
                    } else {
                        requireProtoMatches(
                                firstTxBody, txBody, new HashSet<>(List.of("NodeAccountID")), "TransactionBody");
                    }
                }
            }
            sourceTransactionBody =
                    parseTransactionBody(innerSignedTransactions.get(0).getBodyBytes());
        }

        setTransactionValidDuration(
                DurationConverter.fromProtobuf(sourceTransactionBody.getTransactionValidDuration()));
        setMaxTransactionFee(Hbar.fromTinybars(sourceTransactionBody.getTransactionFee()));
        setTransactionMemo(sourceTransactionBody.getMemo());

        this.customFeeLimits = sourceTransactionBody.getMaxCustomFeesList().stream()
                .map(CustomFeeLimit::fromProtobuf)
                .toList();
        this.batchKey = Key.fromProtobufKey(sourceTransactionBody.getBatchKey());

        // The presence of signatures implies the Transaction should be frozen.
        if (!publicKeys.isEmpty()) {
            frozenBodyBuilder = sourceTransactionBody.toBuilder();
        }
    }

    /**
     * Create the correct transaction from a byte array.
     *
     * @param bytes the byte array
     * @return the new transaction
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static Transaction<?> fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        var list = TransactionList.parseFrom(bytes);

        var txsMap = new LinkedHashMap<
                TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>>();

        TransactionBody.DataCase dataCase;

        if (!list.getTransactionListList().isEmpty()) {
            dataCase = processTransactionList(list.getTransactionListList(), txsMap);
        } else {
            dataCase = processSingleTransaction(bytes, txsMap);
        }

        return createTransactionFromDataCase(dataCase, txsMap);
    }

    /**
     * Process a single transaction
     */
    private static TransactionBody.DataCase processSingleTransaction(
            byte[] bytes,
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txsMap)
            throws InvalidProtocolBufferException {

        var transaction = com.hedera.hashgraph.sdk.proto.Transaction.parseFrom(bytes);
        var builtTransaction = prepareSingleTransaction(transaction);

        var signedTransaction = SignedTransaction.parseFrom(builtTransaction.getSignedTransactionBytes());
        var txBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());

        addTransactionToMap(builtTransaction, txBody, txsMap);

        return txBody.getDataCase();
    }

    /**
     * Prepare a single transaction by ensuring it has SignedTransactionBytes
     */
    private static com.hedera.hashgraph.sdk.proto.Transaction prepareSingleTransaction(
            com.hedera.hashgraph.sdk.proto.Transaction transaction) {

        if (transaction.getSignedTransactionBytes().isEmpty()) {
            var txBuilder = transaction.toBuilder();
            var bodyBytes = txBuilder.getBodyBytes();
            var sigMap = txBuilder.getSigMap();

            txBuilder
                    .setSignedTransactionBytes(SignedTransaction.newBuilder()
                            .setBodyBytes(bodyBytes)
                            .setSigMap(sigMap)
                            .build()
                            .toByteString())
                    .clearBodyBytes()
                    .clearSigMap();

            return txBuilder.build();
        }

        return transaction;
    }

    /**
     * Process a list of transactions with integrity verification
     */
    private static TransactionBody.DataCase processTransactionList(
            List<com.hedera.hashgraph.sdk.proto.Transaction> transactionList,
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txsMap)
            throws InvalidProtocolBufferException {

        if (transactionList.isEmpty()) {
            return TransactionBody.DataCase.DATA_NOT_SET;
        }

        var firstTransaction = transactionList.get(0);
        var firstSignedTransaction = SignedTransaction.parseFrom(firstTransaction.getSignedTransactionBytes());
        var firstTxBody = TransactionBody.parseFrom(firstSignedTransaction.getBodyBytes());
        var dataCase = firstTxBody.getDataCase();

        for (com.hedera.hashgraph.sdk.proto.Transaction transaction : transactionList) {
            var signedTransaction = SignedTransaction.parseFrom(transaction.getSignedTransactionBytes());
            var txBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());

            addTransactionToMap(transaction, txBody, txsMap);
        }

        return dataCase;
    }

    /**
     * Add a transaction to the transaction map
     */
    private static void addTransactionToMap(
            com.hedera.hashgraph.sdk.proto.Transaction transaction,
            TransactionBody txBody,
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txsMap) {

        var account = txBody.hasNodeAccountID() ? AccountId.fromProtobuf(txBody.getNodeAccountID()) : DUMMY_ACCOUNT_ID;
        var transactionId = txBody.hasTransactionID()
                ? TransactionId.fromProtobuf(txBody.getTransactionID())
                : DUMMY_TRANSACTION_ID;

        var linked = txsMap.containsKey(transactionId)
                ? Objects.requireNonNull(txsMap.get(transactionId))
                : new LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>();

        linked.put(account, transaction);
        txsMap.put(transactionId, linked);
    }

    /**
     * Creates the appropriate transaction type based on the data case.
     */
    private static Transaction<?> createTransactionFromDataCase(
            TransactionBody.DataCase dataCase,
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {

        return switch (dataCase) {
            case CONTRACTCALL -> new ContractExecuteTransaction(txs);
            case CONTRACTCREATEINSTANCE -> new ContractCreateTransaction(txs);
            case CONTRACTUPDATEINSTANCE -> new ContractUpdateTransaction(txs);
            case CONTRACTDELETEINSTANCE -> new ContractDeleteTransaction(txs);
            case ETHEREUMTRANSACTION -> new EthereumTransaction(txs);
            case CRYPTOADDLIVEHASH -> new LiveHashAddTransaction(txs);
            case CRYPTOCREATEACCOUNT -> new AccountCreateTransaction(txs);
            case CRYPTODELETE -> new AccountDeleteTransaction(txs);
            case CRYPTODELETELIVEHASH -> new LiveHashDeleteTransaction(txs);
            case CRYPTOTRANSFER -> new TransferTransaction(txs);
            case CRYPTOUPDATEACCOUNT -> new AccountUpdateTransaction(txs);
            case FILEAPPEND -> new FileAppendTransaction(txs);
            case FILECREATE -> new FileCreateTransaction(txs);
            case FILEDELETE -> new FileDeleteTransaction(txs);
            case FILEUPDATE -> new FileUpdateTransaction(txs);
            case NODECREATE -> new NodeCreateTransaction(txs);
            case NODEUPDATE -> new NodeUpdateTransaction(txs);
            case NODEDELETE -> new NodeDeleteTransaction(txs);
            case SYSTEMDELETE -> new SystemDeleteTransaction(txs);
            case SYSTEMUNDELETE -> new SystemUndeleteTransaction(txs);
            case FREEZE -> new FreezeTransaction(txs);
            case CONSENSUSCREATETOPIC -> new TopicCreateTransaction(txs);
            case CONSENSUSUPDATETOPIC -> new TopicUpdateTransaction(txs);
            case CONSENSUSDELETETOPIC -> new TopicDeleteTransaction(txs);
            case CONSENSUSSUBMITMESSAGE -> new TopicMessageSubmitTransaction(txs);
            case TOKENASSOCIATE -> new TokenAssociateTransaction(txs);
            case TOKENBURN -> new TokenBurnTransaction(txs);
            case TOKENCREATION -> new TokenCreateTransaction(txs);
            case TOKENDELETION -> new TokenDeleteTransaction(txs);
            case TOKENDISSOCIATE -> new TokenDissociateTransaction(txs);
            case TOKENFREEZE -> new TokenFreezeTransaction(txs);
            case TOKENGRANTKYC -> new TokenGrantKycTransaction(txs);
            case TOKENMINT -> new TokenMintTransaction(txs);
            case TOKENREVOKEKYC -> new TokenRevokeKycTransaction(txs);
            case TOKENUNFREEZE -> new TokenUnfreezeTransaction(txs);
            case TOKENUPDATE -> new TokenUpdateTransaction(txs);
            case TOKEN_UPDATE_NFTS -> new TokenUpdateNftsTransaction(txs);
            case TOKENWIPE -> new TokenWipeTransaction(txs);
            case TOKEN_FEE_SCHEDULE_UPDATE -> new TokenFeeScheduleUpdateTransaction(txs);
            case SCHEDULECREATE -> new ScheduleCreateTransaction(txs);
            case SCHEDULEDELETE -> new ScheduleDeleteTransaction(txs);
            case SCHEDULESIGN -> new ScheduleSignTransaction(txs);
            case TOKEN_PAUSE -> new TokenPauseTransaction(txs);
            case TOKEN_UNPAUSE -> new TokenUnpauseTransaction(txs);
            case TOKENREJECT -> new TokenRejectTransaction(txs);
            case TOKENAIRDROP -> new TokenAirdropTransaction(txs);
            case TOKENCANCELAIRDROP -> new TokenCancelAirdropTransaction(txs);
            case TOKENCLAIMAIRDROP -> new TokenClaimAirdropTransaction(txs);
            case CRYPTOAPPROVEALLOWANCE -> new AccountAllowanceApproveTransaction(txs);
            case CRYPTODELETEALLOWANCE -> new AccountAllowanceDeleteTransaction(txs);
            case ATOMIC_BATCH -> new BatchTransaction(txs);
            default -> throw new IllegalArgumentException("parsed transaction body has no data");
        };
    }

    /**
     * Create the correct transaction from a scheduled transaction.
     *
     * @param scheduled the scheduled transaction
     * @return the new transaction
     */
    public static Transaction<?> fromScheduledTransaction(
            com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody scheduled) {
        var body = TransactionBody.newBuilder()
                .setMemo(scheduled.getMemo())
                .setTransactionFee(scheduled.getTransactionFee());

        return switch (scheduled.getDataCase()) {
            case CONTRACTCALL -> new ContractExecuteTransaction(
                    body.setContractCall(scheduled.getContractCall()).build());
            case CONTRACTCREATEINSTANCE -> new ContractCreateTransaction(
                    body.setContractCreateInstance(scheduled.getContractCreateInstance())
                            .build());
            case CONTRACTUPDATEINSTANCE -> new ContractUpdateTransaction(
                    body.setContractUpdateInstance(scheduled.getContractUpdateInstance())
                            .build());
            case CONTRACTDELETEINSTANCE -> new ContractDeleteTransaction(
                    body.setContractDeleteInstance(scheduled.getContractDeleteInstance())
                            .build());
            case CRYPTOAPPROVEALLOWANCE -> new AccountAllowanceApproveTransaction(
                    body.setCryptoApproveAllowance(scheduled.getCryptoApproveAllowance())
                            .build());
            case CRYPTODELETEALLOWANCE -> new AccountAllowanceDeleteTransaction(
                    body.setCryptoDeleteAllowance(scheduled.getCryptoDeleteAllowance())
                            .build());
            case CRYPTOCREATEACCOUNT -> new AccountCreateTransaction(
                    body.setCryptoCreateAccount(scheduled.getCryptoCreateAccount())
                            .build());
            case CRYPTODELETE -> new AccountDeleteTransaction(
                    body.setCryptoDelete(scheduled.getCryptoDelete()).build());
            case CRYPTOTRANSFER -> new TransferTransaction(
                    body.setCryptoTransfer(scheduled.getCryptoTransfer()).build());
            case CRYPTOUPDATEACCOUNT -> new AccountUpdateTransaction(
                    body.setCryptoUpdateAccount(scheduled.getCryptoUpdateAccount())
                            .build());
            case FILEAPPEND -> new FileAppendTransaction(
                    body.setFileAppend(scheduled.getFileAppend()).build());
            case FILECREATE -> new FileCreateTransaction(
                    body.setFileCreate(scheduled.getFileCreate()).build());
            case FILEDELETE -> new FileDeleteTransaction(
                    body.setFileDelete(scheduled.getFileDelete()).build());
            case FILEUPDATE -> new FileUpdateTransaction(
                    body.setFileUpdate(scheduled.getFileUpdate()).build());
            case NODECREATE -> new NodeCreateTransaction(
                    body.setNodeCreate(scheduled.getNodeCreate()).build());
            case NODEUPDATE -> new NodeUpdateTransaction(
                    body.setNodeUpdate(scheduled.getNodeUpdate()).build());
            case NODEDELETE -> new NodeDeleteTransaction(
                    body.setNodeDelete(scheduled.getNodeDelete()).build());
            case SYSTEMDELETE -> new SystemDeleteTransaction(
                    body.setSystemDelete(scheduled.getSystemDelete()).build());
            case SYSTEMUNDELETE -> new SystemUndeleteTransaction(
                    body.setSystemUndelete(scheduled.getSystemUndelete()).build());
            case FREEZE -> new FreezeTransaction(
                    body.setFreeze(scheduled.getFreeze()).build());
            case CONSENSUSCREATETOPIC -> new TopicCreateTransaction(
                    body.setConsensusCreateTopic(scheduled.getConsensusCreateTopic())
                            .build());
            case CONSENSUSUPDATETOPIC -> new TopicUpdateTransaction(
                    body.setConsensusUpdateTopic(scheduled.getConsensusUpdateTopic())
                            .build());
            case CONSENSUSDELETETOPIC -> new TopicDeleteTransaction(
                    body.setConsensusDeleteTopic(scheduled.getConsensusDeleteTopic())
                            .build());
            case CONSENSUSSUBMITMESSAGE -> new TopicMessageSubmitTransaction(
                    body.setConsensusSubmitMessage(scheduled.getConsensusSubmitMessage())
                            .build());
            case TOKENCREATION -> new TokenCreateTransaction(
                    body.setTokenCreation(scheduled.getTokenCreation()).build());
            case TOKENFREEZE -> new TokenFreezeTransaction(
                    body.setTokenFreeze(scheduled.getTokenFreeze()).build());
            case TOKENUNFREEZE -> new TokenUnfreezeTransaction(
                    body.setTokenUnfreeze(scheduled.getTokenUnfreeze()).build());
            case TOKENGRANTKYC -> new TokenGrantKycTransaction(
                    body.setTokenGrantKyc(scheduled.getTokenGrantKyc()).build());
            case TOKENREVOKEKYC -> new TokenRevokeKycTransaction(
                    body.setTokenRevokeKyc(scheduled.getTokenRevokeKyc()).build());
            case TOKENDELETION -> new TokenDeleteTransaction(
                    body.setTokenDeletion(scheduled.getTokenDeletion()).build());
            case TOKENUPDATE -> new TokenUpdateTransaction(
                    body.setTokenUpdate(scheduled.getTokenUpdate()).build());
            case TOKEN_UPDATE_NFTS -> new TokenUpdateNftsTransaction(
                    body.setTokenUpdateNfts(scheduled.getTokenUpdateNfts()).build());
            case TOKENMINT -> new TokenMintTransaction(
                    body.setTokenMint(scheduled.getTokenMint()).build());
            case TOKENBURN -> new TokenBurnTransaction(
                    body.setTokenBurn(scheduled.getTokenBurn()).build());
            case TOKENWIPE -> new TokenWipeTransaction(
                    body.setTokenWipe(scheduled.getTokenWipe()).build());
            case TOKENASSOCIATE -> new TokenAssociateTransaction(
                    body.setTokenAssociate(scheduled.getTokenAssociate()).build());
            case TOKENDISSOCIATE -> new TokenDissociateTransaction(
                    body.setTokenDissociate(scheduled.getTokenDissociate()).build());
            case TOKEN_FEE_SCHEDULE_UPDATE -> new TokenFeeScheduleUpdateTransaction(
                    body.setTokenFeeScheduleUpdate(scheduled.getTokenFeeScheduleUpdate())
                            .build());
            case TOKEN_PAUSE -> new TokenPauseTransaction(
                    body.setTokenPause(scheduled.getTokenPause()).build());
            case TOKEN_UNPAUSE -> new TokenUnpauseTransaction(
                    body.setTokenUnpause(scheduled.getTokenUnpause()).build());
            case TOKENREJECT -> new TokenRejectTransaction(
                    body.setTokenReject(scheduled.getTokenReject()).build());
            case TOKENAIRDROP -> new TokenAirdropTransaction(
                    body.setTokenAirdrop(scheduled.getTokenAirdrop()).build());
            case TOKENCANCELAIRDROP -> new TokenCancelAirdropTransaction(
                    body.setTokenCancelAirdrop(scheduled.getTokenCancelAirdrop())
                            .build());
            case TOKENCLAIMAIRDROP -> new TokenClaimAirdropTransaction(
                    body.setTokenCancelAirdrop(scheduled.getTokenCancelAirdrop())
                            .build());
            case SCHEDULEDELETE -> new ScheduleDeleteTransaction(
                    body.setScheduleDelete(scheduled.getScheduleDelete()).build());
            default -> throw new IllegalStateException("schedulable transaction did not have a transaction set");
        };
    }

    private static void throwProtoMatchException(String fieldName, String aWas, String bWas) {
        throw new IllegalArgumentException("fromBytes() failed because " + fieldName
                + " fields in TransactionBody protobuf messages in the TransactionList did not match: A was "
                + aWas
                + ", B was " + bWas);
    }

    private static void requireProtoMatches(Object protoA, Object protoB, Set<String> ignoreSet, String thisFieldName) {
        var aIsNull = protoA == null;
        var bIsNull = protoB == null;
        if (aIsNull != bIsNull) {
            throwProtoMatchException(thisFieldName, aIsNull ? "null" : "not null", bIsNull ? "null" : "not null");
        }
        if (aIsNull) {
            return;
        }
        var protoAClass = protoA.getClass();
        var protoBClass = protoB.getClass();
        if (!protoAClass.equals(protoBClass)) {
            throwProtoMatchException(thisFieldName, "of class " + protoAClass, "of class " + protoBClass);
        }
        if (protoA instanceof Boolean
                || protoA instanceof Integer
                || protoA instanceof Long
                || protoA instanceof Float
                || protoA instanceof Double
                || protoA instanceof String
                || protoA instanceof ByteString) {
            // System.out.println("values A = " + protoA.toString() + ", B = " + protoB.toString());
            if (!protoA.equals(protoB)) {
                throwProtoMatchException(thisFieldName, protoA.toString(), protoB.toString());
            }
        }
        for (var method : protoAClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            int methodModifiers = method.getModifiers();
            if ((!Modifier.isPublic(methodModifiers)) || Modifier.isStatic(methodModifiers)) {
                continue;
            }
            var methodName = method.getName();
            if (!methodName.startsWith("get")) {
                continue;
            }
            var isList = methodName.endsWith("List") && List.class.isAssignableFrom(method.getReturnType());
            var methodFieldName = methodName.substring(3, methodName.length() - (isList ? 4 : 0));
            if (ignoreSet.contains(methodFieldName) || methodFieldName.equals("DefaultInstance")) {
                continue;
            }
            if (!isList) {
                try {
                    var hasMethod = protoAClass.getMethod("has" + methodFieldName);
                    var hasA = (Boolean) hasMethod.invoke(protoA);
                    var hasB = (Boolean) hasMethod.invoke(protoB);
                    if (!hasA.equals(hasB)) {
                        throwProtoMatchException(
                                methodFieldName, hasA ? "present" : "not present", hasB ? "present" : "not present");
                    }
                    if (!hasA) {
                        continue;
                    }
                } catch (NoSuchMethodException ignored) {
                    // pass if there is no has method
                } catch (IllegalArgumentException error) {
                    throw error;
                } catch (Throwable error) {
                    throw new IllegalArgumentException("fromBytes() failed due to error", error);
                }
            }
            try {
                var retvalA = method.invoke(protoA);
                var retvalB = method.invoke(protoB);
                if (isList) {
                    var listA = (List<?>) retvalA;
                    var listB = (List<?>) retvalB;
                    if (listA.size() != listB.size()) {
                        throwProtoMatchException(methodFieldName, "of size " + listA.size(), "of size " + listB.size());
                    }
                    for (int i = 0; i < listA.size(); i++) {
                        // System.out.println("comparing " + thisFieldName + "." + methodFieldName + "[" + i + "]");
                        requireProtoMatches(listA.get(i), listB.get(i), ignoreSet, methodFieldName + "[" + i + "]");
                    }
                } else {
                    // System.out.println("comparing " + thisFieldName + "." + methodFieldName);
                    requireProtoMatches(retvalA, retvalB, ignoreSet, methodFieldName);
                }
            } catch (IllegalArgumentException error) {
                throw error;
            } catch (Throwable error) {
                throw new IllegalArgumentException("fromBytes() failed due to error", error);
            }
        }
    }

    /**
     * Generate a hash from a byte array.
     *
     * @param bytes the byte array
     * @return the hash
     */
    static byte[] hash(byte[] bytes) {
        var digest = new SHA384Digest();
        var hash = new byte[digest.getDigestSize()];

        digest.update(bytes, 0, bytes.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    private static boolean publicKeyIsInSigPairList(ByteString publicKeyBytes, List<SignaturePair> sigPairList) {
        for (var pair : sigPairList) {
            if (pair.getPubKeyPrefix().equals(publicKeyBytes)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts transaction into a scheduled version
     *
     * @param bodyBuilder the transaction's body builder
     * @return the scheduled transaction
     */
    protected ScheduleCreateTransaction doSchedule(TransactionBody.Builder bodyBuilder) {
        var schedulable = SchedulableTransactionBody.newBuilder()
                .setTransactionFee(bodyBuilder.getTransactionFee())
                .setMemo(bodyBuilder.getMemo());

        onScheduled(schedulable);

        var scheduled = new ScheduleCreateTransaction().setScheduledTransactionBody(schedulable.build());

        if (!transactionIds.isEmpty()) {
            scheduled.setTransactionId(transactionIds.get(0));
        }

        return scheduled;
    }

    protected boolean isBatchedAndNotBatchTransaction() {
        return batchKey != null && !(this instanceof BatchTransaction);
    }

    /**
     * Extract the scheduled transaction.
     *
     * @return the scheduled transaction
     */
    public ScheduleCreateTransaction schedule() {
        requireNotFrozen();
        if (!nodeAccountIds.isEmpty()) {
            throw new IllegalStateException(
                    "The underlying transaction for a scheduled transaction cannot have node account IDs set");
        }

        var bodyBuilder = spawnBodyBuilder(null);

        onFreeze(bodyBuilder);

        return doSchedule(bodyBuilder);
    }

    /**
     * Set the account IDs of the nodes that this transaction will be submitted to.
     * <p>
     * Providing an explicit node account ID interferes with client-side load balancing of the network. By default, the
     * SDK will pre-generate a transaction for 1/3 of the nodes on the network. If a node is down, busy, or otherwise
     * reports a fatal error, the SDK will try again with a different node.
     *
     * @param nodeAccountIds The list of node AccountIds to be set
     * @return {@code this}
     */
    @Override
    public final T setNodeAccountIds(List<AccountId> nodeAccountIds) {
        requireNotFrozen();
        Objects.requireNonNull(nodeAccountIds);
        return super.setNodeAccountIds(nodeAccountIds);
    }

    /**
     * Extract the valid transaction duration.
     *
     * @return the transaction valid duration
     */
    @Nullable
    public final Duration getTransactionValidDuration() {
        return transactionValidDuration;
    }

    /**
     * Sets the duration that this transaction is valid for.
     * <p>
     * This is defaulted by the SDK to 120 seconds (or two minutes).
     *
     * @param validDuration The duration to be set
     * @return {@code this}
     */
    public final T setTransactionValidDuration(Duration validDuration) {
        requireNotFrozen();
        Objects.requireNonNull(validDuration);
        transactionValidDuration = validDuration;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Extract the maximum transaction fee.
     *
     * @return the maximum transaction fee
     */
    @Nullable
    public final Hbar getMaxTransactionFee() {
        return maxTransactionFee;
    }

    /**
     * Set the maximum transaction fee the operator (paying account) is willing to pay.
     *
     * @param maxTransactionFee the maximum transaction fee, in tinybars.
     * @return {@code this}
     */
    public final T setMaxTransactionFee(Hbar maxTransactionFee) {
        requireNotFrozen();
        Objects.requireNonNull(maxTransactionFee);
        this.maxTransactionFee = maxTransactionFee;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Extract the default maximum transaction fee.
     *
     * @return the default maximum transaction fee
     */
    public final Hbar getDefaultMaxTransactionFee() {
        return defaultMaxTransactionFee;
    }

    /**
     * Extract the memo for the transaction.
     *
     * @return the memo for the transaction
     */
    public final String getTransactionMemo() {
        return memo;
    }

    /**
     * Set a note or description that should be recorded in the transaction record (maximum length of 100 characters).
     *
     * @param memo any notes or descriptions for this transaction.
     * @return {@code this}
     */
    public final T setTransactionMemo(String memo) {
        requireNotFrozen();
        Objects.requireNonNull(memo);
        this.memo = memo;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * batchify method is used to mark a transaction as part of a batch transaction or make it so-called inner transaction.
     * The Transaction will be frozen and signed by the operator of the client.
     * @param client sdk client
     * @param batchKey batch key
     * @return {@code this}
     */
    public final T batchify(Client client, Key batchKey) {
        requireNotFrozen();
        Objects.requireNonNull(batchKey);
        this.batchKey = batchKey;
        signWithOperator(client);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the key that will sign the batch of which this Transaction is a part of.
     */
    public final T setBatchKey(Key batchKey) {
        requireNotFrozen();
        Objects.requireNonNull(batchKey);
        this.batchKey = batchKey;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Get the key that will sign the batch of which this Transaction is a part of.
     */
    public Key getBatchKey() {
        return batchKey;
    }
    /**
     * Extract a byte array representation.
     *
     * @return the byte array representation
     */
    public byte[] toBytes() {
        var list = TransactionList.newBuilder();

        // If no nodes have been selected yet,
        // the new TransactionBody can be used to build a Transaction protobuf object.
        if (nodeAccountIds.isEmpty()) {
            var bodyBuilder = spawnBodyBuilder(null);
            if (!transactionIds.isEmpty()) {
                bodyBuilder.setTransactionID(transactionIds.get(0).toProtobuf());
            }
            onFreeze(bodyBuilder);

            var signedTransaction = SignedTransaction.newBuilder()
                    .setBodyBytes(bodyBuilder.build().toByteString())
                    .build();

            var transaction = com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(signedTransaction.toByteString())
                    .build();

            list.addTransactionList(transaction);
        } else {
            // Generate the SignedTransaction protobuf objects if the Transaction's not frozen.
            if (!this.isFrozen()) {
                frozenBodyBuilder = spawnBodyBuilder(null);
                if (!transactionIds.isEmpty()) {
                    frozenBodyBuilder.setTransactionID(transactionIds.get(0).toProtobuf());
                }
                onFreeze(frozenBodyBuilder);

                int requiredChunks = getRequiredChunks();
                if (!transactionIds.isEmpty()) {
                    generateTransactionIds(transactionIds.get(0), requiredChunks);
                }
                wipeTransactionLists(requiredChunks);
            }

            // Build all the Transaction protobuf objects and add them to the TransactionList protobuf object.
            buildAllTransactions();
            for (var transaction : outerTransactions) {
                list.addTransactionList(transaction);
            }
        }

        return list.build().toByteArray();
    }

    /**
     * Extract a byte array of the transaction hash.
     *
     * @return the transaction hash
     */
    public byte[] getTransactionHash() {
        if (!this.isFrozen()) {
            throw new IllegalStateException(
                    "transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        transactionIds.setLocked(true);
        nodeAccountIds.setLocked(true);

        var index = transactionIds.getIndex() * nodeAccountIds.size() + nodeAccountIds.getIndex();

        buildTransaction(index);

        return hash(outerTransactions.get(index).getSignedTransactionBytes().toByteArray());
    }

    /**
     * Extract the list of account id and hash records.
     *
     * @return the list of account id and hash records
     */
    public Map<AccountId, byte[]> getTransactionHashPerNode() {
        if (!this.isFrozen()) {
            throw new IllegalStateException(
                    "transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        buildAllTransactions();

        var hashes = new HashMap<AccountId, byte[]>();

        for (var i = 0; i < outerTransactions.size(); i++) {
            hashes.put(
                    nodeAccountIds.get(i),
                    hash(outerTransactions.get(i).getSignedTransactionBytes().toByteArray()));
        }

        return hashes;
    }

    @Override
    final TransactionId getTransactionIdInternal() {
        return transactionIds.getCurrent();
    }

    /**
     * Extract the transaction id.
     *
     * @return the transaction id
     */
    public final TransactionId getTransactionId() {
        if (transactionIds.isEmpty() || !this.isFrozen()) {
            throw new IllegalStateException(
                    "No transaction ID generated yet. Try freezing the transaction or manually setting the transaction ID.");
        }

        return transactionIds.setLocked(true).getCurrent();
    }

    /**
     * Set the ID for this transaction.
     * <p>
     * The transaction ID includes the operator's account ( the account paying the transaction fee). If two transactions
     * have the same transaction ID, they won't both have an effect. One will complete normally and the other will fail
     * with a duplicate transaction status.
     * <p>
     * Normally, you should not use this method. Just before a transaction is executed, a transaction ID will be
     * generated from the operator on the client.
     *
     * @param transactionId The TransactionId to be set
     * @return {@code this}
     * @see TransactionId
     */
    public final T setTransactionId(TransactionId transactionId) {
        requireNotFrozen();

        transactionIds.setList(Collections.singletonList(transactionId)).setLocked(true);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Should the transaction id be regenerated.
     *
     * @return should the transaction id be regenerated
     */
    public final Boolean getRegenerateTransactionId() {
        return regenerateTransactionId;
    }

    /**
     * Regenerate the transaction id.
     *
     * @param regenerateTransactionId should the transaction id be regenerated
     * @return {@code this}
     */
    public final T setRegenerateTransactionId(boolean regenerateTransactionId) {
        this.regenerateTransactionId = regenerateTransactionId;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Sign the transaction.
     *
     * @param privateKey the private key
     * @return the signed transaction
     */
    public final T sign(PrivateKey privateKey) {
        return signWith(privateKey.getPublicKey(), privateKey::sign);
    }

    /**
     * Sign the transaction.
     *
     * @param publicKey         the public key
     * @param transactionSigner the key list
     * @return {@code this}
     */
    public T signWith(PublicKey publicKey, UnaryOperator<byte[]> transactionSigner) {
        if (!isFrozen()) {
            throw new IllegalStateException("Signing requires transaction to be frozen");
        }

        if (keyAlreadySigned(publicKey)) {
            // noinspection unchecked
            return (T) this;
        }

        for (int i = 0; i < outerTransactions.size(); i++) {
            outerTransactions.set(i, null);
        }
        publicKeys.add(publicKey);
        signers.add(transactionSigner);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Sign the transaction with the configured client.
     *
     * @param client the configured client
     * @return the signed transaction
     */
    public T signWithOperator(Client client) {
        var operator = client.getOperator();

        if (operator == null) {
            throw new IllegalStateException("`client` must have an `operator` to sign with the operator");
        }

        if (!isFrozen()) {
            freezeWith(client);
        }

        return signWith(operator.publicKey, operator.transactionSigner);
    }

    /**
     * Checks if a public key is already added to the transaction
     *
     * @param key the public key
     * @return if the public key is already added
     */
    protected boolean keyAlreadySigned(PublicKey key) {
        return publicKeys.contains(key);
    }

    /**
     * Add a signature to the transaction.
     *
     * @param publicKey the public key
     * @param signature the signature
     * @return {@code this}
     */
    public T addSignature(PublicKey publicKey, byte[] signature) {
        requireOneNodeAccountId();
        if (!isFrozen()) {
            freeze();
        }

        if (keyAlreadySigned(publicKey)) {
            // noinspection unchecked
            return (T) this;
        }

        transactionIds.setLocked(true);
        nodeAccountIds.setLocked(true);

        for (int i = 0; i < outerTransactions.size(); i++) {
            outerTransactions.set(i, null);
        }
        publicKeys.add(publicKey);
        signers.add(null);
        sigPairLists.get(0).addSigPair(publicKey.toSignaturePairProtobuf(signature));

        // noinspection unchecked
        return (T) this;
    }

    protected Map<AccountId, Map<PublicKey, byte[]>> getSignaturesAtOffset(int offset) {
        var map = new HashMap<AccountId, Map<PublicKey, byte[]>>(nodeAccountIds.size());

        for (int i = 0; i < nodeAccountIds.size(); i++) {
            var sigMap = sigPairLists.get(i + offset);
            var nodeAccountId = nodeAccountIds.get(i);

            var keyMap = map.containsKey(nodeAccountId)
                    ? Objects.requireNonNull(map.get(nodeAccountId))
                    : new HashMap<PublicKey, byte[]>(sigMap.getSigPairCount());
            map.put(nodeAccountId, keyMap);

            for (var sigPair : sigMap.getSigPairList()) {
                keyMap.put(
                        PublicKey.fromBytes(sigPair.getPubKeyPrefix().toByteArray()),
                        sigPair.getEd25519().toByteArray());
            }
        }

        return map;
    }

    /**
     * Extract list of account id and public keys.
     *
     * @return the list of account id and public keys
     */
    public Map<AccountId, Map<PublicKey, byte[]>> getSignatures() {
        if (!isFrozen()) {
            throw new IllegalStateException("Transaction must be frozen in order to have signatures.");
        }

        if (publicKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        buildAllTransactions();

        return getSignaturesAtOffset(0);
    }

    /**
     * Check if transaction is frozen.
     *
     * @return is the transaction frozen
     */
    protected boolean isFrozen() {
        return frozenBodyBuilder != null;
    }

    /**
     * Throw an exception if the transaction is frozen.
     */
    protected void requireNotFrozen() {
        if (isFrozen()) {
            throw new IllegalStateException(
                    "transaction is immutable; it has at least one signature or has been explicitly frozen");
        }
    }

    /**
     * Throw an exception if there is not exactly one node id set.
     */
    protected void requireOneNodeAccountId() {
        if (nodeAccountIds.size() != 1) {
            throw new IllegalStateException("transaction did not have exactly one node ID set");
        }
    }

    protected TransactionBody.Builder spawnBodyBuilder(@Nullable Client client) {
        var clientDefaultFee = client != null ? client.getDefaultMaxTransactionFee() : null;

        var defaultFee = clientDefaultFee != null ? clientDefaultFee : defaultMaxTransactionFee;

        var feeHbars = maxTransactionFee != null ? maxTransactionFee : defaultFee;

        var builder = TransactionBody.newBuilder()
                .setTransactionFee(feeHbars.toTinybars())
                .setTransactionValidDuration(DurationConverter.toProtobuf(transactionValidDuration).toBuilder())
                .addAllMaxCustomFees(
                        customFeeLimits.stream().map(CustomFeeLimit::toProtobuf).collect(Collectors.toList()))
                .setMemo(memo);
        if (batchKey != null) {
            builder.setBatchKey(batchKey.toProtobufKey());
        }
        return builder;
    }

    /**
     * Freeze this transaction from further modification to prepare for signing or serialization.
     *
     * @return {@code this}
     */
    public T freeze() {
        return freezeWith(null);
    }

    /**
     * Freeze this transaction from further modification to prepare for signing or serialization.
     * <p>
     * Will use the `Client`, if available, to generate a default Transaction ID and select 1/3 nodes to prepare this
     * transaction for.
     *
     * @param client the configured client
     * @return {@code this}
     */
    public T freezeWith(@Nullable Client client) {
        if (isFrozen()) {
            // noinspection unchecked
            return (T) this;
        }

        if (transactionIds.isEmpty()) {
            if (client != null) {
                var operator = client.getOperator();

                if (operator != null) {
                    // Set a default transaction ID, generated from the operator account ID

                    transactionIds.setList(Collections.singletonList(TransactionId.generate(operator.accountId)));
                } else {
                    // no client means there must be an explicitly set node ID and transaction ID
                    throw new IllegalStateException("`client` must have an `operator` or `transactionId` must be set");
                }
            } else {
                throw new IllegalStateException(
                        "Transaction ID must be set, or operator must be provided via freezeWith()");
            }
        }

        if (nodeAccountIds.isEmpty()) {
            if (client == null) {
                throw new IllegalStateException(
                        "`client` must be provided or both `nodeId` and `transactionId` must be set");
            }

            try {
                if (batchKey == null) {
                    nodeAccountIds.setList(client.network.getNodeAccountIdsForExecute());
                } else {
                    nodeAccountIds.setList(
                            Collections.singletonList(AccountId.fromString(ATOMIC_BATCH_NODE_ACCOUNT_ID)));
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        frozenBodyBuilder =
                spawnBodyBuilder(client).setTransactionID(transactionIds.get(0).toProtobuf());
        onFreeze(frozenBodyBuilder);

        int requiredChunks = getRequiredChunks();
        generateTransactionIds(transactionIds.get(0), requiredChunks);
        wipeTransactionLists(requiredChunks);

        var clientDefaultRegenerateTransactionId = client != null ? client.getDefaultRegenerateTransactionId() : null;
        regenerateTransactionId =
                regenerateTransactionId != null ? regenerateTransactionId : clientDefaultRegenerateTransactionId;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * There must be at least one chunk.
     *
     * @return there is 1 required chunk
     */
    int getRequiredChunks() {
        return 1;
    }

    /**
     * Generate transaction id's.
     *
     * @param initialTransactionId the initial transaction id
     * @param count                the number of id's to generate.
     */
    void generateTransactionIds(TransactionId initialTransactionId, int count) {
        var locked = transactionIds.isLocked();
        transactionIds.setLocked(false);

        if (count == 1) {
            transactionIds.setList(Collections.singletonList(initialTransactionId));
            return;
        }

        var nextTransactionId = initialTransactionId.toProtobuf().toBuilder();
        transactionIds.ensureCapacity(count);
        transactionIds.clear();
        for (int i = 0; i < count; i++) {
            transactionIds.add(TransactionId.fromProtobuf(nextTransactionId.build()));

            // add 1 ns to the validStart to make cascading transaction IDs
            var nextValidStart = nextTransactionId.getTransactionValidStart().toBuilder();
            nextValidStart.setNanos(nextValidStart.getNanos() + 1);

            nextTransactionId.setTransactionValidStart(nextValidStart);
        }

        transactionIds.setLocked(locked);
    }

    /**
     * Wipe / reset the transaction list.
     *
     * @param requiredChunks the number of required chunks
     */
    void wipeTransactionLists(int requiredChunks) {
        if (!transactionIds.isEmpty()) {
            Objects.requireNonNull(frozenBodyBuilder)
                    .setTransactionID(getTransactionIdInternal().toProtobuf());
        }

        outerTransactions = new ArrayList<>(nodeAccountIds.size());
        sigPairLists = new ArrayList<>(nodeAccountIds.size());
        innerSignedTransactions = new ArrayList<>(nodeAccountIds.size());

        for (AccountId nodeId : nodeAccountIds) {
            sigPairLists.add(SignatureMap.newBuilder());
            innerSignedTransactions.add(SignedTransaction.newBuilder()
                    .setBodyBytes(Objects.requireNonNull(frozenBodyBuilder)
                            .setNodeAccountID(nodeId.toProtobuf())
                            .build()
                            .toByteString()));
            outerTransactions.add(null);
        }
    }

    /**
     * Build all the transactions.
     */
    void buildAllTransactions() {
        transactionIds.setLocked(true);
        nodeAccountIds.setLocked(true);

        for (var i = 0; i < innerSignedTransactions.size(); ++i) {
            buildTransaction(i);
        }
    }

    /**
     * Will build the specific transaction at {@code index} This function is only ever called after the transaction is
     * frozen.
     *
     * @param index the index of the transaction to be built
     */
    void buildTransaction(int index) {
        // Check if transaction is already built.
        // Every time a signer is added via sign() or signWith(), all outerTransactions are nullified.
        if (outerTransactions.get(index) != null
                && !outerTransactions.get(index).getSignedTransactionBytes().isEmpty()) {
            return;
        }

        signTransaction(index);

        outerTransactions.set(
                index,
                com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                        .setSignedTransactionBytes(innerSignedTransactions
                                .get(index)
                                .setSigMap(sigPairLists.get(index))
                                .build()
                                .toByteString())
                        .build());
    }

    /**
     * Will sign the specific transaction at {@code index} This function is only ever called after the transaction is
     * frozen.
     *
     * @param index the index of the transaction to sign
     */
    void signTransaction(int index) {
        var bodyBytes = innerSignedTransactions.get(index).getBodyBytes().toByteArray();
        var thisSigPairList = sigPairLists.get(index).getSigPairList();

        for (var i = 0; i < publicKeys.size(); i++) {
            if (signers.get(i) == null) {
                continue;
            }
            if (publicKeyIsInSigPairList(ByteString.copyFrom(publicKeys.get(i).toBytesRaw()), thisSigPairList)) {
                continue;
            }

            var signatureBytes = signers.get(i).apply(bodyBytes);

            sigPairLists.get(index).addSigPair(publicKeys.get(i).toSignaturePairProtobuf(signatureBytes));
        }
    }

    /**
     * Called in {@link #freezeWith(Client)} just before the transaction body is built. The intent is for the derived
     * class to assign their data variant to the transaction body.
     */
    abstract void onFreeze(TransactionBody.Builder bodyBuilder);

    /**
     * Called in {@link #schedule()} when converting transaction into a scheduled version.
     */
    abstract void onScheduled(SchedulableTransactionBody.Builder scheduled);

    @Override
    final com.hedera.hashgraph.sdk.proto.Transaction makeRequest() {
        var index = nodeAccountIds.getIndex() + (transactionIds.getIndex() * nodeAccountIds.size());

        buildTransaction(index);

        return outerTransactions.get(index);
    }

    @Override
    TransactionResponse mapResponse(
            com.hedera.hashgraph.sdk.proto.TransactionResponse transactionResponse,
            AccountId nodeId,
            com.hedera.hashgraph.sdk.proto.Transaction request) {
        var transactionId = Objects.requireNonNull(getTransactionIdInternal());
        var hash = hash(request.getSignedTransactionBytes().toByteArray());
        // advance is needed for chunked transactions
        transactionIds.advance();
        return new TransactionResponse(nodeId, transactionId, hash, null, this);
    }

    @Override
    final Status mapResponseStatus(com.hedera.hashgraph.sdk.proto.TransactionResponse transactionResponse) {
        return Status.valueOf(transactionResponse.getNodeTransactionPrecheckCode());
    }

    abstract void validateChecksums(Client client) throws BadEntityIdException;

    /**
     * Prepare the transactions to be executed.
     *
     * @param client the configured client
     */
    void onExecute(Client client) {
        if (!isFrozen()) {
            freezeWith(client);
        }

        var accountId = Objects.requireNonNull(Objects.requireNonNull(transactionIds.get(0)).accountId);

        if (client.isAutoValidateChecksumsEnabled()) {
            try {
                accountId.validateChecksum(client);
                validateChecksums(client);
            } catch (BadEntityIdException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        }

        var operatorId = client.getOperatorAccountId();
        if (operatorId != null && operatorId.equals(accountId)) {
            // on execute, sign each transaction with the operator, if present
            // and we are signing a transaction that used the default transaction ID
            signWithOperator(client);
        }
    }

    @Override
    CompletableFuture<Void> onExecuteAsync(Client client) {
        onExecute(client);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    ExecutionState getExecutionState(Status status, com.hedera.hashgraph.sdk.proto.TransactionResponse response) {
        if (status == Status.TRANSACTION_EXPIRED) {
            if ((regenerateTransactionId != null && !regenerateTransactionId) || transactionIds.isLocked()) {
                return ExecutionState.REQUEST_ERROR;
            } else {
                var firstTransactionId = Objects.requireNonNull(transactionIds.get(0));
                var accountId = Objects.requireNonNull(firstTransactionId.accountId);
                generateTransactionIds(TransactionId.generate(accountId), transactionIds.size());
                wipeTransactionLists(transactionIds.size());
                return ExecutionState.RETRY;
            }
        }
        return super.getExecutionState(status, response);
    }

    Transaction regenerateTransactionId(Client client) {
        Objects.requireNonNull(client.getOperatorAccountId());
        transactionIds.setLocked(false);
        var newTransactionID = TransactionId.generate(client.getOperatorAccountId());
        transactionIds.set(transactionIds.getIndex(), newTransactionID);
        transactionIds.setLocked(true);
        return this;
    }

    @Override
    @SuppressWarnings("LiteProtoToString")
    public String toString() {
        // NOTE: regex is for removing the instance address from the default debug output
        TransactionBody.Builder body = spawnBodyBuilder(null);

        if (!transactionIds.isEmpty()) {
            body.setTransactionID(transactionIds.get(0).toProtobuf());
        }
        if (!nodeAccountIds.isEmpty()) {
            body.setNodeAccountID(nodeAccountIds.get(0).toProtobuf());
        }

        onFreeze(body);

        return body.buildPartial().toString().replaceAll("@[A-Za-z0-9]+", "");
    }

    /**
     * This method retrieves the size of the transaction
     * @return
     */
    public int getTransactionSize() {
        if (!this.isFrozen()) {
            throw new IllegalStateException(
                    "transaction must have been frozen before getting it's size, try calling `freeze`");
        }

        return makeRequest().getSerializedSize();
    }

    /**
     * This method retrieves the transaction body size
     * @return
     */
    public int getTransactionBodySize() {
        if (!this.isFrozen()) {
            throw new IllegalStateException(
                    "transaction must have been frozen before getting it's body size, try calling `freeze`");
        }

        if (frozenBodyBuilder != null) {
            return frozenBodyBuilder.build().getSerializedSize();
        }

        return 0;
    }

    public static class SignableNodeTransactionBodyBytes {
        private AccountId nodeID;
        private TransactionId transactionID;
        private byte[] body;

        public SignableNodeTransactionBodyBytes(AccountId nodeID, TransactionId transactionID, byte[] body) {
            this.nodeID = nodeID;
            this.transactionID = transactionID;
            this.body = body;
        }

        public AccountId getNodeID() {
            return nodeID;
        }

        public TransactionId getTransactionID() {
            return transactionID;
        }

        public byte[] getBody() {
            return body;
        }
    }

    /**
     * Returns a list of SignableNodeTransactionBodyBytes objects for each signed transaction in the transaction list.
     * The NodeID represents the node that this transaction is signed for.
     * The TransactionID is useful for signing chunked transactions like FileAppendTransaction,
     * since they can have multiple transaction ids.
     *
     * @return List of SignableNodeTransactionBodyBytes
     * @throws RuntimeException if transaction is not frozen or protobuf parsing fails
     */
    public List<SignableNodeTransactionBodyBytes> getSignableNodeBodyBytesList() {
        if (!this.isFrozen()) {
            throw new RuntimeException("Transaction is not frozen");
        }

        List<SignableNodeTransactionBodyBytes> signableNodeTransactionBodyBytesList = new ArrayList<>();

        for (int i = 0; i < innerSignedTransactions.size(); i++) {
            SignedTransaction signableNodeTransactionBodyBytes =
                    innerSignedTransactions.get(i).build();

            TransactionBody body = parseTransactionBody(signableNodeTransactionBodyBytes.getBodyBytes());

            AccountId nodeID = AccountId.fromProtobuf(body.getNodeAccountID());
            TransactionId transactionID = TransactionId.fromProtobuf(body.getTransactionID());

            signableNodeTransactionBodyBytesList.add(new SignableNodeTransactionBodyBytes(
                    nodeID,
                    transactionID,
                    signableNodeTransactionBodyBytes.getBodyBytes().toByteArray()));
        }

        return signableNodeTransactionBodyBytesList;
    }

    /**
     * Adds a signature to the transaction for a specific transaction id and node id.
     * This is useful for signing chunked transactions like FileAppendTransaction,
     * since they can have multiple transaction ids.
     *
     * @param publicKey The public key to add signature for
     * @param signature The signature bytes
     * @param transactionID The specific transaction ID to match
     * @param nodeID The specific node ID to match
     * @return The child transaction (this)
     * @throws RuntimeException if unmarshaling fails or invalid signed transaction
     */
    public T addSignature(PublicKey publicKey, byte[] signature, TransactionId transactionID, AccountId nodeID) {

        if (innerSignedTransactions.isEmpty()) {
            // noinspection unchecked
            return (T) this;
        }

        transactionIds.setLocked(true);

        for (int index = 0; index < innerSignedTransactions.size(); index++) {
            if (processedSignatureForTransaction(index, publicKey, signature, transactionID, nodeID)) {
                updateTransactionState(publicKey);
            }
        }

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Processes signature addition for a single transaction at the given index.
     *
     * @param index The index of the transaction to process
     * @param publicKey The public key to add signature for
     * @param signature The signature bytes
     * @param transactionID The specific transaction ID to match
     * @param nodeID The specific node ID to match
     * @return true if signature was added, false otherwise
     */
    private boolean processedSignatureForTransaction(
            int index, PublicKey publicKey, byte[] signature, TransactionId transactionID, AccountId nodeID) {
        SignedTransaction.Builder temp = innerSignedTransactions.get(index);

        TransactionBody body = parseTransactionBody(temp);
        if (body == null) {
            return false;
        }

        if (!matchesTargetTransactionAndNode(body, transactionID, nodeID)) {
            return false;
        }

        return addSignatureIfNotExists(index, publicKey, signature);
    }

    /**
     * Parses the transaction body from a signed transaction builder.
     *
     * @param signedTransactionBuilder The signed transaction builder
     * @return The parsed transaction body, or null if parsing fails
     */
    private static TransactionBody parseTransactionBody(SignedTransaction.Builder signedTransactionBuilder) {
        try {
            return TransactionBody.parseFrom(signedTransactionBuilder.getBodyBytes());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Failed to parse transaction body", e);
        }
    }

    private static TransactionBody parseTransactionBody(ByteString signedTransactionBuilder) {
        try {
            return TransactionBody.parseFrom(signedTransactionBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Failed to parse transaction body", e);
        }
    }

    /**
     * Checks if the transaction body matches the target transaction ID and node ID.
     *
     * @param body The transaction body to check
     * @param targetTransactionID The target transaction ID to match against
     * @param targetNodeID The target node ID to match against
     * @return true if both the transaction ID and node ID match the targets, false otherwise
     */
    private boolean matchesTargetTransactionAndNode(
            TransactionBody body, TransactionId targetTransactionID, AccountId targetNodeID) {
        TransactionId bodyTxID = TransactionId.fromProtobuf(body.getTransactionID());
        AccountId bodyNodeID = AccountId.fromProtobuf(body.getNodeAccountID());

        return bodyTxID.toString().equals(targetTransactionID.toString())
                && bodyNodeID.toString().equals(targetNodeID.toString());
    }

    /**
     * Adds signature if it doesn't already exist for the given public key.
     *
     * @param index The transaction index
     * @param publicKey The public key
     * @param signature The signature bytes
     * @return true if signature was added, false if it already existed
     */
    private boolean addSignatureIfNotExists(int index, PublicKey publicKey, byte[] signature) {
        SignatureMap.Builder sigMapBuilder = sigPairLists.get(index);

        // Check if the signature is already in the signature map
        if (isSignatureAlreadyPresent(sigMapBuilder, publicKey)) {
            return false;
        }

        // Add the signature to the signature map
        SignaturePair newSigPair = publicKey.toSignaturePairProtobuf(signature);
        sigMapBuilder.addSigPair(newSigPair);

        return true;
    }

    /**
     * Checks if a signature for the given public key already exists.
     *
     * @param sigMapBuilder The signature map builder
     * @param publicKey The public key to check
     * @return true if signature already exists, false otherwise
     */
    private boolean isSignatureAlreadyPresent(SignatureMap.Builder sigMapBuilder, PublicKey publicKey) {
        for (SignaturePair sig : sigMapBuilder.getSigPairList()) {
            if (Arrays.equals(sig.getPubKeyPrefix().toByteArray(), publicKey.toBytesRaw())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the transaction state after adding a signature.
     *
     * @param publicKey The public key that was added
     */
    private void updateTransactionState(PublicKey publicKey) {
        publicKeys.add(publicKey);
        signers.add(null);
    }
}
