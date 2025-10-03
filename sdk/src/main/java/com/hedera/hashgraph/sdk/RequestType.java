// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.HederaFunctionality;

/**
 * Enum for the request types.
 */
public enum RequestType {
    /**
     * FUTURE - Uncomment when https://github.com/hashgraph/pbj/issues/339 is fixed;
     * currently the PBJ-generated unit tests fail when using reserved ordinals
     * reserved 96, 97, 98, 99;
     * Unused - The first value is unused because this default value is
     * ambiguous with an "unset" value and therefore should not be used.
     */
    NONE(HederaFunctionality.NONE),

    /**
     * Transfer tokens among accounts.
     */
    CRYPTO_TRANSFER(HederaFunctionality.CryptoTransfer),

    /**
     * Update an account.
     */
    CRYPTO_UPDATE(HederaFunctionality.CryptoUpdate),

    /**
     * Delete an account.
     */
    CRYPTO_DELETE(HederaFunctionality.CryptoDelete),

    /**
     * Add a livehash to an account
     */
    CRYPTO_ADD_LIVE_HASH(HederaFunctionality.CryptoAddLiveHash),

    /**
     * Delete a livehash from an account
     */
    CRYPTO_DELETE_LIVE_HASH(HederaFunctionality.CryptoDeleteLiveHash),

    /**
     * Execute a smart contract call.
     */
    CONTRACT_CALL(HederaFunctionality.ContractCall),

    /**
     * Create a smart contract.
     */
    CONTRACT_CREATE(HederaFunctionality.ContractCreate),

    /**
     * Update a smart contract.
     */
    CONTRACT_UPDATE(HederaFunctionality.ContractUpdate),

    /**
     * Create a "file" stored in the ledger.
     */
    FILE_CREATE(HederaFunctionality.FileCreate),

    /**
     * Append data to a "file" stored in the ledger.
     */
    FILE_APPEND(HederaFunctionality.FileAppend),

    /**
     * Update a "file" stored in the ledger.
     */
    FILE_UPDATE(HederaFunctionality.FileUpdate),

    /**
     * Delete a "file" stored in the ledger.
     */
    FILE_DELETE(HederaFunctionality.FileDelete),

    /**
     * Get the balance for an account.
     */
    CRYPTO_GET_ACCOUNT_BALANCE(HederaFunctionality.CryptoGetAccountBalance),

    /**
     * Get a full account record.
     */
    CRYPTO_GET_ACCOUNT_RECORDS(HederaFunctionality.CryptoGetAccountRecords),

    /**
     * Get information about a token.
     */
    CRYPTO_GET_INFO(HederaFunctionality.CryptoGetInfo),

    /**
     * Execute a local smart contract call.<br/>
     * Used by contracts to call other contracts.
     */
    CONTRACT_CALL_LOCAL(HederaFunctionality.ContractCallLocal),

    /**
     * Get information about a smart contract.
     */
    CONTRACT_GET_INFO(HederaFunctionality.ContractGetInfo),

    /**
     * Get the compiled bytecode that implements a smart contract.
     */
    CONTRACT_GET_BYTECODE(HederaFunctionality.ContractGetBytecode),

    /**
     * Get a smart contract record by reference to the solidity ID.
     */
    GET_BY_SOLIDITY_ID(HederaFunctionality.GetBySolidityID),

    /**
     * Get a smart contract by reference to the contract key.
     */
    GET_BY_KEY(HederaFunctionality.GetByKey),

    /**
     * Get the live hash for an account
     */
    CRYPTO_GET_LIVE_HASH(HederaFunctionality.CryptoGetLiveHash),

    /**
     * Get the accounts proxy staking to a given account.
     */
    CRYPTO_GET_STAKERS(HederaFunctionality.CryptoGetStakers),

    /**
     * Get the contents of a "file" stored in the ledger.
     */
    FILE_GET_CONTENTS(HederaFunctionality.FileGetContents),

    /**
     * Get the metadata for a "file" stored in the ledger.
     */
    FILE_GET_INFO(HederaFunctionality.FileGetInfo),

