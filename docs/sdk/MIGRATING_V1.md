# Migrating from v1 to v2

This guide outlines the key changes, renames, additions, and removals in the SDK migration from version 1 to version 2. It covers updates to classes, methods, and functionalities to help you transition your code effectively.

## Key Renames and Structural Changes

### Renamed `Ed25519PublicKey` to `PublicKey`
- **Added** `boolean verify(byte[], byte[])`: Verifies a message was signed by the respective private key.
- **Added** `boolean verifyTransaction(Transaction)`: Verifies the transaction was signed by the respective private key.
- **Removed** `Key toKeyProto()`.
- **Removed** `boolean hasPrefix()`.
- **Removed** `SignatureCase getSignatureCase()`.

### Renamed `Ed25519PrivateKey` to `PrivateKey`
- **Added** `byte[] signTransaction(Transaction)`: Signs the `Transaction` and returns the signature.
- **Added** `PublicKey getPublicKey()`.
- **Added** `PrivateKey fromLegacyMnemonic(byte[])`.
- **Renamed** `boolean supportsDerivation()` to `boolean isDerivable()`.
- **Removed** `PrivateKey generate(SecureRandom)`.
- **Removed** `PrivateKey fromKeystore(Keystore)`: Use `Keystore.getEd25519()` instead.
- **Removed** `PrivateKey readKeystore(InputStream, String)`: Use `Keystore.fromStream()` instead.
- **Removed** `PrivateKey writeKeystore(OutputStream, String)`: Use `Keystore.export(OutputStream, String)` instead.
- **Removed** `Keystore toKeystore()`: Use `new Keystore(PrivateKey)` followed by `Keystore.export(OutputStream, String)` instead.

### Removed `ThresholdKey`
- Use `KeyList.withThreshold()` or `KeyList.setThreshold()` instead.

### Renamed `PublicKey` to `Key`
- **Added** by `PublicKey`.
- **Added** by `PrivateKey`.
- **Added** by `KeyList`.
- **Added** by `ContractId`.

### `KeyList`
- **Exposed** `int threshold`.
- **Added** `KeyList of(Key...)`.
- **Added** `KeyList withThreshold(int)`.
- **Added** `int getThreshold()`.
- **Removed** `Key toProtoKey()`.
- **Removed** `SignatureCase getSignatureCase()`.
- **Removed** `byte[] toBytes()`.

### `Mnemonic`
- **Exposed** `boolean isLegacy`.
- **Added** `Mnemonic fromWords(List)`.
- **Added** `Mnemonic generate12()`.
- **Added** `Mnemonic generate24()`.
- **Removed** `Mnemonic generate()`: Use `generate12()` or `generate24()` instead.
- **Removed** `byte[] toSeed()`.
- **Removed** `Mnemonic(List)`: Use `Mnemonic.fromWords(List)` instead.

### Renamed `MnemonicValidationResult` to `BadMnemonicException`
- **Added** `Mnemonic mnemonic`.
- **Added** `BadMnemonicReason reason`.
- **Removed** `boolean isOk()`.
- **Removed** `String toString()`.
- **Removed** `MnemonicValidationStatus status`.

### Renamed `MnemonicValidationStatus` to `BadMnemonicReason`

### Removed `MirrorClient`
- Use `Client` instead, and set the mirror network using `setMirrorNetwork()`.

### Renamed `MirrorSubscriptionHandle` to `SubscriptionHandle`

### Renamed `QueryBuilder` to `Query`
- **Changed** `long getCost(Client)` to `Hbar getCost(Client)`.
- **Removed** `setPaymentTransaction()`.
- **Removed** `setQueryPayment(long)`.
- **Removed** `setMaxQueryPayment(long)`.
- **Removed** `Query toProto()`.

