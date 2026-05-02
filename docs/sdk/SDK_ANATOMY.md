> Please note that this document may be a bit outdated.

## Tools and Libraries:

Gradle plugins are used for including libraries and tools.

### Protobufs:

Files with extension `.proto` get compiled into classes used for serializing and deserializing data, which can be used to save data locally, or send data over a channel.

### gRPC:

Used to perform remote procedure calls.  Tightly coupled to Protobufs.  Protobufs define the remote procedure calls (rpcs within services), the gRPC layer implements the client and server code for connecting clients and servers via channels. Hedera extends the gRPC server code to perform the RPC, and the SDK extends the gRPC client code to call the RPC.

### BouncyCastle:

Encryption stuff.

### Jupiter (Junit):

Unit testing framework.

### Jacoco:

Reports code coverage (how much code actually gets tested by tests).

### Error-prone:

Augments the Java compiler to output more comprehensive errors and warnings.

### JavaPoet:

Library to assist in code generation (see **FunctionalExecutableProcessor**).

## Classes:

### `LockableList`:

An internal utility class that represents a list of things, and which has these capabilities:
* It can be locked, which prevents the list from being mutated.
* It has an index which can be incremented with the `advance()` method, and the index will loop back around to 0 on reaching the end of the list.

### `Client`:

This is the wallet app's connection to the network.  It has a `Network`, a `MirrorNetwork`, an `ExecutorService`, an `Operator`, and various fields for configuring behavior (for example `requestTimeout`, `maxTransactionFee`, `maxQueryPayment`).

An `Operator` is an inner class of `Client`, and has an `AccountId`, a `PublicKey`, and a transaction signer (a function that does the signing of transactions). The `transactionSigner` defaults to `privateKey::sign`.

A `Client` can be initialized from a config file (json).  A `Client` can be initialized for previewnet, testnet, or mainnet, or a custom network, where a custom network is a list of `<"ipAddress:portNumber", AccountID>` pairs (in the form of a hashtable).  Multiple endpoints may be mapped to the same node account ID (put another way, there may be multiple proxies for the same node).  If initialized for previewnet, testnet, or mainnet, the `Client` just uses a hard-coded list of `<"ipAddress:portNumber", AccountID>` pairs.

`executor` will be used to initialize the gRPC `BaseChannel`, and in the event that an RPC fails and needs to be retried after a delay, `executor` will be used to schedule that delayed retry.

### `BaseNode`:

Has an `address`, a `channel`, an `executor` (ultimately from `Client`), `lastUsed` and `useCount`.

`BaseNode` is inherited by `Node` and `MirrorNode`.

`BaseNode` keeps track of stats about how this node has been used, and it constructs its channel on demand, which is a `grpc.BaseChannel`, and which is built as a plaintext channel, using the `executor`, and using the user agent from `getUserAgent()`.

`channel` is a `grpc.BaseChannel` instead of a normal `grpc.Channel` so that we can customize how it is set up (for example, we can give it the specified executor, and we can shut it down in the desired manner).

`BaseNode` has the methods `inUse()`, which causes the `BaseNode` to record that it is being used, `getChannel()`, `close()`, and `getUserAgent()`.

The user agent is a string that is used to identify the client to the server.  In this case, it's `"hiero-sdk-java/v{NUMBER}"`.

### `BaseNetwork`:

This represents a network of `BaseNode`s.  `Network` and `MirrorNetwork` inherit from this.