    /**
     * Get transaction record(s) for a specified transaction ID.
     */
    TRANSACTION_GET_RECORD(HederaFunctionality.TransactionGetRecord),

    /**
     * Get all transaction records for a specified contract ID in
     * the past 24 hours.<br/>
     * deprecated since version 0.9.0
     */
    CONTRACT_GET_RECORDS(HederaFunctionality.ContractGetRecords),

    /**
     * Create a new account
     */
    CRYPTO_CREATE(HederaFunctionality.CryptoCreate),

    /**
     * Delete a "system" "file" stored in the ledger.<br/>
     * "System" files are files with special purpose and ID values within a
     * specific range.<br/>
     * These files require additional controls and can only be deleted when
     * authorized by accounts with elevated privilege.
     */
    SYSTEM_DELETE(HederaFunctionality.SystemDelete),

    /**
     * Undo the delete of a "system" "file" stored in the ledger.<br/>
     * "System" files are files with special purpose and ID values within a
     * specific range.<br/>
     * These files require additional controls and can only be deleted when
     * authorized by accounts with elevated privilege. This operation allows
     * such files to be restored, within a reasonable timeframe, if
     * deleted improperly.
     */
    SYSTEM_UNDELETE(HederaFunctionality.SystemUndelete),

    /**
     * Delete a smart contract
     */
    CONTRACT_DELETE(HederaFunctionality.ContractDelete),

    /**
     * Stop all processing and "freeze" the entire network.<br/>
     * This is generally sent immediately prior to upgrading the network.<br/>
     * After processing this transactions all nodes enter a quiescent state.
     */
    FREEZE(HederaFunctionality.Freeze),

    /**
     * Create a Transaction Record.<br/>
     * This appears to be purely internal and unused.
     */
    CREATE_TRANSACTION_RECORD(HederaFunctionality.CreateTransactionRecord),

    /**
     * Auto-renew an account.<br/>
     * This is used for internal fee calculations.
     */
    CRYPTO_ACCOUNT_AUTO_RENEW(HederaFunctionality.CryptoAccountAutoRenew),

    /**
     * Auto-renew a smart contract.<br/>
     * This is used for internal fee calculations.
     */
    CONTRACT_AUTO_RENEW(HederaFunctionality.ContractAutoRenew),

    /**
     * Get version information for the ledger.<br/>
     * This returns a the version of the software currently running the network
     * for both the protocol buffers and the network services (node).
     */
    GET_VERSION_INFO(HederaFunctionality.GetVersionInfo),

    /**
     * Get a receipt for a specified transaction ID.
     */
    TRANSACTION_GET_RECEIPT(HederaFunctionality.TransactionGetReceipt),

    /**
     * Create a topic for the Hedera Consensus Service (HCS).
     */
    CONSENSUS_CREATE_TOPIC(HederaFunctionality.ConsensusCreateTopic),

    /**
     * Update an HCS topic.
     */
    CONSENSUS_UPDATE_TOPIC(HederaFunctionality.ConsensusUpdateTopic),

    /**
     * Delete an HCS topic.
     */
    CONSENSUS_DELETE_TOPIC(HederaFunctionality.ConsensusDeleteTopic),

    /**
     * Get metadata (information) for an HCS topic.
     */
    CONSENSUS_GET_TOPIC_INFO(HederaFunctionality.ConsensusGetTopicInfo),

    /**
     * Publish a message to an HCS topic.
     */
    CONSENSUS_SUBMIT_MESSAGE(HederaFunctionality.ConsensusSubmitMessage),

    /**
     * Submit a transaction, bypassing intake checking.
     * Only enabled in local-mode.
     */
    UNCHECKED_SUBMIT(HederaFunctionality.UncheckedSubmit),

    /**
     * Create a token for the Hedera Token Service (HTS).
     */
    TOKEN_CREATE(HederaFunctionality.TokenCreate),

    /**
     * Get metadata (information) for an HTS token.
     */
    TOKEN_GET_INFO(HederaFunctionality.TokenGetInfo),