### Combined `TransactionBuilder` and `Transaction`
- **Added** `Transaction fromBytes(byte[])`.
- **Added** `byte[] toBytes()`.
- **Added** `TransactionId getTransactionId()`.
- **Added** `Hbar getMaxTransactionFee()`.
- **Added** `String getTransactionMemo()`.
- **Added** `Map getTransactionHashPerNode()`.
- **Added** `Duration getTransactionValidDuration()`.
- **Added** `Transaction signWithOperator(Client)`.
- **Added** `Transaction addSignature(PublicKey, byte[])`.
- **Added** `Map> getSignatures()`.
- **Renamed** `Transaction build(null)` to `Transaction freeze()`.
- **Renamed** `Transaction build(Client)` to `Transaction freezeWith(Client)`.
- **Removed** `setMaxQueryPayment(long)`.
- **Renamed** `setNodeId(AccountId)` to `setNodeAccountIds(List)`.

## Account-Related Changes

### `AccountBalanceQuery` Extends `Query`
- **Added** `AccountId getAccountId()`.
- **Added** `ContractId getContractId()`.
- **Changed** `Hbar execute(Client)` to `AccountBalance execute(Client)`.

### Added `AccountBalance`
- **Added** `Hbar balance`.
- **Added** `Map tokenBalances`.

### `AccountCreateTransaction` Extends `Transaction`
- **Added** `Key getKey()`.
- **Added** `Hbar getInitialBalance()`.
- **Added** `boolean getReceiverSignatureRequired()`.
- **Added** `AccountId getProxyAccountId()`.
- **Added** `Duration getAutoRenewPeriod()`.
- **Removed** `setSendRecordThreshold(long)` and `setSendRecordThreshold(Hbar)`.
- **Removed** `setReceiveRecordThreshold(long)` and `setReceiveRecordThreshold(Hbar)`.

### `AccountDeleteTransaction` Extends `Transaction`
- **Added** `AccountId getAccountId()`.
- **Added** `AccountId getTransferAccountId()`.
- **Renamed** `setDeleteAccountId()` to `setAccountId()`.

### `AccountId`
- **Added** `byte[] toBytes()`.
- **Added** `AccountId fromBytes(byte[])`.
- **Renamed** `long account` to `long num`.
- **Removed** `AccountId(AccountIDOrBuilder)`.
- **Removed** `AccountId toProto()`.

### `AccountInfo`
- **Added** `byte[] toBytes()`.
- **Added** `AccountInfo fromBytes(byte[])`.
- **Added** `List liveHashes`.
- **Changed** `long balance` to `Hbar balance`.
- **Renamed** `generateSendRecordThreshold` to `sendRecordThreshold`.
- **Renamed** `generateReceiveRecordThreshold` to `receiveRecordThreshold`.

### `AccountInfoQuery` Extends `Query`
- **Added** `AccountId getAccountId()`.

### `AccountRecordsQuery` Extends `Query`
- **Added** `AccountId getAccountId()`.

### `AccountStakersQuery` Extends `Query`
- **Added** `AccountId getAccountId()`.

### `AccountUpdateTransaction` Extends `Transaction`
- **Added** `AccountId getAccountId()`.
- **Added** `Key getKey()`.
- **Added** `Hbar getInitialBalance()`.
- **Added** `boolean getReceiverSignatureRequired()`.
- **Added** `AccountId getProxyAccountId()`.
- **Added** `Duration getAutoRenewPeriod()`.
- **Added** `Instant getExpirationTime()`.
- **Removed** `setSendRecordThreshold(long)` and `setSendRecordThreshold(Hbar)`.
- **Removed** `setReceiveRecordThreshold(long)` and `setReceiveRecordThreshold(Hbar)`.

## Transfer and Transaction Changes

### Removed `CryptoTransferTransaction`
- Use `TransferTransaction` instead.

### `TransferTransaction` Extends `Transaction`
- **Added** `TransferTransaction addTokenTransfer(TokenId, AccountId, long)`.
- **Added** `Map> getTokenTransfers()`.
- **Added** `TransferTransaction addHbarTransfer(AccountId, Hbar)`.
- **Added** `Map getHbarTransfers()`.

## Contract-Related Changes

### Renamed `ContractBytecodeQuery` to `ContractByteCodeQuery` Extends `Query`
- **Added** `ContractId getContractId()`.

### `ContractCallQuery` Extends `Query`
- **Added** `ContractId getContractId()`.
- **Added** `long getGas()`.
- **Added** `byte[] getFunctionParameters()`.

