// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import java.util.Objects;

/**
 * Returned in {@link TransactionReceipt}, {@link PrecheckStatusException}
 * and {@link ReceiptStatusException}.
 * <p>
 * The success variant is {@link #SUCCESS} which is what a {@link TransactionReceipt} will contain for a
 * successful transaction.
 */
public enum Status {
    /**
     * The transaction passed the precheck validations.
     */
    OK(ResponseCodeEnum.OK),

    /**
     * For any error not handled by specific error codes listed below.
     */
    INVALID_TRANSACTION(ResponseCodeEnum.INVALID_TRANSACTION),

    /**
     * Payer account does not exist.
     */
    PAYER_ACCOUNT_NOT_FOUND(ResponseCodeEnum.PAYER_ACCOUNT_NOT_FOUND),

    /**
     * Node Account provided does not match the node account of the node the transaction was submitted
     * to.
     */
    INVALID_NODE_ACCOUNT(ResponseCodeEnum.INVALID_NODE_ACCOUNT),

    /**
     * Pre-Check error when TransactionValidStart + transactionValidDuration is less than current
     * consensus time.
     */
    TRANSACTION_EXPIRED(ResponseCodeEnum.TRANSACTION_EXPIRED),

    /**
     * Transaction start time is greater than current consensus time
     */
    INVALID_TRANSACTION_START(ResponseCodeEnum.INVALID_TRANSACTION_START),

    /**
     * The given transactionValidDuration was either non-positive, or greater than the maximum
     * valid duration of 180 secs.
     */
    INVALID_TRANSACTION_DURATION(ResponseCodeEnum.INVALID_TRANSACTION_DURATION),

    /**
     * The transaction signature is not valid
     */
    INVALID_SIGNATURE(ResponseCodeEnum.INVALID_SIGNATURE),

    /**
     * Transaction memo size exceeded 100 bytes
     */
    MEMO_TOO_LONG(ResponseCodeEnum.MEMO_TOO_LONG),

    /**
     * The fee provided in the transaction is insufficient for this type of transaction
     */
    INSUFFICIENT_TX_FEE(ResponseCodeEnum.INSUFFICIENT_TX_FEE),

    /**
     * The payer account has insufficient cryptocurrency to pay the transaction fee
     */
    INSUFFICIENT_PAYER_BALANCE(ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE),

    /**
     * This transaction ID is a duplicate of one that was submitted to this node or reached consensus
     * in the last 180 seconds (receipt period)
     */
    DUPLICATE_TRANSACTION(ResponseCodeEnum.DUPLICATE_TRANSACTION),

    /**
     * If API is throttled out
     */
    BUSY(ResponseCodeEnum.BUSY),

    /**
     * The API is not currently supported
     */
    NOT_SUPPORTED(ResponseCodeEnum.NOT_SUPPORTED),

    /**
     * The file id is invalid or does not exist
     */
    INVALID_FILE_ID(ResponseCodeEnum.INVALID_FILE_ID),

    /**
     * The account id is invalid or does not exist
     */
    INVALID_ACCOUNT_ID(ResponseCodeEnum.INVALID_ACCOUNT_ID),

    /**
     * The contract id is invalid or does not exist
     */
    INVALID_CONTRACT_ID(ResponseCodeEnum.INVALID_CONTRACT_ID),

    /**
     * Transaction id is not valid
     */
    INVALID_TRANSACTION_ID(ResponseCodeEnum.INVALID_TRANSACTION_ID),

    /**
     * Receipt for given transaction id does not exist
     */
    RECEIPT_NOT_FOUND(ResponseCodeEnum.RECEIPT_NOT_FOUND),

    /**
     * Record for given transaction id does not exist
     */
    RECORD_NOT_FOUND(ResponseCodeEnum.RECORD_NOT_FOUND),

    /**
     * The solidity id is invalid or entity with this solidity id does not exist
     */
    INVALID_SOLIDITY_ID(ResponseCodeEnum.INVALID_SOLIDITY_ID),

    /**
     * The responding node has submitted the transaction to the network. Its final status is still
     * unknown.
     */
    UNKNOWN(ResponseCodeEnum.UNKNOWN),

    /**
     * The transaction succeeded
     */
    SUCCESS(ResponseCodeEnum.SUCCESS),

    /**
     * There was a system error and the transaction failed because of invalid request parameters.
     */
    FAIL_INVALID(ResponseCodeEnum.FAIL_INVALID),

    /**
     * There was a system error while performing fee calculation, reserved for future.
     */
    FAIL_FEE(ResponseCodeEnum.FAIL_FEE),

    /**
     * There was a system error while performing balance checks, reserved for future.
     */
    FAIL_BALANCE(ResponseCodeEnum.FAIL_BALANCE),

    /**
     * Key not provided in the transaction body
     */
    KEY_REQUIRED(ResponseCodeEnum.KEY_REQUIRED),

    /**
     * Unsupported algorithm/encoding used for keys in the transaction
     */
    BAD_ENCODING(ResponseCodeEnum.BAD_ENCODING),

    /**
     * When the account balance is not sufficient for the transfer
     */
    INSUFFICIENT_ACCOUNT_BALANCE(ResponseCodeEnum.INSUFFICIENT_ACCOUNT_BALANCE),

    /**
     * During an update transaction when the system is not able to find the Users Solidity address
     */
    INVALID_SOLIDITY_ADDRESS(ResponseCodeEnum.INVALID_SOLIDITY_ADDRESS),

    /**
     * Not enough gas was supplied to execute transaction
     */
    INSUFFICIENT_GAS(ResponseCodeEnum.INSUFFICIENT_GAS),