Has these critical fields:
* `network`, which is a map of `<KeyT, List<BaseNodeT>>`.  In `Network`, `KeyT` is `AccountId`, so that we can get a list of proxies for a given node account ID (each proxy is represented by a separate `BaseNodeT`).
* `nodes`, which is a list of all `ManageNodeT`s in the network.
* `healthyNodes`, a list of currently healthy nodes which are selected from while attempting to execute a transaction or query.
* `executor`, a reference to the executor which will be used to create channels for the `Node`s (in practice, this is always the `Client`'s executor).

`setNetwork()` will update this `Network` to the given list.  It will close a `Node` and remove it from this network if it is not in the given list, and then it will then add nodes from the list.

### `Network`:

This represents a network of Hedera nodes, a `Client` connects to a `Network`.

`getNodeAccountIdsForExecute()` gets a list of N randomly selected `AccountId`s where N is 1/3rd (rounded up) of healthy nodes in this `Network`.  This is used by  `Query` and `Transaction` to populate their `nodeAccountId`s, lists containing the `AccountId`s of `Node`s that the `Query` or `Transaction` will be attempted with.

### `Node`:

This is a connection to one node in the network.  Inherits from `BaseNode` (which is where much of the meat is).

### `Executable`:

An `Executable` object represents a request to the server.

`Query` and `Transaction` both extend `Executable`, and then other classes in turn extend `Query` and `Transaction`.

The code for using any `Executable` subclass object should look something like this:

```java
AccountBalance accountBalanceNew = new AccountBalanceQuery()
    .setAccountId(newAccountId)
    .execute(client);
```

`execute()` is a method of `Executable`, and how `execute()` actually behaves is determined by a handful of abstract methods that are overridden by `Executable`'s subclasses.

`nodeAccountIds` is a `LockableList<AccountId>` field of `Executable`, and it is a list of nodes to which we will attempt to submit this transaction or query.  `execute()` should attempt to submit the transaction or query to the current node in the list, and if the request fails, it should `advance()` the list and try again.

The methods that are meant to be overridden by the subclasses are:

* `onExecute()` performs any pre-execution preparation.
* `onExecuteAsync()` performs the same function as `onExecute()`, and returns an initial future to be completed before `executeAsync()`'s future.
* `makeRequest()` generates the desired request proto message for the rpc.
* `mapResponse()` turns the response from the rpc into the desired return type.
* `mapResponseStatus()` turns the response from the rpc into a `Status`.
* `getMethodDescriptor()` returns a `grpc.MethodDescriptor`, which is an object that describes the rpc to be called.  `MethodDescriptor`s are fetched from the grpc-generated `"*Service*.java"` classes.
* `getTransactionIdInternal()` is the unique ID for this transaction.

The `Executable` class implements `execute()` and `executeAsync()`, and then the `FunctionalExecutableProcessor` generates several variants of the `executeAsync()` method during the build process.  The `@FunctionalExecutable` annotation triggers this code generation.  This pattern also appears in `ChunkedTransaction`, `Transaction`, and `Query`.

`Executable` has a public `executeAsync()` method that calls `onExecuteAsync()` and then chains onto that future a call to the private `executeAsync()` method, which sets up and makes the rpc with `grpc.ClientCalls.futureUnaryCall()`, and then chains to _that_ future a handler which handles the result of the rpc.  Depending on the result of the rpc, the handler may complete the future that was returned by `executeAsync()`, _or_ it may chain onto that future _another_, recursive call to the internal `executeAsync()` method, and this is how `executeAsync()` loops through multiple attempts to execute with different nodes, up to `maxAttempts`.

Before the inner `executeAsync()` method will work, `nodeAccountIds`,  which is a member of the `Executable` object, needs to be filled.  This is implicitly done by the `onExecuteAsync()` method that is implemented by the subclasses.  On each attempt we increment the index of `NodeAccountIds` looping through all of the elements.

It should also be noted that the future returned by `grpc.ClientCalls.futureUnaryCall()` is a guava `ListenableFuture`, and in order to return the right kind of future (`CompletableFuture`), `executeAsync()` uses some black magic from the `FutureConverter` class to convert the guava `ListenableFuture` into a `CompletableFuture`.

`execute()` is simpler by virtue of not being async, but it does approximately the same thing, just without the future mumbo jumbo.

### `Query`:

The `Query` class extends `Executable`.

It has a `builder`, a `headerBuilder`, a `paymentTransactionId`, a list of `paymentTransactions` (more on this in a moment), and then `queryPayment` and `maxQueryPayment` are Hbar amounts set by the user for the query fee.

A `Query` proto message is basically a union (`oneof`) of all the different kinds of query messages that can be sent.  Every one of these query proto messages has a `QueryHeader` message nested in it.

The `Query` class implements most of the abstract methods from `Executable`, _except_ for `mapResponse()` and `getMethodDescriptor()`, which `Query` leaves to be overridden by its subclasses.

Query also adds some abstract methods of its own:
* `RequestT onMakeRequest(queryBuilder, queryHeader)`: because every type of `Query` proto message has a `QueryHeader` inside of it, this method has to place the `QueryHeader` inside of the internal `Query` message in addition to generally preparing the builder to build the request message.
* `ResponseHeader mapResponseHeader(Response)`: the same nested-header pattern is repeated here for the `Response` proto message.  This method fetches the `ResponseHeader` message from the the particular query response message.  This method seems to be used for `Query`'s implementation of `mapResponseStatus` to check the `precheckStatus`.  It doesn't look like it's used for anything else.
* `QueryHeader mapRequestHeader(proto.Query)`: this actually fetches the header from _this_ request.  I see it used for `toString()`, but nothing else.
* `void validateChecksums(client)`: checks whether checksums on entity IDs in the query are valid for the ledger that `client` is configured for (EG testnet or mainnet).

`Query` has an inner class, `QueryCostQuery` (a query for the cost of querying).  This is basically a fake query to the network, not actually intended to be successful, that is made in order to get the cost from the response header.  We then assume that the cost for our real query will be the same as the cost for our fake query.

`onExecute[async]()` seems to be where most of the action is in `Query`.  It first makes sure that `nodeAccountIds` is filled, then it fetches the `queryPayment` amount (via `QueryCostQuery`) if one hasn't been set, then it generates the payment transactions for paying the query fee. The `paymentTransactions` list is a parallel array to `nodeAccountIds`.  The `Query` proto message includes a `Transaction` proto message inside of it for paying the query fee, and `onExecuteAsync()` just goes ahead and builds a parallel array of `Transaction` messages which are to be used in the event that we attempt to send our query to that node.

### `Transaction`:

The `Transaction` class extends `Executable`.

A transaction is used like this:
- Instantiate a subclass of `Transaction`.
- Call methods to configure it.
- OPTIONAL:
- Freeze the transaction.
- Add more signatures.
- Execute the transaction (it will be frozen if not already frozen, and will be signed with client operator).
- `execute()` returns (or in the case of `executeAsync()`, returns in future) a `TransactionResponse`.
- OPTIONAL: use the resulting `TransactionResponse` to get the `TransactionReceipt` for free, or pay a fee to get the `TransactionRecord`.  Fetching either of these is itself a query.

The `Transaction` class is greatly complicated by three factors:

**A)** A `Transaction` object can correspond to one of three proto messages:
1. `TransactionList`
2. `Transaction` with `signedTransactionBytes` set
3. `Transaction` without `signedTransactionBytes` set (this form is deprecated)