    /**
     * Freeze a specific account with respect to a specific HTS token.
     * <p>
     * Once this transaction completes that account CANNOT send or receive
     * the specified token.
     */
    TOKEN_FREEZE_ACCOUNT(HederaFunctionality.TokenFreezeAccount),

    /**
     * Remove a "freeze" from an account with respect to a specific HTS token.
     */
    TOKEN_UNFREEZE_ACCOUNT(HederaFunctionality.TokenUnfreezeAccount),

    /**
     * Grant KYC status to an account for a specific HTS token.
     */
    TOKEN_GRANT_KYC_TO_ACCOUNT(HederaFunctionality.TokenGrantKycToAccount),

    /**
     * Revoke KYC status from an account for a specific HTS token.
     */
    TOKEN_REVOKE_KYC_FROM_ACCOUNT(HederaFunctionality.TokenRevokeKycFromAccount),

    /**
     * Delete a specific HTS token.
     */
    TOKEN_DELETE(HederaFunctionality.TokenDelete),

    /**
     * Update a specific HTS token.
     */
    TOKEN_UPDATE(HederaFunctionality.TokenUpdate),

    /**
     * Mint HTS token amounts to the treasury account for that token.
     */
    TOKEN_MINT(HederaFunctionality.TokenMint),

    /**
     * Burn HTS token amounts from the treasury account for that token.
     */
    TOKEN_BURN(HederaFunctionality.TokenBurn),

    /**
     * Wipe all amounts for a specific HTS token from a specified account.
     */
    TOKEN_ACCOUNT_WIPE(HederaFunctionality.TokenAccountWipe),

    /**
     * Associate a specific HTS token to an account.
     */
    TOKEN_ASSOCIATE_TO_ACCOUNT(HederaFunctionality.TokenAssociateToAccount),

    /**
     * Dissociate a specific HTS token from an account.
     */
    TOKEN_DISSOCIATE_FROM_ACCOUNT(HederaFunctionality.TokenDissociateFromAccount),

    /**
     * Create a scheduled transaction
     */
    SCHEDULE_CREATE(HederaFunctionality.ScheduleCreate),

    /**
     * Delete a scheduled transaction
     */
    SCHEDULE_DELETE(HederaFunctionality.ScheduleDelete),

    /**
     * Sign a scheduled transaction
     */
    SCHEDULE_SIGN(HederaFunctionality.ScheduleSign),

    /**
     * Get metadata (information) for a scheduled transaction
     */
    SCHEDULE_GET_INFO(HederaFunctionality.ScheduleGetInfo),

    /**
     * Get NFT metadata (information) for a range of NFTs associated to a
     * specific non-fungible/unique HTS token and owned by a specific account.
     */
    TOKEN_GET_ACCOUNT_NFT_INFOS(HederaFunctionality.TokenGetAccountNftInfos),

    /**
     * Get metadata (information) for a specific NFT identified by token and
     * serial number.
     */
    TOKEN_GET_NFT_INFO(HederaFunctionality.TokenGetNftInfo),

    /**
     * Get NFT metadata (information) for a range of NFTs associated to a
     * specific non-fungible/unique HTS token.
     */
    TOKEN_GET_NFT_INFOS(HederaFunctionality.TokenGetNftInfos),

    /**
     * Update a token's custom fee schedule.
     * <p>
     * If a transaction of this type is not signed by the token
     * `fee_schedule_key` it SHALL fail with INVALID_SIGNATURE, or
     * TOKEN_HAS_NO_FEE_SCHEDULE_KEY if there is no `fee_schedule_key` set.
     */
    TOKEN_FEE_SCHEDULE_UPDATE(HederaFunctionality.TokenFeeScheduleUpdate),

    /**
     * Get execution time(s) for one or more "recent" TransactionIDs.
     */
    NETWORK_GET_EXECUTION_TIME(HederaFunctionality.NetworkGetExecutionTime),

    /**
     * Pause a specific HTS token
     */
    TOKEN_PAUSE(HederaFunctionality.TokenPause),