### `ContractCreateTransaction` Extends `Transaction`
- **Added** `FileId getBytecodeFileId()`.
- **Added** `Key getAdminKey()`.
- **Added** `long getGas()`.
- **Added** `Hbar getInitialBalance()`.
- **Added** `Duration getAutoRenewDuration()`.
- **Added** `AccountId getProxyAccountId()`.
- **Added** `String getContractMemo()`.
- **Added** `byte[] getConstructorParameters()`.
- **Removed** `setInitialBalance(long)`.

### `ContractDeleteTransaction` Extends `Transaction`
- **Added** `ContractId getContractId()`.
- **Added** `AccountId getTransferAccountId()`.
- **Added** `ContractId getTransferContractId()`.

### `ContractExecuteTransaction` Extends `Transaction`
- **Added** `ContractId getContractId()`.
- **Added** `long getGas()`.
- **Added** `Hbar getPayableAmount()`.
- **Added** `byte[] getFunctionParameters()`.
- **Removed** `setPayableAmount(long)`.

### `ContractId`
- **Added** `byte[] toBytes()`.
- **Added** `ContractId fromBytes(byte[])`.
- **Renamed** `long contract` to `long num`.
- **Removed** `ContractId(ContractIDOrBuilder)`.
- **Removed** `ContractId toProto()`.
- **Removed** `SignatureCase getSignatureCase()`.
- **Removed** `Key toProtoKey()`.

### `ContractInfo`
- **Added** `byte[] toBytes()`.
- **Added** `ContractInfo fromBytes(byte[])`.

### `ContractInfoQuery` Extends `Query`
- **Added** `ContractId getContractId()`.
- **Removed** `Method getMethod()`.

### Removed `ContractRecordsQuery`

### `ContractUpdateTransaction` Extends `Transaction`
- **Added** `ContractId getContractId()`.
- **Added** `FileId getBytecodeFileId()`.
- **Added** `Key getAdminKey()`.
- **Added** `Duration getAutoRenewDuration()`.
- **Added** `AccountId getProxyAccountId()`.
- **Added** `String getContractMemo()`.
- **Added** `Instant getExpirationTime()`.

## File-Related Changes

### `FileAppendTransaction`
- **Added** `FileId getFileId()`.
- **Added** `byte[] getContents()`.

### `FileContentsQuery`
- **Added** `FileId getFileId()`.

### `FileCreateTransaction`
- **Added** `byte[] getContents()`.
- **Added** `Collection getKeys()`.
- **Added** `Instant getExpirationTime()`.
- **Renamed** `addKey(Key)` to `setKeys(Key...)`.

### `FileDeleteTransaction`
- **Added** `FileId getFileId()`.

### `FileId`
- **Added** `byte[] toBytes()`.
- **Added** `FileId fromBytes(byte[])`.
- **Renamed** `long file` to `long num`.
- **Removed** `FileId fromSolidityAddress()`.
- **Removed** `FileId(FileIDOrBuilder)`.
- **Removed** `FileId toProto()`.
- **Removed** `String toSolidityAddress()`.

### `FileInfo`
- **Added** `byte[] toBytes()`.
- **Added** `FileInfo fromBytes(byte[])`.
- **Updated** `List keys` to `KeyList keys`.

### `FileInfoQuery`
- **Added** `FileId getFileId()`.

### `FileUpdateTransaction`
- **Added** `FileId getFileId()`.
- **Added** `byte[] getContents()`.
- **Added** `Collection getKeys()`.
- **Added** `Instant getExpirationTime()`.
- **Renamed** `addKey(Key)` to `setKeys(Key...)`.

## Consensus and Topic Changes

### Removed `ConsensusClient`
- Use `Client` instead, and set mirror network using `Client.setMirrorNetwork()`.

### Removed `ConsensusTopicMessage`

### Renamed `MirrorConsensusTopicResponse` to `TopicMessage`
- **Added** `TopicMessageChunk[] chunks`: This will be non-null for a topic message constructed from multiple transactions.
- **Renamed** `byte[] message` to `byte[] contents`.
- **Removed** `byte[] getMessage()`.
- **Removed** `ConsensusTopicId topicId`.