**B)** `Transaction`'s relationship to `ChunkedTransaction`.

**C)** `Transaction`'s relationships to `ScheduleCreateTransaction` and `ScheduleInfo`

Before we delve in, let's discuss signatures.

The `SignatureMap` proto message is defined in `BasicTypes.proto` and has a repeated `SignaturePair` field.  A `SignaturePair` contains a public key and a signature, which combined can be used to confirm whether some data was signed by a party who holds a copy of the private key associated with that public key.

Protobuf does not guarantee that all implementations of protobuf will serialize the same message the same way, it only guarantees that any implementation will be able to parse messages that were serialized by any other implementation.  As such, for the purposes of signing or hashing a message, the message must be serialized into bytes before it can be signed or hashed, and the signature or hash verification must be performed on that same serialized bytes form of the message.

#### Let's tackle factor A:

We must first clarify that `TransactionList` is a proto message type that's only used internally by the SDK.  It is _not_ used in any Hedera network.  The `fromBytes()` and `toBytes()` methods in the SDK are not used for serializing or deserializing `Transaction`s into or from any proto messages that are sent to or from any Hedera network.

The `fromBytes()` method tries to parse the input bytes to a `TransactionList` proto message, and to a `Transaction` proto message.  The `protoc`-generated methods fail silently if the bytes do not encode the message type in question, so `fromBytes()` simply looks at the fields of the objects outputted by the parse methods to see if the bytes encoded either of those messages.

The `fromBytes()` method then stores the results to an odd type: `Map<TransactionId, Map<AccountId, proto.Transaction>>`, typically referred to as `txs` in the code ("transactions").  The `accountId` is the ID of the node that the transaction is addressed to. This type will begin to make sense as I address factor B.

Finally `fromBytes()` detects the type of `Transaction` with `dataCase()`, and it passes the `txs` to the constructor for the correct `Transaction` subclass.  The subclass's constructor will always make use of the `Transaction(txs)` constructor, which will check to make sure the bodies of all of the transaction bodies in the `TransactionList` are identical using the `requireProtoMatches()` static method.