    /**
     * Unpause a paused HTS token.
     */
    TOKEN_UNPAUSE(HederaFunctionality.TokenUnpause),

    /**
     * Approve an allowance for a spender relative to the owner account, which
     * MUST sign the transaction.
     */
    CRYPTO_APPROVE_ALLOWANCE(HederaFunctionality.CryptoApproveAllowance),

    /**
     * Delete (unapprove) an allowance previously approved
     * for the owner account.
     */
    CRYPTO_DELETE_ALLOWANCE(HederaFunctionality.CryptoDeleteAllowance),

    /**
     * Get all the information about an account, including balance
     * and allowances.<br/>
     * This does not get a list of account records.
     */
    GET_ACCOUNT_DETAILS(HederaFunctionality.GetAccountDetails),

    /**
     * Perform an Ethereum (EVM) transaction.<br/>
     * CallData may be inline if small, or in a "file" if large.
     */
    ETHEREUM_TRANSACTION(HederaFunctionality.EthereumTransaction),

    /**
     * Used to indicate when the network has updated the staking information
     * at the end of a staking period and to indicate a new staking period
     * has started.
     */
    NODE_STAKE_UPDATE(HederaFunctionality.NodeStakeUpdate),

    /**
     * Generate and return a pseudorandom number based on network state.
     */
    PRNG(HederaFunctionality.UtilPrng),

    /**
     * Get a record for a "recent" transaction.
     */
    TRANSACTION_GET_FAST_RECORD(HederaFunctionality.TransactionGetFastRecord),

    /**
     * Update the metadata of one or more NFT's of a specific token type.
     */
    TOKEN_UPDATE_NFTS(HederaFunctionality.TokenUpdateNfts),

    /**
     * Create a node
     */
    NODE_CREATE(HederaFunctionality.NodeCreate),

    /**
     * Update a node
     */
    NODE_UPDATE(HederaFunctionality.NodeUpdate),

    /**
     * Delete a node
     */
    NODE_DELETE(HederaFunctionality.NodeDelete),

    /**
     * Transfer one or more token balances held by the requesting account
     * to the treasury for each token type.
     */
    TOKEN_REJECT(HederaFunctionality.TokenReject),

    /**
     * Airdrop one or more tokens to one or more accounts.
     */
    TOKEN_AIRDROP(HederaFunctionality.TokenAirdrop),

    /**
     * Remove one or more pending airdrops from state on behalf of
     * the sender(s) for each airdrop.
     */
    TOKEN_CANCEL_AIRDROP(HederaFunctionality.TokenCancelAirdrop),

    /**
     * Claim one or more pending airdrops
     */
    TOKEN_CLAIM_AIRDROP(HederaFunctionality.TokenClaimAirdrop),

    /**
     * Submit a signature of a state root hash gossiped to other nodes
     */
    STATE_SIGNATURE_TRANSACTION(HederaFunctionality.StateSignatureTransaction),

    /**
     * Publish a hinTS key to the network.
     */
    HINTS_KEY_PUBLICATION(HederaFunctionality.HintsKeyPublication),

    /**
     * Vote for a particular preprocessing output of a hinTS construction.
     */
    HINTS_PREPROCESSING_VOTE(HederaFunctionality.HintsPreprocessingVote),

    /**
     * Sign a partial signature for the active hinTS construction.
     */
    HINTS_PARTIAL_SIGNATURE(HederaFunctionality.HintsPartialSignature),

    /**
     * Sign a particular history assembly.
     */
    HISTORY_ASSEMBLY_SIGNATURE(HederaFunctionality.HistoryAssemblySignature),

    /**
     * Publish a roster history proof key to the network.
     */
    HISTORY_PROOF_KEY_PUBLICATION(HederaFunctionality.HistoryProofKeyPublication),

    /**
     * Vote for a particular history proof.
     */
    HISTORY_PROOF_VOTE(HederaFunctionality.HistoryProofVote),

    /**
     * Publish a random CRS to the network.
     */
    CRS_PUBLICATION(HederaFunctionality.CrsPublication),

