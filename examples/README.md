## Java examples

### SDK

* [Construct a client](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ConstructClientExample.java)
* [Generate a key](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/GenerateKeyExample.java)
* [Generate a key (with mnemonic)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/GenerateKeyWithMnemonicExample.java)
* [Get address book](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/GetAddressBookExample.java)
* [Get exchange rates](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/GetExchangeRatesExample.java)
* [Logger](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/LoggerFunctionalitiesExample.java)

### Transactions

* [Sign a transaction](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/SignTransactionExample.java)
* [Transaction serialization (HIP-745)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/TransactionSerializationExample.java)

### Schedule Transaction

* [Sign a scheduled transfer transaction](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ScheduleExample.java)
* [Sign a scheduled transfer transaction (with comments)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ScheduledTransferExample.java)
* [Schedule identical transaction](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ScheduleIdenticalTransactionExample.java)
* [Schedule multisig transaction](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ScheduleMultiSigTransactionExample.java)
* [Schedule multisig transaction (with threshold)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ScheduledTransactionMultiSigThresholdExample.java)

### Accounts and HBAR

* [Create an account](../examples/src/main/java/org/hiero/sdk/java/examples/CreateAccountExample.java)
* [Create an account (threshold key)](../examples/src/main/java/org/hiero/sdk/java/examples/CreateAccountThresholdKeyExample.java)
* [Create an account (with alias)](../examples/src/main/java/org/hiero/sdk/java/examples/CreateAccountWithAliasExample.java)
* [Create an account (with alias and receiver signature required)](../examples/src/main/java/org/hiero/sdk/java/examples/CreateAccountWithAliasAndReceiverSignatureRequiredExample.java)
* [Account creation ways](../examples/src/main/java/org/hiero/sdk/java/examples/AccountCreationWaysExample.java)
* [Create an account with Hts](../examples/src/main/java/org/hiero/sdk/java/examples/AccountCreateWithHtsExample.java)
* [Auto create an account with transfer transaction](../examples/src/main/java/org/hiero/sdk/java/examples/AutoCreateAccountTransferTransactionExample.java)
* [Account alias](../examples/src/main/java/org/hiero/sdk/java/examples/AccountAliasExample.java)
* [Account allowance](../examples/src/main/java/org/hiero/sdk/java/examples/AccountAllowanceExample.java)
* [Get account info](../examples/src/main/java/org/hiero/sdk/java/examples/GetAccountInfoExample.java)
* [Get account balance](../examples/src/main/java/org/hiero/sdk/java/examples/GetAccountBalanceExample.java)
* [Update an account](../examples/src/main/java/org/hiero/sdk/java/examples/UpdateAccountPublicKeyExample.java)
* [Delete an account](../examples/src/main/java/org/hiero/sdk/java/examples/DeleteAccountExample.java)
* [Staking](../examples/src/main/java/org/hiero/sdk/java/examples/StakingExample.java)
* [Staking (with update)](../examples/src/main/java/org/hiero/sdk/java/examples/StakingWithUpdateExample.java)
* [Multisig](../examples/src/main/java/org/hiero/sdk/java/examples/MultiSigOfflineExample.java)
* [Account hooks](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/AccountHooksExample.java)
* [Lambda SStore](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/LambdaSStoreExample.java)

### Consensus Service

* [Create a topic](../examples/src/main/java/org/hiero/sdk/java/examples/CreateTopicExample.java)
* [Topic management](../examples/src/main/java/org/hiero/sdk/java/examples/TopicWithAdminKeyExample.java)
* [Consensus Pub Sub](../examples/src/main/java/org/hiero/sdk/java/examples/ConsensusPubSubExample.java)
* [Consensus Pub Sub (chunked)](../examples/src/main/java/org/hiero/sdk/java/examples/ConsensusPubSubChunkedExample.java)
* [Consensus Pub Sub (with submit key)](../examples/src/main/java/org/hiero/sdk/java/examples/ConsensusPubSubWithSubmitKeyExample.java)

### Token Service

* [Transfer Hbar](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/TransferCryptoExample.java)
* [Transfer Hbar (multi app)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/MultiAppTransferExample.java)
* [Transfer tokens](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/TransferTokensExample.java)
* [Transfer using EVM address](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/TransferUsingEvmAddressExample.java)
* [Custom fees](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/CustomFeesExample.java)
* [Custom fees (exempt)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ExemptCustomFeesExample.java)
* [NFT Allowances](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/NftAddRemoveAllowancesExample.java)
* [Zero token operations](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ZeroTokenOperationsExample.java)
* [Change Or Remove Existing Keys From A Token (HIP-540)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ChangeRemoveTokenKeys.java)
* [Reject A Token (HIP-904)](../examples/src/main/java/org/hiero/sdk/java/examples/TokenRejectExample.java)

### File Service

* [Create a file](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/CreateFileExample.java)
* [File append (chunked)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/FileAppendChunkedExample.java)
* [Get file contents](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/GetFileContentsExample.java)
* [Delete a file](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/DeleteFileExample.java)

### Smart Contract Service

* [Create a contract](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/CreateSimpleContractExample.java)
* [Create a stateful contract](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/CreateStatefulContractExample.java)
* [Contract nonce (HIP-729)](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ContractNoncesExample.java)
* [Interaction with a contract](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/SolidityPrecompileExample.java)
* [Contract hooks](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ContractHooksExample.java)

### Miscellaneous

* [Checksum validation](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/ValidateChecksumExample.java)
* [Pseudorandom Number Generator](../examples/src/main/java/com/hedera/hashgraph/sdk/examples/PrngExample.java)

## Usage

### Configuration

Running the examples requires `.env` file to exist in the [`examples`](.) folder if running with Gradle:

```sh
cp .env.sample .env
```

And in the root project folder if running with Intellij IDEA:

```sh
cp .env.sample ../.env
```

The `OPERATOR_ID` and `OPERATOR_KEY` variables should be set in a `.env` file.
Optionally, you can set the `HEDERA_NETWORK` and `SDK_LOG_LEVEL` variables:
- You can set the `HEDERA_NETWORK` to `localhost`, `testnet`, `previewnet` or `mainnet`
for configuring the network. If the `HEDERA_NETWORK` is not set, it will default to `testnet`.
- You can set the `SDK_LOG_LEVEL` to `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` or `SILENT`
for configuring the logging. If the `SDK_LOG_LEVEL` is not set, it will default to `SILENT`.
Important pre-requisite to see logs: set simple logger log level to same level as the `SDK_LOG_LEVEL`,
for example via VM options: `-Dorg.slf4j.simpleLogger.log.org.hiero=trace`.

For hook-related examples (like `AddHooksToAccountExample`), you also need to set the `HOOK_CONTRACT_ID` variable:
- In our examples we use a bytecote from a specific smart contract which can be found in `/contracts/HieroHookContract.sol`.
- Note: Hook functionality may not be available on all networks. The example will demonstrate the configuration even if hooks are not supported.

Therefore, the format of the configuration file should be as follows:

```.properties
OPERATOR_ID=0.0.102...
OPERATOR_KEY=0xeae...
# Optionally set HEDERA_NETWORK and SDK_LOG_LEVEL
HEDERA_NETWORK=previewnet
SDK_LOG_LEVEL=WARN
```

### Running with Gradle

> Note that the below `./gradlew` commands should be run from the root of the project.

Template:

```sh
./gradlew -q :examples:run<NameOfExample>
```

Concrete example:

```sh
./gradlew -q :examples:runGenerateKey
```

### Running with Intellij

Simply execute the main function of the desired example.
