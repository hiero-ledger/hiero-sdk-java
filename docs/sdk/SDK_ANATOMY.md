# Hedera SDK Architecture Documentation

> Last updated: August 2025

## Table of Contents
- [Tools and Libraries](#tools-and-libraries)
- [Core Architecture](#core-architecture)
- [Network Components](#network-components)
- [Execution Framework](#execution-framework)
- [Transaction System](#transaction-system)
- [Query System](#query-system)
- [Mirror Network](#mirror-network)
- [Build System](#build-system)
- [Best Practices](#best-practices)

## Tools and Libraries

### Core Dependencies

#### Protobufs
Protocol Buffer files (`.proto`) are compiled into Java classes for serializing and deserializing data. These classes enable:
- Local data persistence
- Network communication
- Type-safe message handling

#### gRPC
Remote procedure call framework tightly integrated with Protobufs:
- Defines RPCs within services in `.proto` files
- Generates client and server code for channel communication
- Hedera extends server code for RPC implementation
- SDK extends client code for RPC invocation

#### BouncyCastle
Comprehensive cryptography library providing:
- Digital signature algorithms
- Hash functions
- Key generation and management
- Encryption/decryption utilities

#### Testing Framework
- **JUnit Jupiter**: Modern unit testing framework
- **Jacoco**: Code coverage analysis and reporting
- **Mockito**: Mocking framework for unit tests

#### Code Quality Tools
- **Error-prone**: Enhanced Java compiler with additional error detection
- **SpotBugs**: Static analysis for bug detection
- **Checkstyle**: Code style enforcement

#### Code Generation
- **JavaPoet**: Library for programmatic Java code generation
- **FunctionalExecutableProcessor**: Custom annotation processor for method variants

## Core Architecture

### LockableList<T>
Thread-safe utility class providing:
```java
public class LockableList<T> {
    // Prevents mutations when locked
    public void lock()
    
    // Circular iteration through elements
    public void advance()
    
    // Current element access
    public T getCurrent()
}
```

**Use Cases:**
- Node rotation during retries
- Transaction ID cycling
- Failover mechanism implementation

### Client
Central connection manager to the Hedera network:

```java
public class Client {
    private Network network;
    private MirrorNetwork mirrorNetwork;
    private ExecutorService executor;
    private Operator operator;
    
    // Configuration fields
    private Duration requestTimeout;
    private Hbar maxTransactionFee;
    private Hbar maxQueryPayment;
}
```

#### Operator (Inner Class)
```java
public static class Operator {
    private AccountId accountId;
    private PublicKey publicKey;
    private Function<byte[], byte[]> transactionSigner;
}
```

#### Initialization Options
1. **Predefined Networks**: `Client.forTestnet()`, `Client.forMainnet()`, `Client.forPreviewnet()`
2. **Custom Network**: Map of `<"ipAddress:port", AccountId>` pairs
3. **Configuration File**: JSON-based client configuration

## Network Components

### BaseNode
Foundation class for network nodes:

```java
public abstract class BaseNode {
    protected String address;
    protected ManagedChannel channel;
    protected ExecutorService executor;
    protected Instant lastUsed;
    protected long useCount;
    
    // Channel management
    public ManagedChannel getChannel()
    public void close()
    
    // Usage tracking
    public void inUse()
    public String getUserAgent() // Returns "hiero-sdk-java/v{VERSION}"
}
```

### BaseNetwork<KeyT, BaseNodeT, ManageNodeT>
Generic network management:

```java
public abstract class BaseNetwork<KeyT, BaseNodeT, ManageNodeT> {
    // Node mapping: Key -> List of proxy nodes
    protected Map<KeyT, List<BaseNodeT>> network;
    
    // All nodes in the network
    protected List<ManageNodeT> nodes;
    
    // Currently healthy nodes for load balancing
    protected List<ManageNodeT> healthyNodes;
    
    protected ExecutorService executor;
}
```

### Network (extends BaseNetwork<AccountId, Node, Node>)
Hedera consensus network implementation:

```java
public class Network extends BaseNetwork<AccountId, Node, Node> {
    // Returns 1/3 of healthy nodes (rounded up) for execution
    public List<AccountId> getNodeAccountIdsForExecute()
}
```

### Node (extends BaseNode)
Individual Hedera consensus node connection with health monitoring and automatic failover capabilities.

### MirrorNetwork & MirrorNode
Specialized components for Hedera Mirror Node connectivity, providing historical data and real-time streaming capabilities.

## Execution Framework

### Executable<RequestT, ResponseT, O>
Abstract base class for all network operations:

```java
public abstract class Executable<RequestT, ResponseT, O> {
    protected LockableList<AccountId> nodeAccountIds;
    protected int maxAttempts = 10;
    
    // Abstract methods for subclass implementation
    protected abstract void onExecute(Client client);
    protected abstract CompletableFuture<Void> onExecuteAsync(Client client);
    protected abstract RequestT makeRequest();
    protected abstract O mapResponse(ResponseT response);
    protected abstract Status mapResponseStatus(ResponseT response);
    protected abstract MethodDescriptor<RequestT, ResponseT> getMethodDescriptor();
}
```

#### Execution Flow
1. **Preparation**: `onExecuteAsync()` sets up node lists and parameters
2. **Request Creation**: `makeRequest()` generates protobuf message
3. **Network Call**: gRPC unary call with retry logic
4. **Response Processing**: `mapResponse()` converts to return type
5. **Error Handling**: Status checking and retry logic

#### Retry Mechanism
- Automatic node failover using `nodeAccountIds` rotation
- Exponential backoff for transient failures
- Maximum attempt limits with circuit breaker pattern

### @FunctionalExecutable Annotation
Generates multiple method variants from a single async method:

```java
// Original method
@FunctionalExecutable(type = TransactionReceipt.class)
public CompletableFuture<TransactionReceipt> executeAsync(Client client) { ... }

// Generated variants:
// - void executeAsync(Client, BiConsumer<TransactionReceipt, Throwable>)
// - void executeAsync(Client, Duration, BiConsumer<TransactionReceipt, Throwable>)
// - TransactionReceipt execute(Client)
// - TransactionReceipt execute(Client, Duration)
```

## Transaction System

### Transaction Architecture
Transactions follow a sophisticated multi-dimensional structure:

```
Transaction Matrix (Chunks × Nodes):
      Node0  Node1  Node2  Node3
Chunk0  T0     T1     T2     T3
Chunk1  T4     T5     T6     T7
Chunk2  T8     T9    T10    T11
```

### Core Data Structures
```java
public abstract class Transaction<T extends Transaction<T>> extends Executable<TransactionOuterClass.Transaction, TransactionOuterClass.TransactionResponse, TransactionReceipt> {
    // Transaction identification
    protected LockableList<TransactionId> transactionIds;
    
    // Parallel arrays for T×N matrix
    protected List<SignatureMap.Builder> sigPairLists;           // [T×N]
    protected List<SignedTransaction> innerSignedTransactions;    // [T×N]
    protected List<TransactionOuterClass.Transaction> outerTransactions; // [T×N]
    
    // Configuration
    protected Hbar transactionFee;
    protected Duration transactionValidDuration;
    protected String memo;
}
```

### Transaction Lifecycle

#### 1. Construction Phase
```java
CryptoTransferTransaction transaction = new CryptoTransferTransaction()
    .addHbarTransfer(fromAccount, Hbar.fromTinybars(-1000))
    .addHbarTransfer(toAccount, Hbar.fromTinybars(1000))
    .setTransactionMemo("Payment for services");
```

#### 2. Freezing Phase
```java
transaction.freezeWith(client);
// Transaction is now immutable and ready for signing
```

**Freezing Process:**
- Validates all required fields
- Generates `TransactionBody` protobuf messages
- Creates `innerSignedTransactions` matrix
- Locks transaction for modifications

#### 3. Signing Phase
```java
// Additional signatures beyond operator
transaction.sign(privateKey1);
transaction.sign(privateKey2);
```

**Signature Management:**
- Automatic operator signing during execution
- Support for multi-signature transactions
- Signature aggregation in `SignatureMap`

#### 4. Execution Phase
```java
TransactionResponse response = transaction.execute(client);
TransactionReceipt receipt = response.getReceipt(client);
TransactionRecord record = response.getRecord(client); // Requires fee
```

### Protobuf Message Structure

#### Current Format (Recommended)
```protobuf
message Transaction {
    bytes signedTransactionBytes = 1; // Serialized SignedTransaction
}

message SignedTransaction {
    bytes bodyBytes = 1;     // Serialized TransactionBody
    SignatureMap sigMap = 2;  // All signatures
}
```

#### Legacy Format (Deprecated)
```protobuf
message Transaction {
    bytes bodyBytes = 2;     // Direct TransactionBody bytes
    SignatureMap sigMap = 3; // Signatures
}
```

### ChunkedTransaction
Handles large data by splitting into multiple transactions:

```java
public abstract class ChunkedTransaction<T extends ChunkedTransaction<T>> extends Transaction<T> {
    @Override
    protected void onFreeze(TransactionBody.Builder bodyBuilder) {
        // Creates multiple transaction chunks
        // Each chunk becomes a row in the T×N matrix
    }
}
```

**Examples:**
- `FileAppendTransaction`: Large file uploads
- `ContractUpdateTransaction`: Large bytecode updates
- `TopicMessageSubmitTransaction`: Large messages

### Scheduled Transactions
Deferred execution with multi-party signing:

```java
// Create base transaction
CryptoTransferTransaction transfer = new CryptoTransferTransaction()
    .addHbarTransfer(account1, Hbar.fromTinybars(-1000))
    .addHbarTransfer(account2, Hbar.fromTinybars(1000));

// Convert to scheduled transaction
ScheduleCreateTransaction scheduled = transfer.schedule()
    .setScheduleMemo("Multi-sig payment")
    .setAdminKey(adminKey);

TransactionResponse response = scheduled.execute(client);
ScheduleId scheduleId = response.getReceipt(client).scheduleId;

// Additional parties can sign
new ScheduleSignTransaction()
    .setScheduleId(scheduleId)
    .execute(signerClient);
```

#### Key Methods
- `schedule()`: Converts transaction to `ScheduleCreateTransaction`
- `fromScheduledTransaction()`: Reconstructs transaction from `ScheduleInfo`
- `onScheduled()`: Abstract method for schedulable transaction body creation

## Query System

### Query Architecture
```java
public abstract class Query<RequestT, ResponseT, O> extends Executable<RequestT, ResponseT, O> {
    protected Message.Builder builder;
    protected QueryHeader.Builder headerBuilder;
    
    // Payment system
    protected TransactionId paymentTransactionId;
    protected List<Transaction> paymentTransactions; // Parallel to nodeAccountIds
    protected Hbar queryPayment;
    protected Hbar maxQueryPayment;
}
```

### Query Payment System
All queries (except free queries) require payment:

1. **Cost Estimation**: `QueryCostQuery` determines required payment
2. **Payment Generation**: Creates payment transactions for each target node
3. **Execution**: Includes payment transaction in query message

### Query Lifecycle

#### 1. Configuration
```java
AccountBalanceQuery query = new AccountBalanceQuery()
    .setAccountId(accountId)
    .setMaxQueryPayment(Hbar.fromTinybars(1000));
```

#### 2. Execution with Automatic Payment
```java
AccountBalance balance = query.execute(client);
// SDK automatically:
// - Estimates cost via QueryCostQuery
// - Generates payment transactions
// - Includes payment in query message
```

#### 3. Manual Payment Control
```java
Hbar cost = query.getCost(client);
query.setQueryPayment(cost);
AccountBalance balance = query.execute(client);
```

### Abstract Methods
```java
// Query-specific message building
protected abstract RequestT onMakeRequest(Message.Builder queryBuilder, QueryHeader queryHeader);

// Response header extraction
protected abstract ResponseHeader mapResponseHeader(ResponseT response);

// Request header extraction (for debugging)
protected abstract QueryHeader mapRequestHeader(Query.Query request);

// Checksum validation
protected abstract void validateChecksums(Client client);
```

### Common Query Types
- **Account Queries**: `AccountBalanceQuery`, `AccountInfoQuery`
- **Contract Queries**: `ContractCallQuery`, `ContractInfoQuery`
- **File Queries**: `FileContentsQuery`, `FileInfoQuery`
- **Network Queries**: `NetworkVersionInfoQuery`, `TransactionReceiptQuery`

## Mirror Network

### TopicMessageQuery
Real-time streaming from Hedera Mirror Nodes:

```java
TopicMessageQuery query = new TopicMessageQuery()
    .setTopicId(topicId)
    .setStartTime(Instant.now())
    .subscribe(client, message -> {
        System.out.println("Received: " + new String(message.contents));
    });
```

#### Key Features
- **Streaming RPC**: Long-lived connection for real-time data
- **Automatic Chunking**: Reassembles large messages automatically
- **Error Handling**: Configurable retry and error handlers
- **Message Ordering**: Guaranteed in-order delivery per topic

#### Handler Configuration
```java
query.setCompletionHandler(() -> System.out.println("Stream completed"))
     .setErrorHandler((error) -> System.err.println("Error: " + error))
     .setRetryHandler((error) -> true); // Return true to retry
```

#### Message Chunking
Large topic messages are automatically chunked by consensus nodes and reassembled by the SDK:

```java
// SDK handles this automatically
message ConsensusTopicResponse {
    ConsensusMessageChunkInfo chunkInfo = 5;
    bytes message = 6; // Actual message content
}
```

### Mirror Node Capabilities
- **Transaction History**: Complete transaction records
- **Account History**: Historical account states
- **Smart Contract Events**: Event logs and state changes
- **Topic Messages**: Real-time consensus service data

## Build System

### FunctionalExecutableProcessor
Annotation processor that generates method variants:

#### Input
```java
@FunctionalExecutable(type = TransactionReceipt.class)
public CompletableFuture<TransactionReceipt> executeAsync(Client client) {
    // Implementation
}
```

#### Generated Interface (WithExecute.java)
```java
public interface WithExecute<T> {
    CompletableFuture<TransactionReceipt> executeAsync(Client client);
    
    // Generated variants
    default void executeAsync(Client client, BiConsumer<TransactionReceipt, Throwable> callback) { ... }
    default void executeAsync(Client client, Duration timeout, BiConsumer<TransactionReceipt, Throwable> callback) { ... }
    default void executeAsync(Client client, Consumer<TransactionReceipt> onSuccess, Consumer<Throwable> onFailure) { ... }
    default TransactionReceipt execute(Client client) { ... }
    default TransactionReceipt execute(Client client, Duration timeout) { ... }
}
```

#### Implementation
```java
public class MyTransaction extends Transaction<MyTransaction> implements WithExecute<MyTransaction> {
    @Override
    public CompletableFuture<TransactionReceipt> executeAsync(Client client) {
        // Your implementation here
    }
}
```

### Build Process Integration
1. **Compilation**: Standard Java compilation
2. **Annotation Processing**: `FunctionalExecutableProcessor` generates interfaces
3. **Code Generation**: JavaPoet creates method variants
4. **Final Compilation**: Generated code compiled with main sources

## Best Practices

### Error Handling
```java
try {
    TransactionResponse response = transaction.execute(client);
    TransactionReceipt receipt = response.getReceipt(client);
    
    if (receipt.status == Status.SUCCESS) {
        // Handle success
    }
} catch (ReceiptStatusException e) {
    // Handle known Hedera errors
    System.err.println("Transaction failed: " + e.receipt.status);
} catch (PrecheckStatusException e) {
    // Handle precheck failures
    System.err.println("Precheck failed: " + e.status);
} catch (TimeoutException e) {
    // Handle network timeouts
    System.err.println("Request timed out");
}
```

### Resource Management
```java
// Always close clients to free resources
try (Client client = Client.forTestnet()) {
    client.setOperator(accountId, privateKey);
    
    // Perform operations
    
} // Client automatically closed
```

### Transaction Optimization
```java
// Batch related operations
Transaction transaction = new AccountCreateTransaction()
    .setKey(publicKey)
    .setInitialBalance(Hbar.fromTinybars(1000))
    .setMaxTransactionFee(Hbar.fromTinybars(200000))  // Set appropriate fees
    .setTransactionValidDuration(Duration.ofSeconds(120))  // Reasonable duration
    .freezeWith(client);

// Add any additional signatures before execution
transaction.sign(additionalPrivateKey);

TransactionResponse response = transaction.execute(client);
```

### Query Optimization
```java
// Set reasonable payment limits
AccountBalanceQuery query = new AccountBalanceQuery()
    .setAccountId(accountId)
    .setMaxQueryPayment(Hbar.fromTinybars(100000)); // Prevent unexpected costs

AccountBalance balance = query.execute(client);
```

### Network Configuration
```java
// Configure appropriate timeouts and limits
Client client = Client.forTestnet()
    .setOperator(accountId, privateKey)
    .setRequestTimeout(Duration.ofSeconds(30))
    .setMaxTransactionFee(Hbar.fromTinybars(200000))
    .setMaxQueryPayment(Hbar.fromTinybars(100000));
```

## Additional Components

### Status and Error Codes
The SDK provides comprehensive status handling through the `Status` enum, mapping to Hedera's `ResponseCodeEnum`:

```java
public enum Status {
    OK,
    INVALID_TRANSACTION,
    PAYER_ACCOUNT_NOT_FOUND,
    INVALID_NODE_ACCOUNT,
    // ... hundreds of status codes
}
```

### Checksum Validation
Entity IDs support checksum validation to prevent accidental cross-network operations:

```java
// Testnet account with checksum
AccountId accountId = AccountId.fromString("0.0.123-vfmkw"); // Testnet checksum
accountId.validateChecksum(client); // Validates against client's network
```

### Custom Transaction Types
The SDK supports all Hedera services:
- **Cryptocurrency Service**: Account and token operations
- **Smart Contract Service**: Contract deployment and execution
- **File Service**: File storage and retrieval
- **Consensus Service**: Topic creation and messaging
- **Token Service**: HTS token management
- **Schedule Service**: Scheduled transaction management
- **Network Service**: Network information and utilities

---

*This documentation covers the core architecture and components of the Hedera Java SDK. For specific API usage examples and detailed method documentation, please refer to the JavaDoc and official SDK examples.*