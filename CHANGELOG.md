# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.58.0

### Added

- Manual Signature Injection Support for HSM-based Transaction Signing https://github.com/hiero-ledger/hiero-sdk-java/issues/2350
  - new APIs added
    - SignableNodeTransactionBodyBytes - class that represents a transaction body ready for external signing, explicitly associated with a node account ID and transaction ID.
    - Transaction.signableNodeBodyBytesList() - array of SignableNodeTransactionBodyBytes containing the canonical bodyBytes paired explicitly with their respective nodeAccountId and transactionId for signing.
      https://github.com/hiero-ledger/hiero-sdk-java/pull/2356

### Changed

- Refactor of "fromBytes" method inside Transaction class https://github.com/hiero-ledger/hiero-sdk-java/issues/2357
  - Introduced smaller private methods that improve readability and follow single responsibility principle https://github.com/hiero-ledger/hiero-sdk-java/pull/2338

## 2.57.0

### Added

- Support for HIP-1046. https://hips.hedera.com/hip/hip-1046
  - Introduced new grpc web proxy field in the address book schema, making node operators manage their web proxies. https://github.com/hiero-ledger/hiero-sdk-java/pull/2337
  - NodeCreateTransaction
    - Endpoint grpcWebProxyEndpoint - A web proxy for gRPC from non-gRPC clients.
    - Endpoint getGrpcWebProxyEndpoint()
    - NodeCreateTransaction setGrpcWebProxyEndpoint(Endpoint)
  - NodeUpdateTransaction
    - Endpoint grpcWebProxyEndpoint - A web proxy for gRPC from non-gRPC clients.
    - Endpoint getGrpcWebProxyEndpoint()
    - NodeUpdateTransaction setGrpcWebProxyEndpoint(Endpoint)
- Support transaction size calculation before submission. This is useful for fee estimation, transaction validation, and batching logic. https://github.com/hiero-ledger/hiero-sdk-java/issues/2330
  - Following APIs were implemented: https://github.com/hiero-ledger/hiero-sdk-java/pull/2324
    - Transaction.size: uint
      Returns the total size (in bytes) of the protobuf-encoded transaction, including signatures and metadata.

    - Transaction.bodySize: uint
      Returns the protobuf-encoded transaction body size (excluding signatures), using a placeholder node account ID.

    - ChunkTransaction.bodySizeAllChunks: uint[]
      For chunked transactions (e.g. FileAppendTransaction, TopicMessageSubmitTransaction), returns an array of body sizes for each chunk.

## 2.56.1

### Added

- support for decline reward in node create and node update transaction https://github.com/hiero-ledger/hiero-sdk-java/issues/2329

  https://github.com/hiero-ledger/hiero-sdk-java/pull/2333

## 2.56.0

### Changed

- Improve `setECDSAKeyWithAlias` and `setKeyWithAlias` for `AccountCreateTransaction` https://github.com/hiero-ledger/hiero-sdk-java/issues/2319
  - Update and unify the method signatures to accept a more flexible input type, such as Key, which can represent either an ECDSA PrivateKey or a PublicKey. The updated behavior should support both scenarios:
    - If a PrivateKey is provided: use it to set the key and internally derive the alias from its corresponding public key.
    - If a PublicKey is provided: use it directly to set the key and derive the alias from that public key.

    This ensures that both PrivateKey and PublicKey inputs result in the same outcome: https://github.com/hiero-ledger/hiero-sdk-java/pull/2318

### Fixed

- Client.setNetwork() does not propagate address book https://github.com/hiero-ledger/hiero-sdk-java/issues/2317
  - Now, on Node creation (after Client.setNetwork() call) we attach the corresponding address book if one is present https://github.com/hiero-ledger/hiero-sdk-java/pull/2322

## 2.55.0

### Added

- Support for HIP-551 Batch Transaction https://hips.hedera.com/hip/hip-551
  It defines a mechanism to execute batch transactions such that a series of transactions (HAPI calls) depending on each other can be rolled into one transaction that passes the ACID test (atomicity, consistency, isolation, and durability).
  https://github.com/hiero-ledger/hiero-sdk-java/pull/2285
  - New BatchTransaction class that consists of List<Transaction> innerTransactions and List<TransactionId> innerTransactionIds.
  - batchKey field in Transaction class that must sign the BatchTransaction
  - new batchify method that sets the batch key and marks a transaction as part of a batch transaction (inner transaction). The transaction is signed by the client of the operator and frozen.
  - freezeWith: Additional check if batchKey is null.
    If not present → indication that this transaction is not meant to be inside a batch
    If preset → indication that this transaction is part of a batch and setting nodeAccountId to 0.0.0
- New TCK method that handles TokenAirdropTransaction https://github.com/hiero-ledger/hiero-sdk-java/issues/2280
- Handling of non-zero shard and realms for static files https://github.com/hiero-ledger/hiero-sdk-java/pull/2308

### Fixed

- Not verifying the receipt for multiple `THROTTLED_AT_COSENSUS` responses. Also enhancing the retry mechanism for this status code using backoffs.

## 2.54.0

### Added