    /**
     * contract byte code size is over the limit
     */
    CONTRACT_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.CONTRACT_SIZE_LIMIT_EXCEEDED),

    /**
     * local execution (query) is requested for a function which changes state
     */
    LOCAL_CALL_MODIFICATION_EXCEPTION(ResponseCodeEnum.LOCAL_CALL_MODIFICATION_EXCEPTION),

    /**
     * Contract REVERT OPCODE executed
     */
    CONTRACT_REVERT_EXECUTED(ResponseCodeEnum.CONTRACT_REVERT_EXECUTED),

    /**
     * For any contract execution related error not handled by specific error codes listed above.
     */
    CONTRACT_EXECUTION_EXCEPTION(ResponseCodeEnum.CONTRACT_EXECUTION_EXCEPTION),

    /**
     * In Query validation, account with +ve(amount) value should be Receiving node account, the
     * receiver account should be only one account in the list
     */
    INVALID_RECEIVING_NODE_ACCOUNT(ResponseCodeEnum.INVALID_RECEIVING_NODE_ACCOUNT),

    /**
     * Header is missing in Query request
     */
    MISSING_QUERY_HEADER(ResponseCodeEnum.MISSING_QUERY_HEADER),

    /**
     * The update of the account failed
     */
    ACCOUNT_UPDATE_FAILED(ResponseCodeEnum.ACCOUNT_UPDATE_FAILED),

    /**
     * Provided key encoding was not supported by the system
     */
    INVALID_KEY_ENCODING(ResponseCodeEnum.INVALID_KEY_ENCODING),

    /**
     * null solidity address
     */
    NULL_SOLIDITY_ADDRESS(ResponseCodeEnum.NULL_SOLIDITY_ADDRESS),

    /**
     * update of the contract failed
     */
    CONTRACT_UPDATE_FAILED(ResponseCodeEnum.CONTRACT_UPDATE_FAILED),

    /**
     * the query header is invalid
     */
    INVALID_QUERY_HEADER(ResponseCodeEnum.INVALID_QUERY_HEADER),

    /**
     * Invalid fee submitted
     */
    INVALID_FEE_SUBMITTED(ResponseCodeEnum.INVALID_FEE_SUBMITTED),

    /**
     * Payer signature is invalid
     */
    INVALID_PAYER_SIGNATURE(ResponseCodeEnum.INVALID_PAYER_SIGNATURE),

    /**
     * The keys were not provided in the request.
     */
    KEY_NOT_PROVIDED(ResponseCodeEnum.KEY_NOT_PROVIDED),

    /**
     * Expiration time provided in the transaction was invalid.
     */
    INVALID_EXPIRATION_TIME(ResponseCodeEnum.INVALID_EXPIRATION_TIME),

    /**
     * WriteAccess Control Keys are not provided for the file
     */
    NO_WACL_KEY(ResponseCodeEnum.NO_WACL_KEY),

    /**
     * The contents of file are provided as empty.
     */
    FILE_CONTENT_EMPTY(ResponseCodeEnum.FILE_CONTENT_EMPTY),

    /**
     * The crypto transfer credit and debit do not sum equal to 0
     */
    INVALID_ACCOUNT_AMOUNTS(ResponseCodeEnum.INVALID_ACCOUNT_AMOUNTS),

    /**
     * Transaction body provided is empty
     */
    EMPTY_TRANSACTION_BODY(ResponseCodeEnum.EMPTY_TRANSACTION_BODY),

    /**
     * Invalid transaction body provided
     */
    INVALID_TRANSACTION_BODY(ResponseCodeEnum.INVALID_TRANSACTION_BODY),

    /**
     * the type of key (base ed25519 key, KeyList, or ThresholdKey) does not match the type of
     * signature (base ed25519 signature, SignatureList, or ThresholdKeySignature)
     */
    INVALID_SIGNATURE_TYPE_MISMATCHING_KEY(ResponseCodeEnum.INVALID_SIGNATURE_TYPE_MISMATCHING_KEY),

    /**
     * the number of key (KeyList, or ThresholdKey) does not match that of signature (SignatureList,
     * or ThresholdKeySignature). e.g. if a keyList has 3 base keys, then the corresponding
     * signatureList should also have 3 base signatures.
     */
    INVALID_SIGNATURE_COUNT_MISMATCHING_KEY(ResponseCodeEnum.INVALID_SIGNATURE_COUNT_MISMATCHING_KEY),

    /**
     * the livehash body is empty
     */
    EMPTY_LIVE_HASH_BODY(ResponseCodeEnum.EMPTY_LIVE_HASH_BODY),

    /**
     * the livehash data is missing
     */
    EMPTY_LIVE_HASH(ResponseCodeEnum.EMPTY_LIVE_HASH),

    /**
     * the keys for a livehash are missing
     */
    EMPTY_LIVE_HASH_KEYS(ResponseCodeEnum.EMPTY_LIVE_HASH_KEYS),

    /**
     * the livehash data is not the output of a SHA-384 digest
     */
    INVALID_LIVE_HASH_SIZE(ResponseCodeEnum.INVALID_LIVE_HASH_SIZE),

    /**
     * the query body is empty
     */
    EMPTY_QUERY_BODY(ResponseCodeEnum.EMPTY_QUERY_BODY),

    /**
     * the crypto livehash query is empty
     */
    EMPTY_LIVE_HASH_QUERY(ResponseCodeEnum.EMPTY_LIVE_HASH_QUERY),

    /**
     * the livehash is not present
     */
    LIVE_HASH_NOT_FOUND(ResponseCodeEnum.LIVE_HASH_NOT_FOUND),

    /**
     * the account id passed has not yet been created.
     */
    ACCOUNT_ID_DOES_NOT_EXIST(ResponseCodeEnum.ACCOUNT_ID_DOES_NOT_EXIST),

    /**
     * the livehash already exists for a given account
     */
    LIVE_HASH_ALREADY_EXISTS(ResponseCodeEnum.LIVE_HASH_ALREADY_EXISTS),

    /**
     * File WACL keys are invalid
     */
    INVALID_FILE_WACL(ResponseCodeEnum.INVALID_FILE_WACL),

    /**
     * Serialization failure
     */
    SERIALIZATION_FAILED(ResponseCodeEnum.SERIALIZATION_FAILED),

    /**
     * The size of the Transaction is greater than transactionMaxBytes
     */
    TRANSACTION_OVERSIZE(ResponseCodeEnum.TRANSACTION_OVERSIZE),

    /**
     * The Transaction has more than 50 levels
     */
    TRANSACTION_TOO_MANY_LAYERS(ResponseCodeEnum.TRANSACTION_TOO_MANY_LAYERS),

    /**
     * Contract is marked as deleted
     */
    CONTRACT_DELETED(ResponseCodeEnum.CONTRACT_DELETED),

    /**
     * the platform node is either disconnected or lagging behind.
     */
    PLATFORM_NOT_ACTIVE(ResponseCodeEnum.PLATFORM_NOT_ACTIVE),

    /**
     * one public key matches more than one prefixes on the signature map
     */
    KEY_PREFIX_MISMATCH(ResponseCodeEnum.KEY_PREFIX_MISMATCH),

    /**
     * transaction not created by platform due to large backlog
     */
    PLATFORM_TRANSACTION_NOT_CREATED(ResponseCodeEnum.PLATFORM_TRANSACTION_NOT_CREATED),

    /**
     * auto renewal period is not a positive number of seconds
     */
    INVALID_RENEWAL_PERIOD(ResponseCodeEnum.INVALID_RENEWAL_PERIOD),

    /**
     * the response code when a smart contract id is passed for a crypto API request
     */
    INVALID_PAYER_ACCOUNT_ID(ResponseCodeEnum.INVALID_PAYER_ACCOUNT_ID),

    /**
     * the account has been marked as deleted
     */
    ACCOUNT_DELETED(ResponseCodeEnum.ACCOUNT_DELETED),

    /**
     * the file has been marked as deleted
     */
    FILE_DELETED(ResponseCodeEnum.FILE_DELETED),

    /**
     * same accounts repeated in the transfer account list
     */
    ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS(ResponseCodeEnum.ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS),

    /**
     * attempting to set negative balance value for crypto account
     */
    SETTING_NEGATIVE_ACCOUNT_BALANCE(ResponseCodeEnum.SETTING_NEGATIVE_ACCOUNT_BALANCE),

    /**
     * when deleting smart contract that has crypto balance either transfer account or transfer smart
     * contract is required
     */
    OBTAINER_REQUIRED(ResponseCodeEnum.OBTAINER_REQUIRED),

    /**
     * when deleting smart contract that has crypto balance you can not use the same contract id as
     * transferContractId as the one being deleted
     */
    OBTAINER_SAME_CONTRACT_ID(ResponseCodeEnum.OBTAINER_SAME_CONTRACT_ID),

    /**
     * transferAccountId or transferContractId specified for contract delete does not exist
     */
    OBTAINER_DOES_NOT_EXIST(ResponseCodeEnum.OBTAINER_DOES_NOT_EXIST),

    /**
     * attempting to modify (update or delete a immutable smart contract, i.e. one created without a
     * admin key)
     */
    MODIFYING_IMMUTABLE_CONTRACT(ResponseCodeEnum.MODIFYING_IMMUTABLE_CONTRACT),

    /**
     * Unexpected exception thrown by file system functions
     */
    FILE_SYSTEM_EXCEPTION(ResponseCodeEnum.FILE_SYSTEM_EXCEPTION),

    /**
     * the duration is not a subset of [MINIMUM_AUTORENEW_DURATION,MAXIMUM_AUTORENEW_DURATION]
     */
    AUTORENEW_DURATION_NOT_IN_RANGE(ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE),

    /**
     * Decoding the smart contract binary to a byte array failed. Check that the input is a valid hex
     * string.
     */
    ERROR_DECODING_BYTESTRING(ResponseCodeEnum.ERROR_DECODING_BYTESTRING),

    /**
     * File to create a smart contract was of length zero
     */
    CONTRACT_FILE_EMPTY(ResponseCodeEnum.CONTRACT_FILE_EMPTY),

    /**
     * Bytecode for smart contract is of length zero
     */
    CONTRACT_BYTECODE_EMPTY(ResponseCodeEnum.CONTRACT_BYTECODE_EMPTY),

    /**
     * Attempt to set negative initial balance
     */
    INVALID_INITIAL_BALANCE(ResponseCodeEnum.INVALID_INITIAL_BALANCE),

    /**
     * Attempt to set negative receive record threshold
     */
    INVALID_RECEIVE_RECORD_THRESHOLD(ResponseCodeEnum.INVALID_RECEIVE_RECORD_THRESHOLD),

    /**
     * Attempt to set negative send record threshold
     */
    INVALID_SEND_RECORD_THRESHOLD(ResponseCodeEnum.INVALID_SEND_RECORD_THRESHOLD),

    /**
     * Special Account Operations should be performed by only Genesis account, return this code if it
     * is not Genesis Account
     */
    ACCOUNT_IS_NOT_GENESIS_ACCOUNT(ResponseCodeEnum.ACCOUNT_IS_NOT_GENESIS_ACCOUNT),

    /**
     * The fee payer account doesn't have permission to submit such Transaction
     */
    PAYER_ACCOUNT_UNAUTHORIZED(ResponseCodeEnum.PAYER_ACCOUNT_UNAUTHORIZED),

    /**
     * FreezeTransactionBody is invalid
     */
    INVALID_FREEZE_TRANSACTION_BODY(ResponseCodeEnum.INVALID_FREEZE_TRANSACTION_BODY),

    /**
     * FreezeTransactionBody does not exist
     */
    FREEZE_TRANSACTION_BODY_NOT_FOUND(ResponseCodeEnum.FREEZE_TRANSACTION_BODY_NOT_FOUND),

    /**
     * Exceeded the number of accounts (both from and to) allowed for crypto transfer list
     */
    TRANSFER_LIST_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.TRANSFER_LIST_SIZE_LIMIT_EXCEEDED),

    /**
     * Smart contract result size greater than specified maxResultSize
     */
    RESULT_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED),

    /**
     * The payer account is not a special account(account 0.0.55)
     */
    NOT_SPECIAL_ACCOUNT(ResponseCodeEnum.NOT_SPECIAL_ACCOUNT),

    /**
     * Negative gas was offered in smart contract call
     */
    CONTRACT_NEGATIVE_GAS(ResponseCodeEnum.CONTRACT_NEGATIVE_GAS),

    /**
     * Negative value / initial balance was specified in a smart contract call / create
     */
    CONTRACT_NEGATIVE_VALUE(ResponseCodeEnum.CONTRACT_NEGATIVE_VALUE),

    /**
     * Failed to update fee file
     */
    INVALID_FEE_FILE(ResponseCodeEnum.INVALID_FEE_FILE),

    /**
     * Failed to update exchange rate file
     */
    INVALID_EXCHANGE_RATE_FILE(ResponseCodeEnum.INVALID_EXCHANGE_RATE_FILE),

    /**
     * Payment tendered for contract local call cannot cover both the fee and the gas
     */
    INSUFFICIENT_LOCAL_CALL_GAS(ResponseCodeEnum.INSUFFICIENT_LOCAL_CALL_GAS),

    /**
     * Entities with Entity ID below 1000 are not allowed to be deleted
     */
    ENTITY_NOT_ALLOWED_TO_DELETE(ResponseCodeEnum.ENTITY_NOT_ALLOWED_TO_DELETE),

    /**
     * Violating one of these rules: 1) treasury account can update all entities below 0.0.1000, 2)
     * account 0.0.50 can update all entities from 0.0.51 - 0.0.80, 3) Network Function Master Account
     * A/c 0.0.50 - Update all Network Function accounts and perform all the Network Functions listed
     * below, 4) Network Function Accounts: i) A/c 0.0.55 - Update Address Book files (0.0.101/102),
     * ii) A/c 0.0.56 - Update Fee schedule (0.0.111), iii) A/c 0.0.57 - Update Exchange Rate
     * (0.0.112).
     */
    AUTHORIZATION_FAILED(ResponseCodeEnum.AUTHORIZATION_FAILED),

    /**
     * Fee Schedule Proto uploaded but not valid (append or update is required)
     */
    FILE_UPLOADED_PROTO_INVALID(ResponseCodeEnum.FILE_UPLOADED_PROTO_INVALID),

    /**
     * Fee Schedule Proto uploaded but not valid (append or update is required)
     */
    FILE_UPLOADED_PROTO_NOT_SAVED_TO_DISK(ResponseCodeEnum.FILE_UPLOADED_PROTO_NOT_SAVED_TO_DISK),

    /**
     * Fee Schedule Proto File Part uploaded
     */
    FEE_SCHEDULE_FILE_PART_UPLOADED(ResponseCodeEnum.FEE_SCHEDULE_FILE_PART_UPLOADED),

    /**
     * The change on Exchange Rate exceeds Exchange_Rate_Allowed_Percentage
     */
    EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED(ResponseCodeEnum.EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED),

    /**
     * Contract permanent storage exceeded the currently allowable limit
     */
    MAX_CONTRACT_STORAGE_EXCEEDED(ResponseCodeEnum.MAX_CONTRACT_STORAGE_EXCEEDED),

    /**
     * Transfer Account should not be same as Account to be deleted
     */
    TRANSFER_ACCOUNT_SAME_AS_DELETE_ACCOUNT(ResponseCodeEnum.TRANSFER_ACCOUNT_SAME_AS_DELETE_ACCOUNT),

    TOTAL_LEDGER_BALANCE_INVALID(ResponseCodeEnum.TOTAL_LEDGER_BALANCE_INVALID),

    /**
     * The expiration date/time on a smart contract may not be reduced
     */
    EXPIRATION_REDUCTION_NOT_ALLOWED(ResponseCodeEnum.EXPIRATION_REDUCTION_NOT_ALLOWED),

    /**
     * Gas exceeded currently allowable gas limit per transaction
     */
    MAX_GAS_LIMIT_EXCEEDED(ResponseCodeEnum.MAX_GAS_LIMIT_EXCEEDED),

    /**
     * File size exceeded the currently allowable limit
     */
    MAX_FILE_SIZE_EXCEEDED(ResponseCodeEnum.MAX_FILE_SIZE_EXCEEDED),

    /**
     * When a valid signature is not provided for operations on account with receiverSigRequired=true
     */
    RECEIVER_SIG_REQUIRED(ResponseCodeEnum.RECEIVER_SIG_REQUIRED),

    /**
     * The Topic ID specified is not in the system.
     */
    INVALID_TOPIC_ID(ResponseCodeEnum.INVALID_TOPIC_ID),

    /**
     * A provided admin key was invalid. Verify the bytes for an Ed25519 public key are exactly 32 bytes; and the bytes for a compressed ECDSA(secp256k1) key are exactly 33 bytes, with the first byte either 0x02 or 0x03..
     */
    INVALID_ADMIN_KEY(ResponseCodeEnum.INVALID_ADMIN_KEY),

    /**
     * A provided submit key was invalid.
     */
    INVALID_SUBMIT_KEY(ResponseCodeEnum.INVALID_SUBMIT_KEY),

    /**
     * An attempted operation was not authorized (ie - a deleteTopic for a topic with no adminKey).
     */
    UNAUTHORIZED(ResponseCodeEnum.UNAUTHORIZED),

    /**
     * A ConsensusService message is empty.
     */
    INVALID_TOPIC_MESSAGE(ResponseCodeEnum.INVALID_TOPIC_MESSAGE),

    /**
     * The autoRenewAccount specified is not a valid, active account.
     */
    INVALID_AUTORENEW_ACCOUNT(ResponseCodeEnum.INVALID_AUTORENEW_ACCOUNT),

    /**
     * An adminKey was not specified on the topic, so there must not be an autoRenewAccount.
     */
    AUTORENEW_ACCOUNT_NOT_ALLOWED(ResponseCodeEnum.AUTORENEW_ACCOUNT_NOT_ALLOWED),

    /**
     * The topic has expired, was not automatically renewed, and is in a 7 day grace period before the
     * topic will be deleted unrecoverably. This error response code will not be returned until
     * autoRenew functionality is supported by HAPI.
     */
    TOPIC_EXPIRED(ResponseCodeEnum.TOPIC_EXPIRED),

    /**
     * chunk number must be from 1 to total (chunks) inclusive.
     */
    INVALID_CHUNK_NUMBER(ResponseCodeEnum.INVALID_CHUNK_NUMBER),

    /**
     * For every chunk, the payer account that is part of initialTransactionID must match the Payer Account of this transaction. The entire initialTransactionID should match the transactionID of the first chunk, but this is not checked or enforced by Hedera except when the chunk number is 1.
     */
    INVALID_CHUNK_TRANSACTION_ID(ResponseCodeEnum.INVALID_CHUNK_TRANSACTION_ID),

    /**
     * Account is frozen and cannot transact with the token
     */
    ACCOUNT_FROZEN_FOR_TOKEN(ResponseCodeEnum.ACCOUNT_FROZEN_FOR_TOKEN),

    /**
     * An involved account already has more than tokens.maxPerAccount associations with non-deleted tokens.
     */
    TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED(ResponseCodeEnum.TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED),

    /**
     * The token is invalid or does not exist
     */
    INVALID_TOKEN_ID(ResponseCodeEnum.INVALID_TOKEN_ID),

    /**
     * Invalid token decimals
     */
    INVALID_TOKEN_DECIMALS(ResponseCodeEnum.INVALID_TOKEN_DECIMALS),

    /**
     * Invalid token initial supply
     */
    INVALID_TOKEN_INITIAL_SUPPLY(ResponseCodeEnum.INVALID_TOKEN_INITIAL_SUPPLY),

    /**
     * Treasury Account does not exist or is deleted
     */
    INVALID_TREASURY_ACCOUNT_FOR_TOKEN(ResponseCodeEnum.INVALID_TREASURY_ACCOUNT_FOR_TOKEN),

    /**
     * Token Symbol is not UTF-8 capitalized alphabetical string
     */
    INVALID_TOKEN_SYMBOL(ResponseCodeEnum.INVALID_TOKEN_SYMBOL),

    /**
     * Freeze key is not set on token
     */
    TOKEN_HAS_NO_FREEZE_KEY(ResponseCodeEnum.TOKEN_HAS_NO_FREEZE_KEY),

    /**
     * Amounts in transfer list are not net zero
     */
    TRANSFERS_NOT_ZERO_SUM_FOR_TOKEN(ResponseCodeEnum.TRANSFERS_NOT_ZERO_SUM_FOR_TOKEN),

    /**
     * A token symbol was not provided
     */
    MISSING_TOKEN_SYMBOL(ResponseCodeEnum.MISSING_TOKEN_SYMBOL),

    /**
     * The provided token symbol was too long
     */
    TOKEN_SYMBOL_TOO_LONG(ResponseCodeEnum.TOKEN_SYMBOL_TOO_LONG),

    /**
     * KYC must be granted and account does not have KYC granted
     */
    ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN(ResponseCodeEnum.ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN),

    /**
     * KYC key is not set on token
     */
    TOKEN_HAS_NO_KYC_KEY(ResponseCodeEnum.TOKEN_HAS_NO_KYC_KEY),

    /**
     * Token balance is not sufficient for the transaction
     */
    INSUFFICIENT_TOKEN_BALANCE(ResponseCodeEnum.INSUFFICIENT_TOKEN_BALANCE),

    /**
     * Token transactions cannot be executed on deleted token
     */
    TOKEN_WAS_DELETED(ResponseCodeEnum.TOKEN_WAS_DELETED),

    /**
     * Supply key is not set on token
     */
    TOKEN_HAS_NO_SUPPLY_KEY(ResponseCodeEnum.TOKEN_HAS_NO_SUPPLY_KEY),

    /**
     * Wipe key is not set on token
     */
    TOKEN_HAS_NO_WIPE_KEY(ResponseCodeEnum.TOKEN_HAS_NO_WIPE_KEY),

    /**
     * The requested token mint amount would cause an invalid total supply
     */
    INVALID_TOKEN_MINT_AMOUNT(ResponseCodeEnum.INVALID_TOKEN_MINT_AMOUNT),

    /**
     * The requested token burn amount would cause an invalid total supply
     */
    INVALID_TOKEN_BURN_AMOUNT(ResponseCodeEnum.INVALID_TOKEN_BURN_AMOUNT),

    /**
     * A required token-account relationship is missing
     */
    TOKEN_NOT_ASSOCIATED_TO_ACCOUNT(ResponseCodeEnum.TOKEN_NOT_ASSOCIATED_TO_ACCOUNT),

    /**
     * The target of a wipe operation was the token treasury account
     */
    CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT(ResponseCodeEnum.CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT),

    /**
     * The provided KYC key was invalid.
     */
    INVALID_KYC_KEY(ResponseCodeEnum.INVALID_KYC_KEY),

    /**
     * The provided wipe key was invalid.
     */
    INVALID_WIPE_KEY(ResponseCodeEnum.INVALID_WIPE_KEY),

    /**
     * The provided freeze key was invalid.
     */
    INVALID_FREEZE_KEY(ResponseCodeEnum.INVALID_FREEZE_KEY),

    /**
     * The provided supply key was invalid.
     */
    INVALID_SUPPLY_KEY(ResponseCodeEnum.INVALID_SUPPLY_KEY),

    /**
     * Token Name is not provided
     */
    MISSING_TOKEN_NAME(ResponseCodeEnum.MISSING_TOKEN_NAME),

    /**
     * Token Name is too long
     */
    TOKEN_NAME_TOO_LONG(ResponseCodeEnum.TOKEN_NAME_TOO_LONG),

    /**
     * The provided wipe amount must not be negative, zero or bigger than the token holder balance
     */
    INVALID_WIPING_AMOUNT(ResponseCodeEnum.INVALID_WIPING_AMOUNT),

    /**
     * Token does not have Admin key set, thus update/delete transactions cannot be performed
     */
    TOKEN_IS_IMMUTABLE(ResponseCodeEnum.TOKEN_IS_IMMUTABLE),

    /**
     * An associateToken operation specified a token already associated to the account
     */
    TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT(ResponseCodeEnum.TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT),

    /**
     * An attempted operation is invalid until all token balances for the target account are zero
     */
    TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES(ResponseCodeEnum.TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES),

    /**
     * An attempted operation is invalid because the account is a treasury
     */
    ACCOUNT_IS_TREASURY(ResponseCodeEnum.ACCOUNT_IS_TREASURY),

    /**
     * Same TokenIDs present in the token list
     */
    TOKEN_ID_REPEATED_IN_TOKEN_LIST(ResponseCodeEnum.TOKEN_ID_REPEATED_IN_TOKEN_LIST),

    /**
     * Exceeded the number of token transfers (both from and to) allowed for token transfer list
     */
    TOKEN_TRANSFER_LIST_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.TOKEN_TRANSFER_LIST_SIZE_LIMIT_EXCEEDED),

    /**
     * TokenTransfersTransactionBody has no TokenTransferList
     */
    EMPTY_TOKEN_TRANSFER_BODY(ResponseCodeEnum.EMPTY_TOKEN_TRANSFER_BODY),

    /**
     * TokenTransfersTransactionBody has a TokenTransferList with no AccountAmounts
     */
    EMPTY_TOKEN_TRANSFER_ACCOUNT_AMOUNTS(ResponseCodeEnum.EMPTY_TOKEN_TRANSFER_ACCOUNT_AMOUNTS),

    /**
     * The Scheduled entity does not exist; or has now expired, been deleted, or been executed
     */
    INVALID_SCHEDULE_ID(ResponseCodeEnum.INVALID_SCHEDULE_ID),

    /**
     * The Scheduled entity cannot be modified. Admin key not set
     */
    SCHEDULE_IS_IMMUTABLE(ResponseCodeEnum.SCHEDULE_IS_IMMUTABLE),

    /**
     * The provided Scheduled Payer does not exist
     */
    INVALID_SCHEDULE_PAYER_ID(ResponseCodeEnum.INVALID_SCHEDULE_PAYER_ID),

    /**
     * The Schedule Create Transaction TransactionID account does not exist
     */
    INVALID_SCHEDULE_ACCOUNT_ID(ResponseCodeEnum.INVALID_SCHEDULE_ACCOUNT_ID),

    /**
     * The provided sig map did not contain any new valid signatures from required signers of the scheduled transaction
     */
    NO_NEW_VALID_SIGNATURES(ResponseCodeEnum.NO_NEW_VALID_SIGNATURES),

    /**
     * The required signers for a scheduled transaction cannot be resolved, for example because they do not exist or have been deleted
     */
    UNRESOLVABLE_REQUIRED_SIGNERS(ResponseCodeEnum.UNRESOLVABLE_REQUIRED_SIGNERS),

    /**
     * Only whitelisted transaction types may be scheduled
     */
    SCHEDULED_TRANSACTION_NOT_IN_WHITELIST(ResponseCodeEnum.SCHEDULED_TRANSACTION_NOT_IN_WHITELIST),

    /**
     * At least one of the signatures in the provided sig map did not represent a valid signature for any required signer
     */
    SOME_SIGNATURES_WERE_INVALID(ResponseCodeEnum.SOME_SIGNATURES_WERE_INVALID),

    /**
     * The scheduled field in the TransactionID may not be set to true
     */
    TRANSACTION_ID_FIELD_NOT_ALLOWED(ResponseCodeEnum.TRANSACTION_ID_FIELD_NOT_ALLOWED),

    /**
     * A schedule already exists with the same identifying fields of an attempted ScheduleCreate (that is, all fields other than scheduledPayerAccountID)
     */
    IDENTICAL_SCHEDULE_ALREADY_CREATED(ResponseCodeEnum.IDENTICAL_SCHEDULE_ALREADY_CREATED),

    /**
     * A string field in the transaction has a UTF-8 encoding with the prohibited zero byte
     */
    INVALID_ZERO_BYTE_IN_STRING(ResponseCodeEnum.INVALID_ZERO_BYTE_IN_STRING),

    /**
     * A schedule being signed or deleted has already been deleted
     */
    SCHEDULE_ALREADY_DELETED(ResponseCodeEnum.SCHEDULE_ALREADY_DELETED),

    /**
     * A schedule being signed or deleted has already been executed
     */
    SCHEDULE_ALREADY_EXECUTED(ResponseCodeEnum.SCHEDULE_ALREADY_EXECUTED),

    /**
     * ConsensusSubmitMessage request's message size is larger than allowed.
     */
    MESSAGE_SIZE_TOO_LARGE(ResponseCodeEnum.MESSAGE_SIZE_TOO_LARGE),

    /**
     * An operation was assigned to more than one throttle group in a given bucket
     */
    OPERATION_REPEATED_IN_BUCKET_GROUPS(ResponseCodeEnum.OPERATION_REPEATED_IN_BUCKET_GROUPS),

    /**
     * The capacity needed to satisfy all opsPerSec groups in a bucket overflowed a signed 8-byte integral type
     */
    BUCKET_CAPACITY_OVERFLOW(ResponseCodeEnum.BUCKET_CAPACITY_OVERFLOW),

    /**
     * Given the network size in the address book, the node-level capacity for an operation would never be enough to accept a single request; usually means a bucket burstPeriod should be increased
     */
    NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION(ResponseCodeEnum.NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION),

    /**
     * A bucket was defined without any throttle groups
     */
    BUCKET_HAS_NO_THROTTLE_GROUPS(ResponseCodeEnum.BUCKET_HAS_NO_THROTTLE_GROUPS),

    /**
     * A throttle group was granted zero opsPerSec
     */
    THROTTLE_GROUP_HAS_ZERO_OPS_PER_SEC(ResponseCodeEnum.THROTTLE_GROUP_HAS_ZERO_OPS_PER_SEC),

    /**
     * The throttle definitions file was updated, but some supported operations were not assigned a bucket
     */
    SUCCESS_BUT_MISSING_EXPECTED_OPERATION(ResponseCodeEnum.SUCCESS_BUT_MISSING_EXPECTED_OPERATION),

    /**
     * The new contents for the throttle definitions system file were not valid protobuf
     */
    UNPARSEABLE_THROTTLE_DEFINITIONS(ResponseCodeEnum.UNPARSEABLE_THROTTLE_DEFINITIONS),

    /**
     * The new throttle definitions system file were invalid, and no more specific error could be divined
     */
    INVALID_THROTTLE_DEFINITIONS(ResponseCodeEnum.INVALID_THROTTLE_DEFINITIONS),

    /**
     * The transaction references an account which has passed its expiration without renewal funds available, and currently remains in the ledger only because of the grace period given to expired entities
     */
    ACCOUNT_EXPIRED_AND_PENDING_REMOVAL(ResponseCodeEnum.ACCOUNT_EXPIRED_AND_PENDING_REMOVAL),

    /**
     * Invalid token max supply
     */
    INVALID_TOKEN_MAX_SUPPLY(ResponseCodeEnum.INVALID_TOKEN_MAX_SUPPLY),

    /**
     * Invalid token nft serial number
     */
    INVALID_TOKEN_NFT_SERIAL_NUMBER(ResponseCodeEnum.INVALID_TOKEN_NFT_SERIAL_NUMBER),

    /**
     * Invalid nft id
     */
    INVALID_NFT_ID(ResponseCodeEnum.INVALID_NFT_ID),

    /**
     * Nft metadata is too long
     */
    METADATA_TOO_LONG(ResponseCodeEnum.METADATA_TOO_LONG),

    /**
     * Repeated operations count exceeds the limit
     */
    BATCH_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.BATCH_SIZE_LIMIT_EXCEEDED),

    /**
     * The range of data to be gathered is out of the set boundaries
     */
    INVALID_QUERY_RANGE(ResponseCodeEnum.INVALID_QUERY_RANGE),

    /**
     * A custom fractional fee set a denominator of zero
     */
    FRACTION_DIVIDES_BY_ZERO(ResponseCodeEnum.FRACTION_DIVIDES_BY_ZERO),

    /**
     * The transaction payer could not afford a custom fee
     */
    INSUFFICIENT_PAYER_BALANCE_FOR_CUSTOM_FEE(ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE_FOR_CUSTOM_FEE),

    /**
     * More than 10 custom fees were specified
     */
    CUSTOM_FEES_LIST_TOO_LONG(ResponseCodeEnum.CUSTOM_FEES_LIST_TOO_LONG),

    /**
     * Any of the feeCollector accounts for customFees is invalid
     */
    INVALID_CUSTOM_FEE_COLLECTOR(ResponseCodeEnum.INVALID_CUSTOM_FEE_COLLECTOR),

    /**
     * Any of the token Ids in customFees is invalid
     */
    INVALID_TOKEN_ID_IN_CUSTOM_FEES(ResponseCodeEnum.INVALID_TOKEN_ID_IN_CUSTOM_FEES),

    /**
     * Any of the token Ids in customFees are not associated to feeCollector
     */
    TOKEN_NOT_ASSOCIATED_TO_FEE_COLLECTOR(ResponseCodeEnum.TOKEN_NOT_ASSOCIATED_TO_FEE_COLLECTOR),

    /**
     * A token cannot have more units minted due to its configured supply ceiling
     */
    TOKEN_MAX_SUPPLY_REACHED(ResponseCodeEnum.TOKEN_MAX_SUPPLY_REACHED),

    /**
     * The transaction attempted to move an NFT serial number from an account other than its owner
     */
    SENDER_DOES_NOT_OWN_NFT_SERIAL_NO(ResponseCodeEnum.SENDER_DOES_NOT_OWN_NFT_SERIAL_NO),

    /**
     * A custom fee schedule entry did not specify either a fixed or fractional fee
     */
    CUSTOM_FEE_NOT_FULLY_SPECIFIED(ResponseCodeEnum.CUSTOM_FEE_NOT_FULLY_SPECIFIED),

    /**
     * Only positive fees may be assessed at this time
     */
    CUSTOM_FEE_MUST_BE_POSITIVE(ResponseCodeEnum.CUSTOM_FEE_MUST_BE_POSITIVE),

    /**
     * Fee schedule key is not set on token
     */
    TOKEN_HAS_NO_FEE_SCHEDULE_KEY(ResponseCodeEnum.TOKEN_HAS_NO_FEE_SCHEDULE_KEY),

    /**
     * A fractional custom fee exceeded the range of a 64-bit signed integer
     */
    CUSTOM_FEE_OUTSIDE_NUMERIC_RANGE(ResponseCodeEnum.CUSTOM_FEE_OUTSIDE_NUMERIC_RANGE),

    /**
     * A royalty cannot exceed the total fungible value exchanged for an NFT
     */
    ROYALTY_FRACTION_CANNOT_EXCEED_ONE(ResponseCodeEnum.ROYALTY_FRACTION_CANNOT_EXCEED_ONE),

    /**
     * Each fractional custom fee must have its maximum_amount, if specified, at least its minimum_amount
     */
    FRACTIONAL_FEE_MAX_AMOUNT_LESS_THAN_MIN_AMOUNT(ResponseCodeEnum.FRACTIONAL_FEE_MAX_AMOUNT_LESS_THAN_MIN_AMOUNT),

    /**
     * A fee schedule update tried to clear the custom fees from a token whose fee schedule was already empty
     */
    CUSTOM_SCHEDULE_ALREADY_HAS_NO_FEES(ResponseCodeEnum.CUSTOM_SCHEDULE_ALREADY_HAS_NO_FEES),

    /**
     * Only tokens of type FUNGIBLE_COMMON can be used to as fee schedule denominations
     */
    CUSTOM_FEE_DENOMINATION_MUST_BE_FUNGIBLE_COMMON(ResponseCodeEnum.CUSTOM_FEE_DENOMINATION_MUST_BE_FUNGIBLE_COMMON),

    /**
     * Only tokens of type FUNGIBLE_COMMON can have fractional fees
     */
    CUSTOM_FRACTIONAL_FEE_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON(
            ResponseCodeEnum.CUSTOM_FRACTIONAL_FEE_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON),

    /**
     * The provided custom fee schedule key was invalid
     */
    INVALID_CUSTOM_FEE_SCHEDULE_KEY(ResponseCodeEnum.INVALID_CUSTOM_FEE_SCHEDULE_KEY),

    /**
     * The requested token mint metadata was invalid
     */
    INVALID_TOKEN_MINT_METADATA(ResponseCodeEnum.INVALID_TOKEN_MINT_METADATA),

    /**
     * The requested token burn metadata was invalid
     */
    INVALID_TOKEN_BURN_METADATA(ResponseCodeEnum.INVALID_TOKEN_BURN_METADATA),

    /**
     * The treasury for a unique token cannot be changed until it owns no NFTs
     */
    CURRENT_TREASURY_STILL_OWNS_NFTS(ResponseCodeEnum.CURRENT_TREASURY_STILL_OWNS_NFTS),

    /**
     * An account cannot be dissociated from a unique token if it owns NFTs for the token
     */
    ACCOUNT_STILL_OWNS_NFTS(ResponseCodeEnum.ACCOUNT_STILL_OWNS_NFTS),

    /**
     * A NFT can only be burned when owned by the unique token's treasury
     */
    TREASURY_MUST_OWN_BURNED_NFT(ResponseCodeEnum.TREASURY_MUST_OWN_BURNED_NFT),

    /**
     * An account did not own the NFT to be wiped
     */
    ACCOUNT_DOES_NOT_OWN_WIPED_NFT(ResponseCodeEnum.ACCOUNT_DOES_NOT_OWN_WIPED_NFT),

    /**
     * An AccountAmount token transfers list referenced a token type other than FUNGIBLE_COMMON
     */
    ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON(
            ResponseCodeEnum.ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON),

    /**
     * All the NFTs allowed in the current price regime have already been minted
     */
    MAX_NFTS_IN_PRICE_REGIME_HAVE_BEEN_MINTED(ResponseCodeEnum.MAX_NFTS_IN_PRICE_REGIME_HAVE_BEEN_MINTED),

    /**
     * The payer account has been marked as deleted
     */
    PAYER_ACCOUNT_DELETED(ResponseCodeEnum.PAYER_ACCOUNT_DELETED),

    /**
     * The reference chain of custom fees for a transferred token exceeded the maximum length of 2
     */
    CUSTOM_FEE_CHARGING_EXCEEDED_MAX_RECURSION_DEPTH(ResponseCodeEnum.CUSTOM_FEE_CHARGING_EXCEEDED_MAX_RECURSION_DEPTH),

    /**
     * More than 20 balance adjustments were to satisfy a CryptoTransfer and its implied custom fee payments
     */
    CUSTOM_FEE_CHARGING_EXCEEDED_MAX_ACCOUNT_AMOUNTS(ResponseCodeEnum.CUSTOM_FEE_CHARGING_EXCEEDED_MAX_ACCOUNT_AMOUNTS),

    /**
     * The sender account in the token transfer transaction could not afford a custom fee
     */
    INSUFFICIENT_SENDER_ACCOUNT_BALANCE_FOR_CUSTOM_FEE(
            ResponseCodeEnum.INSUFFICIENT_SENDER_ACCOUNT_BALANCE_FOR_CUSTOM_FEE),

    /**
     * Currently no more than 4,294,967,295 NFTs may be minted for a given unique token type
     */
    SERIAL_NUMBER_LIMIT_REACHED(ResponseCodeEnum.SERIAL_NUMBER_LIMIT_REACHED),

    /**
     * Only tokens of type NON_FUNGIBLE_UNIQUE can have royalty fees
     */
    CUSTOM_ROYALTY_FEE_ONLY_ALLOWED_FOR_NON_FUNGIBLE_UNIQUE(
            ResponseCodeEnum.CUSTOM_ROYALTY_FEE_ONLY_ALLOWED_FOR_NON_FUNGIBLE_UNIQUE),

    /**
     * The account has reached the limit on the automatic associations count.
     */
    NO_REMAINING_AUTOMATIC_ASSOCIATIONS(ResponseCodeEnum.NO_REMAINING_AUTOMATIC_ASSOCIATIONS),

    /**
     * Already existing automatic associations are more than the new maximum automatic associations.
     */
    EXISTING_AUTOMATIC_ASSOCIATIONS_EXCEED_GIVEN_LIMIT(
            ResponseCodeEnum.EXISTING_AUTOMATIC_ASSOCIATIONS_EXCEED_GIVEN_LIMIT),

    /**
     * Cannot set the number of automatic associations for an account more than the maximum allowed
     * token associations tokens.maxPerAccount.
     */
    REQUESTED_NUM_AUTOMATIC_ASSOCIATIONS_EXCEEDS_ASSOCIATION_LIMIT(
            ResponseCodeEnum.REQUESTED_NUM_AUTOMATIC_ASSOCIATIONS_EXCEEDS_ASSOCIATION_LIMIT),

    /**
     * Token is paused. This Token cannot be a part of any kind of Transaction until unpaused.
     */
    TOKEN_IS_PAUSED(ResponseCodeEnum.TOKEN_IS_PAUSED),

    /**
     * Pause key is not set on token
     */
    TOKEN_HAS_NO_PAUSE_KEY(ResponseCodeEnum.TOKEN_HAS_NO_PAUSE_KEY),

    /**
     * The provided pause key was invalid
     */
    INVALID_PAUSE_KEY(ResponseCodeEnum.INVALID_PAUSE_KEY),

    /**
     * The update file in a freeze transaction body must exist.
     */
    FREEZE_UPDATE_FILE_DOES_NOT_EXIST(ResponseCodeEnum.FREEZE_UPDATE_FILE_DOES_NOT_EXIST),

    /**
     * The hash of the update file in a freeze transaction body must match the in-memory hash.
     */
    FREEZE_UPDATE_FILE_HASH_DOES_NOT_MATCH(ResponseCodeEnum.FREEZE_UPDATE_FILE_HASH_DOES_NOT_MATCH),

    /**
     * A FREEZE_UPGRADE transaction was handled with no previous update prepared.
     */
    NO_UPGRADE_HAS_BEEN_PREPARED(ResponseCodeEnum.NO_UPGRADE_HAS_BEEN_PREPARED),

    /**
     * A FREEZE_ABORT transaction was handled with no scheduled freeze.
     */
    NO_FREEZE_IS_SCHEDULED(ResponseCodeEnum.NO_FREEZE_IS_SCHEDULED),

    /**
     * The update file hash when handling a FREEZE_UPGRADE transaction differs from the file
     * hash at the time of handling the PREPARE_UPGRADE transaction.
     */
    UPDATE_FILE_HASH_CHANGED_SINCE_PREPARE_UPGRADE(ResponseCodeEnum.UPDATE_FILE_HASH_CHANGED_SINCE_PREPARE_UPGRADE),

    /**
     * The given freeze start time was in the (consensus) past.
     */
    FREEZE_START_TIME_MUST_BE_FUTURE(ResponseCodeEnum.FREEZE_START_TIME_MUST_BE_FUTURE),

    /**
     * The prepared update file cannot be updated or appended until either the upgrade has
     * been completed, or a FREEZE_ABORT has been handled.
     */
    PREPARED_UPDATE_FILE_IS_IMMUTABLE(ResponseCodeEnum.PREPARED_UPDATE_FILE_IS_IMMUTABLE),

    /**
     * Once a freeze is scheduled, it must be aborted before any other type of freeze can
     * can be performed.
     */
    FREEZE_ALREADY_SCHEDULED(ResponseCodeEnum.FREEZE_ALREADY_SCHEDULED),

    /**
     * If an NMT upgrade has been prepared, the following operation must be a FREEZE_UPGRADE.
     * (To issue a FREEZE_ONLY, submit a FREEZE_ABORT first.)
     */
    FREEZE_UPGRADE_IN_PROGRESS(ResponseCodeEnum.FREEZE_UPGRADE_IN_PROGRESS),

    /**
     * If an NMT upgrade has been prepared, the subsequent FREEZE_UPGRADE transaction must
     * confirm the id of the file to be used in the upgrade.
     */
    UPDATE_FILE_ID_DOES_NOT_MATCH_PREPARED(ResponseCodeEnum.UPDATE_FILE_ID_DOES_NOT_MATCH_PREPARED),

    /**
     * If an NMT upgrade has been prepared, the subsequent FREEZE_UPGRADE transaction must
     * confirm the hash of the file to be used in the upgrade.
     */
    UPDATE_FILE_HASH_DOES_NOT_MATCH_PREPARED(ResponseCodeEnum.UPDATE_FILE_HASH_DOES_NOT_MATCH_PREPARED),

    /**
     * Consensus throttle did not allow execution of this transaction. System is throttled at
     * consensus level.
     */
    CONSENSUS_GAS_EXHAUSTED(ResponseCodeEnum.CONSENSUS_GAS_EXHAUSTED),

    /**
     * A precompiled contract succeeded, but was later reverted.
     */
    REVERTED_SUCCESS(ResponseCodeEnum.REVERTED_SUCCESS),

    /**
     * All contract storage allocated to the current price regime has been consumed.
     */
    MAX_STORAGE_IN_PRICE_REGIME_HAS_BEEN_USED(ResponseCodeEnum.MAX_STORAGE_IN_PRICE_REGIME_HAS_BEEN_USED),

    /**
     * An alias used in a CryptoTransfer transaction is not the serialization of a primitive Key
     * message--that is, a Key with a single Ed25519 or ECDSA(secp256k1) public key and no
     * unknown protobuf fields.
     */
    INVALID_ALIAS_KEY(ResponseCodeEnum.INVALID_ALIAS_KEY),

    /**
     * A fungible token transfer expected a different number of decimals than the involved
     * type actually has.
     */
    UNEXPECTED_TOKEN_DECIMALS(ResponseCodeEnum.UNEXPECTED_TOKEN_DECIMALS),

    /**
     * The proxy account id is invalid or does not exist.
     */
    INVALID_PROXY_ACCOUNT_ID(ResponseCodeEnum.INVALID_PROXY_ACCOUNT_ID),

    /**
     * The transfer account id in CryptoDelete transaction is invalid or does not exist.
     */
    INVALID_TRANSFER_ACCOUNT_ID(ResponseCodeEnum.INVALID_TRANSFER_ACCOUNT_ID),

    /**
     * The fee collector account id in TokenFeeScheduleUpdate is invalid or does not exist.
     */
    INVALID_FEE_COLLECTOR_ACCOUNT_ID(ResponseCodeEnum.INVALID_FEE_COLLECTOR_ACCOUNT_ID),

    /**
     * The alias already set on an account cannot be updated using CryptoUpdate transaction.
     */
    ALIAS_IS_IMMUTABLE(ResponseCodeEnum.ALIAS_IS_IMMUTABLE),

    /**
     * An approved allowance specifies a spender account that is the same as the hbar/token
     * owner account.
     */
    SPENDER_ACCOUNT_SAME_AS_OWNER(ResponseCodeEnum.SPENDER_ACCOUNT_SAME_AS_OWNER),

    /**
     * The establishment or adjustment of an approved allowance cause the token allowance
     * to exceed the token maximum supply.
     */
    AMOUNT_EXCEEDS_TOKEN_MAX_SUPPLY(ResponseCodeEnum.AMOUNT_EXCEEDS_TOKEN_MAX_SUPPLY),

    /**
     * The specified amount for an approved allowance cannot be negative.
     */
    NEGATIVE_ALLOWANCE_AMOUNT(ResponseCodeEnum.NEGATIVE_ALLOWANCE_AMOUNT),

    /**
     * The approveForAll flag cannot be set for a fungible token.
     */
    CANNOT_APPROVE_FOR_ALL_FUNGIBLE_COMMON(ResponseCodeEnum.CANNOT_APPROVE_FOR_ALL_FUNGIBLE_COMMON),

    /**
     * The spender does not have an existing approved allowance with the hbar/token owner.
     */
    SPENDER_DOES_NOT_HAVE_ALLOWANCE(ResponseCodeEnum.SPENDER_DOES_NOT_HAVE_ALLOWANCE),

    /**
     * The transfer amount exceeds the current approved allowance for the spender account.
     */
    AMOUNT_EXCEEDS_ALLOWANCE(ResponseCodeEnum.AMOUNT_EXCEEDS_ALLOWANCE),

    /**
     * The payer account of an approveAllowances or adjustAllowance transaction is attempting
     * to go beyond the maximum allowed number of allowances.
     */
    MAX_ALLOWANCES_EXCEEDED(ResponseCodeEnum.MAX_ALLOWANCES_EXCEEDED),

    /**
     * No allowances have been specified in the approval transaction.
     */
    EMPTY_ALLOWANCES(ResponseCodeEnum.EMPTY_ALLOWANCES),

    /**
     * Spender is repeated more than once in Crypto or Token or NFT allowance lists in a single
     * CryptoApproveAllowance transaction.
     */
    SPENDER_ACCOUNT_REPEATED_IN_ALLOWANCES(ResponseCodeEnum.SPENDER_ACCOUNT_REPEATED_IN_ALLOWANCES),

    /**
     * Serial numbers are repeated in nft allowance for a single spender account
     */
    REPEATED_SERIAL_NUMS_IN_NFT_ALLOWANCES(ResponseCodeEnum.REPEATED_SERIAL_NUMS_IN_NFT_ALLOWANCES),

    /**
     * Fungible common token used in NFT allowances
     */
    FUNGIBLE_TOKEN_IN_NFT_ALLOWANCES(ResponseCodeEnum.FUNGIBLE_TOKEN_IN_NFT_ALLOWANCES),

    /**
     * Non fungible token used in fungible token allowances
     */
    NFT_IN_FUNGIBLE_TOKEN_ALLOWANCES(ResponseCodeEnum.NFT_IN_FUNGIBLE_TOKEN_ALLOWANCES),

    /**
     * The account id specified as the owner is invalid or does not exist.
     */
    INVALID_ALLOWANCE_OWNER_ID(ResponseCodeEnum.INVALID_ALLOWANCE_OWNER_ID),

    /**
     * The account id specified as the spender is invalid or does not exist.
     */
    INVALID_ALLOWANCE_SPENDER_ID(ResponseCodeEnum.INVALID_ALLOWANCE_SPENDER_ID),

    /**
     * [Deprecated] If the CryptoDeleteAllowance transaction has repeated crypto or token or Nft allowances to delete.
     */
    REPEATED_ALLOWANCES_TO_DELETE(ResponseCodeEnum.REPEATED_ALLOWANCES_TO_DELETE),

    /**
     * If the account Id specified as the delegating spender is invalid or does not exist.
     */
    INVALID_DELEGATING_SPENDER(ResponseCodeEnum.INVALID_DELEGATING_SPENDER),

    /**
     * The delegating Spender cannot grant approveForAll allowance on a NFT token type for another spender.
     */
    DELEGATING_SPENDER_CANNOT_GRANT_APPROVE_FOR_ALL(ResponseCodeEnum.DELEGATING_SPENDER_CANNOT_GRANT_APPROVE_FOR_ALL),

    /**
     * The delegating Spender cannot grant allowance on a NFT serial for another spender as it doesnt not have approveForAll
     * granted on token-owner.
     */
    DELEGATING_SPENDER_DOES_NOT_HAVE_APPROVE_FOR_ALL(ResponseCodeEnum.DELEGATING_SPENDER_DOES_NOT_HAVE_APPROVE_FOR_ALL),

    /**
     * The scheduled transaction could not be created because it's expiration_time was too far in the future.
     */
    SCHEDULE_EXPIRATION_TIME_TOO_FAR_IN_FUTURE(ResponseCodeEnum.SCHEDULE_EXPIRATION_TIME_TOO_FAR_IN_FUTURE),

    /**
     * The scheduled transaction could not be created because it's expiration_time was less than or equal to the consensus time.
     */
    SCHEDULE_EXPIRATION_TIME_MUST_BE_HIGHER_THAN_CONSENSUS_TIME(
            ResponseCodeEnum.SCHEDULE_EXPIRATION_TIME_MUST_BE_HIGHER_THAN_CONSENSUS_TIME),

    /**
     * The scheduled transaction could not be created because it would cause throttles to be violated on the specified expiration_time.
     */
    SCHEDULE_FUTURE_THROTTLE_EXCEEDED(ResponseCodeEnum.SCHEDULE_FUTURE_THROTTLE_EXCEEDED),

    /**
     * The scheduled transaction could not be created because it would cause the gas limit to be violated on the specified expiration_time.
     */
    SCHEDULE_FUTURE_GAS_LIMIT_EXCEEDED(ResponseCodeEnum.SCHEDULE_FUTURE_GAS_LIMIT_EXCEEDED),

    /**
     * The ethereum transaction either failed parsing or failed signature validation, or some other EthereumTransaction error not covered by another response code.
     */
    INVALID_ETHEREUM_TRANSACTION(ResponseCodeEnum.INVALID_ETHEREUM_TRANSACTION),

    /**
     * EthereumTransaction was signed against a chainId that this network does not support.
     */
    WRONG_CHAIN_ID(ResponseCodeEnum.WRONG_CHAIN_ID),

    /**
     * This transaction specified an ethereumNonce that is not the current ethereumNonce of the account.
     */
    WRONG_NONCE(ResponseCodeEnum.WRONG_NONCE),

    /**
     * The ethereum transaction specified an access list, which the network does not support.
     */
    ACCESS_LIST_UNSUPPORTED(ResponseCodeEnum.ACCESS_LIST_UNSUPPORTED),

    /**
     * A schedule being signed or deleted has passed it's expiration date and is pending execution if needed and then expiration.
     */
    SCHEDULE_PENDING_EXPIRATION(ResponseCodeEnum.SCHEDULE_PENDING_EXPIRATION),

    /**
     * A selfdestruct or ContractDelete targeted a contract that is a token treasury.
     */
    CONTRACT_IS_TOKEN_TREASURY(ResponseCodeEnum.CONTRACT_IS_TOKEN_TREASURY),

    /**
     * A selfdestruct or ContractDelete targeted a contract with non-zero token balances.
     */
    CONTRACT_HAS_NON_ZERO_TOKEN_BALANCES(ResponseCodeEnum.CONTRACT_HAS_NON_ZERO_TOKEN_BALANCES),

    /**
     * A contract referenced by a transaction is "detached"; that is, expired and lacking any
     * hbar funds for auto-renewal payment---but still within its post-expiry grace period.
     */
    CONTRACT_EXPIRED_AND_PENDING_REMOVAL(ResponseCodeEnum.CONTRACT_EXPIRED_AND_PENDING_REMOVAL),

    /**
     * A ContractUpdate requested removal of a contract's auto-renew account, but that contract has
     * no auto-renew account.
     */
    CONTRACT_HAS_NO_AUTO_RENEW_ACCOUNT(ResponseCodeEnum.CONTRACT_HAS_NO_AUTO_RENEW_ACCOUNT),

    /**
     * A delete transaction submitted via HAPI set permanent_removal=true
     */
    PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION(ResponseCodeEnum.PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION),

    /**
     * A CryptoCreate or ContractCreate used the deprecated proxyAccountID field.
     */
    PROXY_ACCOUNT_ID_FIELD_IS_DEPRECATED(ResponseCodeEnum.PROXY_ACCOUNT_ID_FIELD_IS_DEPRECATED),

    /**
     * An account set the staked_account_id to itself in CryptoUpdate or ContractUpdate transactions.
     */
    SELF_STAKING_IS_NOT_ALLOWED(ResponseCodeEnum.SELF_STAKING_IS_NOT_ALLOWED),

    /**
     * The staking account id or staking node id given is invalid or does not exist.
     */
    INVALID_STAKING_ID(ResponseCodeEnum.INVALID_STAKING_ID),

    /**
     * Native staking, while implemented, has not yet enabled by the council.
     */
    STAKING_NOT_ENABLED(ResponseCodeEnum.STAKING_NOT_ENABLED),

    /**
     * The range provided in UtilPrng transaction is negative.
     */
    INVALID_PRNG_RANGE(ResponseCodeEnum.INVALID_PRNG_RANGE),

    /**
     * The maximum number of entities allowed in the current price regime have been created.
     */
    MAX_ENTITIES_IN_PRICE_REGIME_HAVE_BEEN_CREATED(ResponseCodeEnum.MAX_ENTITIES_IN_PRICE_REGIME_HAVE_BEEN_CREATED),

    /**
     * The full prefix signature for precompile is not valid
     */
    INVALID_FULL_PREFIX_SIGNATURE_FOR_PRECOMPILE(ResponseCodeEnum.INVALID_FULL_PREFIX_SIGNATURE_FOR_PRECOMPILE),

    /**
     * The combined balances of a contract and its auto-renew account (if any) did not cover
     * the rent charged for net new storage used in a transaction.
     */
    INSUFFICIENT_BALANCES_FOR_STORAGE_RENT(ResponseCodeEnum.INSUFFICIENT_BALANCES_FOR_STORAGE_RENT),

    /**
     * A contract transaction tried to use more than the allowed number of child records, via
     * either system contract records or internal contract creations.
     */
    MAX_CHILD_RECORDS_EXCEEDED(ResponseCodeEnum.MAX_CHILD_RECORDS_EXCEEDED),

    /**
     * The combined balances of a contract and its auto-renew account (if any) or balance of an account did not cover
     * the auto-renewal fees in a transaction.
     */
    INSUFFICIENT_BALANCES_FOR_RENEWAL_FEES(ResponseCodeEnum.INSUFFICIENT_BALANCES_FOR_RENEWAL_FEES),

    /**
     * A transaction's protobuf message includes unknown fields; could mean that a client
     * expects not-yet-released functionality to be available.
     */
    TRANSACTION_HAS_UNKNOWN_FIELDS(ResponseCodeEnum.TRANSACTION_HAS_UNKNOWN_FIELDS),

    /**
     * The account cannot be modified. Account's key is not set
     */
    ACCOUNT_IS_IMMUTABLE(ResponseCodeEnum.ACCOUNT_IS_IMMUTABLE),

    /**
     * An alias that is assigned to an account or contract cannot be assigned to another account or contract.
     */
    ALIAS_ALREADY_ASSIGNED(ResponseCodeEnum.ALIAS_ALREADY_ASSIGNED),

    /**
     * A provided metadata key was invalid. Verification includes, for example, checking the size of Ed25519 and ECDSA(secp256k1) public keys.
     */
    INVALID_METADATA_KEY(ResponseCodeEnum.INVALID_METADATA_KEY),

    /**
     * Metadata key is not set on token
     */
    TOKEN_HAS_NO_METADATA_KEY(ResponseCodeEnum.TOKEN_HAS_NO_METADATA_KEY),

    /**
     * Token Metadata is not provided
     */
    MISSING_TOKEN_METADATA(ResponseCodeEnum.MISSING_TOKEN_METADATA),

    /**
     * NFT serial numbers are missing in the TokenUpdateNftsTransactionBody
     */
    MISSING_SERIAL_NUMBERS(ResponseCodeEnum.MISSING_SERIAL_NUMBERS),

    /**
     * Admin key is not set on token
     */
    TOKEN_HAS_NO_ADMIN_KEY(ResponseCodeEnum.TOKEN_HAS_NO_ADMIN_KEY),

    /**
     * A transaction failed because the consensus node identified is
     * deleted from the address book.
     */
    NODE_DELETED(ResponseCodeEnum.NODE_DELETED),

    /**
     * A transaction failed because the consensus node identified is not valid or
     * does not exist in state.
     */
    INVALID_NODE_ID(ResponseCodeEnum.INVALID_NODE_ID),

    /**
     * A transaction failed because one or more entries in the list of
     * service endpoints for the `gossip_endpoint` field is invalid.<br/>
     * The most common cause for this response is a service endpoint that has
     * the domain name (DNS) set rather than address and port.
     */
    INVALID_GOSSIP_ENDPOINT(ResponseCodeEnum.INVALID_GOSSIP_ENDPOINT),

    /**
     * A transaction failed because the node account identifier provided
     * does not exist or is not valid.<br/>
     * One common source of this error is providing a node account identifier
     * using the "alias" form rather than "numeric" form.
     * It is also used for atomic batch transaction for child transaction if the node account id is not 0.0.0.
     */
    INVALID_NODE_ACCOUNT_ID(ResponseCodeEnum.INVALID_NODE_ACCOUNT_ID),

    /**
     * A transaction failed because the description field cannot be encoded
     * as UTF-8 or is more than 100 bytes when encoded.
     */
    INVALID_NODE_DESCRIPTION(ResponseCodeEnum.INVALID_NODE_DESCRIPTION),

    /**
     * A transaction failed because one or more entries in the list of
     * service endpoints for the `service_endpoint` field is invalid.<br/>
     * The most common cause for this response is a service endpoint that has
     * the domain name (DNS) set rather than address and port.
     */
    INVALID_SERVICE_ENDPOINT(ResponseCodeEnum.INVALID_SERVICE_ENDPOINT),

    /**
     * A transaction failed because the TLS certificate provided for the
     * node is missing or invalid.
     * <p>
     * #### Probable Causes
     * The certificate MUST be a TLS certificate of a type permitted for gossip
     * signatures.<br/>
     * The value presented MUST be a UTF-8 NFKD encoding of the TLS
     * certificate.<br/>
     * The certificate encoded MUST be in PEM format.<br/>
     * The `gossip_ca_certificate` field is REQUIRED and MUST NOT be empty.
     */
    INVALID_GOSSIP_CA_CERTIFICATE(ResponseCodeEnum.INVALID_GOSSIP_CA_CERTIFICATE),

    /**
     * A transaction failed because the hash provided for the gRPC certificate
     * is present but invalid.
     * <p>
     * #### Probable Causes
     * The `grpc_certificate_hash` MUST be a SHA-384 hash.<br/>
     * The input hashed MUST be a UTF-8 NFKD encoding of the actual TLS
     * certificate.<br/>
     * The certificate to be encoded MUST be in PEM format.
     */
    INVALID_GRPC_CERTIFICATE(ResponseCodeEnum.INVALID_GRPC_CERTIFICATE),

    /**
     * The maximum automatic associations value is not valid.<br/>
     * The most common cause for this error is a value less than `-1`.
     */
    INVALID_MAX_AUTO_ASSOCIATIONS(ResponseCodeEnum.INVALID_MAX_AUTO_ASSOCIATIONS),

    /**
     * The maximum number of nodes allowed in the address book have been created.
     */
    MAX_NODES_CREATED(ResponseCodeEnum.MAX_NODES_CREATED),

    /**
     * In ServiceEndpoint, domain_name and ipAddressV4 are mutually exclusive
     */
    IP_FQDN_CANNOT_BE_SET_FOR_SAME_ENDPOINT(ResponseCodeEnum.IP_FQDN_CANNOT_BE_SET_FOR_SAME_ENDPOINT),

    /**
     * Fully qualified domain name is not allowed in gossip_endpoint
     */
    GOSSIP_ENDPOINT_CANNOT_HAVE_FQDN(ResponseCodeEnum.GOSSIP_ENDPOINT_CANNOT_HAVE_FQDN),

    /**
     * In ServiceEndpoint, domain_name size too large
     */
    FQDN_SIZE_TOO_LARGE(ResponseCodeEnum.FQDN_SIZE_TOO_LARGE),

    /**
     * ServiceEndpoint is invalid
     */
    INVALID_ENDPOINT(ResponseCodeEnum.INVALID_ENDPOINT),

    /**
     * The number of gossip endpoints exceeds the limit
     */
    GOSSIP_ENDPOINTS_EXCEEDED_LIMIT(ResponseCodeEnum.GOSSIP_ENDPOINTS_EXCEEDED_LIMIT),

    /**
     * The transaction attempted to use duplicate `TokenReference`.<br/>
     * This affects `TokenReject` attempting to reject same token reference more than once.
     */
    TOKEN_REFERENCE_REPEATED(ResponseCodeEnum.TOKEN_REFERENCE_REPEATED),

    /**
     * The account id specified as the owner in `TokenReject` is invalid or does not exist.
     */
    INVALID_OWNER_ID(ResponseCodeEnum.INVALID_OWNER_ID),

    /**
     * The transaction attempted to use more than the allowed number of `TokenReference`.
     */
    TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED(ResponseCodeEnum.TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED),

    /**
     * The number of service endpoints exceeds the limit
     */
    SERVICE_ENDPOINTS_EXCEEDED_LIMIT(ResponseCodeEnum.SERVICE_ENDPOINTS_EXCEEDED_LIMIT),

    /**
     * The IPv4 address is invalid
     */
    INVALID_IPV4_ADDRESS(ResponseCodeEnum.INVALID_IPV4_ADDRESS),

    /**
     * The transaction attempted to use empty `TokenReference` list.
     */
    EMPTY_TOKEN_REFERENCE_LIST(ResponseCodeEnum.EMPTY_TOKEN_REFERENCE_LIST),

    /**
     * The node account is not allowed to be updated
     */
    UPDATE_NODE_ACCOUNT_NOT_ALLOWED(ResponseCodeEnum.UPDATE_NODE_ACCOUNT_NOT_ALLOWED),

    /**
     * The token has no metadata or supply key
     */
    TOKEN_HAS_NO_METADATA_OR_SUPPLY_KEY(ResponseCodeEnum.TOKEN_HAS_NO_METADATA_OR_SUPPLY_KEY),

    /**
     * The list of `PendingAirdropId`s is empty and MUST NOT be empty.
     */
    EMPTY_PENDING_AIRDROP_ID_LIST(ResponseCodeEnum.EMPTY_PENDING_AIRDROP_ID_LIST),

    /**
     * A `PendingAirdropId` is repeated in a `claim` or `cancel` transaction.
     */
    PENDING_AIRDROP_ID_REPEATED(ResponseCodeEnum.PENDING_AIRDROP_ID_REPEATED),

    /**
     * The number of `PendingAirdropId` values in the list exceeds the maximum
     * allowable number.
     */
    PENDING_AIRDROP_ID_LIST_TOO_LONG(ResponseCodeEnum.PENDING_AIRDROP_ID_LIST_TOO_LONG),

    /**
     * A pending airdrop already exists for the specified NFT.
     */
    PENDING_NFT_AIRDROP_ALREADY_EXISTS(ResponseCodeEnum.PENDING_NFT_AIRDROP_ALREADY_EXISTS),

    /**
     * The identified account is sender for one or more pending airdrop(s)
     * and cannot be deleted.
     * <p>
     * The requester SHOULD cancel all pending airdrops before resending
     * this transaction.
     */
    ACCOUNT_HAS_PENDING_AIRDROPS(ResponseCodeEnum.ACCOUNT_HAS_PENDING_AIRDROPS),

    /**
     * Consensus throttle did not allow execution of this transaction.<br/>
     * The transaction should be retried after a modest delay.
     */
    THROTTLED_AT_CONSENSUS(ResponseCodeEnum.THROTTLED_AT_CONSENSUS),

    /**
     * The provided pending airdrop id is invalid.<br/>
     * This pending airdrop MAY already be claimed or cancelled.
     * <p>
     * The client SHOULD query a mirror node to determine the current status of
     * the pending airdrop.
     */
    INVALID_PENDING_AIRDROP_ID(ResponseCodeEnum.INVALID_PENDING_AIRDROP_ID),

    /**
     * The token to be airdropped has a fallback royalty fee and cannot be
     * sent or claimed via an airdrop transaction.
     */
    TOKEN_AIRDROP_WITH_FALLBACK_ROYALTY(ResponseCodeEnum.TOKEN_AIRDROP_WITH_FALLBACK_ROYALTY),

    /**
     * This airdrop claim is for a pending airdrop with an invalid token.<br/>
     * The token might be deleted, or the sender may not have enough tokens
     * to fulfill the offer.
     * <p>
     * The client SHOULD query mirror node to determine the status of the
     * pending airdrop and whether the sender can fulfill the offer.
     */
    INVALID_TOKEN_IN_PENDING_AIRDROP(ResponseCodeEnum.INVALID_TOKEN_IN_PENDING_AIRDROP),

    /**
     * A scheduled transaction configured to wait for expiry to execute was given
     * an expiry time at which there is already too many transactions scheduled to
     * expire; its creation must be retried with a different expiry.
     */
    SCHEDULE_EXPIRY_IS_BUSY(ResponseCodeEnum.SCHEDULE_EXPIRY_IS_BUSY),

    /**
     * The provided gRPC certificate hash is invalid.
     */
    INVALID_GRPC_CERTIFICATE_HASH(ResponseCodeEnum.INVALID_GRPC_CERTIFICATE_HASH),

    /**
     * A scheduled transaction configured to wait for expiry to execute was not
     * given an explicit expiration time.
     */
    MISSING_EXPIRY_TIME(ResponseCodeEnum.MISSING_EXPIRY_TIME),

    /**
     * A contract operation attempted to schedule another transaction after it
     * had already scheduled a recursive contract call.
     */
    NO_SCHEDULING_ALLOWED_AFTER_SCHEDULED_RECURSION(ResponseCodeEnum.NO_SCHEDULING_ALLOWED_AFTER_SCHEDULED_RECURSION),

    /**
     * A contract can schedule recursive calls a finite number of times (this is
     * approximately four million times with typical network configuration.)
     */
    RECURSIVE_SCHEDULING_LIMIT_REACHED(ResponseCodeEnum.RECURSIVE_SCHEDULING_LIMIT_REACHED),

    /**
     * The target network is waiting for the ledger ID to be set, which is a
     * side effect of finishing the network's TSS construction.
     */
    WAITING_FOR_LEDGER_ID(ResponseCodeEnum.WAITING_FOR_LEDGER_ID),

    /**
     * The provided fee exempt key list size exceeded the limit.
     */
    MAX_ENTRIES_FOR_FEE_EXEMPT_KEY_LIST_EXCEEDED(ResponseCodeEnum.MAX_ENTRIES_FOR_FEE_EXEMPT_KEY_LIST_EXCEEDED),

    /**
     * The provided fee exempt key list contains duplicated keys.
     */
    FEE_EXEMPT_KEY_LIST_CONTAINS_DUPLICATED_KEYS(ResponseCodeEnum.FEE_EXEMPT_KEY_LIST_CONTAINS_DUPLICATED_KEYS),

    /**
     * The provided fee exempt key list contains an invalid key.
     */
    INVALID_KEY_IN_FEE_EXEMPT_KEY_LIST(ResponseCodeEnum.INVALID_KEY_IN_FEE_EXEMPT_KEY_LIST),

    /**
     * The provided fee schedule key contains an invalid key.
     */
    INVALID_FEE_SCHEDULE_KEY(ResponseCodeEnum.INVALID_FEE_SCHEDULE_KEY),

    /**
     * If a fee schedule key is not set when we create a topic
     * we cannot add it on update.
     */
    FEE_SCHEDULE_KEY_CANNOT_BE_UPDATED(ResponseCodeEnum.FEE_SCHEDULE_KEY_CANNOT_BE_UPDATED),

    /**
     * If the topic's custom fees are updated the topic SHOULD have a
     * fee schedule key
     */
    FEE_SCHEDULE_KEY_NOT_SET(ResponseCodeEnum.FEE_SCHEDULE_KEY_NOT_SET),

    /**
     * The fee amount is exceeding the amount that the payer
     * is willing to pay.
     */
    MAX_CUSTOM_FEE_LIMIT_EXCEEDED(ResponseCodeEnum.MAX_CUSTOM_FEE_LIMIT_EXCEEDED),

    /**
     * There are no corresponding custom fees.
     */
    NO_VALID_MAX_CUSTOM_FEE(ResponseCodeEnum.NO_VALID_MAX_CUSTOM_FEE),

    /**
     * The provided list contains invalid max custom fee.
     */
    INVALID_MAX_CUSTOM_FEES(ResponseCodeEnum.INVALID_MAX_CUSTOM_FEES),

    /**
     * The provided max custom fee list contains fees with
     * duplicate denominations.
     */
    DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST(ResponseCodeEnum.DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST),

    /**
     * The provided max custom fee list contains fees with
     * duplicate account id.
     */
    DUPLICATE_ACCOUNT_ID_IN_MAX_CUSTOM_FEE_LIST(ResponseCodeEnum.DUPLICATE_ACCOUNT_ID_IN_MAX_CUSTOM_FEE_LIST),

    /**
     * Max custom fees list is not supported for this operation.
     */
    MAX_CUSTOM_FEES_IS_NOT_SUPPORTED(ResponseCodeEnum.MAX_CUSTOM_FEES_IS_NOT_SUPPORTED),

    /**
     * The list of batch transactions is empty
     */
    BATCH_LIST_EMPTY(ResponseCodeEnum.BATCH_LIST_EMPTY),

    /**
     * The list of batch transactions contains duplicated transactions
     */
    BATCH_LIST_CONTAINS_DUPLICATES(ResponseCodeEnum.BATCH_LIST_CONTAINS_DUPLICATES),

    /**
     * The list of batch transactions contains a transaction type that is
     * in the AtomicBatch blacklist as configured in the network.
     */
    BATCH_TRANSACTION_IN_BLACKLIST(ResponseCodeEnum.BATCH_TRANSACTION_IN_BLACKLIST),

    /**
     * The inner transaction of a batch transaction failed
     */
    INNER_TRANSACTION_FAILED(ResponseCodeEnum.INNER_TRANSACTION_FAILED),

    /**
     * The inner transaction of a batch transaction is missing a batch key
     */
    MISSING_BATCH_KEY(ResponseCodeEnum.MISSING_BATCH_KEY),

    /**
     * The batch key is set for a non batch transaction
     */
    BATCH_KEY_SET_ON_NON_INNER_TRANSACTION(ResponseCodeEnum.BATCH_KEY_SET_ON_NON_INNER_TRANSACTION),

    /**
     * The batch key is not valid
     */
    INVALID_BATCH_KEY(ResponseCodeEnum.INVALID_BATCH_KEY),

    /**
     * The provided schedule expiry time is not configurable.
     */
    SCHEDULE_EXPIRY_NOT_CONFIGURABLE(ResponseCodeEnum.SCHEDULE_EXPIRY_NOT_CONFIGURABLE),

    /**
     * The network just started at genesis and is creating system entities.
     */
    CREATING_SYSTEM_ENTITIES(ResponseCodeEnum.CREATING_SYSTEM_ENTITIES),

    /**
     * The least common multiple of the throttle group's milliOpsPerSec is
     * too large and it's overflowing.
     */
    THROTTLE_GROUP_LCM_OVERFLOW(ResponseCodeEnum.THROTTLE_GROUP_LCM_OVERFLOW),

    /**
     * Token airdrop transactions can not contain multiple senders for a single token.
     */
    AIRDROP_CONTAINS_MULTIPLE_SENDERS_FOR_A_TOKEN(ResponseCodeEnum.AIRDROP_CONTAINS_MULTIPLE_SENDERS_FOR_A_TOKEN),

    /**
     * The GRPC proxy endpoint is set in the NodeCreate or NodeUpdate transaction,
     * which the network does not support.
     */
    GRPC_WEB_PROXY_NOT_SUPPORTED(ResponseCodeEnum.GRPC_WEB_PROXY_NOT_SUPPORTED);

    final ResponseCodeEnum code;

    Status(ResponseCodeEnum code) {
        this.code = code;
    }

    static Status valueOf(ResponseCodeEnum code) {
        return switch (code) {
            case OK -> OK;
            case INVALID_TRANSACTION -> INVALID_TRANSACTION;
            case PAYER_ACCOUNT_NOT_FOUND -> PAYER_ACCOUNT_NOT_FOUND;
            case INVALID_NODE_ACCOUNT -> INVALID_NODE_ACCOUNT;
            case TRANSACTION_EXPIRED -> TRANSACTION_EXPIRED;
            case INVALID_TRANSACTION_START -> INVALID_TRANSACTION_START;
            case INVALID_TRANSACTION_DURATION -> INVALID_TRANSACTION_DURATION;
            case INVALID_SIGNATURE -> INVALID_SIGNATURE;
            case MEMO_TOO_LONG -> MEMO_TOO_LONG;
            case INSUFFICIENT_TX_FEE -> INSUFFICIENT_TX_FEE;
            case INSUFFICIENT_PAYER_BALANCE -> INSUFFICIENT_PAYER_BALANCE;
            case DUPLICATE_TRANSACTION -> DUPLICATE_TRANSACTION;
            case BUSY -> BUSY;
            case NOT_SUPPORTED -> NOT_SUPPORTED;
            case INVALID_FILE_ID -> INVALID_FILE_ID;
            case INVALID_ACCOUNT_ID -> INVALID_ACCOUNT_ID;
            case INVALID_CONTRACT_ID -> INVALID_CONTRACT_ID;
            case INVALID_TRANSACTION_ID -> INVALID_TRANSACTION_ID;
            case RECEIPT_NOT_FOUND -> RECEIPT_NOT_FOUND;
            case RECORD_NOT_FOUND -> RECORD_NOT_FOUND;
            case INVALID_SOLIDITY_ID -> INVALID_SOLIDITY_ID;
            case UNKNOWN -> UNKNOWN;
            case SUCCESS -> SUCCESS;
            case FAIL_INVALID -> FAIL_INVALID;
            case FAIL_FEE -> FAIL_FEE;
            case FAIL_BALANCE -> FAIL_BALANCE;
            case KEY_REQUIRED -> KEY_REQUIRED;
            case BAD_ENCODING -> BAD_ENCODING;
            case INSUFFICIENT_ACCOUNT_BALANCE -> INSUFFICIENT_ACCOUNT_BALANCE;
            case INVALID_SOLIDITY_ADDRESS -> INVALID_SOLIDITY_ADDRESS;
            case INSUFFICIENT_GAS -> INSUFFICIENT_GAS;
            case CONTRACT_SIZE_LIMIT_EXCEEDED -> CONTRACT_SIZE_LIMIT_EXCEEDED;
            case LOCAL_CALL_MODIFICATION_EXCEPTION -> LOCAL_CALL_MODIFICATION_EXCEPTION;
            case CONTRACT_REVERT_EXECUTED -> CONTRACT_REVERT_EXECUTED;
            case CONTRACT_EXECUTION_EXCEPTION -> CONTRACT_EXECUTION_EXCEPTION;
            case INVALID_RECEIVING_NODE_ACCOUNT -> INVALID_RECEIVING_NODE_ACCOUNT;
            case MISSING_QUERY_HEADER -> MISSING_QUERY_HEADER;
            case ACCOUNT_UPDATE_FAILED -> ACCOUNT_UPDATE_FAILED;
            case INVALID_KEY_ENCODING -> INVALID_KEY_ENCODING;
            case NULL_SOLIDITY_ADDRESS -> NULL_SOLIDITY_ADDRESS;
            case CONTRACT_UPDATE_FAILED -> CONTRACT_UPDATE_FAILED;
            case INVALID_QUERY_HEADER -> INVALID_QUERY_HEADER;
            case INVALID_FEE_SUBMITTED -> INVALID_FEE_SUBMITTED;
            case INVALID_PAYER_SIGNATURE -> INVALID_PAYER_SIGNATURE;
            case KEY_NOT_PROVIDED -> KEY_NOT_PROVIDED;
            case INVALID_EXPIRATION_TIME -> INVALID_EXPIRATION_TIME;
            case NO_WACL_KEY -> NO_WACL_KEY;
            case FILE_CONTENT_EMPTY -> FILE_CONTENT_EMPTY;
            case INVALID_ACCOUNT_AMOUNTS -> INVALID_ACCOUNT_AMOUNTS;
            case EMPTY_TRANSACTION_BODY -> EMPTY_TRANSACTION_BODY;
            case INVALID_TRANSACTION_BODY -> INVALID_TRANSACTION_BODY;
            case INVALID_SIGNATURE_TYPE_MISMATCHING_KEY -> INVALID_SIGNATURE_TYPE_MISMATCHING_KEY;
            case INVALID_SIGNATURE_COUNT_MISMATCHING_KEY -> INVALID_SIGNATURE_COUNT_MISMATCHING_KEY;
            case EMPTY_LIVE_HASH_BODY -> EMPTY_LIVE_HASH_BODY;
            case EMPTY_LIVE_HASH -> EMPTY_LIVE_HASH;
            case EMPTY_LIVE_HASH_KEYS -> EMPTY_LIVE_HASH_KEYS;
            case INVALID_LIVE_HASH_SIZE -> INVALID_LIVE_HASH_SIZE;
            case EMPTY_QUERY_BODY -> EMPTY_QUERY_BODY;
            case EMPTY_LIVE_HASH_QUERY -> EMPTY_LIVE_HASH_QUERY;
            case LIVE_HASH_NOT_FOUND -> LIVE_HASH_NOT_FOUND;
            case ACCOUNT_ID_DOES_NOT_EXIST -> ACCOUNT_ID_DOES_NOT_EXIST;
            case LIVE_HASH_ALREADY_EXISTS -> LIVE_HASH_ALREADY_EXISTS;
            case INVALID_FILE_WACL -> INVALID_FILE_WACL;
            case SERIALIZATION_FAILED -> SERIALIZATION_FAILED;
            case TRANSACTION_OVERSIZE -> TRANSACTION_OVERSIZE;
            case TRANSACTION_TOO_MANY_LAYERS -> TRANSACTION_TOO_MANY_LAYERS;
            case CONTRACT_DELETED -> CONTRACT_DELETED;
            case PLATFORM_NOT_ACTIVE -> PLATFORM_NOT_ACTIVE;
            case KEY_PREFIX_MISMATCH -> KEY_PREFIX_MISMATCH;
            case PLATFORM_TRANSACTION_NOT_CREATED -> PLATFORM_TRANSACTION_NOT_CREATED;
            case INVALID_RENEWAL_PERIOD -> INVALID_RENEWAL_PERIOD;
            case INVALID_PAYER_ACCOUNT_ID -> INVALID_PAYER_ACCOUNT_ID;
            case ACCOUNT_DELETED -> ACCOUNT_DELETED;
            case FILE_DELETED -> FILE_DELETED;
            case ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS -> ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS;
            case SETTING_NEGATIVE_ACCOUNT_BALANCE -> SETTING_NEGATIVE_ACCOUNT_BALANCE;
            case OBTAINER_REQUIRED -> OBTAINER_REQUIRED;
            case OBTAINER_SAME_CONTRACT_ID -> OBTAINER_SAME_CONTRACT_ID;
            case OBTAINER_DOES_NOT_EXIST -> OBTAINER_DOES_NOT_EXIST;
            case MODIFYING_IMMUTABLE_CONTRACT -> MODIFYING_IMMUTABLE_CONTRACT;
            case FILE_SYSTEM_EXCEPTION -> FILE_SYSTEM_EXCEPTION;
            case AUTORENEW_DURATION_NOT_IN_RANGE -> AUTORENEW_DURATION_NOT_IN_RANGE;
            case ERROR_DECODING_BYTESTRING -> ERROR_DECODING_BYTESTRING;
            case CONTRACT_FILE_EMPTY -> CONTRACT_FILE_EMPTY;
            case CONTRACT_BYTECODE_EMPTY -> CONTRACT_BYTECODE_EMPTY;
            case INVALID_INITIAL_BALANCE -> INVALID_INITIAL_BALANCE;
            case INVALID_RECEIVE_RECORD_THRESHOLD -> INVALID_RECEIVE_RECORD_THRESHOLD;
            case INVALID_SEND_RECORD_THRESHOLD -> INVALID_SEND_RECORD_THRESHOLD;
            case ACCOUNT_IS_NOT_GENESIS_ACCOUNT -> ACCOUNT_IS_NOT_GENESIS_ACCOUNT;
            case PAYER_ACCOUNT_UNAUTHORIZED -> PAYER_ACCOUNT_UNAUTHORIZED;
            case INVALID_FREEZE_TRANSACTION_BODY -> INVALID_FREEZE_TRANSACTION_BODY;
            case FREEZE_TRANSACTION_BODY_NOT_FOUND -> FREEZE_TRANSACTION_BODY_NOT_FOUND;
            case TRANSFER_LIST_SIZE_LIMIT_EXCEEDED -> TRANSFER_LIST_SIZE_LIMIT_EXCEEDED;
            case RESULT_SIZE_LIMIT_EXCEEDED -> RESULT_SIZE_LIMIT_EXCEEDED;
            case NOT_SPECIAL_ACCOUNT -> NOT_SPECIAL_ACCOUNT;
            case CONTRACT_NEGATIVE_GAS -> CONTRACT_NEGATIVE_GAS;
            case CONTRACT_NEGATIVE_VALUE -> CONTRACT_NEGATIVE_VALUE;
            case INVALID_FEE_FILE -> INVALID_FEE_FILE;
            case INVALID_EXCHANGE_RATE_FILE -> INVALID_EXCHANGE_RATE_FILE;
            case INSUFFICIENT_LOCAL_CALL_GAS -> INSUFFICIENT_LOCAL_CALL_GAS;
            case ENTITY_NOT_ALLOWED_TO_DELETE -> ENTITY_NOT_ALLOWED_TO_DELETE;
            case AUTHORIZATION_FAILED -> AUTHORIZATION_FAILED;
            case FILE_UPLOADED_PROTO_INVALID -> FILE_UPLOADED_PROTO_INVALID;
            case FILE_UPLOADED_PROTO_NOT_SAVED_TO_DISK -> FILE_UPLOADED_PROTO_NOT_SAVED_TO_DISK;
            case FEE_SCHEDULE_FILE_PART_UPLOADED -> FEE_SCHEDULE_FILE_PART_UPLOADED;
            case EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED -> EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED;
            case MAX_CONTRACT_STORAGE_EXCEEDED -> MAX_CONTRACT_STORAGE_EXCEEDED;
            case TRANSFER_ACCOUNT_SAME_AS_DELETE_ACCOUNT -> TRANSFER_ACCOUNT_SAME_AS_DELETE_ACCOUNT;
            case TOTAL_LEDGER_BALANCE_INVALID -> TOTAL_LEDGER_BALANCE_INVALID;
            case EXPIRATION_REDUCTION_NOT_ALLOWED -> EXPIRATION_REDUCTION_NOT_ALLOWED;
            case MAX_GAS_LIMIT_EXCEEDED -> MAX_GAS_LIMIT_EXCEEDED;
            case MAX_FILE_SIZE_EXCEEDED -> MAX_FILE_SIZE_EXCEEDED;
            case RECEIVER_SIG_REQUIRED -> RECEIVER_SIG_REQUIRED;
            case INVALID_TOPIC_ID -> INVALID_TOPIC_ID;
            case INVALID_ADMIN_KEY -> INVALID_ADMIN_KEY;
            case INVALID_SUBMIT_KEY -> INVALID_SUBMIT_KEY;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case INVALID_TOPIC_MESSAGE -> INVALID_TOPIC_MESSAGE;
            case INVALID_AUTORENEW_ACCOUNT -> INVALID_AUTORENEW_ACCOUNT;
            case AUTORENEW_ACCOUNT_NOT_ALLOWED -> AUTORENEW_ACCOUNT_NOT_ALLOWED;
            case TOPIC_EXPIRED -> TOPIC_EXPIRED;
            case INVALID_CHUNK_NUMBER -> INVALID_CHUNK_NUMBER;
            case INVALID_CHUNK_TRANSACTION_ID -> INVALID_CHUNK_TRANSACTION_ID;
            case ACCOUNT_FROZEN_FOR_TOKEN -> ACCOUNT_FROZEN_FOR_TOKEN;
            case TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED -> TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED;
            case INVALID_TOKEN_ID -> INVALID_TOKEN_ID;
            case INVALID_TOKEN_DECIMALS -> INVALID_TOKEN_DECIMALS;
            case INVALID_TOKEN_INITIAL_SUPPLY -> INVALID_TOKEN_INITIAL_SUPPLY;
            case INVALID_TREASURY_ACCOUNT_FOR_TOKEN -> INVALID_TREASURY_ACCOUNT_FOR_TOKEN;
            case INVALID_TOKEN_SYMBOL -> INVALID_TOKEN_SYMBOL;
            case TOKEN_HAS_NO_FREEZE_KEY -> TOKEN_HAS_NO_FREEZE_KEY;
            case TRANSFERS_NOT_ZERO_SUM_FOR_TOKEN -> TRANSFERS_NOT_ZERO_SUM_FOR_TOKEN;
            case MISSING_TOKEN_SYMBOL -> MISSING_TOKEN_SYMBOL;
            case TOKEN_SYMBOL_TOO_LONG -> TOKEN_SYMBOL_TOO_LONG;
            case ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN -> ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN;
            case TOKEN_HAS_NO_KYC_KEY -> TOKEN_HAS_NO_KYC_KEY;
            case INSUFFICIENT_TOKEN_BALANCE -> INSUFFICIENT_TOKEN_BALANCE;
            case TOKEN_WAS_DELETED -> TOKEN_WAS_DELETED;
            case TOKEN_HAS_NO_SUPPLY_KEY -> TOKEN_HAS_NO_SUPPLY_KEY;
            case TOKEN_HAS_NO_WIPE_KEY -> TOKEN_HAS_NO_WIPE_KEY;
            case INVALID_TOKEN_MINT_AMOUNT -> INVALID_TOKEN_MINT_AMOUNT;
            case INVALID_TOKEN_BURN_AMOUNT -> INVALID_TOKEN_BURN_AMOUNT;
            case TOKEN_NOT_ASSOCIATED_TO_ACCOUNT -> TOKEN_NOT_ASSOCIATED_TO_ACCOUNT;
            case CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT -> CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT;
            case INVALID_KYC_KEY -> INVALID_KYC_KEY;
            case INVALID_WIPE_KEY -> INVALID_WIPE_KEY;
            case INVALID_FREEZE_KEY -> INVALID_FREEZE_KEY;
            case INVALID_SUPPLY_KEY -> INVALID_SUPPLY_KEY;
            case MISSING_TOKEN_NAME -> MISSING_TOKEN_NAME;
            case TOKEN_NAME_TOO_LONG -> TOKEN_NAME_TOO_LONG;
            case INVALID_WIPING_AMOUNT -> INVALID_WIPING_AMOUNT;
            case TOKEN_IS_IMMUTABLE -> TOKEN_IS_IMMUTABLE;
            case TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT -> TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT;
            case TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES -> TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES;
            case ACCOUNT_IS_TREASURY -> ACCOUNT_IS_TREASURY;
            case TOKEN_ID_REPEATED_IN_TOKEN_LIST -> TOKEN_ID_REPEATED_IN_TOKEN_LIST;
            case TOKEN_TRANSFER_LIST_SIZE_LIMIT_EXCEEDED -> TOKEN_TRANSFER_LIST_SIZE_LIMIT_EXCEEDED;
            case EMPTY_TOKEN_TRANSFER_BODY -> EMPTY_TOKEN_TRANSFER_BODY;
            case EMPTY_TOKEN_TRANSFER_ACCOUNT_AMOUNTS -> EMPTY_TOKEN_TRANSFER_ACCOUNT_AMOUNTS;
            case INVALID_SCHEDULE_ID -> INVALID_SCHEDULE_ID;
            case SCHEDULE_IS_IMMUTABLE -> SCHEDULE_IS_IMMUTABLE;
            case INVALID_SCHEDULE_PAYER_ID -> INVALID_SCHEDULE_PAYER_ID;
            case INVALID_SCHEDULE_ACCOUNT_ID -> INVALID_SCHEDULE_ACCOUNT_ID;
            case NO_NEW_VALID_SIGNATURES -> NO_NEW_VALID_SIGNATURES;
            case UNRESOLVABLE_REQUIRED_SIGNERS -> UNRESOLVABLE_REQUIRED_SIGNERS;
            case SCHEDULED_TRANSACTION_NOT_IN_WHITELIST -> SCHEDULED_TRANSACTION_NOT_IN_WHITELIST;
            case SOME_SIGNATURES_WERE_INVALID -> SOME_SIGNATURES_WERE_INVALID;
            case TRANSACTION_ID_FIELD_NOT_ALLOWED -> TRANSACTION_ID_FIELD_NOT_ALLOWED;
            case IDENTICAL_SCHEDULE_ALREADY_CREATED -> IDENTICAL_SCHEDULE_ALREADY_CREATED;
            case INVALID_ZERO_BYTE_IN_STRING -> INVALID_ZERO_BYTE_IN_STRING;
            case SCHEDULE_ALREADY_DELETED -> SCHEDULE_ALREADY_DELETED;
            case SCHEDULE_ALREADY_EXECUTED -> SCHEDULE_ALREADY_EXECUTED;
            case MESSAGE_SIZE_TOO_LARGE -> MESSAGE_SIZE_TOO_LARGE;
            case OPERATION_REPEATED_IN_BUCKET_GROUPS -> OPERATION_REPEATED_IN_BUCKET_GROUPS;
            case BUCKET_CAPACITY_OVERFLOW -> BUCKET_CAPACITY_OVERFLOW;
            case NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION -> NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION;
            case BUCKET_HAS_NO_THROTTLE_GROUPS -> BUCKET_HAS_NO_THROTTLE_GROUPS;
            case THROTTLE_GROUP_HAS_ZERO_OPS_PER_SEC -> THROTTLE_GROUP_HAS_ZERO_OPS_PER_SEC;
            case SUCCESS_BUT_MISSING_EXPECTED_OPERATION -> SUCCESS_BUT_MISSING_EXPECTED_OPERATION;
            case UNPARSEABLE_THROTTLE_DEFINITIONS -> UNPARSEABLE_THROTTLE_DEFINITIONS;
            case INVALID_THROTTLE_DEFINITIONS -> INVALID_THROTTLE_DEFINITIONS;
            case ACCOUNT_EXPIRED_AND_PENDING_REMOVAL -> ACCOUNT_EXPIRED_AND_PENDING_REMOVAL;
            case INVALID_TOKEN_MAX_SUPPLY -> INVALID_TOKEN_MAX_SUPPLY;
            case INVALID_TOKEN_NFT_SERIAL_NUMBER -> INVALID_TOKEN_NFT_SERIAL_NUMBER;
            case INVALID_NFT_ID -> INVALID_NFT_ID;
            case METADATA_TOO_LONG -> METADATA_TOO_LONG;
            case BATCH_SIZE_LIMIT_EXCEEDED -> BATCH_SIZE_LIMIT_EXCEEDED;
            case INVALID_QUERY_RANGE -> INVALID_QUERY_RANGE;
            case FRACTION_DIVIDES_BY_ZERO -> FRACTION_DIVIDES_BY_ZERO;
            case INSUFFICIENT_PAYER_BALANCE_FOR_CUSTOM_FEE -> INSUFFICIENT_PAYER_BALANCE_FOR_CUSTOM_FEE;
            case CUSTOM_FEES_LIST_TOO_LONG -> CUSTOM_FEES_LIST_TOO_LONG;
            case INVALID_CUSTOM_FEE_COLLECTOR -> INVALID_CUSTOM_FEE_COLLECTOR;
            case INVALID_TOKEN_ID_IN_CUSTOM_FEES -> INVALID_TOKEN_ID_IN_CUSTOM_FEES;
            case TOKEN_NOT_ASSOCIATED_TO_FEE_COLLECTOR -> TOKEN_NOT_ASSOCIATED_TO_FEE_COLLECTOR;
            case TOKEN_MAX_SUPPLY_REACHED -> TOKEN_MAX_SUPPLY_REACHED;
            case SENDER_DOES_NOT_OWN_NFT_SERIAL_NO -> SENDER_DOES_NOT_OWN_NFT_SERIAL_NO;
            case CUSTOM_FEE_NOT_FULLY_SPECIFIED -> CUSTOM_FEE_NOT_FULLY_SPECIFIED;
            case CUSTOM_FEE_MUST_BE_POSITIVE -> CUSTOM_FEE_MUST_BE_POSITIVE;
            case TOKEN_HAS_NO_FEE_SCHEDULE_KEY -> TOKEN_HAS_NO_FEE_SCHEDULE_KEY;
            case CUSTOM_FEE_OUTSIDE_NUMERIC_RANGE -> CUSTOM_FEE_OUTSIDE_NUMERIC_RANGE;
            case ROYALTY_FRACTION_CANNOT_EXCEED_ONE -> ROYALTY_FRACTION_CANNOT_EXCEED_ONE;
            case FRACTIONAL_FEE_MAX_AMOUNT_LESS_THAN_MIN_AMOUNT -> FRACTIONAL_FEE_MAX_AMOUNT_LESS_THAN_MIN_AMOUNT;
            case CUSTOM_SCHEDULE_ALREADY_HAS_NO_FEES -> CUSTOM_SCHEDULE_ALREADY_HAS_NO_FEES;
            case CUSTOM_FEE_DENOMINATION_MUST_BE_FUNGIBLE_COMMON -> CUSTOM_FEE_DENOMINATION_MUST_BE_FUNGIBLE_COMMON;
            case CUSTOM_FRACTIONAL_FEE_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON -> CUSTOM_FRACTIONAL_FEE_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON;
            case INVALID_CUSTOM_FEE_SCHEDULE_KEY -> INVALID_CUSTOM_FEE_SCHEDULE_KEY;
            case INVALID_TOKEN_MINT_METADATA -> INVALID_TOKEN_MINT_METADATA;
            case INVALID_TOKEN_BURN_METADATA -> INVALID_TOKEN_BURN_METADATA;
            case CURRENT_TREASURY_STILL_OWNS_NFTS -> CURRENT_TREASURY_STILL_OWNS_NFTS;
            case ACCOUNT_STILL_OWNS_NFTS -> ACCOUNT_STILL_OWNS_NFTS;
            case TREASURY_MUST_OWN_BURNED_NFT -> TREASURY_MUST_OWN_BURNED_NFT;
            case ACCOUNT_DOES_NOT_OWN_WIPED_NFT -> ACCOUNT_DOES_NOT_OWN_WIPED_NFT;
            case ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON -> ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON;
            case MAX_NFTS_IN_PRICE_REGIME_HAVE_BEEN_MINTED -> MAX_NFTS_IN_PRICE_REGIME_HAVE_BEEN_MINTED;
            case PAYER_ACCOUNT_DELETED -> PAYER_ACCOUNT_DELETED;
            case CUSTOM_FEE_CHARGING_EXCEEDED_MAX_RECURSION_DEPTH -> CUSTOM_FEE_CHARGING_EXCEEDED_MAX_RECURSION_DEPTH;
            case CUSTOM_FEE_CHARGING_EXCEEDED_MAX_ACCOUNT_AMOUNTS -> CUSTOM_FEE_CHARGING_EXCEEDED_MAX_ACCOUNT_AMOUNTS;
            case INSUFFICIENT_SENDER_ACCOUNT_BALANCE_FOR_CUSTOM_FEE -> INSUFFICIENT_SENDER_ACCOUNT_BALANCE_FOR_CUSTOM_FEE;
            case SERIAL_NUMBER_LIMIT_REACHED -> SERIAL_NUMBER_LIMIT_REACHED;
            case CUSTOM_ROYALTY_FEE_ONLY_ALLOWED_FOR_NON_FUNGIBLE_UNIQUE -> CUSTOM_ROYALTY_FEE_ONLY_ALLOWED_FOR_NON_FUNGIBLE_UNIQUE;
            case NO_REMAINING_AUTOMATIC_ASSOCIATIONS -> NO_REMAINING_AUTOMATIC_ASSOCIATIONS;
            case EXISTING_AUTOMATIC_ASSOCIATIONS_EXCEED_GIVEN_LIMIT -> EXISTING_AUTOMATIC_ASSOCIATIONS_EXCEED_GIVEN_LIMIT;
            case REQUESTED_NUM_AUTOMATIC_ASSOCIATIONS_EXCEEDS_ASSOCIATION_LIMIT -> REQUESTED_NUM_AUTOMATIC_ASSOCIATIONS_EXCEEDS_ASSOCIATION_LIMIT;
            case TOKEN_IS_PAUSED -> TOKEN_IS_PAUSED;
            case TOKEN_HAS_NO_PAUSE_KEY -> TOKEN_HAS_NO_PAUSE_KEY;
            case INVALID_PAUSE_KEY -> INVALID_PAUSE_KEY;
            case FREEZE_UPDATE_FILE_DOES_NOT_EXIST -> FREEZE_UPDATE_FILE_DOES_NOT_EXIST;
            case FREEZE_UPDATE_FILE_HASH_DOES_NOT_MATCH -> FREEZE_UPDATE_FILE_HASH_DOES_NOT_MATCH;
            case NO_UPGRADE_HAS_BEEN_PREPARED -> NO_UPGRADE_HAS_BEEN_PREPARED;
            case NO_FREEZE_IS_SCHEDULED -> NO_FREEZE_IS_SCHEDULED;
            case UPDATE_FILE_HASH_CHANGED_SINCE_PREPARE_UPGRADE -> UPDATE_FILE_HASH_CHANGED_SINCE_PREPARE_UPGRADE;
            case FREEZE_START_TIME_MUST_BE_FUTURE -> FREEZE_START_TIME_MUST_BE_FUTURE;
            case PREPARED_UPDATE_FILE_IS_IMMUTABLE -> PREPARED_UPDATE_FILE_IS_IMMUTABLE;
            case FREEZE_ALREADY_SCHEDULED -> FREEZE_ALREADY_SCHEDULED;
            case FREEZE_UPGRADE_IN_PROGRESS -> FREEZE_UPGRADE_IN_PROGRESS;
            case UPDATE_FILE_ID_DOES_NOT_MATCH_PREPARED -> UPDATE_FILE_ID_DOES_NOT_MATCH_PREPARED;
            case UPDATE_FILE_HASH_DOES_NOT_MATCH_PREPARED -> UPDATE_FILE_HASH_DOES_NOT_MATCH_PREPARED;
            case CONSENSUS_GAS_EXHAUSTED -> CONSENSUS_GAS_EXHAUSTED;
            case REVERTED_SUCCESS -> REVERTED_SUCCESS;
            case MAX_STORAGE_IN_PRICE_REGIME_HAS_BEEN_USED -> MAX_STORAGE_IN_PRICE_REGIME_HAS_BEEN_USED;
            case INVALID_ALIAS_KEY -> INVALID_ALIAS_KEY;
            case UNEXPECTED_TOKEN_DECIMALS -> UNEXPECTED_TOKEN_DECIMALS;
            case INVALID_PROXY_ACCOUNT_ID -> INVALID_PROXY_ACCOUNT_ID;
            case INVALID_TRANSFER_ACCOUNT_ID -> INVALID_TRANSFER_ACCOUNT_ID;
            case INVALID_FEE_COLLECTOR_ACCOUNT_ID -> INVALID_FEE_COLLECTOR_ACCOUNT_ID;
            case ALIAS_IS_IMMUTABLE -> ALIAS_IS_IMMUTABLE;
            case SPENDER_ACCOUNT_SAME_AS_OWNER -> SPENDER_ACCOUNT_SAME_AS_OWNER;
            case AMOUNT_EXCEEDS_TOKEN_MAX_SUPPLY -> AMOUNT_EXCEEDS_TOKEN_MAX_SUPPLY;
            case NEGATIVE_ALLOWANCE_AMOUNT -> NEGATIVE_ALLOWANCE_AMOUNT;
            case CANNOT_APPROVE_FOR_ALL_FUNGIBLE_COMMON -> CANNOT_APPROVE_FOR_ALL_FUNGIBLE_COMMON;
            case SPENDER_DOES_NOT_HAVE_ALLOWANCE -> SPENDER_DOES_NOT_HAVE_ALLOWANCE;
            case AMOUNT_EXCEEDS_ALLOWANCE -> AMOUNT_EXCEEDS_ALLOWANCE;
            case MAX_ALLOWANCES_EXCEEDED -> MAX_ALLOWANCES_EXCEEDED;
            case EMPTY_ALLOWANCES -> EMPTY_ALLOWANCES;
            case SPENDER_ACCOUNT_REPEATED_IN_ALLOWANCES -> SPENDER_ACCOUNT_REPEATED_IN_ALLOWANCES;
            case REPEATED_SERIAL_NUMS_IN_NFT_ALLOWANCES -> REPEATED_SERIAL_NUMS_IN_NFT_ALLOWANCES;
            case FUNGIBLE_TOKEN_IN_NFT_ALLOWANCES -> FUNGIBLE_TOKEN_IN_NFT_ALLOWANCES;
            case NFT_IN_FUNGIBLE_TOKEN_ALLOWANCES -> NFT_IN_FUNGIBLE_TOKEN_ALLOWANCES;
            case INVALID_ALLOWANCE_OWNER_ID -> INVALID_ALLOWANCE_OWNER_ID;
            case INVALID_ALLOWANCE_SPENDER_ID -> INVALID_ALLOWANCE_SPENDER_ID;
            case REPEATED_ALLOWANCES_TO_DELETE -> REPEATED_ALLOWANCES_TO_DELETE;
            case INVALID_DELEGATING_SPENDER -> INVALID_DELEGATING_SPENDER;
            case DELEGATING_SPENDER_CANNOT_GRANT_APPROVE_FOR_ALL -> DELEGATING_SPENDER_CANNOT_GRANT_APPROVE_FOR_ALL;
            case DELEGATING_SPENDER_DOES_NOT_HAVE_APPROVE_FOR_ALL -> DELEGATING_SPENDER_DOES_NOT_HAVE_APPROVE_FOR_ALL;
            case SCHEDULE_EXPIRATION_TIME_TOO_FAR_IN_FUTURE -> SCHEDULE_EXPIRATION_TIME_TOO_FAR_IN_FUTURE;
            case SCHEDULE_EXPIRATION_TIME_MUST_BE_HIGHER_THAN_CONSENSUS_TIME -> SCHEDULE_EXPIRATION_TIME_MUST_BE_HIGHER_THAN_CONSENSUS_TIME;
            case SCHEDULE_FUTURE_THROTTLE_EXCEEDED -> SCHEDULE_FUTURE_THROTTLE_EXCEEDED;
            case SCHEDULE_FUTURE_GAS_LIMIT_EXCEEDED -> SCHEDULE_FUTURE_GAS_LIMIT_EXCEEDED;
            case INVALID_ETHEREUM_TRANSACTION -> INVALID_ETHEREUM_TRANSACTION;
            case WRONG_CHAIN_ID -> WRONG_CHAIN_ID;
            case WRONG_NONCE -> WRONG_NONCE;
            case ACCESS_LIST_UNSUPPORTED -> ACCESS_LIST_UNSUPPORTED;
            case SCHEDULE_PENDING_EXPIRATION -> SCHEDULE_PENDING_EXPIRATION;
            case CONTRACT_IS_TOKEN_TREASURY -> CONTRACT_IS_TOKEN_TREASURY;
            case CONTRACT_HAS_NON_ZERO_TOKEN_BALANCES -> CONTRACT_HAS_NON_ZERO_TOKEN_BALANCES;
            case CONTRACT_EXPIRED_AND_PENDING_REMOVAL -> CONTRACT_EXPIRED_AND_PENDING_REMOVAL;
            case CONTRACT_HAS_NO_AUTO_RENEW_ACCOUNT -> CONTRACT_HAS_NO_AUTO_RENEW_ACCOUNT;
            case PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION -> PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION;
            case PROXY_ACCOUNT_ID_FIELD_IS_DEPRECATED -> PROXY_ACCOUNT_ID_FIELD_IS_DEPRECATED;
            case SELF_STAKING_IS_NOT_ALLOWED -> SELF_STAKING_IS_NOT_ALLOWED;
            case INVALID_STAKING_ID -> INVALID_STAKING_ID;
            case STAKING_NOT_ENABLED -> STAKING_NOT_ENABLED;
            case INVALID_PRNG_RANGE -> INVALID_PRNG_RANGE;
            case MAX_ENTITIES_IN_PRICE_REGIME_HAVE_BEEN_CREATED -> MAX_ENTITIES_IN_PRICE_REGIME_HAVE_BEEN_CREATED;
            case INVALID_FULL_PREFIX_SIGNATURE_FOR_PRECOMPILE -> INVALID_FULL_PREFIX_SIGNATURE_FOR_PRECOMPILE;
            case INSUFFICIENT_BALANCES_FOR_STORAGE_RENT -> INSUFFICIENT_BALANCES_FOR_STORAGE_RENT;
            case MAX_CHILD_RECORDS_EXCEEDED -> MAX_CHILD_RECORDS_EXCEEDED;
            case INSUFFICIENT_BALANCES_FOR_RENEWAL_FEES -> INSUFFICIENT_BALANCES_FOR_RENEWAL_FEES;
            case TRANSACTION_HAS_UNKNOWN_FIELDS -> TRANSACTION_HAS_UNKNOWN_FIELDS;
            case ACCOUNT_IS_IMMUTABLE -> ACCOUNT_IS_IMMUTABLE;
            case ALIAS_ALREADY_ASSIGNED -> ALIAS_ALREADY_ASSIGNED;
            case INVALID_METADATA_KEY -> INVALID_METADATA_KEY;
            case TOKEN_HAS_NO_METADATA_KEY -> TOKEN_HAS_NO_METADATA_KEY;
            case MISSING_TOKEN_METADATA -> MISSING_TOKEN_METADATA;
            case MISSING_SERIAL_NUMBERS -> MISSING_SERIAL_NUMBERS;
            case TOKEN_HAS_NO_ADMIN_KEY -> TOKEN_HAS_NO_ADMIN_KEY;
            case NODE_DELETED -> NODE_DELETED;
            case INVALID_NODE_ID -> INVALID_NODE_ID;
            case INVALID_GOSSIP_ENDPOINT -> INVALID_GOSSIP_ENDPOINT;
            case INVALID_NODE_ACCOUNT_ID -> INVALID_NODE_ACCOUNT_ID;
            case INVALID_NODE_DESCRIPTION -> INVALID_NODE_DESCRIPTION;
            case INVALID_SERVICE_ENDPOINT -> INVALID_SERVICE_ENDPOINT;
            case INVALID_GOSSIP_CA_CERTIFICATE -> INVALID_GOSSIP_CA_CERTIFICATE;
            case INVALID_GRPC_CERTIFICATE -> INVALID_GRPC_CERTIFICATE;
            case INVALID_MAX_AUTO_ASSOCIATIONS -> INVALID_MAX_AUTO_ASSOCIATIONS;
            case MAX_NODES_CREATED -> MAX_NODES_CREATED;
            case IP_FQDN_CANNOT_BE_SET_FOR_SAME_ENDPOINT -> IP_FQDN_CANNOT_BE_SET_FOR_SAME_ENDPOINT;
            case GOSSIP_ENDPOINT_CANNOT_HAVE_FQDN -> GOSSIP_ENDPOINT_CANNOT_HAVE_FQDN;
            case FQDN_SIZE_TOO_LARGE -> FQDN_SIZE_TOO_LARGE;
            case INVALID_ENDPOINT -> INVALID_ENDPOINT;
            case GOSSIP_ENDPOINTS_EXCEEDED_LIMIT -> GOSSIP_ENDPOINTS_EXCEEDED_LIMIT;
            case TOKEN_REFERENCE_REPEATED -> TOKEN_REFERENCE_REPEATED;
            case INVALID_OWNER_ID -> INVALID_OWNER_ID;
            case TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED -> TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED;
            case SERVICE_ENDPOINTS_EXCEEDED_LIMIT -> SERVICE_ENDPOINTS_EXCEEDED_LIMIT;
            case INVALID_IPV4_ADDRESS -> INVALID_IPV4_ADDRESS;
            case EMPTY_TOKEN_REFERENCE_LIST -> EMPTY_TOKEN_REFERENCE_LIST;
            case UPDATE_NODE_ACCOUNT_NOT_ALLOWED -> UPDATE_NODE_ACCOUNT_NOT_ALLOWED;
            case TOKEN_HAS_NO_METADATA_OR_SUPPLY_KEY -> TOKEN_HAS_NO_METADATA_OR_SUPPLY_KEY;
            case EMPTY_PENDING_AIRDROP_ID_LIST -> EMPTY_PENDING_AIRDROP_ID_LIST;
            case PENDING_AIRDROP_ID_REPEATED -> PENDING_AIRDROP_ID_REPEATED;
            case PENDING_AIRDROP_ID_LIST_TOO_LONG -> PENDING_AIRDROP_ID_LIST_TOO_LONG;
            case PENDING_NFT_AIRDROP_ALREADY_EXISTS -> PENDING_NFT_AIRDROP_ALREADY_EXISTS;
            case ACCOUNT_HAS_PENDING_AIRDROPS -> ACCOUNT_HAS_PENDING_AIRDROPS;
            case THROTTLED_AT_CONSENSUS -> THROTTLED_AT_CONSENSUS;
            case INVALID_PENDING_AIRDROP_ID -> INVALID_PENDING_AIRDROP_ID;
            case TOKEN_AIRDROP_WITH_FALLBACK_ROYALTY -> TOKEN_AIRDROP_WITH_FALLBACK_ROYALTY;
            case INVALID_TOKEN_IN_PENDING_AIRDROP -> INVALID_TOKEN_IN_PENDING_AIRDROP;
            case SCHEDULE_EXPIRY_IS_BUSY -> SCHEDULE_EXPIRY_IS_BUSY;
            case INVALID_GRPC_CERTIFICATE_HASH -> INVALID_GRPC_CERTIFICATE_HASH;
            case MISSING_EXPIRY_TIME -> MISSING_EXPIRY_TIME;
            case NO_SCHEDULING_ALLOWED_AFTER_SCHEDULED_RECURSION -> NO_SCHEDULING_ALLOWED_AFTER_SCHEDULED_RECURSION;
            case RECURSIVE_SCHEDULING_LIMIT_REACHED -> RECURSIVE_SCHEDULING_LIMIT_REACHED;
            case WAITING_FOR_LEDGER_ID -> WAITING_FOR_LEDGER_ID;
            case MAX_ENTRIES_FOR_FEE_EXEMPT_KEY_LIST_EXCEEDED -> MAX_ENTRIES_FOR_FEE_EXEMPT_KEY_LIST_EXCEEDED;
            case FEE_EXEMPT_KEY_LIST_CONTAINS_DUPLICATED_KEYS -> FEE_EXEMPT_KEY_LIST_CONTAINS_DUPLICATED_KEYS;
            case INVALID_KEY_IN_FEE_EXEMPT_KEY_LIST -> INVALID_KEY_IN_FEE_EXEMPT_KEY_LIST;
            case INVALID_FEE_SCHEDULE_KEY -> INVALID_FEE_SCHEDULE_KEY;
            case FEE_SCHEDULE_KEY_CANNOT_BE_UPDATED -> FEE_SCHEDULE_KEY_CANNOT_BE_UPDATED;
            case FEE_SCHEDULE_KEY_NOT_SET -> FEE_SCHEDULE_KEY_NOT_SET;
            case MAX_CUSTOM_FEE_LIMIT_EXCEEDED -> MAX_CUSTOM_FEE_LIMIT_EXCEEDED;
            case NO_VALID_MAX_CUSTOM_FEE -> NO_VALID_MAX_CUSTOM_FEE;
            case INVALID_MAX_CUSTOM_FEES -> INVALID_MAX_CUSTOM_FEES;
            case DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST -> DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST;
            case DUPLICATE_ACCOUNT_ID_IN_MAX_CUSTOM_FEE_LIST -> DUPLICATE_ACCOUNT_ID_IN_MAX_CUSTOM_FEE_LIST;
            case MAX_CUSTOM_FEES_IS_NOT_SUPPORTED -> MAX_CUSTOM_FEES_IS_NOT_SUPPORTED;
            case BATCH_LIST_EMPTY -> BATCH_LIST_EMPTY;
            case BATCH_LIST_CONTAINS_DUPLICATES -> BATCH_LIST_CONTAINS_DUPLICATES;
            case BATCH_TRANSACTION_IN_BLACKLIST -> BATCH_TRANSACTION_IN_BLACKLIST;
            case INNER_TRANSACTION_FAILED -> INNER_TRANSACTION_FAILED;
            case MISSING_BATCH_KEY -> MISSING_BATCH_KEY;
            case BATCH_KEY_SET_ON_NON_INNER_TRANSACTION -> BATCH_KEY_SET_ON_NON_INNER_TRANSACTION;
            case INVALID_BATCH_KEY -> INVALID_BATCH_KEY;
            case SCHEDULE_EXPIRY_NOT_CONFIGURABLE -> SCHEDULE_EXPIRY_NOT_CONFIGURABLE;
            case CREATING_SYSTEM_ENTITIES -> CREATING_SYSTEM_ENTITIES;
            case THROTTLE_GROUP_LCM_OVERFLOW -> THROTTLE_GROUP_LCM_OVERFLOW;
            case AIRDROP_CONTAINS_MULTIPLE_SENDERS_FOR_A_TOKEN -> AIRDROP_CONTAINS_MULTIPLE_SENDERS_FOR_A_TOKEN;
            case GRPC_WEB_PROXY_NOT_SUPPORTED -> GRPC_WEB_PROXY_NOT_SUPPORTED;
            case UNRECOGNIZED ->
            // NOTE: Protobuf deserialization will not give us the code on the wire
            throw new IllegalArgumentException(
                    "network returned unrecognized response code; your SDK may be out of date");
        };
    }

    public static Status fromResponseCode(int reponseCode) {
        return Status.valueOf(Objects.requireNonNull(ResponseCodeEnum.forNumber(reponseCode)));
    }

    public int toResponseCode() {
        return code.getNumber();
    }

    @Override
    public String toString() {
        return code.name();
    }
}
