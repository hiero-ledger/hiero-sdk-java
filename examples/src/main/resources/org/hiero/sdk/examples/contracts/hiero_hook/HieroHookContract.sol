// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.4.9 <0.9.0;
pragma experimental ABIEncoderV2;
/// The interface for a generic EVM hook.
interface IHieroHook {
    /// The context the hook is executing in
    struct HookContext {
        /// The address of the entity the hook is executing on behalf of
        address owner;
        /// The fee the transaction payer was charged for the triggering transaction
        uint256 txnFee;
        /// The gas cost the transaction payer was charged for specifically this hook
        uint256 gasCost;
        /// The memo of the triggering transaction
        string memo;
        /// Any extra call data passed to the hook
        bytes data;
    }
}
/// The interface for an account allowance hook invoked once before a CryptoTransfer.
interface IHieroAccountAllowanceHook {
    /// A single balance adjustment in the range of a Hiero native token
    struct AccountAmount {
        // The address of the account whose balance is changing
        address account;
        // The amount in atomic units of the change
        int64 amount;
    }
    /// A single NFT ownership change
    struct NftTransfer {
        // The address of the sender
        address sender;
        // The address of the receiver
        address receiver;
        // The serial number being transferred
        int64 serialNo;
    }
    /// A zero-sum list of balance adjustments for a Hiero-native token
    struct TokenTransferList {
        // The Hiero token address
        address token;
        // For a fungible token, the zero-sum balance adjustments
        AccountAmount[] adjustments;
        // For a non-fungible token, the NFT ownership changes
        NftTransfer[] nftTransfers;
    }
    /// Combines HBAR and HTS asset transfers.
    struct Transfers {
        /// A zero-sum list of balance adjustments for HBAR specifically
        AccountAmount[] hbarAdjustments;
        /// The HTS token transfers
        TokenTransferList[] tokens;
    }
    /// Combines the full proposed transfers for a Hiero transaction,
    /// including both its direct transfers and the implied HIP-18
    /// custom fee transfers.
    struct ProposedTransfers {
        /// The transaction's direct transfers
        Transfers direct;
        /// The transaction's assessed custom fees
        Transfers customFee;
    }
    /// Decides if the proposed transfers are allowed, optionally in
    /// the presence of additional context encoded by the transaction
    /// payer in the extra calldata.
    /// @param context The context of the hook call
    /// @param proposedTransfers The proposed transfers
    /// @return true If the proposed transfers are allowed, false or revert otherwise
    function allow(
        IHieroHook.HookContext calldata context,
        ProposedTransfers memory proposedTransfers
    ) external payable returns (bool);
}
contract AlwaysAllowAccountAllowancePrePostHook is IHieroAccountAllowanceHook {
    function allowPre(
        IHieroHook.HookContext calldata /* context */,
        ProposedTransfers memory /* proposedTransfers */
    ) external payable returns (bool) {
        return true;
    }
    function allowPost(
        IHieroHook.HookContext calldata /* context */,
        ProposedTransfers memory /* proposedTransfers */
    ) external payable returns (bool) {
        return true;
    }
    function allow(
        IHieroHook.HookContext calldata context,
        ProposedTransfers memory proposedTransfers
    ) external payable returns (bool) {
        return true;
    }
}