### Renamed `MirrorConsensusTopicChunk` to `TopicMessageChunk`

### Renamed `MirrorTopicMessageQuery` to `TopicMessageQuery`
- **Added** `setErrorHandler(BiConsumer)`: This error handler will be called if the max retry count is exceeded, or if the subscribe callback errors out for a specific `TopicMessage`.
- **Changed** `MirrorSubscriptionHandle subscribe(MirrorClient, Consumer, Consumer)` to `subscribe(Client, Consumer)`: Use `setErrorHandler()` instead of passing it in as the third parameter.

### Renamed `ConsensusTopicCreateTransaction` to `TopicCreateTransaction`
- **Added** `String getTopicMemo()`.
- **Added** `Key getAdminKey()`.
- **Added** `Key getSubmitKey()`.
- **Added** `Duration getAutoRenewDuration()`.
- **Added** `AccountId getAutoRenewAccountId()`.

### Renamed `ConsensusTopicDeleteTransaction` to `TopicDeleteTransaction`
- **Added** `TopicId getTopicId()`.

### Renamed `ConsensusMessageSubmitTransaction` to `TopicMessageSubmitTransaction`
- **Added** `TopicId getTopicId()`.
- **Added** `byte[] getMessage()`.
- **Removed** `setChunkInfo(TransactionId, int, int)`.

### Renamed `ConsensusTopicId` to `TopicId`
- **Renamed** `long topic` to `long num`.
- **Removed** `ConsensusTopicId(TopicIDOrBuilder)`.

### Renamed `ConsensusTopicInfo` to `TopicInfo`
- **Added** `byte[] toBytes()`.
- **Added** `TopicInfo fromBytes()`.
- **Renamed** `ConsensusTopicId id` to `TopicId topicId`.

### Renamed `ConsensusTopicInfoQuery` to `TopicInfoQuery`
- **Added** `TopicId getTopicId()`.

### Renamed `ConsensusTopicUpdateTransaction` to `TopicUpdateTransaction`
- **Added** `TopicId getTopicId()`.
- **Added** `String getTopicMemo()`.
- **Added** `Key getAdminKey()`.
- **Added** `Key getSubmitKey()`.
- **Added** `Duration getAutoRenewDuration()`.
- **Added** `AccountId getAutoRenewAccountId()`.

## Token-Related Changes

### `TokenAssociateTransaction` Extends `Transaction`
- **Added** `AccountId getAccountId()`.
- **Added** `List getTokenIds()`.
- **Renamed** `addTokenId(TokenId)` to `setTokenIds(List)`.

### Removed `TokenBalanceQuery`
- Use `AccountBalanceQuery` to fetch token balances since `AccountBalance` contains `tokenBalances`.

### `TokenBurnTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `long getAmount()`.

### `TokenCreateTransaction` Extends `Transaction`
- **Added** `AccountId getTreasuryAccountId()`.
- **Added** `Key getAdminKey()`.
- **Added** `Key getKycKey()`.
- **Added** `Key getSupplyKey()`.
- **Added** `Key getWipeKey()`.
- **Added** `Key getFreezeKey()`.
- **Added** `boolean getFreezeDefault()`.
- **Added** `Instant getExpirationTime()`.
- **Added** `AccountId getAutoRenewAccountId()`.
- **Added** `Duration getAutoRenewPeriod()`.
- **Added** `int getDecimals()`.
- **Renamed** `setName(String)` to `setTokenName(String)`.
- **Renamed** `setSymbol(String)` to `setTokenSymbol(String)`.
- **Renamed** `setTreasury(AccountId)` to `setTreasuryAccountId(AccountId)`.
- **Renamed** `setAutoRenewAccount(AccountId)` to `setAutoRenewAccountId(AccountId)`.

### `TokenDeleteTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.

### `TokenDissociateTransaction` Extends `Transaction`
- **Added** `AccountId getAccountId()`.
- **Added** `List getTokenIds()`.
- **Renamed** `addTokenId(TokenId)` to `setTokenIds(List)`.