Let's now discuss how the `Transaction` proto message is structured.  In its deprecated form, `Transaction` would have two fields, `bodyBytes` and `sigMap`. `bodyBytes` contains the serialized bytes form of a `TransactionBody` proto message which contains the actual meat of the transaction.

In its _non_-deprecated form, the Transaction message just contains a `signedTransactionBytes` field, which is the bytes form of a `SignedTransaction` proto message (defined in `TransactionContents.proto`), which contains the `bodyBytes` and the `sigMap`.

The `bodyBytes` are what need to be signed, but then on Hedera's end, they hash the `signedTransactionBytes` for some purpose.  So the `TransactionBody` needs to be serialized before it can be signed, and then the `SignedTransaction` needs to be serialized before it can be hashed.

`TransactionBody` contains a `transactionID`, a `nodeAccountID`, a `transactionFee` (which is the client's maximum tolerated fee), `transactionValidDuration` (the window of time for the network to process the transaction), a `memo`, and a `oneof` called `data` with the internal data for all the various transaction types.

The `toBytes()` method always serializes the contents of this `Transaction` as a `TransactionList`, and internally, we basically always think of a `Transaction` object as a `TransactionList`.

#### Now factor B, `Transaction`'s relationship to `ChunkedTransaction`:

I've mentioned that we internally think of a `Transaction` object as representing a `TransactionList`.  There are two reasons for this:

1. As covered in `Executable`, we make multiple attempts, trying different nodes, cycling through `NodeAccountIds`, until we've reached `maxAttempts`.  Similarly to `Query`, we need to create the `Transaction` proto messages for each `Node`, and so we keep around a `List` of `Transaction` proto messages which are basically the same transaction, but addressed to different nodes.
2. In the `ChunkedTransaction` subclass, we break one transaction into a series of transactions.  For example, to append to a file,we have to break the data we want to append to the file into multiple chunks, because one `FileAppendTransaction` can only append a small amount of data.  So in `ChunkedTransaction`, we need to keep a list of transactions that really _do_ represent _different transactions_, not just the same Transaction addressed to different nodes.

So at the end of the day, it's best to think of a `Transaction` as containing something like a 2D array of `Transaction` proto messages.  Here each row represents a transaction, and each column represents a node, and at the intersections I have put the indices of the associated `Transaction` and `SignedTransaction` proto messages in the `this.transactions` and `this.signedTransactions` lists:

```
   N0 N1 N2 N3
T0 0  1  2  3
T1 4  5  6  7
T2 8  9  10 11
T3 12 13 14 15
```

This is the best way to think about the members of the `Transaction` class.  Even if your transaction will not be chunked, internally, the `Transaction` object will be set up like a chunked transaction with only one chunk, and the methods of `Transaction` are generally written to be compatible with the behavior of a chunked transaction.  `ChunkedTransaction` overrides the `freezeWith()` method to create multiple rows in the 2D array.

There is a `transactionIds` field of type `LockableList<TransactionId>`, which operates similarly to the `nodeAccountIds` lockable list in `Executable`.  Together, the indices of `nodeAccountIds` and `transactionIds` specify the coordinate of an element in the 2D array.

Now we can discuss the various parallel arrays in a `Transaction` object and how they're related.  `T` = transaction count, `N` = node count:

- `LockableList<TransactionId> transactionIds[T]`
- `List<proto.SignatureMap.Builder> sigPairLists[T*N]`
- `List<proto.SignedTransaction> innerSignedTransactions[T*N]`
- `List<proto.Transaction> outerTransactions[T*N]`

A `TransactionId` is used to uniquely identify each transaction, so that when the same transaction is submitted to multiple nodes, only one transaction with the same ID will be permitted.  It consists of the `AccountId` of the account who originated the transaction, and the timestamp.

Remember that the name of the proto message that's ultimately sent is `Transaction`, and that `signedTransactionBytes` is a field in the `Transaction` proto message.  So `innerSignedTransactions` gets populated first, and then `outerTransactions` gets populated with signed transactions.

Also be sure to keep in mind that a `SignatureMap` is itself a list of signature pairs, so each element of `sigPairLists` is a list of signature pairs.  `SignatureMap`s permit for a transaction to be signed by multiple accounts.

#### Factor C, Scheduled Transactions:

Scheduled transactions would be more accurately described as _pending_ transactions.  A `ScheduleCreateTransaction` proto message is sent to the network to indicate that you would like to open a scheduled transaction, and then the scheduled transaction will live on the network for up to a half hour.  During that window, other accounts may sign the scheduled transaction with `ScheduleSignTransaction`, and they can refer to it by its `ScheduleId`.

On the proto side of things, the `ScheduleCreate` proto message contains a `SchedulableTransactionBody`, which in turn contains a `oneof` of all of the transaction bodies that are schedulable (not all transactions are schedulable).

Because the schedulable transaction types are, well, already existing transaction types, the SDK user who wants to create a scheduled transaction first instantiates a normal `Transaction` subclass, and then derives a `ScheduleCreateTransaction` from that transaction, and then they execute that `ScheduleCreateTransaction` to actually create the scheduled transaction on the network.

The `Transaction` class has a couple of methods that interact with scheduled transactions: `schedule()` and `fromScheduledTransaction()`.

`schedule()` is the method that turns this `Transaction` (whatever subclass it is) into a `ScheduleCreateTransaction`.  It uses the `onScheduled()` abstract method that is implemented by the subclass to fill the `SchedulableTransactionBody` proto message in the new `ScheduleCreateTransaction` object before returning it.

`fromScheduledTransaction()` is a static constructor method that instantiates an appropriate `Transaction` subclass from a `SchedulableTransactionBody` proto message.  It's used by `ScheduleInfo` (which is the response from `ScheduleInfoQuery`) to create an SDK representation of the scheduled transaction that was queried about.  For example, if you query about a scheduled transaction, and it's a `CryptoTransfer` transaction, then when you call `scheduleInfo.getSceduledTransaction()`, it will use `Transaction.fromScheduledTransaction(transactionBody)` to create and return a new `CryptoTransferTransaction` object that contains all the info about the scheduled transaction.

#### Freezing:

All methods that modify the transaction (including those of subclasses) are guarded with `requireNotFrozen()`.  `isFrozen()` returns true if there is no `frozenBodyBuilder`.

The `Transaction` is not immediately sent after freezing.  Instead, the user of the SDK has an opportunity to add signatures.  In `onExecuteAsync()`, The `Transaction` will be frozen if it is not already frozen, and it will be signed by the client's operator, but if any additional signatures are desired, they should be added after freezing and before executing.

`this.innerSignedTransactions` is populated by `freezeWith()`.  `freezeWith()` creates only a single row of `innerSignedTransactions`.  `ChunkedTransaction` overrides `freezeWith()` to create an actual 2D array of `SignedTransactions`.  `this.outerTransactions`, however, is not populated until `makeRequest()` or some other, similar method that requires `Transaction` proto messages builds them using `buildTransaction()`.

#### Abstract methods:

`Transaction` overrides `getTransactionId()` to get the current transaction id.

`Transaction` overrides `makeRequest()` to produce the Transaction request message.  It also uses `buildTransactions()` to populate the `this.transactions` list.

`Transaction` overrides `mapResponse()` to create a `transactionResponse` and advance `transactionIds` to the next transaction.

`Transaction` overrides `mapResponseStatus()`.

`Transaction` adds the overridable abstract methods `onFreeze()`, `onScheduled()`, and `validateChecksums()`

`onFreeze()` takes a transaction body builder as an input, and should build out the body of the transaction.

`onScheduled()` does something similar with the schedulable body.

`validateChecksums()` has the same function as in `Query`

### `TopicMessageQuery`

Unlike most classes in the Hedera SDK, this is _not_ a query to a Hedera Hashgraph network, it is a query to a _mirror_ network.  As such, it is _not_ a subclass of `Query`, despite its name.

To use a `TopicMessageQuery`, instantiate one, configure it to specify which messages you want to receive, add any custom handlers you want, and then call its `MakeStreamingCall()` method.  The user must pass an `onNext()` handler to handle each response message.  The mirror network of the given `Client` will be used.  `MakeStreamingCall()` will make an asynchronous streaming rpc to one node in the mirror network.

The proto messages used under the hood are defined in `"proto/mirror/ConsensusService.proto"`, and response messages are parsed into `TopicMessage`s before being handed over to the `onNext()` handler.  The `ConsensusTopicResponse` proto message contains a `chunkInfo` field of type `ConsensusMessageChunkInfo` , which is defined in `ConsensusSubmitMessage.proto`.  `ConsensusTopicResponse` also has a `message` field, which is of type `bytes`, and these bytes are what the user is really querying for.  The SDK does not do anything to parse these bytes.  The meaning and parsing of these bytes is left to the user.

The responses may be chunked.  If they are, `TopicMessageQuery` will collect all of the chunks into one `TopicMessage` before passing it to the `onNext()` handler.  The `initialTransactionID` field of each responses' `chunkInfo` field is used to identify which pending message this response is a chunk of and store it appropriately.  `chunkInfo`'s `total` field is used to identify whether we've collected all of the chunks of a pending message, and if we have, we construct the `TopicMessage` and dispatch it to the `onNext()` handler.  Because grpc works over HTTP, we're guaranteed to receive all of the chunks, and in the correct order (unless an error occurs, obviously), though chunks from different topic messages may be interleaved.

In addition to the `onNext()` handler, there are several optional handlers which can be set with `setCompletionHandler()`, `setErrorhandler()`, and `setRetryHandler()`.  The retry handler returns a boolean to indicate whether the query should be retried.

### `FunctionalExecutable` and `FunctionalExecutableProcessor`

These classes aren't themselves components of the SDK, they are components in the SDK's build process.  `FunctionalExecutable` is a custom annotation defined in the `executable-annotation` directory, and we use this annotation is in the SDK source code to mark methods that require additional processing during the build process.  This additional processing is performed by the `FunctionalExecutableProcessor`, which is defined in the `executable-processor` directory.

The `FunctionalExecutableProcessor` uses the JavaPoet library to generate variations of each method marked with the `@FunctionalExecutable` annotation.  It presumes that the marked method is named `"*Async"`, that it has a `Client` parameter (or you may specify `onClient=true` if the method is _on_ `Client`, and you may specify an additional input type with `inputType=T`), and that it returns a `CompletableFuture<O>` (you may specify a type other than `O`, as shown in the example below), and the `FunctionalExecutableProcessor` adds to the class several variations of that method which build on that original `*Async()` method.

For example, if in class `Bar` you create the method `CompletableFuture<Integer> fooAsync(Client client)` and mark it with `@FunctionalExecutable(type=Integer.class)` (`type` here is the return type of the marked method), the `FunctionalExecutableProcessor` will add the following methods to `Bar`:

- `void fooAsync(Client client, BiConsumer<Integer, Throwable> callback)`
- `void fooAsync(Client client, Duration timeout, BiConsumer<Integer, Throwable> callback)`
- `void fooAsync(Client client, Consumer<Integer> onSuccess, Consumer<Throwable> onFailure)`
- `void fooAsync(Client client, Duration timeout, Consumer<Integer> onSuccess, Consumer<Throwable> onFailure)`
- `Integer foo(Client client)`
- `Integer foo(Client client, Duration timeout)`

Note that the last two are synchronous versions, and in `Executable`, the synchronous variants generated from `executeAsync()` are overridden with manually programmed `execute()` methods.

The `FunctionalAnnotationProcessor` can't add these methods to `Bar` directly.  Instead, it will generate a new interface called `WithFoo`.  The `WithFoo` interface will have an abstract `CompletableFuture<Integer> fooAsync(Client client)` method, and it will have all of the variations of the `fooAsync()` and `foo()` methods which are listed above, which will use and build on the abstract `fooAsync()` method.  You are expected to declare `Bar` as an implementation of this generated `WithFoo` interface.  You should use the `@Override` annotation on your `fooAsync()` method in `Bar` to make sure that it overrides the abstract `fooAsync()` method from `WithFoo`.

If you want to get a better grasp on what the `FunctionalExecutableProcessor` actually does, I suggest that after building the SDK, you should look at the files in `hiero-sdk/build/generated/sources/annotationProcessor/java/main/org/hiero/sdk/`.  These are the `With*.java` files that are generated by the `FunctionalExecutableProcessor` during the build process.  For example, the `WithExecute.java` file was generated because of the `@FunctionalExecutable` annotation on the `Executable.executeAsync()` method, and if you look at `WithExecute.java` side-by-side with the `FunctionalExecutableProcessor.process()` method body, you should be able to see how each of the default methods in the `WithExecute` interface were generated by the processor

**This document is not comprehensive.  There are classes I have not yet documented, or which I have only documented in passing, like `ChunkedTransaction`.**