    /**
     * Submit a batch of transactions to run atomically
     */
    ATOMIC_BATCH(HederaFunctionality.AtomicBatch),

    /**
     * Update one or more storage slots in an lambda EVM hook.
     */
    LAMBDA_S_STORE(HederaFunctionality.LambdaSStore),

    /**
     * (Internal-only) Dispatch a hook action.
     */
    HOOK_DISPATCH(HederaFunctionality.HookDispatch);

    final HederaFunctionality code;

    RequestType(HederaFunctionality code) {
        this.code = code;
    }

    static RequestType valueOf(HederaFunctionality code) {
        return switch (code) {
            case NONE -> NONE;
            case CryptoTransfer -> CRYPTO_TRANSFER;
            case CryptoUpdate -> CRYPTO_UPDATE;
            case CryptoDelete -> CRYPTO_DELETE;
            case CryptoAddLiveHash -> CRYPTO_ADD_LIVE_HASH;
            case CryptoDeleteLiveHash -> CRYPTO_DELETE_LIVE_HASH;
            case ContractCall -> CONTRACT_CALL;
            case ContractCreate -> CONTRACT_CREATE;
            case ContractUpdate -> CONTRACT_UPDATE;
            case FileCreate -> FILE_CREATE;
            case FileAppend -> FILE_APPEND;
            case FileUpdate -> FILE_UPDATE;
            case FileDelete -> FILE_DELETE;
            case CryptoGetAccountBalance -> CRYPTO_GET_ACCOUNT_BALANCE;
            case CryptoGetAccountRecords -> CRYPTO_GET_ACCOUNT_RECORDS;
            case CryptoGetInfo -> CRYPTO_GET_INFO;
            case ContractCallLocal -> CONTRACT_CALL_LOCAL;
            case ContractGetInfo -> CONTRACT_GET_INFO;
            case ContractGetBytecode -> CONTRACT_GET_BYTECODE;
            case GetBySolidityID -> GET_BY_SOLIDITY_ID;
            case GetByKey -> GET_BY_KEY;
            case CryptoGetLiveHash -> CRYPTO_GET_LIVE_HASH;
            case CryptoGetStakers -> CRYPTO_GET_STAKERS;
            case FileGetContents -> FILE_GET_CONTENTS;
            case FileGetInfo -> FILE_GET_INFO;
            case TransactionGetRecord -> TRANSACTION_GET_RECORD;
            case ContractGetRecords -> CONTRACT_GET_RECORDS;
            case CryptoCreate -> CRYPTO_CREATE;
            case SystemDelete -> SYSTEM_DELETE;
            case SystemUndelete -> SYSTEM_UNDELETE;
            case ContractDelete -> CONTRACT_DELETE;
            case Freeze -> FREEZE;
            case CreateTransactionRecord -> CREATE_TRANSACTION_RECORD;
            case CryptoAccountAutoRenew -> CRYPTO_ACCOUNT_AUTO_RENEW;
            case ContractAutoRenew -> CONTRACT_AUTO_RENEW;
            case GetVersionInfo -> GET_VERSION_INFO;
            case TransactionGetReceipt -> TRANSACTION_GET_RECEIPT;
            case ConsensusCreateTopic -> CONSENSUS_CREATE_TOPIC;
            case ConsensusUpdateTopic -> CONSENSUS_UPDATE_TOPIC;
            case ConsensusDeleteTopic -> CONSENSUS_DELETE_TOPIC;
            case ConsensusGetTopicInfo -> CONSENSUS_GET_TOPIC_INFO;
            case ConsensusSubmitMessage -> CONSENSUS_SUBMIT_MESSAGE;
            case UncheckedSubmit -> UNCHECKED_SUBMIT;
            case TokenCreate -> TOKEN_CREATE;
            case TokenGetInfo -> TOKEN_GET_INFO;
            case TokenFreezeAccount -> TOKEN_FREEZE_ACCOUNT;
            case TokenUnfreezeAccount -> TOKEN_UNFREEZE_ACCOUNT;
            case TokenGrantKycToAccount -> TOKEN_GRANT_KYC_TO_ACCOUNT;
            case TokenRevokeKycFromAccount -> TOKEN_REVOKE_KYC_FROM_ACCOUNT;
            case TokenDelete -> TOKEN_DELETE;
            case TokenUpdate -> TOKEN_UPDATE;
            case TokenMint -> TOKEN_MINT;
            case TokenBurn -> TOKEN_BURN;
            case TokenAccountWipe -> TOKEN_ACCOUNT_WIPE;
            case TokenAssociateToAccount -> TOKEN_ASSOCIATE_TO_ACCOUNT;
            case TokenDissociateFromAccount -> TOKEN_DISSOCIATE_FROM_ACCOUNT;
            case ScheduleCreate -> SCHEDULE_CREATE;
            case ScheduleDelete -> SCHEDULE_DELETE;
            case ScheduleSign -> SCHEDULE_SIGN;
            case ScheduleGetInfo -> SCHEDULE_GET_INFO;
            case TokenGetAccountNftInfos -> TOKEN_GET_ACCOUNT_NFT_INFOS;
            case TokenGetNftInfo -> TOKEN_GET_NFT_INFO;
            case TokenGetNftInfos -> TOKEN_GET_NFT_INFOS;
            case TokenFeeScheduleUpdate -> TOKEN_FEE_SCHEDULE_UPDATE;
            case NetworkGetExecutionTime -> NETWORK_GET_EXECUTION_TIME;
            case TokenPause -> TOKEN_PAUSE;
            case TokenUnpause -> TOKEN_UNPAUSE;
            case CryptoApproveAllowance -> CRYPTO_APPROVE_ALLOWANCE;
            case CryptoDeleteAllowance -> CRYPTO_DELETE_ALLOWANCE;
            case GetAccountDetails -> GET_ACCOUNT_DETAILS;
            case EthereumTransaction -> ETHEREUM_TRANSACTION;
            case NodeStakeUpdate -> NODE_STAKE_UPDATE;
            case UtilPrng -> PRNG;
            case TransactionGetFastRecord -> TRANSACTION_GET_FAST_RECORD;
            case TokenUpdateNfts -> TOKEN_UPDATE_NFTS;
            case NodeCreate -> NODE_CREATE;
            case NodeUpdate -> NODE_UPDATE;
            case NodeDelete -> NODE_DELETE;
            case TokenReject -> TOKEN_REJECT;
            case TokenAirdrop -> TOKEN_AIRDROP;
            case TokenCancelAirdrop -> TOKEN_CANCEL_AIRDROP;
            case TokenClaimAirdrop -> TOKEN_CLAIM_AIRDROP;
            case StateSignatureTransaction -> STATE_SIGNATURE_TRANSACTION;
            case HintsKeyPublication -> HINTS_KEY_PUBLICATION;
            case HintsPreprocessingVote -> HINTS_PREPROCESSING_VOTE;
            case HintsPartialSignature -> HINTS_PARTIAL_SIGNATURE;
            case HistoryAssemblySignature -> HISTORY_ASSEMBLY_SIGNATURE;
            case HistoryProofKeyPublication -> HISTORY_PROOF_KEY_PUBLICATION;
            case HistoryProofVote -> HISTORY_PROOF_VOTE;
            case CrsPublication -> CRS_PUBLICATION;
            case AtomicBatch -> ATOMIC_BATCH;
            case LambdaSStore -> LAMBDA_S_STORE;
            case HookDispatch -> HOOK_DISPATCH;
            default -> throw new IllegalStateException("(BUG) unhandled HederaFunctionality");
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case NONE -> "NONE";
            case CRYPTO_TRANSFER -> "CRYPTO_TRANSFER";
            case CRYPTO_UPDATE -> "CRYPTO_UPDATE";
            case CRYPTO_DELETE -> "CRYPTO_DELETE";
            case CRYPTO_ADD_LIVE_HASH -> "CRYPTO_ADD_LIVE_HASH";
            case CRYPTO_DELETE_LIVE_HASH -> "CRYPTO_DELETE_LIVE_HASH";
            case CONTRACT_CALL -> "CONTRACT_CALL";
            case CONTRACT_CREATE -> "CONTRACT_CREATE";
            case CONTRACT_UPDATE -> "CONTRACT_UPDATE";
            case FILE_CREATE -> "FILE_CREATE";
            case FILE_APPEND -> "FILE_APPEND";
            case FILE_UPDATE -> "FILE_UPDATE";
            case FILE_DELETE -> "FILE_DELETE";
            case CRYPTO_GET_ACCOUNT_BALANCE -> "CRYPTO_GET_ACCOUNT_BALANCE";
            case CRYPTO_GET_ACCOUNT_RECORDS -> "CRYPTO_GET_ACCOUNT_RECORDS";
            case CRYPTO_GET_INFO -> "CRYPTO_GET_INFO";
            case CONTRACT_CALL_LOCAL -> "CONTRACT_CALL_LOCAL";
            case CONTRACT_GET_INFO -> "CONTRACT_GET_INFO";
            case CONTRACT_GET_BYTECODE -> "CONTRACT_GET_BYTECODE";
            case GET_BY_SOLIDITY_ID -> "GET_BY_SOLIDITY_ID";
            case GET_BY_KEY -> "GET_BY_KEY";
            case CRYPTO_GET_LIVE_HASH -> "CRYPTO_GET_LIVE_HASH";
            case CRYPTO_GET_STAKERS -> "CRYPTO_GET_STAKERS";
            case FILE_GET_CONTENTS -> "FILE_GET_CONTENTS";
            case FILE_GET_INFO -> "FILE_GET_INFO";
            case TRANSACTION_GET_RECORD -> "TRANSACTION_GET_RECORD";
            case CONTRACT_GET_RECORDS -> "CONTRACT_GET_RECORDS";
            case CRYPTO_CREATE -> "CRYPTO_CREATE";
            case SYSTEM_DELETE -> "SYSTEM_DELETE";
            case SYSTEM_UNDELETE -> "SYSTEM_UNDELETE";
            case CONTRACT_DELETE -> "CONTRACT_DELETE";
            case FREEZE -> "FREEZE";
            case CREATE_TRANSACTION_RECORD -> "CREATE_TRANSACTION_RECORD";
            case CRYPTO_ACCOUNT_AUTO_RENEW -> "CRYPTO_ACCOUNT_AUTO_RENEW";
            case CONTRACT_AUTO_RENEW -> "CONTRACT_AUTO_RENEW";
            case GET_VERSION_INFO -> "GET_VERSION_INFO";
            case TRANSACTION_GET_RECEIPT -> "TRANSACTION_GET_RECEIPT";
            case CONSENSUS_CREATE_TOPIC -> "CONSENSUS_CREATE_TOPIC";
            case CONSENSUS_UPDATE_TOPIC -> "CONSENSUS_UPDATE_TOPIC";
            case CONSENSUS_DELETE_TOPIC -> "CONSENSUS_DELETE_TOPIC";
            case CONSENSUS_GET_TOPIC_INFO -> "CONSENSUS_GET_TOPIC_INFO";
            case CONSENSUS_SUBMIT_MESSAGE -> "CONSENSUS_SUBMIT_MESSAGE";
            case UNCHECKED_SUBMIT -> "UNCHECKED_SUBMIT";
            case TOKEN_CREATE -> "TOKEN_CREATE";
            case TOKEN_GET_INFO -> "TOKEN_GET_INFO";
            case TOKEN_FREEZE_ACCOUNT -> "TOKEN_FREEZE_ACCOUNT";
            case TOKEN_UNFREEZE_ACCOUNT -> "TOKEN_UNFREEZE_ACCOUNT";
            case TOKEN_GRANT_KYC_TO_ACCOUNT -> "TOKEN_GRANT_KYC_TO_ACCOUNT";
            case TOKEN_REVOKE_KYC_FROM_ACCOUNT -> "TOKEN_REVOKE_KYC_FROM_ACCOUNT";
            case TOKEN_DELETE -> "TOKEN_DELETE";
            case TOKEN_UPDATE -> "TOKEN_UPDATE";
            case TOKEN_MINT -> "TOKEN_MINT";
            case TOKEN_BURN -> "TOKEN_BURN";
            case TOKEN_ACCOUNT_WIPE -> "TOKEN_ACCOUNT_WIPE";
            case TOKEN_ASSOCIATE_TO_ACCOUNT -> "TOKEN_ASSOCIATE_TO_ACCOUNT";
            case TOKEN_DISSOCIATE_FROM_ACCOUNT -> "TOKEN_DISSOCIATE_FROM_ACCOUNT";
            case SCHEDULE_CREATE -> "SCHEDULE_CREATE";
            case SCHEDULE_DELETE -> "SCHEDULE_DELETE";
            case SCHEDULE_SIGN -> "SCHEDULE_SIGN";
            case SCHEDULE_GET_INFO -> "SCHEDULE_GET_INFO";
            case TOKEN_GET_ACCOUNT_NFT_INFOS -> "TOKEN_GET_ACCOUNT_NFT_INFOS";
            case TOKEN_GET_NFT_INFO -> "TOKEN_GET_NFT_INFO";
            case TOKEN_GET_NFT_INFOS -> "TOKEN_GET_NFT_INFOS";
            case TOKEN_FEE_SCHEDULE_UPDATE -> "TOKEN_FEE_SCHEDULE_UPDATE";
            case NETWORK_GET_EXECUTION_TIME -> "NETWORK_GET_EXECUTION_TIME";
            case TOKEN_PAUSE -> "TOKEN_PAUSE";
            case TOKEN_UNPAUSE -> "TOKEN_UNPAUSE";
            case CRYPTO_APPROVE_ALLOWANCE -> "CRYPTO_APPROVE_ALLOWANCE";
            case CRYPTO_DELETE_ALLOWANCE -> "CRYPTO_DELETE_ALLOWANCE";
            case GET_ACCOUNT_DETAILS -> "GET_ACCOUNT_DETAILS";
            case ETHEREUM_TRANSACTION -> "ETHEREUM_TRANSACTION";
            case NODE_STAKE_UPDATE -> "NODE_STAKE_UPDATE";
            case PRNG -> "PRNG";
            case TRANSACTION_GET_FAST_RECORD -> "TRANSACTION_GET_FAST_RECORD";
            case TOKEN_UPDATE_NFTS -> "TOKEN_UPDATE_NFTS";
            case NODE_CREATE -> "NODE_CREATE";
            case NODE_UPDATE -> "NODE_UPDATE";
            case NODE_DELETE -> "NODE_DELETE";
            case TOKEN_REJECT -> "TOKEN_REJECT";
            case TOKEN_AIRDROP -> "TOKEN_AIRDROP";
            case TOKEN_CANCEL_AIRDROP -> "TOKEN_CANCEL_AIRDROP";
            case TOKEN_CLAIM_AIRDROP -> "TOKEN_CLAIM_AIRDROP";
            case STATE_SIGNATURE_TRANSACTION -> "STATE_SIGNATURE_TRANSACTION";
            case HINTS_KEY_PUBLICATION -> "HINTS_KEY_PUBLICATION";
            case HINTS_PREPROCESSING_VOTE -> "HINTS_PREPROCESSING_VOTE";
            case HINTS_PARTIAL_SIGNATURE -> "HINTS_PARTIAL_SIGNATURE";
            case HISTORY_ASSEMBLY_SIGNATURE -> "HISTORY_ASSEMBLY_SIGNATURE";
            case HISTORY_PROOF_KEY_PUBLICATION -> "HISTORY_PROOF_KEY_PUBLICATION";
            case HISTORY_PROOF_VOTE -> "HISTORY_PROOF_VOTE";
            case CRS_PUBLICATION -> "CRS_PUBLICATION";
            case ATOMIC_BATCH -> "ATOMIC_BATCH";
            case LAMBDA_S_STORE -> "LAMBDA_S_STORE";
            case HOOK_DISPATCH -> "HOOK_DISPATCH";
        };
    }
}