### `TokenFreezeTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `AccountId getAccountId()`.

### `TokenGrantKycTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `AccountId getAccountId()`.

### `TokenId`
- **Added** `byte[] toBytes()`.
- **Added** `TokenId fromBytes(byte[])`.
- **Removed** `TokenId(TokenIDOrBuilder)`.
- **Removed** `fromSolidityAddress(String)`.
- **Removed** `String toSolidityAddress()`.
- **Removed** `TokenId toProto()`.

### `TokenInfo`
- **Added** `byte[] toBytes()`.
- **Added** `TokenInfo fromBytes(byte[])`.
- **Renamed** `AccountId treasury` to `AccountId treasuryAccountId`.
- **Renamed** `Instant expiry` to `Instant expirationTime`.

### `TokenInfoQuery` Extends `Query`
- **Added** `TokenId getTokenId()`.

### `TokenMintTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `long getAmount()`.

### `TokenRelationship`
- **Added** `byte[] toBytes()`.
- **Added** `TokenRelationship fromBytes(byte[])`.

### `TokenRevokeKycTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `AccountId getAccountId()`.

### Removed `TokenTransferTransaction`
- Use `TransferTransaction` instead.

### `TokenUnfreezeTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `AccountId getAccountId()`.

### `TokenUpdateTransaction` Extends `Transaction`
- **Added** `AccountId getTreasuryAccountId()`.
- **Added** `Key getAdminKey()`.
- **Added** `Key getKycKey()`.
- **Added** `Key getSupplyKey()`.
- **Added** `Key getWipeKey()`.
- **Added** `Key getFreezeKey()`.
- **Added** `boolean getFreezeDefault()`.
- **Added** `Instant getExpirationTime()`.
- **Added** `AccountId getAutoRenewAccountId()`.
- **Added** `Duration getAutoRenewPeriod()`.
- **Added** `int getDecimals()`.
- **Renamed** `setName(String)` to `setTokenName(String)`.
- **Renamed** `setSymbol(String)` to `setTokenSymbol(String)`.
- **Renamed** `setTreasury(AccountId)` to `setTreasuryAccountId(AccountId)`.
- **Renamed** `setAutoRenewAccount(AccountId)` to `setAutoRenewAccountId(AccountId)`.

### `TokenWipeTransaction` Extends `Transaction`
- **Added** `TokenId getTokenId()`.
- **Added** `AccountId getAccountId()`.

## System and Exception Changes

### `FreezeTransaction`
- **Added** `Instant getStartTime()`.
- **Added** `Instant getEndTime()`.

### Removed `HbarRangeException`
- If `Hbar` is out of range, `Hedera` will error instead.

### Removed `HederaConstants`
- No replacement.

### Removed `HederaNetworkException`

### Renamed `HederaPrecheckStatusException` to `PrecheckStatusException`

### Renamed `HederaReceiptStatusException` to `ReceiptStatusException`

### Removed `HederaRecordStatusException`
- `ReceiptStatusException` will be thrown instead.

### Removed `HederaStatusException`
- A `PrecheckStatusException` or `ReceiptStatusException` will be thrown instead.

### Removed `HederaThrowable`
- No replacement.

### Removed `LocalValidationException`
- No replacement. Local validation is no longer done.

### `SystemDeleteTransaction`
- **Added** `FileId getFileId()`.
- **Added** `ContractId getContractId()`.
- **Added** `Instant getExpirationTime()`.

### `SystemUndeleteTransaction`
- **Added** `FileId getFileId()`.
- **Added** `ContractId getContractId()`.

### `TransactionId`
- **Added** `byte[] toBytes()`.
- **Added** `TransactionId fromBytes(byte[])`.
- **Removed** `TransactionId(TransactionIDOrBuilder)`.
- **Removed** `TransactionID toProto()`.
- **Removed** `TransactionId withValidStart(AccountId, Instant)`: Use `new TransactionId(AccountId, Instant)` instead.
- **Removed** `TransactionId(AccountId)`: Use `TransactionId generate(AccountId)` instead.

### Removed `TransactionList`

