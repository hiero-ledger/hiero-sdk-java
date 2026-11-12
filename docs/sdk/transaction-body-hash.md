# Whole-transaction body hash candidate

`Transaction.getTransactionBodyHash()` returns 48 raw SHA-384 bytes for a frozen transaction's prepared
content, across every node and chunk. This implements the discussion profile in SDK collaboration hub
PR #288. Maintainers must agree on its framing, import ordering and rejection rules before SDK PRs merge.
The local implementation and shared fixtures do not establish an approved contract.

```java
var transaction = new TransferTransaction()
        .setTransactionId(transactionId)
        .setNodeAccountIds(nodeAccountIds)
        .addHbarTransfer(sender, amount.negated())
        .addHbarTransfer(recipient, amount)
        .freeze();

byte[] reviewedBodyHash = transaction.getTransactionBodyHash();
transaction.sign(privateKey); // Signature collection remains available after review.
```

The hash commits to a protobuf representation. IDs, selected nodes, fees, memo, contents, chunk boundaries,
chunk order and the first chunk's node order affect it. Independently encoded messages with equal decoded
values can have different hashes. This API promises no semantic canonicalization across encoders.
A matching digest does not verify signatures, authorization or network acceptance.

## Framing

For each ordinary prepared body, `entryHash = SHA384(original bodyBytes)`.

For each atomic batch:

```text
entryHash = SHA384(
    SHA384(original outer bodyBytes with the entire field-74 record removed)
    || SHA384(child[0].original bodyBytes)
    || ...
)
```

Children are `SignedTransaction` envelopes stored in the batch wrapper's field 1. Outer and child
signature maps and other envelope metadata, including the network hash algorithm flag, are excluded.
Unknown outer and child **body** fields are retained as original bytes. Signature-shaped data inside a
body remains content. No hashed body is decoded and re-encoded.

The final result is `SHA384(entryHash[0] || ... || entryHash[n-1])`, including the outer hash for a single
entry. Each part is a fixed 48-byte digest.

## Imports and ordering

Hash traversal groups entries temporarily by decoded transaction ID, in first-appearance order. Within
every chunk it follows the decoded node IDs' order in the first chunk. Node-major lists and differing
later-chunk node orders therefore share the digest of their equivalent chunk-major traversal. Hashing
does not reorder stored entries or change network serialization.

Execution selects a prepared request by its decoded transaction ID and node ID, including every chunk
in imports with different later-node orders or node-major layouts. It uses the matching original body
and preserves stored envelope order and bytes. Existing network hash APIs keep their algorithms and
result shapes.

Duplicate decoded `(transaction ID, node ID)` entries and incomplete or different node sets are rejected
before import regrouping can discard evidence. Existing transaction-type, body-content and chunk
validation still applies. Modern signed-transaction envelopes and supported legacy `bodyBytes` imports
retain their original prepared bodies. Unsigned imports remain editable: freezing an unchanged import
retains its original bytes, chunk IDs and boundaries; edits rebuild its bodies through the normal path.

Batch content consistency compares each child's original body bytes in order. Child signatures and
envelope metadata may differ between prepared node entries. A temporary comparison projection excludes
that metadata while preserving every child body byte and all other outer content checks. The projection
never replaces stored body bytes, child envelopes, signatures or network serialization.

## State and rejection rules

The getter requires a frozen, complete prepared body set. It performs no signing, signer callbacks or
network requests. Repeated calls preserve cursors, stored signatures and wire content. Success locks
transaction IDs and node membership so expiration handling cannot regenerate reviewed content; later
signature collection remains possible. Failure leaves state unchanged.

Body review, freeze selection and complete body preparation synchronize on the transaction instance.
A concurrent review waits for preparation to finish and hashes the completed bodies. A signer callback
that reenters the getter during preparation receives an error without locking IDs or marking content
reviewed. Failed preparation leaves the transaction unfrozen and retryable, retaining its imported
source. An already-frozen transaction's freeze call remains a no-op.

After retry backoff, receipt resubmission rechecks review state under that same monitor before changing
IDs or unfreezing the transaction. A rejected reviewed retry leaves it frozen. A review waits for an already started
resubmission to complete, then hashes its replacement prepared bodies. This synchronization protects
the body-review transition; it does not make the SDK generally thread safe.

Malformed or truncated wire data, conflicting operation records, repeated field 74, empty batches,
unknown batch-wrapper fields, malformed children, missing/empty/repeated child `bodyBytes`, invalid child
bodies and nested batches produce local errors. Batch children require explicit transaction and node
IDs, including an explicit `0.0.0` node where applicable.

The proposed wire bounds apply to `TransactionBody`, its transaction/node identifiers (including known
`TransactionID` timestamp and payer `AccountID` messages), the atomic batch wrapper and child
`SignedTransaction` envelopes. Their records support varint, fixed64, length-delimited and fixed32
(wire types 0, 1, 2 and 5). Legacy groups are rejected, including unknown identifier fields. Tag and length varints
use at most five bytes; value varints may use up to ten bytes without 64-bit overflow. Nonminimal
encodings within these widths are accepted and retained verbatim. These bounds are part of the
candidate awaiting maintainer agreement in #288, not a claim of a canonical protobuf standard.
Unknown length-delimited identifier values are opaque bytes. Operation-specific submessages retain
the SDK's existing schema decoding and transaction checks; this is not a universal protobuf validator.

The shared fixture suite contains 35 vectors: 20 expected hashes and 15 expected rejections. It includes
an unknown nonminimal value-varint success case and rejection cases for tags and lengths longer than
five bytes and for legacy groups. Two multi-node batches with different child signature collection in
only one node entry share the same independently calculated digest. All vectors run through public
import, freeze and hash APIs.

The separate identifier boundary dataset adds five cases: one byte-preserving digest and four
rejections. Together the 40 shared inputs contain 21 expected digests and 19 rejections. The core
35-vector inventory remains separate.

`getTransactionHash()`, per-node network hashes and their existing chunking restrictions keep their
existing behavior. Body hashing does not change their framing or broaden those APIs.