- Support for HIP-1021: Improve Assignment of Auto-Renew Account ID for Topics (https://hips.hedera.com/hip/hip-1021). The autoRenewAccountId will automatically be set to the payer_account_id of the transaction
  if an Admin Key is not provided during topic creation [#2301](https://github.com/hiero-ledger/hiero-sdk-java/pull/2301)

### Fixed

- Upon `TopicUpdateTransaction` you are not able to clear the custom fees, feeExemptKey and feeScheduleKey [#2284](https://github.com/hiero-ledger/hiero-sdk-java/pull/2284)
  The logic is updated as follows:
  - If we have null list (that is the default value) → Do nothing (don’t send a request).
  - If we have empty list → Send a request to clear the list.
  - For Non-empty list → Send a request with the provided list.
  - `clearFeeScheduleKey` sets the key to empty KeyList.

## 2.53.0

### Added

- Enabled gRPC retry option for more stable connection with nodes [#2264](https://github.com/hiero-ledger/hiero-sdk-java/pull/2264)

## 2.52.0

### Removed

- Automatic setting of autorenew account for topic create.

### Added

- `ScheduledNetworkUpdate` example.

## 2.51.0

### Added

- Support for HIP-1021: Improve Assignment of Auto-Renew Account ID for Topics.

### Deprecated

- Methods that explicitly set shard and realm to 0.

### Fixed

- `EthereumFlow` for large contract operations.

## 2.50.0

### Added

- Support for HIP-991: revenue generating topics.

### Removed

- `AccountStakersQuery`, since it was not supported in consensus node for a long time and now it's removed permanently.

### Deprecated

- `Livehash` transactions and queries `SystemDeleteTransaction` and `SystemUndeleteTransaction`.

### Fixed

- Race condition when updating the address books.

## 2.49.0

### Added

- New APIs in `AccountCreateTransaction` : `setKeyWithAlias(ECDSAKey)`, `setKeyWithAlias(Key, ECDSAKey)` and `setKeyWithoutAlias(Key)`.

### Changed

- Deprecated `setKey` in `AccountCreateTransaction`.

## 2.48.0

### Changed

- New query payment is generated when the SDK receives status `BUSY`.

### Added

- Retry mechanism for resubmitting the transaction in case the SDK receives status `THROTTLED_AT_CONSENSUS` in the transaction receipt.

## 2.47.0

### Added

- `nextExchangeRate` property in the `TransactionReceipt`.
- Warning log when trying to execute transaction against nodeID which is not in the client configuration.

### Changed

- Optimised `MirrorNodeContractEstimateGasQuery` and `MirrorNodeContractCallQuery` to call `toEvmAddress` instead calling the mirror node.

## 2.46.0

### Added

- 2 new queries `MirrorNodeContractCallQuery` and `MirrorNodeContractEstimateGasQuery` for estimation/simulation of contract operations.
- Support for Long Term Scheduled Transactions (HIP-423).
- Validation for creating Public keys from bytes.

### Fixed

- A bug where service port was overridden for `NodeUpdateTransaction`.
- Runtime errors using older Android APIs (26+).

## 2.45.0

### Fixed

- A bug where optional fields for `NodeUpdateTransaction` were being set as default values.

### Added

- New api for creating client with mirror network - `forMirrorNetwork`

## 2.44.0

### Fixed

- Do not shut down externally provided executor
- Fix `IllegalStateException` when specific node id is not present in address book

## 2.43.0

### Changed

- Update protobufs from `hedera-services`
- `client.setNetworkFromAddressBook` updates the address book by default

## 2.42.0

### Added

- `addBytes4` and `addBytes4Array` methods for `ContractFunctionParameters`
- Support ports that are not well known for use with port forwarding

### Fixed

- `setFileId` resets `contractId`, and similarly, when `setContractId` resets `fileId`

## 2.41.0

### Fixed

- Android runtime errors for API levels 26-33.

## 2.40.0

### Added

- The ability to create multiple Clients using shared `ExecutorService`
- `closeChannels` method to `Client` to close the channels without closing the executor
- New method to update security parameters when updating the addressbook

### Fixed

- handling of `BUSY` status code

## 2.39.0

### Added

- `TokenAirdropTransaction`,`TokenClaimAirdropTransaction`,`TokenCancelAirdropTransaction` (HIP-904)

### Fixed

- handling of `FEE_SCHEDULE_FILE_PART_UPLOADED` status code

## 2.38.0

### Added

- `NodeCreateTransaction`,`NodeUpdateTransaction`,`NodeDeleteTransaction` (part of HIP-869)

### Changed

- updated examples

## 2.37.0

### Changed

- updated `bouncycastle` dependency
- updated `protoc` dependency
- other various codebase chores

## 2.36.0

### Added

- Token Reject functionality (part of HIP-904)

## 2.36.0-beta.1

### Added

- Token Reject functionality (part of HIP-904)

## 2.35.1

### Added

- `toStandardECDSAsecp256k1PrivateKeyCustomDerivationPath` function to `Mnemonic`
- `fromBytes` function to `Key`

### Changed

- build scripts rework

### Fixed

- handling of `PLATFORM_NOT_ACTIVE` status code when calling `getReceipt()` or `getRecord()`
- _build scripts fix_
- _paths_ in examples/README.md

## 2.35.0

### Added

- `toStandardECDSAsecp256k1PrivateKeyCustomDerivationPath` function to `Mnemonic`
- `fromBytes` function to `Key`

### Changed

- build scripts rework

### Fixed

- handling of `PLATFORM_NOT_ACTIVE` status code when calling `getReceipt()` or `getRecord()`

## 2.34.0

### Added

- possibility to change or remove existing keys from a token (HIP-540)

## 2.33.1

### Changed

- `AccountBalanceQuery`, `AccountInfoQuery`, and `ContractInfoQuery` get all the data from consensus nodes again

## 2.33.0

### Added

- add `decimal` field to `TokenRelationship` class

### Changed

- `AccountBalanceQuery`, `AccountInfoQuery`, and `ContractInfoQuery` get part of the data from the Mirror Node REST API (HIP-367)
- improved documentation in repository
- updated protobufs

### Fixed

- undeprecate `AccountBalance.tokens`, `AccountBalance.tokenDecimals`, `AccountInfo.tokenRelationships`, `ContractInfo.tokenRelationships`

### Deprecated

- `TokenRelationship.symbol`, use `TokenInfo.symbol` instead
- `AccountId.populateAccountNumAsync`, use `AccountId.populateAccountNum` instead
- `AccountId.populateAccountEvmAddressAsync`, use `AccountId.populateAccountEvmAddress` instead
- `ContractId.populateContractNumAsync`, use `ContractId.populateContractNum` instead

## 2.32.0

### Added

- METADATA key and possibility to update NFT metadata (HIP-657)
- Fungible Token Metadata Field (HIP-646)
- NFT Collection Token Metadata Field (HIP-765)
- a method to calculate the recoveryId for ECDSA signatures

### Changed

- improved documentation in repository
- updated protobufs

## 2.32.0-beta.1

### Added

- METADATA key and possibility to update NFT metadata (HIP-657)
- Fungible Token Metadata Field (HIP-646)
- NFT Collection Token Metadata Field (HIP-765)

### Changed

- improved documentation in repository
- updated protobufs

## 2.31.0

### Added

- possibility to optionally send transaction data without required transaction fields (HIP-745)

## 2.30.0

### Added

- `signerNonce` field to `ContractFunctionResult` (HIP-844)

## 2.29.2

### Fixed

- removed `streamsupport` dependency

## 2.29.1

### Fixed

- `Transaction.fromBytes()` and `Transaction.getSignatures()` throw an exception for transactions signed using an ECDSA key
- spurious `INVALID_TRANSACTION_START` in `TransactionId.generate()`

## 2.29.0

### Added

- `populateAccountEvmAddress` and `populateAccountEvmAddressAsync` to `AccountId`

### Fixed

- duplicate transaction IDs at high TPS (>25K)
- android compatibility issues

### Changed

- GRPC configurations

## 2.28.0

### Added

- `addBoolArray` function to `ContractFunctionParameters`

### Fixed

- `java.lang.VerifyError` exception in examples

### Changed

- updated addressbooks
- improved timeout handling

## 2.27.0

### Fixed

- Timeout for TransactionResponse.getReceipt not working well

## 2.26.0

### Added

- `contractNonces` to `ContractFunctionResult`
- Support for ECDSA keys generated by OpenSSL

## 2.25.0

### Added

- Custom logger used in `Client` and `Executable`

### Removed

- ThreeTen dependency
- Jabel dependency
- future-converter-java8-guava dependency
- sdk-jdk7 artifact

### Changed

- Minimum Java version to 17
- Minimum supported Android version to 8

## 2.24.1

### Fixed

- The `transactionId` nanoseconds are now left padded with 0s for 9 chars
- Importing and exporting ECDSA keys to DER format is now compatible with OpenSSL

## 2.24.0

### Added

- Alias support in `AccountCreateTransaction`
- `CreateAccountWithAliasExample`
- `CreateAccountWithAliasAndReceiverSignatureRequiredExample`

## 2.22.0

### Added

- Remove the insecure port of mirror nodes
- Documentation for all API classes, methods and fields
- Delegate spender functionality to `AccountAllowanceApproveTransaction`

### Fixed

- Some failing integration tests

## 2.21.0

### Added

- `TransactionRecord.evmAddress`
- `PublicKeyECDSA.toEvmAddress()`
- `AccountId.fromEvmAddress()`
- `AccountId.fromString()` now supports EVM address
- `TransferTransaction.addHbarTransfer()` now supports EVM address
- `AutoCreateAccountTransferTransactionExample`
- `TransferUsingEvmAddressExample`
- `AccountCreationWaysExample`

## 2.20.0

### Added

- `TRANSACTION_HAS_UNKNOWN_FIELDS` and `ACCOUNT_IS_IMMUTABLE` in `Status`
- `toStandard[Ed25519|ECDSAsecp256k1]PrivateKey()` to `Mnemonic`
- `fromSeed[ED25519|ECDSAsecp256k1]()` to `PrivateKey`
- `[PrivateKeyED25519|PrivateKeyECDSA].fromSeed()`
- `Bip32Utils` class

### Fixed

- Misleading logging when an unhealthy node is hit
- Default mirror node for mainnet is now `mainnet-public.mirrornode.hedera.com:443` instead of `mainnet-public.mirrornode.hedera.com:5600`
- Default mirror node for testnet is now `testnet.mirrornode.hedera.com:443` instead of `hcs.testnet.mirrornode.hedera.com:5600`
- Default mirror node for previewnet is now `previewnet.mirrornode.hedera.com:443` instead of `hcs.previewnet.mirrornode.hedera.com:5600`
- ECDSA secp256k1 keys now support derivation

### Deprecated

- `Mnemonic.toPrivateKey()` use `Mnemonic.toStandard[Ed25519|ECDSAsecp256k1]PrivateKey` instead
- `PrivateKey.fromMnemonic()` use `Mnemonic.toStandard[Ed25519|ECDSAsecp256k1]PrivateKey` instead

## 2.19.0

### Added

- `freezeWith()` and `sign()` to `ContractCreateFlow`

### Fixed

- `Executable.execute(Client client, Duration timeout)` now sets gRPC deadline to the underlying gRPC request
- Transaction sometimes being reported as duplicate when submitting large number of transactions
- `RejectedExecutionException` under heavy load
- `nodes` not clearing when reusing transaction
- BIP-39 - unicode mnemonic passphrases are normalized to NFKD
- Renamed allowanceSpenderAccountId to spenderId in TokenNftInfo

## 2.18.2

### Fixed

- `Client.close()` now tracks and automatically unsubscribes from Mirror Node Topic Queries

## 2.18.1

### Added

- `ContractHelper` now supports `bytecode` property in compiled contract JSON.
- `ZeroTokenOperationsExample`
- `TransactionResponse.[get|set]ValidateStatus()`
- `TransactionReceipt.validateStatus()`
- `TransactionRecord.validateReceiptStatus()`
- `TransactionReceipt.transactionId`
- `TopicUpdateTransaction.[get|set]ExpirationTime()`
- `CustomFee.[set|get]AllCollectorsAreExempt()`
- `ExemptCustomFeesExample`
- `AccountCreateWithHtsExample`

### Fixed

- Execute with a timeout can ignore timeout and block indefinitely in CI tests
- The Android example can now be run with the local SDK version

## 2.17.4

### Added

- `AccountCreateTransaction.[set|get]alias[Key|EvmAddress]()`
- `ContractCreateFlow.[set|get]MaxChunks()`
- `Status.[to|from]ResponseCode()`
- `ContractCreateFlow.[set|get]AutoRenewAccountId()`
- Client now automatically updates the network via a mirror node query at regular intervals.  You can set/get the interval with `Client.[set|get]NetworkUpdatePeriod()`
- Client can now be set from a `NodeAddressBook` with `Client.setNetworkFromAddressBook()`
- `Client.setMirrorTransportSecurity()`
- `Client.mirrorIsTransportSecurity()`
- `SolidityPrecompileExample`
- Improved PEM file support via `PrivateKey.fromPem()`

### Fixed

- `AccountId`s with `aliasEvmAddress` now serialize/deserialize correctly.
- `TokenCreateTransaction`'s default fee is now 40 Hbar.
- `validateChecksum()`, `toStringWithChecksum()`, `hashCode()`, `equals()`, and `compareTo()` now function correctly for `AccountId`s with `aliasEvmAddress`es.
- Changed the default transaction fee for `AccountCreateTransaction` to 5 Hbar.
- `PrivateKey.is[ED25519|ECDSA]()` is now correct for ED25519 private keys.
- Default mirror node for mainnet is now `mainnet-public.mirrornode.hedera.com:443` instead of `hcs.mainnet.mirrornode.hedera.com:5600`

## v2.17.3

### Fixed

- Thread leak in `Client`
- `Client.setTransportSecurity()` now updates mirror network

## v2.17.2

### Added

- `ContractUpdateTransaction.clearStaked[Account|Node]Id()`

### Deprecated

- `[Contract|Account]CreateTransaction.[set|get]ProxyAccountId()` with no replacement
- `ContractCreateFlow.[set|get]ProxyAccountId()` with no replacement

### Fixed

- `ContractCreateFlow.setMaxAutomaticTokenAssociations()`
- `ContractFunctionResult.senderAccountId` now serializes correctly
- `CustomRoyaltyFee` now clones and `toString()`s correctly
- `ScheduleCreateTransaction.expirationTime` now deserializes correctly
- `ScheduleInfo` now deserializes correctly
- Made `StakingInfo.[to|from]Bytes()` public
- `TransactionReceipt.topicRunningHash` now `toString()`s correctly
- `TransactionRecord.[prngBytes|prngNumber|tokenNftTransfers]` now serializes/deserializes correctly
- `[Account|Contract]UpdateTransaction.getDeclineStakingReward()` now returns `@Nullable Boolean` instead of `boolean`, and no longer throws a `NullPointerException`
- `Client.setNodeMaxBackoff()`
- Undeprecate `*ContractId.fromSolidityAddress()`

## v2.17.1

### Added

- `TokenNftInfo.allowanceSpenderAccountId`

### Deprecated

- `AccountBalance.[tokens|tokenDecimals]` use a mirror node query instead
- `AccountInfo.tokenRelationships` use a mirror node query instead
- `ContractInfo.tokenRelationships` use a mirror node query instead

### Fixed

- `TokenNftInfo.[to|from]Bytes()`

## v2.17.0

### Added

* `PrngThansaction`
* `TransactionRecord.prngBytes`
* `TransactionRecord.prngNumber`

### Deprecated

- `ContractFunctionResult.stateChanges` - Use mirror node for contract traceability instead
- `ContractStateChanges`
- `StorageChange`

## v2.16.3

### Added

* `ContractCreateTransaction.autoRenewAccountId`
* `ContractUpdateTransaction.autoRenewAccountId`

## v2.16.2

### Added

* `HbarUnit.getSymbol()`
* `SemanticVersion.toString()`
* `Executable.setRequestListener()`
* `Executable.setResponseListener()`

### Fixed

* `PrivateKey.fromString()` should support `0x` prefix
* `ManagedNodeAddress.equals()` should compare ports

## v2.16.1

### Added

* `ScheduleInfo.waitForExpiry`
* `ScheduleInfo.ledgerId`
  "

## v2.16.0

### Added

* `StakingInfo`
* `AccountCreateTransaction.stakedAccountId`
* `AccountCreateTransaction.stakedNodeId`
* `AccountCreateTransaction.declineStakingReward`
* `ContractCreateTransaction.stakedAccountId`
* `ContractCreateTransaction.stakedNodeId`
* `ContractCreateTransaction.declineStakingReward`
* `AccountUpdateTransaction.stakedAccountId`
* `AccountUpdateTransaction.stakedNodeId`
* `AccountUpdateTransaction.declineStakingReward`
* `ContractUpdateTransaction.stakedAccountId`
* `ContractUpdateTransaction.stakedNodeId`
* `ContractUpdateTransaction.declineStakingReward`
* `TransactionRecord.paidStakingRewards`
* `ScheduleCreateTransaction.expirationTime`
* `ScheduleCreateTransaction.waitForExpiry`
* Protobuf requests and responses will be logged in hex
* There should be three artifacts now, `sdk-jdk7`, `sdk`, and `sdk-full`

## v2.16.0-beta.1

### Added

* `StakingInfo`
* `AccountCreateTransaction.stakedAccountId`
* `AccountCreateTransaction.stakedNodeId`
* `AccountCreateTransaction.declineStakingReward`
* `ContractCreateTransaction.stakedAccountId`
* `ContractCreateTransaction.stakedNodeId`
* `ContractCreateTransaction.declineStakingReward`
* `AccountUpdateTransaction.stakedAccountId`
* `AccountUpdateTransaction.stakedNodeId`
* `AccountUpdateTransaction.declineStakingReward`
* `ContractUpdateTransaction.stakedAccountId`
* `ContractUpdateTransaction.stakedNodeId`
* `ContractUpdateTransaction.declineStakingReward`
* `TransactionRecord.paidStakingRewards`
* `ScheduleCreateTransaction.expirationTime`
* `ScheduleCreateTransaction.waitForExpiry`

## v2.15.0

### Added

* `EthereumFlow`
* `EthereumTransactionData`
* `EthereumTransactionDataLegacy`
* `EthereumTransactionDataEip1559`

## v2.14.0

## v2.14.0-beta.3

* add missing javadoc to the sdk files

### Deprecated

* `TransactionResponse.scheduledTransactionId` with no replacement.

### Added

* `AccountId.aliasEvmAddress`
* `ContractCreateTransaction.[get|set]MaxAutomaticTokenAssociations()`
* `ContractCreateTransaction.[get|set]Bytecode()`
* `ContractUpdateTransaction.[get|set]MaxAutomaticTokenAssociations()`
* `ContractCreateFlow.[get|set]MaxAutomaticTokenAssociations()`
* `AccountInfo.ethereumNonce`
* `ContractCallResult.senderAccountId`
* `ContractCallQuery.[get|set]SenderAccountId()`
* `TransactionRecord.ethereumHash`
* `EthereumTransaction`
* `CustomRoyaltyFee.getFallbackFee()`
* `TransactionResponse.get[Receipt|Record]Query()`

## v2.13.0 - Where did it go?!

## v2.12.0

### Added

* `AccountAllowanceAdjustTransaction` with no replacement.
* `AccountAllowanceDeleteTransaction`
* `ContractFunctionResult.[gas|hbarAmount|contractFunctionParametersBytes]`
* `TransactionRecord.[hbar|token|tokenNft]AllowanceAdjustments`.
* `AccountInfo.[hbar|token|tokenNft]Allowances`.
* `AccountAllowanceExample`
* License Headers

## v2.12.0-beta.1

### Added

* `AccountAllowanceDeleteTransaction`
* `ContractFunctionResult.[gas|hbarAmount|contractFunctionParametersBytes]`
* `AccountAllowanceExample`
* License Headers

### Deprecated

* `AccountAllowanceAdjustTransaction.revokeTokenNftAllowance()` with no replacement.

## v2.11.0

### Added

* `AccountInfoFlow`
* `Client.[set|get]NodeMinReadmitPeriod()`
* Support for using any node from the entire network upon execution
  if node account IDs have no been locked for the request.
* Support for `ContractFunctionParameters` integers with different bit widths.

### Fixed

* `Transaction.fromBytes()` now verifies that transaction bodies in transaction list match.

## v2.11.0-beta.1

### Added

* `AccountInfoFlow`
* `Client.[set|get]NodeMinReadmitPeriod()`
* Support for using any node from the entire network upon execution
  if node account IDs have no been locked for the request.
* Support for `ContractFunctionParameters` integers with different bit widths.
* `CreateTopicExample`
* `GetAccountInfoExample`
* `MultiSigOfflineExample`
* `ScheduledTransactionMultiSigThresholdExample`
* `ScheduleIdenticalTransactionExample`
* `SignTransactionExample`

### Fixed

* `Transaction.fromBytes()` now verifies that transaction bodies in transaction list match.
* `ConstructClientExample`
* `CreateSimpleContractExample`
* `CreateStatefulContractExample`
* `DeleteAccountExample`

## v2.10.1

### Added

* `AccountAllowanceApproveTransaction.approve[Hbar|Token|TokenNft]Allowance()`
* `AccountAllowanceApproveTransaction.get[Hbar|Token|TokenNft]Approvals()`
* `AccountAllowanceAdjustTransaction.[grant|revoke][Hbar|Token|TokenNft]Allowance()`
* `AccountAllowanceAdjustTransaction.[grant|revoke]TokenNftAllowanceAllSerials()`
* `TransactionRecord.[hbar|token|tokenNft]AllowanceAdjustments`
* `TransferTransaction.addApproved[Hbar|Token|Nft]Transfer()`

### Deprecated

* `AccountAllowanceApproveTransaction.get[Hbar|Token|TokenNft]Allowances()`, use `get*Approvals()` instead.
* `AccountAllowanceApproveTransaction.add[Hbar|Token|TokenNft]Allowance[WithOwner]()`, use `approve*Allowance()` instead.
* `AccountAllowanceAdjustTransaction.add[Hbar|Token|TokenNft]Allowance[WithOwner]()`, use `[grant|revoke]*Allowance()` instead.
* `TransferTransaction.set[Hbar|Token|Nft]TransferApproval()`, use `addApproved*Transfer()` instead.

## v2.10.0

### Added

* `ContractCreateFlow` to simplify contract creation.
* `PrivateKey.isED25519()`
* `PrivateKey.isECDSA()`
* `PrivateKeyED25519.isED25519()`
* `PrivateKeyED25519.isECDSA()`
* `PrivateKeyECDSA.isED25519()`
* `PrivateKeyECDSA.isECDSA()`
* `PublicKey.isED25519()`
* `PublicKey.isECDSA()`
* `PublicKeyED25519.isED25519()`
* `PublicKeyED25519.isECDSA()`
* `PublicKeyECDSA.isED25519()`
* `PublicKeyECDSA.isECDSA()`

## Fixed

* Regenerated AccountIDTest.snap
* `AddressBookQuery`
* Checksums.  As a consequence, all previously generated checksums for `testnet` or `previewnet` will now be
  regarded as incorrect.  Please generate new checksums for testnet and previewnet where necessary.

### Deprecated

* `AccountUpdateTransaction.[set|get]AliasKey()` with no replacement.
* `AccountAllowance[Adjust|Approve]Transaction.add*AllowanceWithOwner()`

### Fixed

* Checksums.  As a consequence, all previously generated checksums for `testnet` or `previewnet` will now be
  regarded as incorrect.  Please generate new checksums for testnet and previewnet where necessary.

### Deprecated

* `AccountUpdateTransaction.[set|get]AliasKey()` with no replacement.

## v2.10.0-beta.1

### Added

* `ContractCreateFlow` to simplify contract creation.
* `PrivateKey.isED25519()`
* `PrivateKey.isECDSA()`
* `PrivateKeyED25519.isED25519()`
* `PrivateKeyED25519.isECDSA()`
* `PrivateKeyECDSA.isED25519()`
* `PrivateKeyECDSA.isECDSA()`
* `PublicKey.isED25519()`
* `PublicKey.isECDSA()`
* `PublicKeyED25519.isED25519()`
* `PublicKeyED25519.isECDSA()`
* `PublicKeyECDSA.isED25519()`
* `PublicKeyECDSA.isECDSA()`

## Fixed

* Regenerated AccountIDTest.snap
* `AccountAllowance[Adjust|Approve]Transaction.add*AllowanceWithOwner()`
* `AddressBookQuery`

### Deprecated

* `AccountUpdateTransaction.[set|get]AliasKey()` with no replacement.

## v2.9.0

### Added

* `owner` field to `*Allowance` classes.
* `Executable.[set|get]GrpcDeadline()`

### Fixed

* `AccountAllowanceAdjustTransaction` now deserializes correctly with `Transaction.fromBytes()`

## v2.9.0-beta.1

### Added

* `owner` field to `*Allowance` classes.
* `Executable.[set|get]GrpcDeadline()`

### Fixed

* `AccountAllowanceAdjustTransaction` now deserializes correctly with `Transaction.fromBytes()`

## v2.8.0

### Added

* CREATE2 Solidity addresses can now be represented by a `ContractId` with `evmAddress` set.
* `ContractId.fromEvmAddress()`
* `ContractFunctionResult.stateChanges`
* `ContractFunctionResult.evmAddress`
* `ContractStateChange`
* `StorageChange`
* New response codes.
* `ChunkedTransaction.[set|get]ChunkSize()`, and changed default chunk size for `FileAppendTransaction` to 2048.

### Fixed

* `TransactionId.setRegenerateTransactionId()`
* `Transaction.execute(client, timeout)`

### Deprecated

* `ContractId.fromSolidityAddress()`, use `ContractId.fromEvmAddress()` instead.

## v2.8.0-beta.1

### Added

* CREATE2 Solidity addresses can now be represented by a `ContractId` with `evmAddress` set.
* `ContractId.fromEvmAddress()`
* `ContractFunctionResult.stateChanges`
* `ContractFunctionResult.evmAddress`
* `ContractStateChange`
* `StorageChange`
* New response codes.
* `ChunkedTransaction.[set|get]ChunkSize()`, and changed default chunk size for `FileAppendTransaction` to 2048.
* `AccountAllowance[Adjust|Approve]Transaction`
* `AccountInfo.[hbar|token|tokenNft]Allowances`
* `[Hbar|Token|TokenNft]Allowance`
* `[Hbar|Token|TokenNft]Allowance`
* `TransferTransaction.set[Hbar|Token|TokenNft]TransferApproval()`

### Fixed

* `TransactionId.setRegenerateTransactionId()`
* `Transaction.execute(client, timeout)`

### Deprecated

* `ContractId.fromSolidityAddress()`, use `ContractId.fromEvmAddress()` instead.

## v2.7.0

### Added

* Support for regenerating transaction IDs on demand if a request
  responsed with `TRANSACITON_EXPIRED`

## v2.7.0-beta.1

### Added

* Support for regenerating transaction IDs on demand if a request
  responds with `TRANSACTION_EXPIRED`

## v2.6.0

### Added

* `LedgerId`
* `Client.[set|get]LedgerId()`
* `TransferTransaction.addTokenTransferWithDecimals()`, `TransferTransaction.getTokenIdDecimals()`.
* `ledgerId` fields in `AccountInfo`, `ContractInfo`, `FileInfo`, `ScheduleInfo`, `TokenInfo`, `TokenNftInfo`, and `TopicInfo`
* `UNEXPECTED_TOKEN_DECIMALS` response code.
* `PublicKey.verifyTransaction()` should use the correct protobuf field per key type
* `AccountId.aliasKey`, including `AccountId.[to|from]String()` support.
* `[PublicKey|PrivateKey].toAccountId()`.
* `aliasKey` fields in `TransactionRecord` and `AccountInfo`.
* `nonce` field in `TransactionId`, including `TransactionId.[set|get]Nonce()`
* `children` fields in `TransactionRecord` and `TransactionReceipt`
* `duplicates` field in `TransactionReceipt`
* `[TransactionReceiptQuery|TransactionRecordQuery].[set|get]IncludeChildren()`
* `TransactionReceiptQuery.[set|get]IncludeDuplicates()`
* New response codes.
* Support for ECDSA SecP256K1 keys.
* `PrivateKey.generate[ED25519|ECDSA]()`
* `[Private|Public]Key.from[Bytes|String][DER|ED25519|ECDSA]()`
* `[Private|Public]Key.to[Bytes|String][Raw|DER]()`
* `DelegateContractId` to easily distingish between having a `ContractId` and `DelegateContractId` for a key

### Deprecated

* `NetworkName`, `Client.[set|get]NetworkName()`, user `LedgerId` and `Client.[set|get]LedgerId()` instead.
* `PrivateKey.generate()`, use `PrivateKey.generate[ED25519|ECDSA]()` instead.

## v2.6.0-beta.3

### Added

* `LedgerId`
* `Client.[set|get]LedgerId()`
* `TransferTransaction.addTokenTransferWithDecimals()`, `TransferTransaction.getTokenIdDecimals()`.
* `ledgerId` fields in `AccountInfo`, `ContractInfo`, `FileInfo`, `ScheduleInfo`, `TokenInfo`, `TokenNftInfo`, and `TopicInfo`
* `UNEXPECTED_TOKEN_DECIMALS` response code.

### Deprecated

* `NetworkName`, `Client.[set|get]NetworkName()`, user `LedgerId` and `Client.[set|get]LedgerId()` instead.

## v2.6.0-beta.2

### Fixed

* `PublicKey.verifyTransaction()` should use the correct protobuf field per key type

## v2.6.0-beta.1

### Added

* `AccountId.aliasKey`, including `AccountId.[to|from]String()` support.
* `[PublicKey|PrivateKey].toAccountId()`.
* `aliasKey` fields in `TransactionRecord` and `AccountInfo`.
* `nonce` field in `TransactionId`, including `TransactionId.[set|get]Nonce()`
* `children` fields in `TransactionRecord` and `TransactionReceipt`
* `duplicates` field in `TransactionReceipt`
* `[TransactionReceiptQuery|TransactionRecordQuery].[set|get]IncludeChildren()`
* `TransactionReceiptQuery.[set|get]IncludeDuplicates()`
* New response codes.
* Support for ECDSA SecP256K1 keys.
* `PrivateKey.generate[ED25519|ECDSA]()`
* `[Private|Public]Key.from[Bytes|String][DER|ED25519|ECDSA]()`
* `[Private|Public]Key.to[Bytes|String][Raw|DER]()`

### Deprecated

* `PrivateKey.generate()`, use `PrivateKey.generate[ED25519|ECDSA]()` instead.

## v2.5.0

### Added

* Support for adding multiple addresses for the same node to the network.
* `*Id` objects are now comparable.
* Adds `createdContractIds` to `ContractFunctionResult`
* Makes `AccountBalance.[to|from]Bytes()` public.
* New smart contract response codes

## v2.5.0-beta.1

### Added

* New smart contract response codes

## v2.4.0

### Fixed

* Implement gRPC connecting timeouts to prevent `TRANSACTION_EXPIRED` from occurring due to
  nodes not responding
* `ManagedNodeAddress` will no longer used named regex groups

### Deprecated

* Deprecated `ContractCallQuery.[set|get]MaxResultSize()` with no replacement.
* Deprecated `ContractUpdateTransaction.[set|get]BytecodeFileId()` with no replacement.

## v2.4.0-beta.1

### Deprecated

* Deprecated `ContractCallQuery.[set|get]MaxResultSize()` with no replacement.
* Deprecated `ContractUpdateTransaction.[set|get]BytecodeFileId()` with no replacement.

## v2.3.0

### Added

* Support for toggling TLS for both mirror network and services network

## v2.2.0

### Added

* `FreezeType`
* `FreezeTransaction.[get|set]FreezeType()`
* `FreezeTransaction.[get|set]FileId()`
* `FreezeTransaction.[get|set]FileHash()`

### Deprecated

* `FreezeTransaction.[get|set]UpdateFileId()`, use `.[get|set]FileId()` instead.
* `FreezeTransaction.[get|set]UpdateFileHash()`, use `.[get|set]FileHash()` instead.

## v2.2.0-beta.2

### Fixed

* Make `TokenPauseTransaction` and `TokenUnpauseTransaction` constructors public

## v2.2.0-beta.1

### Added

* `TokenPauseTransaction`
* `TokenUnpauseTransaction`
* `TokenPauseStatus`
* `pauseKey` field in `TokenUpdateTransaction` and `TokenCreateTransaction`
* `pauseKey` and `pauseStatus` fields in `TokenInfo` (`TokenInfoQuery`)

### Fixed

* Added keep alive timeout of 10 seconds to all gRPC connections

### Added

* `Client.setTransportSecurity()` - Enable/Disable TLS for any node

### Changed

* Updated `*.[execute|getReceipt|getRecord]()` methods to not use the asynchronous version underneath

### Fixed

* `Transaction[Receipt|Record]Query` will no longer error when `TransactionReceipt.status` is not `SUCCESS`. Only `*.get[Receipt|Record]()` should error when `TransactionReceipt.status` is not `SUCCESS`.

## v2.1.0

### Added

* `NftId.[to|from]string()` now uses format `1.2.3/4` instead of `1.2.3@4`
* `TokenNftInfoQuery.setNftId()`
* Support for automatic token associations
  * `TransactionRecord.automaticTokenAssociations`
  * `AccountInfo.maxAutomaticTokenAssociations`
  * `AccountCreateTransaction.maxAutomaticTokenAssociations`
  * `AccountUpdateTransaction.maxAutomaticTokenAssociations`
  * `TokenRelationship.automaticAssociation`
  * `TokenAssociation`
* `networkName` as a supported config file options

## v2.1.0-beta.1

### Added

* `NftId.[to|from]string()` now uses format `1.2.3/4` instead of `1.2.3@4`
* `TokenNftInfoQuery.setNftId()`
* Support for automatic token associations
  * `TransactionRecord.automaticTokenAssociations`
  * `AccountInfo.maxAutomaticTokenAssociations`
  * `AccountCreateTransaction.maxAutomaticTokenAssociations`
  * `AccountUpdateTransaction.maxAutomaticTokenAssociations`
  * `TokenRelationship.automaticAssociation`
  * `TokenAssociation`

## v2.0.14

### Deprecated

* `TokenNftInfoQuery.byNftId()` - Use `TokenNftInfoQuery.setNftId()` instead
* `TokenNftInfoQuery.byAccountId()` with no replacement
* `TokenNftInfoQuery.byTokenId()` with no replacement
* `TokenNftInfoQuery.[set|get]Start()` with no replacement
* `TokenNftInfoQuery.[set|get]End()` with no replacement
* `Client.networkName` can now be specified via config file

### v2.0.13

### Added

* `Account[Create|Update]Transaction.[get|set]MaxAutomaticTokenAssociations`
* `TokenAssociation` and `TransactionRecord.automaticTokenAssociations`
* `AccountInfo.maxAutomaticTokenAssociations`
* `TokenRelationship.automaticAssociation`
* `TokenNftInfoQuery.setNftId()`
* New status codes

### Deprecated

* `TokenNftInfoQuery.[by|get]AccountId()` with no replacement
* `TokenNftInfoQuery.[by|get]TokenId()` with no replacement
* `TokenNftInfoQuery.[set|get]Start()` with no replacement
* `TokenNftInfoQuery.[set|get]End()` with no replacement
* `TokenNftInfoQuery.byNftId()` use `.setNftId()` instead

### Fixed

* TLS connector failing when the networks address book did not have cert hashes

## v2.0.12

### Added

* Support for TLS connections with Hedera Services nodes when network addresses end in `50212` or `443`
* Added `FeeAssessmentMethod`.
* Added `[get|set]AssessmentMethod()` to `CustomFractionalFee`
* Added `CustomRoyaltyFee`
* Added `payerAccountIdList` to `AssessedCustomFee`
* Added fields to `FreezeTransaction`
* Added `[min|max]Backoff` to `Client` and `Executable`

### Fixed

* Bugs in [to|from]Bytes() in `TopicUpdateTransaction` and `TokenUpdateTransaction`

### Deprecated

* Deprecated `Client.setMax[TransactionFee|QueryPayment]()`, added `Client.setDefaultMax[TransactionFee|QueryPayment]()` and `Client.getDefaultMax[TransactionFee|QueryPayment]()`

## v2.0.11

### Added

* `ChunkedTransaction.getAllSignatures()`

### Fixed

* `Transaction.getSignatures()` incorrectly building signature list
* `TopicMessageQuery` pending messages being discarded on retry
* `ChunkedTransaction.getAllTransactionHashesPerNode()` incorrectly building signature map
* `ScheduleInfo.getScheduledTransaction()` still not setting max fee appropriately

### Changed

* `*.setSerials()` will now clone list passed in to prevent changes

## v2.0.10

### Added

* `Client.getRequestTimeout()`
* `Client.pingAsync()` and `Client.pingAllAsync()` useful for validating all nodes within the
  network before executing any real request
* `Client.[set|get]MaxAttempts()` default max attempts for all transactions
* `Client.[set|get]MaxNodeAttempts()` set max attempts to retry a node which returns bad gRPC status
  such as `UNAVAILBLE`
* `Client.[set|get]NodeWaitTime()` change the default delay before attempting a node again which has
  returned a bad gRPC status
* `Client.setAutoValidateChecksums()` set whether checksums on ids will be automatically validated upon attempting to execute a transaction or query.  Disabled by default.  Check status with `Client.isAutoValidateChecksumsEnabled()`
* `*Id.toString()` no longer stringifies with checksums.  Use `*Id.getChecksum()` to get the checksum that was parsed, or use `*Id.toStringWithChecksum(client)` to stringify with the correct checksum for that ID on the client's network.
* `*Id.validateChecksum()` to validate a checksum.  Throws new `BadEntityIdException`
* `Client.[set|get]NetworkName()` declare which network this client is connected to, for purposes of checksum validation.
* `CustomFixedFee.[set|get]HbarAmount()` makes this fixed fee an Hbar fee of the specified amount
* `CustomFixedFee.setDenominatingTokenToSameToken()` this fixed fee will be charged in the same token.

### Deprecated

* `*Id.validate()` use `*Id.validateChecksum()` instead

### Fixed

* `ScheduleInfo.getTransaction()` incorrectly setting max transaction fee to 2 Hbars

## v2.0.9

### Fixed

* `PrivateKey.legacyDerive()` should behave the same as other SDKs

### Removed

* `*.addCustomFee()` use `*.setCustomFees()` instead

## v2.0.9-beta.2

### Fixed

* `TokenUpdateTransaction.clearAutoRenewAccountId()`
* Scheduled `TransferTransaction`

## v2.0.9-beta.1

### Added

* Support for NFTS
  * Creating NFT tokens
  * Minting NFTs
  * Burning NFTs
  * Transfering NFTs
  * Wiping NFTs
  * Query NFT information
* Support for Custom Fees on tokens:
  * Setting custom fees on a token
  * Updating custom fees on an existing token

## v2.0.8

### Added

* Sign on demand functionality which should improve performance slightly

### Fixed

* `AccountBalance.tokenDecimals` incorrectly using `Long` as the key in the map instead of
  `TokenId`. Since this was a major bug making `tokenDecimals` completely unusable, the change
  has been made directly on `tokenDecimals` instead of deprecating and adding another field.

## v2.0.7

### Added

* Support for entity ID checksums which are validated whenever a request begins execution.
  This includes the IDs within the request, the account ID within the transaction ID, and
  query responses will contain entity IDs with a checksum for the network the query was executed on.
* Node validation before execution
* Null checks for most parameters to catch stray `NullPointerException`'s

### Fixed

* `RequestType` missing `UNCHECKED_SUBMIT` for `toString()` and `valueOf()` methods.
* `FeeSchedules` incorrectly serializing nulls causing `NullPointerException`

## v2.0.6

### Added

- Add `FeeSchedule` type to allow a structured parse of file `0.0.111`

- Support for setting `maxBackoff`, `maxAttempts`, `retryHandler`, and `completionHandler` in `TopicMessageQuery`

- Default logging behavior to `TopicMessageQuery` if an error handler or completion handler was not set

- (Internal) CI is run significantly more often, and against previewnet and the master branch of hedera-services.

- Expose `tokenDecimals` from `AccountBalance`

### Fixed

- `TopicMessageQuery` retry handling; this should retry on more gRPC errors

- `TopicMessageQuery` max retry timeout; before this would could wait up to 4m with no feedback

- `Client` should be more thread safe

## v2.0.5

### Added

- Support `memo` for Tokens, Accounts, and Files.

### Fixed

- Scheduled transaction support: `ScheduleCreateTransaction`, `ScheduleDeleteTransaction`, and `ScheduleSignTransaction`
- HMAC Calculation Does Not Include IV [NCC-E001154-010]
- Non-Constant Time Lookup of Mnemonic Words [NCC-E001154-009]
- Decreased `CHUNK_SIZE` 4096->1024 and increased default max chunks 10->20
- Remove use of `computeIfAbsent` and `putIfAbsent` from JDK7 builds

### Deprecated

- `new TransactionId(AccountId, Instant)` - Use `TransactionId.withValidStart()` instead.

## v2.0.5-beta.9

### Fixed

- `TransferTransaction.addTokenTransfer()` was correctly adding tokens
- HMAC Calculation Does Not Include IV [NCC-E001154-010]
- Non-Constant Time Lookup of Mnemonic Words [NCC-E001154-009]
- Decreased `CHUNK_SIZE` 4096->1024 and increased default max chunks 10->20
- Renamed `ScheduleInfo.getTransaction()` -> `ScheduleInfo.getScheduledTransaction()`

## v2.0.5-beta.8

### Fixed

- Remove use of `computeIfAbsent` and `putIfAbsent` from JDK7 builds

## v2.0.5-beta.7

### Fixed

- Scheduled transactions should use new HAPI protobufs
- `ReceiptPrecheckException` should be thrown when the erroring status was in the `TransactionReceipt`
- Removed `nonce` from `TransactionId`
- `Transaction[Receipt|Record]Query` should not error for status `IDENTICAL_SCHEDULE_ALREADY_CREATED`
  because the other fields on the receipt are present with that status.
- `ScheduleMultiSigExample` should use updated scheduled transaction API

### Removed

- `ScheduleCreateTransaction.addScheduledSignature()`
- `ScheduleCreateTransaction.getScheduledSignatures()`
- `ScheduleSignTransaction.addScheduledSignature()`
- `ScheduleSignTransaction.getScheduledSignatures()`

## v2.0.5-beta.6

### Added

- Support for old `proto.Transaction` raw bytes in `Transaction.fromBytes()`

## v2.0.5-beta.5

### Added

- `TransactionRecord.scheduleRef` - Reference to the scheduled transaction
- `TransactionReceipt.scheduledTransactionId`
- `ScheduleInfo.scheduledTransactionId`
- Feature to copy `TransactionId` of a transaction being scheduled
  to the parent `ScheduleCreateTransaction` if one is set.

### Fixed

- `TransactionId.toBytes()` should support `nonce` if set
- `TransactionId.fromBytes()` should support `nonce` if set

## v2.0.5-beta.4

### Added

- Support `memo` for Tokens, Accounts, and Files.
- `TransactionId.fromString()` should support nonce and scheduled.

## v2.0.5-beta.3

### Changed

- `TransactionId.toString()` will append `?scheduled` for scheduled transaction IDs, and
  transaction IDs created from nonce will print in hex.

### Added

- Support for scheduled and nonce in `TransactionId`
  - `TransactionId.withNonce()` - Supports creating transaction ID with random bytes.
  - `TransactionId.[set|get]Scheduled()` - Supports scheduled transaction IDs.
- `TransactionId.withValidStart()`

### Fixed

- `ScheduleCreateTransaction.setTransaction()` and `Transaction.schedule()` not correctly setting
  existing signatures.

### Deprecated

- `new TransactionId(AccountId, Instant)` - Use `TransactionId.withValidStart()` instead.

## v2.0.5-beta.2

### Fixed

- `Schedule[Create|Sign]Transaction.addScheduleSignature()` didn't save added signatures correctly.

## v2.0.5-beta.1

### Added

- Support for scheduled transactions.
  - `ScheduleCreateTransaction` - Create a new scheduled transaction
  - `ScheduleSignTransaction` - Sign an existing scheduled transaction on the network
  - `ScheduleDeleteTransaction` - Delete a scheduled transaction
  - `ScheduleInfoQuery` - Query the info including `bodyBytes` of a scheduled transaction
  - `ScheduleId`

## v2.0.2

### Changes

- Implement `Client.forName()` to support construction of client from network name.
- Implement `PrivateKey.verifyTransaction()` to allow a user to verify a transaction was signed with a partiular key.
- Rename `HederaPreCheckStatusException` to `PrecheckStatusException` and deprecate `HederaPreCheckStatusException`
- Rename `HederaReceipStatusException` to `ReceipStatusException` and deprecate `HederaReceipStatusException`

## v2.0.1

### Bug Fixes

#### `TokenCreateTransaction`

- `long getAutoRenewPeriod()` -> `Duration getAutoRenewPeriod()`
- `setAutoRenewPeriod(long)` -> `setAutoRenewPeriod(Duration)`
- `long getExpirationTime()` -> `Instant getExpirationTime()`
- `setExpirationTime(long)` -> `setExpirationTime(Instant)`

#### `TokenUpdateTransaction`

- `long getAutoRenewPeriod()` -> `Duration getAutoRenewPeriod()`
- `setAutoRenewPeriod(long)` -> `setAutoRenewPeriod(Duration)`
- `long getExpirationTime()` -> `Instant getExpirationTime()`
- `setExpirationTime(long)` -> `setExpirationTime(Instant)`

#### `TokenInfo`

- `AccountId treasury()` -> `AccountId treasuryAccountId()`
- `long autoRenewPeriod()` -> `Duration autoRenewPeriod()`
- `long expirationTime()` -> `Instant expirationTime()`

## v2.0.0

### General changes

- No longer support the use of `long` for `Hbar` parameters. Meaning you can no longer do
  `AccountCreateTransaction().setInitialBalance(5)` and instead **must**
  `AccountCreateTransaction().setInitialBalance(new Hbar(5))`. This of course applies to more than just
  `setInitialBalance()`.
- Any method that used to require a `PublicKey` will now require `Key`.
  - `AccountCreateTransaction.setKey(PublicKey)` is now `AccountCreateTransaction.setKey(Key)` as an example.
- All `Id` types (`Account`, `File`, `Contract`, `Topic`, and `TransactionId`)
  - Support `fromBytes()` and `toBytes()`
  - No longer have the `toProto()` method.
- The use of `Duration` in the SDK will be either `java.time.Duration` or `org.threeten.bp.Duration` depending
  on which JDK and platform you're developing on.
- The use of `Instant` in the SDK will be either `java.time.Instant` or `org.threeten.bp.Instant` depending
  on which JDK and platform you're developing on.
- All transactions and queries will now attempt to execute on more than one node.
- More `getCostAsync` and `executeAsync` variants
  - `void executeAsync(Client)`
  - `Future executeAsync(Client, BiConsumer<O, T>)`
  - `void executeAsync(Client, Duration timeout, BiConsumer<O, T>)`
  - `void getCostAsync(Client)`
  - `Future getCostAsync(Client, BiConsumer<O, T>)`
  - `void getCostAsync(Client, Duration timeout, BiConsumer<O, T>)`
- Building different types from a protobuf type is no longer supported. Use `fromBytes` instead.
- `getSignatureCase()` is no longer accessible
- Field which were `byte[]` are now `ByteString` to prevent extra copy operation. This includes
  the response type of `FileContentsQuery`

### Renamed Classes

- `ConsensusSubmitMessageTransaction` -> `MessageSubmitTransaction`
- `ConsensusTopicCreateTransaction` -> `TopicCreateTransaction`
- `ConsensusTopicDeleteTransaction` -> `TopicDeleteTransaction`
- `ConsensusTopicUpdateTransaction` -> `TopicUpdateTransaction`
- `ConsensusTopicId` -> `TopicId`
- `Ed25519PublicKey` -> `PublicKey`
- `Ed25519PrivateKey` -> `PrivateKey`

### Removed Classes

- `HederaNetworkException`
- `MnemonicValidationResult`
- `HederaConstants`
- `ThresholdKey` use `KeyList.withThreshold()` instead.

### New Classes

- LiveHash: Support for Hedera LiveHashes
- Key: A common base for the signing authority or key entities in Hedera may have.

### Client

#### Renamed

- `Client()` -> `Client.forNetwork()`
- `Client.fromFile()` -> `Client.fromJsonFile()`
- `Client.replaceNodes()` -> `Client.setNetwork()`

### PrivateKey

#### Changes

- `sign()` no longer requires offset or length parameters

#### Removed

- `writePem()`

### PublicKey

#### Added

- `verify()` verifies the message was signed by public key

### Hbar

#### Renamed

- `as()` -> `to()`
- `asTinybar()` -> `toTinybars()`
- `fromTinybar()` -> `fromTinybars()`

#### Added

- `negated()`
- `getValue()`

#### Removed

- `Hbar(BigDecimal, HbarUnit)`
- `Hbar.from(long)`
- `Hbar.from(BigDecimal)`
- `Hbar.of()`

### KeyList

#### Added

- `KeyList.withThreshold()`
- `size()`
- `isEmpty()`
- `contains()`
- `containsAll()`
- `iterator()`
- `toArray()`
- `remove()`
- `removeAll()`
- `retainAll()`
- `clear()`
- `toString()`

### Mnemonic

#### Renamed

- `Mnemonic(List<? extends CharSequence>)` -> `Mnemonic.fromWords() throws BadMnemonicException`

### ContractId

#### Added

- `ContractId(long)`

#### Removed

- `toKeyProto()`
- `implements PublicKey` meaning it can no longer be used in place of a Key

### FileId

#### Added

- `FileId(long)`

#### Removed

- `fromSolidityAddress()`
- `toSolidityAddress()`

### TransactionId

#### Added

- `TransactionId.withValidStart()`
- `TransactionId.generate()`

### Transaction

#### Added

- `Transaction.hash()`
- `Transaction.signWithOperator()`

#### Removed

- `getReceipt()`
- `getRecord()`

### QueryBuilder

#### Removed

- `toProto()`
- `setPaymentTransaction()`

### TransactionBuilder

### AccountInfo

#### Added

- `List<LiveHash> liveHashes`

### AccountDeleteTransaction

#### Renamed

- `setDeleteAccountId()` -> `setAccountId()`
- Removed `addKey()`, use `setKeys(Key...)` instead.

### FileUpdateTransaction

- Removed `addKey()`, use `setKeys(Key...)` instead.

## v1.1.4

### Added

- Support for loading Ed25519 keys from encrypted PEM files (generated from OpenSSL).

- Add a method to validate a mnemonic word list for accurate entry.

### Fixed

- Fixed `TransactionReceiptQuery` not waiting for consensus some times.

## v1.1.3

### Added

- Add additional error classes to allow more introspection on errors:
  - `HederaPrecheckStatusException` - Thrown when the transaction fails at the node (the precheck)
  - `HederaReceiptStatusException` - Thrown when the receipt is checked and has a failing status. The error object contains the full receipt.
  - `HederaRecordStatusException` - Thrown when the record is checked and it has a failing status. The error object contains the full record.

### Fixed

- Add missing `setTransferAccountId` and `setTransferContractId` methods to
  `ContractDeleteTransaction`

- Override `executeAsync` to sign by the operator (if not already)

### Deprecated

- Deprecate `toSolidityAddress` and `fromSolidityAddress` on `FileId`

## v1.1.2

### Fixed

- https://github.com/hashgraph/hedera-sdk-java/issues/350

## v1.1.1

### Fixed

- https://github.com/hashgraph/hedera-sdk-java/issues/342

## v1.1.0

### Added

Add support for Hedera Consensus Service (HCS).

- Add `ConsensusTopicCreateTransaction`, `ConsensusTopicUpdateTransaction`, `ConsensusTopicDeleteTransaction`, and `ConsensusMessageSubmitTransaction` transactions

- Add `ConsensusTopicInfoQuery` query (returns `ConsensusTopicInfo`)

- Add `MirrorClient` and `MirrorConsensusTopicQuery` which can be used to listen for HCS messages from a mirror node

## v1.0.0

Removed all deprecated APIs from v1.0.0.

### Changed

- Instead of returning `ResponseCodeEnum` from the generated protos, return a new `Status` type
  that wraps that and provides some Java conveniences.

- Rename `HederaException` to `HederaStatusException`

- Rename `QueryBuilder.MaxPaymentExceededException` to `MaxQueryPaymentExceededException`

- Change `AccountBalanceQuery` to return `Hbar` (instead of `Long`)

- Change `ContractGetBytecodeQuery` to return `byte[]` (instead of the internal proto type)

- Remove `GetBySolidityIdQuery`. Instead, you should use `AccountId.toSolidityAddress`.

- Change `ContractRecordsQuery` to return `TransactionRecord[]`

## v0.9.0

### Changed

All changes are not immediately breaking as the previous method still should exist and be working. The previous methods are flagged as deprecated and will be removed upon `v1.0`.

- Transactions and queries do not take `Client` in the constructor; instead, `Client` is passed to `execute`.

- Removed `Transaction.executeForReceipt` and `Transaction.executeForRecord`

  These methods have been identified as harmful as they hide too much. If one fails, you do not know if the transaction failed to execute; or, the receipt/record could not be retrieved. In a mission-critical application, that is, of course, an important distinction.

  Now there is only `Transaction.execute` which returns a `TransactionId`. If you don't care about waiting for consensus or retrieving a receipt/record in your application, you're done. Otherwise you can now use any `TransactionId` and ask for the receipt/record (with a stepped retry interval until consensus) with `TransactionId.getReceipt` and `TransactionId.getRecord`.

  v0.8.x and below

  ```java
  AccountId newAccountId = new AccountCreateTransaction(hederaClient)
      .setKey(newKey.getPublicKey())
      .setInitialBalance(1000)
      .executeForReceipt() // TransactionReceipt
      .getAccountId();
  ```

  v0.9.x

  ```java
  AccountId newAccountId = new AccountCreateTransaction()
      .setKey(newKey.getPublicKey())
      .setInitialBalance(1000)
      .execute(hederaClient) // TranactionId
      .getReceipt(hederaClient) // TransactionReceipt
      .getAccountId();
  ```
- `TransactionReceipt`, `AccountInfo`, `TransactionRecord`, etc. now expose public final fields instead of getters (where possible and it makes sense).
- Rename `getCallResult` and `getCreateResult` to `getContractExecuteResult` and `getContractCreateResult` for consistency
- `TransactionBuilder.setMemo` is renamed to `TransactionBuilder.setTransactionMemo` to avoid confusion
  as there are 2 other kinds of memos on transactions
- `CallParams` is removed in favor of `ContractFunctionParams` and closely mirrors type names from solidity
  - `addInt32`
  - `addInt256Array`
  - `addString`
  - etc.
- `ContractFunctionResult` now closely mirrors the solidity type names
  - `getInt32`
  - etc.
- `setFunctionParams(params)` on `ContractCallQuery` and `ContractExecuteTransaction` is now
  `setFunction(name, params)`

### Added

- `TransactionId.getReceipt`

- `TransactionId.getRecord`

- `FileId.ADDRESS_BOOK`, `FileId.FEE_SCHEDULE`, `FileId.EXCHANGE_RATES`

- Experimental support for the Hedera Consensus Service (HCS). HCS is not yet generally available but if you have access
  the SDK can work with the current iteration of it. Due to its experimental nature, a system property must be set before use.

  ```java
  System.setPropery("com.hedera.hashgraph.sdk.experimental", "true")
  ```
- `Client.forTestnet` makes a new client configured to talk to TestNet (use `.setOperator` to set an operater)
- `Client.forMainnet` makes a new client configured to talk to Mainnet (use `.setOperator` to set an operater)

### Fixes

- `FileCreateTransaction` sets a default expiration time; fixes `AUTORENEW_DURATION_NOT_IN_RANGE`.

- `BUSY` is now internally retried in all cases.

- The maximum query payment is now defaulted to 1 Hbar. By default, just before a query is executed we ask Hedera how much the query will cost and if it costs under the defined maximum, an exact payment is sent.

### Removed

- `Transaction` and `Query` types related to claims

## v0.8.0

Fixes compatibility with Android.

## Breaking Changes

- The `Key` interface has been renamed to `PublicKey`
- You are now required to depend on the gRPC transport dependency for your specific environment

#### Maven

```xml
<!-- SELECT ONE: -->
<!-- netty transport (for server or desktop applications) -->
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-netty-shaded</artifactId>
  <version>1.24.0</version>
</dependency>
<!-- netty transport, unshaded (if you have a matching Netty dependency already) -->
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-netty</artifactId>
  <version>1.24.0</version>
</dependency>
<!-- okhttp transport (for lighter-weight applications or Android) -->
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-okhttp</artifactId>
  <version>1.24.0</version>
</dependency>
```

#### Gradle

```groovy
// SELECT ONE:
// netty transport (for high throughput applications)
implementation 'io.grpc:grpc-netty-shaded:1.24.0'
// netty transport, unshaded (if you have a matching Netty dependency already)
implementation 'io.grpc:grpc-netty:1.24.0'
// okhttp transport (for lighter-weight applications or Android)
implementation 'io.grpc:grpc-okhttp:1.24.0'
```