### `TransactionReceipt`
- **Exposed** `ExchangeRate exchangeRate`.
- **Exposed** `AccountId accountId`.
- **Exposed** `FileId fileId`.
- **Exposed** `ContractId contractId`.
- **Exposed** `TopicId topicId`.
- **Exposed** `TokenId tokenId`.
- **Exposed** `long topicSequenceNumber`.
- **Exposed** `byte[] topicRunningHash`.
- **Added** `byte[] toBytes()`.
- **Added** `TransactionReceipt fromBytes()`.
- **Added** `long totalSupply`.
- **Removed** `AccountId getAccountId()`: Use `AccountId accountId` directly instead.
- **Removed** `ContractId getContractId()`: Use `ContractId contractId` directly instead.
- **Removed** `FileId getFileId()`: Use `FileId fileId` directly instead.
- **Removed** `TokenId getTokenId()`: Use `TokenId tokenId` directly instead.
- **Removed** `ConsensusTopicId getConsensusTopicId()`: Use `TopicId topicId` directly instead.
- **Removed** `long getConsensusTopicSequenceNumber()`: Use `long sequenceNumber` directly instead.
- **Removed** `byte[] getConsensusTopicRunningHash()`: Use `byte[] topicRunningHash` directly instead.
- **Removed** `TransactionReceipt toProto()`.

### `TransactionReceiptQuery` Extends `Query`
- **Added** `TransactionId getTransactionId()`.

### `TransactionRecord`
- **Added** `byte[] toBytes()`.
- **Added** `TransactionRecord fromBytes()`.
- **Removed** `ContractFunctionResult getContractExecuteResult()`: Use `ContractFunctionResult contractFunctionResult` directly instead.
- **Removed** `ContractFunctionResult getContractCreateResult()`: Use `ContractFunctionResult contractFunctionResult` directly instead.
- **Removed** `TransactionRecord toProto()`.

### `TransactionRecordQuery` Extends `Query`
- **Added** `TransactionId getTransactionId()`.

## Hbar and Client Changes

### `Hbar`
- **Added** `Hbar fromString(CharSequence)`.
- **Added** `Hbar fromString(CharSequence, HbarUnit)`.
- **Added** `Hbar from(long, HbarUnit)`.
- **Added** `Hbar from(BigDecimal, HbarUnit)`.
- **Added** `BigDecimal getValue()`.
- **Added** `String toString(HbarUnit)`.
- **Renamed** `fromTinybar(long)` to `fromTinybars(long)`.
- **Renamed** `Hbar of(long)` to `from(long)`.
- **Renamed** `Hbar of(BigDecimal)` to `from(BigDecimal)`.
- **Renamed** `Hbar as(HbarUnit)` to `to(HbarUnit)`.
- **Renamed** `long asTinybar()` to `long toTinybars()`.

### `Client`
- **Added** `void setMirrorNetwork(List)`.
- **Added** `List getMirrorNetwork()`.
- **Added** `Client forNetwork(Map)`.
- **Added** `void ping(AccountId)`.
- **Added** `PublicKey getOperatorPublicKey()`.
- **Added** `Client setNetwork(Map)`.
- **Added** `Map getNetwork()`.
- **Renamed** `fromJson(String)` to `fromConfig(String)` and `fromJson(Reader)` to `fromConfig(Reader)`.
- **Renamed** `fromFile(String)` to `fromConfigFile(String)` and `fromFile(File)` to `fromConfigFile(File)`.
- **Renamed** `getOperatorId()` to `getOperatorAccountId()`.
- **Removed** `constructor(Map)`.
- **Removed** `Client replaceNodes(Map)`.
- **Removed** `Client setMaxTransactionFee(long)`.
- **Removed** `Client setMaxQueryPayment(long)`.
- **Removed** `AccountInfo getAccount(AccountId)`.
- **Removed** `void getAccountAsync()`.
- **Removed** `Hbar getAccountBalance(AccountId)`: Use `AccountBalanceQuery` instead.
- **Removed** `void getAccountBalanceAsync()`.
- **Changed** `Client setOperatorWith(AccountId, PublicKey, TransactionSigner)` to `Client setOperatorWith(AccountId, PublicKey, Function)`.