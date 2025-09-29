// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.methods.sdk.AccountService;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountAllowanceParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.*;
import com.hedera.hashgraph.tck.methods.sdk.param.token.*;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.*;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.CustomFeeLimit;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferCryptoParams;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bouncycastle.util.encoders.Hex;

/**
 * Utility class for building Hedera transactions from parameters.
 * Provides reusable builders for all transaction types used across services.
 */
public class TransactionBuilders {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);

    /**
     * Account-related transaction builders
     */
    public static class AccountBuilder {

        public static AccountCreateTransaction buildCreate(AccountCreateParams params) {
            AccountCreateTransaction transaction =
                    new AccountCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getKey().ifPresent(key -> {
                try {
                    transaction.setKeyWithoutAlias(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getInitialBalance()
                    .ifPresent(initialBalanceTinybars -> transaction.setInitialBalance(
                            Hbar.from(Long.parseLong(initialBalanceTinybars), HbarUnit.TINYBAR)));

            params.getReceiverSignatureRequired().ifPresent(transaction::setReceiverSignatureRequired);

            params.getAutoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.getMemo().ifPresent(transaction::setAccountMemo);

            params.getMaxAutoTokenAssociations()
                    .ifPresent(autoAssociations ->
                            transaction.setMaxAutomaticTokenAssociations(autoAssociations.intValue()));

            params.getStakedAccountId()
                    .ifPresent(
                            stakedAccountId -> transaction.setStakedAccountId(AccountId.fromString(stakedAccountId)));

            params.getStakedNodeId()
                    .ifPresent(stakedNodeId -> transaction.setStakedNodeId(Long.parseLong(stakedNodeId)));

            params.getDeclineStakingReward().ifPresent(transaction::setDeclineStakingReward);

            params.getAlias().ifPresent(transaction::setAlias);

            return transaction;
        }

        public static AccountCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                AccountCreateParams typedParams = new AccountCreateParams().parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountCreateParams", e);
            }
        }

        public static AccountUpdateTransaction buildUpdate(AccountUpdateParams params) {
            AccountUpdateTransaction transaction =
                    new AccountUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            params.getKey().ifPresent(key -> {
                try {
                    transaction.setKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getReceiverSignatureRequired().ifPresent(transaction::setReceiverSignatureRequired);

            params.getAutoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.getMemo().ifPresent(transaction::setAccountMemo);

            params.getExpirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.getMaxAutoTokenAssociations()
                    .ifPresent(autoAssociations ->
                            transaction.setMaxAutomaticTokenAssociations(autoAssociations.intValue()));

            params.getStakedAccountId()
                    .ifPresent(
                            stakedAccountId -> transaction.setStakedAccountId(AccountId.fromString(stakedAccountId)));

            params.getStakedNodeId()
                    .ifPresent(stakedNodeId -> transaction.setStakedNodeId(Long.parseLong(stakedNodeId)));

            params.getDeclineStakingReward().ifPresent(transaction::setDeclineStakingReward);

            return transaction;
        }

        public static AccountUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                AccountUpdateParams typedParams = new AccountUpdateParams().parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountUpdateParams", e);
            }
        }

        public static AccountDeleteTransaction buildDelete(AccountDeleteParams params) {
            AccountDeleteTransaction transaction =
                    new AccountDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getDeleteAccountId()
                    .ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            params.getTransferAccountId()
                    .ifPresent(accountId -> transaction.setTransferAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static AccountDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                AccountDeleteParams typedParams = new AccountDeleteParams().parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountDeleteParams", e);
            }
        }

        public static AccountAllowanceApproveTransaction buildApproveAllowance(AccountAllowanceParams params) {
            AccountAllowanceApproveTransaction transaction =
                    new AccountAllowanceApproveTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getAllowances()
                    .ifPresent(allowances -> allowances.forEach(allowance -> approve(transaction, allowance)));

            return transaction;
        }

        public static AccountAllowanceApproveTransaction buildApproveAllowance(Map<String, Object> params) {
            try {
                AccountAllowanceParams typedParams =
                        (AccountAllowanceParams) new AccountAllowanceParams().parse(params);
                return buildApproveAllowance(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountAllowanceParams", e);
            }
        }

        public static AccountAllowanceDeleteTransaction buildDeleteAllowance(AccountAllowanceParams params) {
            AccountAllowanceDeleteTransaction transaction =
                    new AccountAllowanceDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getAllowances()
                    .ifPresent(allowances -> allowances.forEach(allowance -> delete(transaction, allowance)));

            return transaction;
        }

        public static AccountAllowanceDeleteTransaction buildDeleteAllowance(Map<String, Object> params) {
            try {
                AccountAllowanceParams typedParams =
                        (AccountAllowanceParams) new AccountAllowanceParams().parse(params);
                return buildDeleteAllowance(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountAllowanceParams", e);
            }
        }

        private static void approve(
                AccountAllowanceApproveTransaction tx,
                com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams allowance) {
            AccountId owner = AccountId.fromString(allowance.getOwnerAccountId().orElseThrow());
            AccountId spender =
                    AccountId.fromString(allowance.getSpenderAccountId().orElseThrow());

            allowance
                    .getHbar()
                    .ifPresent(hbar -> tx.approveHbarAllowance(
                            owner, spender, Hbar.fromTinybars(Long.parseLong(hbar.getAmount()))));

            allowance
                    .getToken()
                    .ifPresent(token -> tx.approveTokenAllowance(
                            TokenId.fromString(token.getTokenId()), owner, spender, token.getAmount()));

            allowance.getNft().ifPresent(nft -> approveNFT(tx, owner, spender, nft));
        }

        private static void delete(
                AccountAllowanceDeleteTransaction tx,
                com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams allowance) {
            var owner = AccountId.fromString(allowance.getOwnerAccountId().orElseThrow());
            var tokenId = allowance.getTokenId().orElseThrow();

            if (allowance.getSerialNumbers().isPresent()) {
                allowance.getSerialNumbers().get().forEach(serialNumber -> {
                    var nftId = new NftId(TokenId.fromString(tokenId), Long.parseLong(serialNumber));
                    tx.deleteAllTokenNftAllowances(nftId, owner);
                });
            }
        }

        private static void approveNFT(
                AccountAllowanceApproveTransaction tx,
                AccountId owner,
                AccountId spender,
                com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams.TokenNftAllowance nft) {
            TokenId tokenId = TokenId.fromString(nft.getTokenId());
            Optional<String> delegateSpender = Optional.ofNullable(nft.getDelegatingSpender());

            if (!nft.getSerialNumbers().isEmpty()) {
                nft.getSerialNumbers().forEach(serial -> {
                    NftId nftId = new NftId(tokenId, serial);
                    delegateSpender.ifPresentOrElse(
                            ds -> tx.approveTokenNftAllowance(nftId, owner, spender, AccountId.fromString(ds)),
                            () -> tx.approveTokenNftAllowance(nftId, owner, spender));
                });
            } else if (nft.getAllSerials()) {
                tx.approveTokenNftAllowanceAllSerials(tokenId, owner, spender);
            } else {
                tx.deleteTokenNftAllowanceAllSerials(tokenId, owner, spender);
            }
        }
    }

    /**
     * Transfer-related transaction builders
     */
    public static class TransferBuilder {

        public static TransferTransaction buildTransfer(TransferCryptoParams params) {
            TransferTransaction transaction = new TransferTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTransfers()
                    .ifPresent(transfers ->
                            transfers.forEach(txParams -> AccountService.processTransfer(transaction, txParams)));

            return transaction;
        }

        public static TransferTransaction buildTransfer(Map<String, Object> params) {
            try {
                TransferCryptoParams typedParams = (TransferCryptoParams) new TransferCryptoParams().parse(params);
                return buildTransfer(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TransferCryptoParams", e);
            }
        }
    }

    /**
     * Token-related transaction builders
     */
    public static class TokenBuilder {

        public static TokenCreateTransaction buildCreate(TokenCreateParams params) {
            TokenCreateTransaction transaction = new TokenCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            setKey(params.getAdminKey(), transaction::setAdminKey, "admin");
            setKey(params.getKycKey(), transaction::setKycKey, "kyc");
            setKey(params.getFreezeKey(), transaction::setFreezeKey, "freeze");
            setKey(params.getWipeKey(), transaction::setWipeKey, "wipe");
            setKey(params.getSupplyKey(), transaction::setSupplyKey, "supply");
            setKey(params.getFeeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");
            setKey(params.getPauseKey(), transaction::setPauseKey, "pause");
            setKey(params.getMetadataKey(), transaction::setMetadataKey, "metadata");

            params.getName().ifPresent(transaction::setTokenName);
            params.getSymbol().ifPresent(transaction::setTokenSymbol);
            params.getDecimals().ifPresent(decimals -> transaction.setDecimals(decimals.intValue()));
            params.getInitialSupply()
                    .ifPresent(initialSupply -> transaction.setInitialSupply(Long.parseLong(initialSupply)));

            params.getTreasuryAccountId()
                    .ifPresent(treasuryAccountId ->
                            transaction.setTreasuryAccountId(AccountId.fromString(treasuryAccountId)));

            params.getFreezeDefault().ifPresent(transaction::setFreezeDefault);

            params.getExpirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.getAutoRenewAccountId()
                    .ifPresent(autoRenewAccountId ->
                            transaction.setAutoRenewAccountId(AccountId.fromString(autoRenewAccountId)));

            params.getAutoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.getMemo().ifPresent(transaction::setTokenMemo);
            params.getMetadata().ifPresent(metadata -> transaction.setTokenMetadata(metadata.getBytes()));
            params.getTokenType().ifPresent(tokenType -> transaction.setTokenType(parseTokenType(tokenType)));

            params.getSupplyType().ifPresent(supplyType -> transaction.setSupplyType(parseSupplyType(supplyType)));

            params.getMaxSupply().ifPresent(maxSupply -> transaction.setMaxSupply(Long.parseLong(maxSupply)));

            setCustomFees(params, transaction);

            return transaction;
        }

        private static void setKey(Optional<String> keyStr, Consumer<Key> setter, String fieldLabel) {
            keyStr.ifPresent(k -> {
                try {
                    setter.accept(KeyUtils.getKeyFromString(k));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException("Invalid " + fieldLabel + " key", e);
                }
            });
        }

        private static TokenType parseTokenType(String tokenType) {
            if ("ft".equals(tokenType)) {
                return TokenType.FUNGIBLE_COMMON;
            }
            if ("nft".equals(tokenType)) {
                return TokenType.NON_FUNGIBLE_UNIQUE;
            }
            throw new IllegalArgumentException("Invalid token type");
        }

        private static TokenSupplyType parseSupplyType(String supplyType) {
            if ("infinite".equals(supplyType)) {
                return TokenSupplyType.INFINITE;
            }
            if ("finite".equals(supplyType)) {
                return TokenSupplyType.FINITE;
            }
            throw new IllegalArgumentException("Invalid supply type");
        }

        private static void setCustomFees(TokenCreateParams params, TokenCreateTransaction transaction) {
            params.getCustomFees().ifPresent(customFees -> {
                if (!customFees.isEmpty()) {
                    List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                            customFees.get(0).fillOutCustomFees(customFees);
                    transaction.setCustomFees(sdkCustomFees);
                }
            });
        }

        public static TokenCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                TokenCreateParams typedParams = (TokenCreateParams) new TokenCreateParams().parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenCreateParams", e);
            }
        }

        public static TokenUpdateTransaction buildUpdate(TokenUpdateParams params) {
            TokenUpdateTransaction transaction = new TokenUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.getAdminKey().ifPresent(key -> {
                try {
                    transaction.setAdminKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getKycKey().ifPresent(key -> {
                try {
                    transaction.setKycKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getFreezeKey().ifPresent(key -> {
                try {
                    transaction.setFreezeKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getWipeKey().ifPresent(key -> {
                try {
                    transaction.setWipeKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getSupplyKey().ifPresent(key -> {
                try {
                    transaction.setSupplyKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getFeeScheduleKey().ifPresent(key -> {
                try {
                    transaction.setFeeScheduleKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getPauseKey().ifPresent(key -> {
                try {
                    transaction.setPauseKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getMetadataKey().ifPresent(key -> {
                try {
                    transaction.setMetadataKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.getName().ifPresent(transaction::setTokenName);
            params.getSymbol().ifPresent(transaction::setTokenSymbol);
            params.getMemo().ifPresent(transaction::setTokenMemo);

            params.getTreasuryAccountId()
                    .ifPresent(treasuryAccountId ->
                            transaction.setTreasuryAccountId(AccountId.fromString(treasuryAccountId)));

            params.getAutoRenewAccountId()
                    .ifPresent(autoRenewAccountId ->
                            transaction.setAutoRenewAccountId(AccountId.fromString(autoRenewAccountId)));

            params.getAutoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.getExpirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.getMetadata().ifPresent(metadata -> transaction.setTokenMetadata(metadata.getBytes()));

            return transaction;
        }

        public static TokenUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                TokenUpdateParams typedParams = (TokenUpdateParams) new TokenUpdateParams().parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenUpdateParams", e);
            }
        }

        public static TokenDeleteTransaction buildDelete(TokenDeleteParams params) {
            TokenDeleteTransaction transaction = new TokenDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                TokenDeleteParams typedParams = (TokenDeleteParams) new TokenDeleteParams().parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenDeleteParams", e);
            }
        }

        public static TokenMintTransaction buildMint(MintTokenParams params) {
            TokenMintTransaction transaction = new TokenMintTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            try {
                params.getAmount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.getMetadata()
                    .ifPresent(metadata -> transaction.setMetadata(
                            metadata.stream().map(Hex::decode).toList()));

            return transaction;
        }

        public static TokenMintTransaction buildMint(Map<String, Object> params) {
            try {
                MintTokenParams typedParams = (MintTokenParams) new MintTokenParams().parse(params);
                return buildMint(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse MintTokenParams", e);
            }
        }

        public static TokenBurnTransaction buildBurn(BurnTokenParams params) {
            TokenBurnTransaction transaction = new TokenBurnTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            try {
                params.getAmount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.getSerialNumbers().ifPresent(serialNumbers -> {
                List<Long> tokenIdList =
                        serialNumbers.stream().map(Long::parseLong).collect(Collectors.toList());
                transaction.setSerials(tokenIdList);
            });

            return transaction;
        }

        public static TokenBurnTransaction buildBurn(Map<String, Object> params) {
            try {
                BurnTokenParams typedParams = (BurnTokenParams) new BurnTokenParams().parse(params);
                return buildBurn(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse BurnTokenParams", e);
            }
        }

        public static TokenWipeTransaction buildWipe(TokenWipeParams params) {
            TokenWipeTransaction transaction = new TokenWipeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            try {
                params.getAmount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.getSerialNumbers().ifPresent(serialNumbers -> {
                List<Long> serialNumbersList = new ArrayList<>();
                for (String serialNumber : serialNumbers) {
                    serialNumbersList.add(Long.parseLong(serialNumber));
                }
                transaction.setSerials(serialNumbersList);
            });

            return transaction;
        }

        public static TokenWipeTransaction buildWipe(Map<String, Object> params) {
            try {
                TokenWipeParams typedParams = (TokenWipeParams) new TokenWipeParams().parse(params);
                return buildWipe(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenWipeParams", e);
            }
        }

        // Additional token operations for schedule support
        public static TokenAssociateTransaction buildAssociate(Map<String, Object> params) {
            try {
                AssociateDisassociateTokenParams typedParams =
                        (AssociateDisassociateTokenParams) new AssociateDisassociateTokenParams().parse(params);
                return buildAssociate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AssociateDisassociateTokenParams", e);
            }
        }

        public static TokenAssociateTransaction buildAssociate(AssociateDisassociateTokenParams params) {
            TokenAssociateTransaction transaction =
                    new TokenAssociateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));
            params.getTokenIds().ifPresent(tokenIds -> {
                List<TokenId> tokenIdList =
                        tokenIds.stream().map(TokenId::fromString).collect(Collectors.toList());
                transaction.setTokenIds(tokenIdList);
            });

            return transaction;
        }

        public static TokenDissociateTransaction buildDissociate(Map<String, Object> params) {
            try {
                AssociateDisassociateTokenParams typedParams =
                        (AssociateDisassociateTokenParams) new AssociateDisassociateTokenParams().parse(params);
                return buildDissociate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AssociateDisassociateTokenParams", e);
            }
        }

        public static TokenDissociateTransaction buildDissociate(AssociateDisassociateTokenParams params) {
            TokenDissociateTransaction transaction =
                    new TokenDissociateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));
            params.getTokenIds().ifPresent(tokenIds -> {
                List<TokenId> tokenIdList =
                        tokenIds.stream().map(TokenId::fromString).collect(Collectors.toList());
                transaction.setTokenIds(tokenIdList);
            });

            return transaction;
        }

        public static TokenFreezeTransaction buildFreeze(Map<String, Object> params) {
            try {
                FreezeUnfreezeTokenParams typedParams =
                        (FreezeUnfreezeTokenParams) new FreezeUnfreezeTokenParams().parse(params);
                return buildFreeze(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FreezeUnfreezeTokenParams", e);
            }
        }

        public static TokenFreezeTransaction buildFreeze(FreezeUnfreezeTokenParams params) {
            TokenFreezeTransaction transaction = new TokenFreezeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenUnfreezeTransaction buildUnfreeze(Map<String, Object> params) {
            try {
                FreezeUnfreezeTokenParams typedParams =
                        (FreezeUnfreezeTokenParams) new FreezeUnfreezeTokenParams().parse(params);
                return buildUnfreeze(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FreezeUnfreezeTokenParams", e);
            }
        }

        public static TokenUnfreezeTransaction buildUnfreeze(FreezeUnfreezeTokenParams params) {
            TokenUnfreezeTransaction transaction =
                    new TokenUnfreezeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenGrantKycTransaction buildGrantKyc(Map<String, Object> params) {
            try {
                GrantRevokeTokenKycParams typedParams =
                        (GrantRevokeTokenKycParams) new GrantRevokeTokenKycParams().parse(params);
                return buildGrantKyc(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse GrantRevokeTokenKycParams", e);
            }
        }

        public static TokenGrantKycTransaction buildGrantKyc(GrantRevokeTokenKycParams params) {
            TokenGrantKycTransaction transaction =
                    new TokenGrantKycTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenRevokeKycTransaction buildRevokeKyc(Map<String, Object> params) {
            try {
                GrantRevokeTokenKycParams typedParams =
                        (GrantRevokeTokenKycParams) new GrantRevokeTokenKycParams().parse(params);
                return buildRevokeKyc(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse GrantRevokeTokenKycParams", e);
            }
        }

        public static TokenRevokeKycTransaction buildRevokeKyc(GrantRevokeTokenKycParams params) {
            TokenRevokeKycTransaction transaction =
                    new TokenRevokeKycTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenPauseTransaction buildPause(Map<String, Object> params) {
            try {
                PauseUnpauseTokenParams typedParams =
                        (PauseUnpauseTokenParams) new PauseUnpauseTokenParams().parse(params);
                return buildPause(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse PauseUnpauseTokenParams", e);
            }
        }

        public static TokenPauseTransaction buildPause(PauseUnpauseTokenParams params) {
            TokenPauseTransaction transaction = new TokenPauseTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenUnpauseTransaction buildUnpause(Map<String, Object> params) {
            try {
                PauseUnpauseTokenParams typedParams =
                        (PauseUnpauseTokenParams) new PauseUnpauseTokenParams().parse(params);
                return buildUnpause(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse PauseUnpauseTokenParams", e);
            }
        }

        public static TokenUnpauseTransaction buildUnpause(PauseUnpauseTokenParams params) {
            TokenUnpauseTransaction transaction = new TokenUnpauseTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenFeeScheduleUpdateTransaction buildUpdateFeeSchedule(TokenUpdateFeeScheduleParams params) {
            TokenFeeScheduleUpdateTransaction transaction =
                    new TokenFeeScheduleUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.getCustomFees().ifPresent(customFees -> {
                if (!customFees.isEmpty()) {
                    List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                            customFees.get(0).fillOutCustomFees(customFees);
                    transaction.setCustomFees(sdkCustomFees);
                }
            });

            return transaction;
        }

        public static TokenFeeScheduleUpdateTransaction buildUpdateFeeSchedule(Map<String, Object> params) {
            try {
                TokenUpdateFeeScheduleParams typedParams =
                        (TokenUpdateFeeScheduleParams) new TokenUpdateFeeScheduleParams().parse(params);
                return buildUpdateFeeSchedule(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenUpdateFeeScheduleParams", e);
            }
        }

        public static TokenAirdropTransaction buildAirdrop(TokenAirdropParams params) {
            TokenAirdropTransaction transaction = new TokenAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTokenTransfers().ifPresent(transferParams -> {
                for (com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferParams transferParam :
                        transferParams) {
                    try {
                        AirdropUtils.handleAirdropParam(transaction, transferParam);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to handle airdrop transfer parameter", e);
                    }
                }
            });

            return transaction;
        }

        public static TokenAirdropTransaction buildAirdrop(Map<String, Object> params) {
            try {
                TokenAirdropParams typedParams = (TokenAirdropParams) new TokenAirdropParams().parse(params);
                return buildAirdrop(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenAirdropParams", e);
            }
        }

        public static TokenCancelAirdropTransaction buildCancelAirdrop(TokenAirdropCancelParams params) {
            TokenCancelAirdropTransaction transaction =
                    new TokenCancelAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getPendingAirdrops().ifPresent(pendingAirdrops -> {
                for (PendingAirdropParams pendingAirdrop : pendingAirdrops) {
                    String tokenId = pendingAirdrop.getTokenId().orElseThrow();
                    String senderAccountId = pendingAirdrop.getSenderAccountId().orElseThrow();
                    String receiverAccountId =
                            pendingAirdrop.getReceiverAccountId().orElseThrow();

                    // NFT token cancellation
                    if (pendingAirdrop.getSerialNumbers().isPresent()
                            && !pendingAirdrop.getSerialNumbers().get().isEmpty()) {
                        List<String> serialNumbers =
                                pendingAirdrop.getSerialNumbers().get();
                        for (String serialNumber : serialNumbers) {
                            PendingAirdropId pendingAirdropId = new PendingAirdropId(
                                    AccountId.fromString(senderAccountId),
                                    AccountId.fromString(receiverAccountId),
                                    new NftId(TokenId.fromString(tokenId), Long.parseLong(serialNumber)));
                            transaction.addPendingAirdrop(pendingAirdropId);
                        }
                    } else {
                        // Fungible token cancellation
                        PendingAirdropId pendingAirdropId = new PendingAirdropId(
                                AccountId.fromString(senderAccountId),
                                AccountId.fromString(receiverAccountId),
                                TokenId.fromString(tokenId));
                        transaction.addPendingAirdrop(pendingAirdropId);
                    }
                }
            });

            return transaction;
        }

        public static TokenCancelAirdropTransaction buildCancelAirdrop(Map<String, Object> params) {
            try {
                TokenAirdropCancelParams typedParams =
                        (TokenAirdropCancelParams) new TokenAirdropCancelParams().parse(params);
                return buildCancelAirdrop(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenAirdropCancelParams", e);
            }
        }

        public static TokenClaimAirdropTransaction buildClaimAirdrop(TokenClaimAirdropParams params) {
            TokenClaimAirdropTransaction transaction =
                    new TokenClaimAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            String senderAccountId = params.getSenderAccountId().orElseThrow();
            String receiverAccountId = params.getReceiverAccountId().orElseThrow();
            String tokenId = params.getTokenId().orElseThrow();

            // NFT token claiming
            if (params.getSerialNumbers().isPresent()
                    && !params.getSerialNumbers().get().isEmpty()) {
                List<String> serialNumbers = params.getSerialNumbers().get();
                for (String serialNumber : serialNumbers) {
                    PendingAirdropId pendingAirdropId = new PendingAirdropId(
                            AccountId.fromString(senderAccountId),
                            AccountId.fromString(receiverAccountId),
                            new NftId(TokenId.fromString(tokenId), Long.parseLong(serialNumber)));
                    transaction.addPendingAirdrop(pendingAirdropId);
                }
            } else {
                // Fungible token claiming
                PendingAirdropId pendingAirdropId = new PendingAirdropId(
                        AccountId.fromString(senderAccountId),
                        AccountId.fromString(receiverAccountId),
                        TokenId.fromString(tokenId));
                transaction.addPendingAirdrop(pendingAirdropId);
            }

            return transaction;
        }

        public static TokenClaimAirdropTransaction buildClaimAirdrop(Map<String, Object> params) {
            try {
                TokenClaimAirdropParams typedParams =
                        (TokenClaimAirdropParams) new TokenClaimAirdropParams().parse(params);
                return buildClaimAirdrop(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenClaimAirdropParams", e);
            }
        }
    }

    /**
     * Topic-related transaction builders
     */
    public static class TopicBuilder {

        public static TopicCreateTransaction buildCreate(CreateTopicParams params) {
            TopicCreateTransaction transaction = new TopicCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getMemo().ifPresent(transaction::setTopicMemo);

            setTopicKey(params.getAdminKey(), transaction::setAdminKey, "admin");
            setTopicKey(params.getSubmitKey(), transaction::setSubmitKey, "submit");
            setTopicKey(params.getFeeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");

            setFeeExemptKeys(params, transaction);

            params.getAutoRenewPeriod()
                    .ifPresent(
                            periodStr -> transaction.setAutoRenewPeriod(parseDuration(periodStr, "auto renew period")));

            setAccountId(params.getAutoRenewAccountId(), transaction::setAutoRenewAccountId, "auto renew account ID");

            setTopicCustomFees(params, transaction);

            return transaction;
        }

        private static void setTopicKey(Optional<String> keyStr, Consumer<Key> setter, String label) {
            keyStr.ifPresent(k -> {
                try {
                    setter.accept(KeyUtils.getKeyFromString(k));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException("Invalid " + label + " key: " + k, e);
                }
            });
        }

        private static void setFeeExemptKeys(CreateTopicParams params, TopicCreateTransaction transaction) {
            params.getFeeExemptKeys().ifPresent(keyStrings -> {
                if (keyStrings.isEmpty()) {
                    transaction.clearFeeExemptKeys();
                } else {
                    List<Key> keys = new ArrayList<>();
                    for (String keyStr : keyStrings) {
                        try {
                            keys.add(KeyUtils.getKeyFromString(keyStr));
                        } catch (InvalidProtocolBufferException e) {
                            throw new IllegalArgumentException("Invalid fee exempt key: " + keyStr, e);
                        }
                    }
                    transaction.setFeeExemptKeys(keys);
                }
            });
        }

        private static void setFeeExemptKeys(UpdateTopicParams params, TopicUpdateTransaction transaction) {
            params.getFeeExemptKeys().ifPresent(keyStrings -> {
                if (keyStrings.isEmpty()) {
                    // Empty array means clear all fee exempt keys
                    transaction.clearFeeExemptKeys();
                } else {
                    List<Key> keys = new ArrayList<>();
                    for (String keyStr : keyStrings) {
                        try {
                            keys.add(KeyUtils.getKeyFromString(keyStr));
                        } catch (InvalidProtocolBufferException e) {
                            throw new IllegalArgumentException("Invalid fee exempt key: " + keyStr, e);
                        }
                    }
                    transaction.setFeeExemptKeys(keys);
                }
            });
        }

        private static Duration parseDuration(String secondsStr, String label) {
            try {
                long seconds = Long.parseLong(secondsStr);
                return Duration.ofSeconds(seconds);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + label + ": " + secondsStr, e);
            }
        }

        private static void setAccountId(Optional<String> idStr, Consumer<AccountId> setter, String label) {
            idStr.ifPresent(s -> {
                try {
                    setter.accept(AccountId.fromString(s));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid " + label + ": " + s, e);
                }
            });
        }

        private static void setTopicId(Optional<String> topicIdStr, Consumer<TopicId> setter) {
            topicIdStr.ifPresent(s -> {
                try {
                    setter.accept(TopicId.fromString(s));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid topic ID: " + s, e);
                }
            });
        }

        private static void setTopicCustomFees(CreateTopicParams params, TopicCreateTransaction transaction) {
            params.getCustomFees().ifPresent(customFees -> {
                if (customFees.isEmpty()) {
                    transaction.clearCustomFees();
                } else {
                    List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                            customFees.get(0).fillOutCustomFees(customFees);

                    List<CustomFixedFee> topicCustomFees = new ArrayList<>();
                    for (com.hedera.hashgraph.sdk.CustomFee fee : sdkCustomFees) {
                        if (fee instanceof CustomFixedFee) {
                            topicCustomFees.add((CustomFixedFee) fee);
                        }
                    }

                    if (!topicCustomFees.isEmpty()) {
                        transaction.setCustomFees(topicCustomFees);
                    }
                }
            });
        }

        private static void setTopicCustomFees(UpdateTopicParams params, TopicUpdateTransaction transaction) {
            params.getCustomFees().ifPresent(customFees -> {
                if (customFees.isEmpty()) {
                    // Empty array means clear all custom fees
                    transaction.clearCustomFees();
                } else {
                    List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                            customFees.get(0).fillOutCustomFees(customFees);

                    // Filter for fixed fees only as topics don't support fractional/royalty fees
                    List<CustomFixedFee> topicCustomFees = new ArrayList<>();
                    for (com.hedera.hashgraph.sdk.CustomFee fee : sdkCustomFees) {
                        if (fee instanceof CustomFixedFee) {
                            topicCustomFees.add((CustomFixedFee) fee);
                        }
                    }

                    if (!topicCustomFees.isEmpty()) {
                        transaction.setCustomFees(topicCustomFees);
                    }
                }
            });
        }

        public static TopicCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                CreateTopicParams typedParams = (CreateTopicParams) new CreateTopicParams().parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse CreateTopicParams", e);
            }
        }

        public static TopicUpdateTransaction buildUpdate(UpdateTopicParams params) {
            TopicUpdateTransaction transaction = new TopicUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            setTopicId(params.getTopicId(), transaction::setTopicId);

            params.getMemo().ifPresent(transaction::setTopicMemo);

            setTopicKey(params.getAdminKey(), transaction::setAdminKey, "admin");
            setTopicKey(params.getSubmitKey(), transaction::setSubmitKey, "submit");
            setTopicKey(params.getFeeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");

            setFeeExemptKeys(params, transaction);

            params.getAutoRenewPeriod()
                    .ifPresent(
                            periodStr -> transaction.setAutoRenewPeriod(parseDuration(periodStr, "auto renew period")));

            setAccountId(params.getAutoRenewAccountId(), transaction::setAutoRenewAccountId, "auto renew account ID");

            params.getExpirationTime()
                    .ifPresent(expirationTimeStr ->
                            transaction.setExpirationTime(parseDuration(expirationTimeStr, "expiration time")));

            setTopicCustomFees(params, transaction);

            return transaction;
        }

        public static TopicUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                UpdateTopicParams typedParams = (UpdateTopicParams) new UpdateTopicParams().parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse UpdateTopicParams", e);
            }
        }

        public static TopicDeleteTransaction buildDelete(DeleteTopicParams params) {
            TopicDeleteTransaction transaction = new TopicDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTopicId().ifPresent(topicIdStr -> {
                try {
                    transaction.setTopicId(TopicId.fromString(topicIdStr));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid topic ID: " + topicIdStr, e);
                }
            });

            return transaction;
        }

        public static TopicDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                DeleteTopicParams typedParams = (DeleteTopicParams) new DeleteTopicParams().parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse DeleteTopicParams", e);
            }
        }

        public static TopicMessageSubmitTransaction buildSubmitMessage(SubmitTopicMessageParams params) {
            TopicMessageSubmitTransaction transaction =
                    new TopicMessageSubmitTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getTopicId().ifPresent(topicIdStr -> {
                try {
                    transaction.setTopicId(TopicId.fromString(topicIdStr));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid topic ID: " + topicIdStr, e);
                }
            });

            if (params.getMessage().isEmpty()) {
                throw new IllegalArgumentException("Message is required");
            } else {
                String message = params.getMessage().get();
                transaction.setMessage(message.getBytes());
            }

            params.getMaxChunks().ifPresent(maxChunks -> {
                transaction.setMaxChunks(maxChunks.intValue());
            });

            params.getChunkSize().ifPresent(chunkSize -> {
                transaction.setChunkSize(chunkSize.intValue());
            });

            params.getCustomFeeLimits().ifPresent(customFeeLimits -> {
                for (CustomFeeLimit customFeeLimitParam : customFeeLimits) {
                    com.hedera.hashgraph.sdk.CustomFeeLimit sdkCustomFeeLimit =
                            new com.hedera.hashgraph.sdk.CustomFeeLimit();

                    // Set payer ID if present
                    customFeeLimitParam.getPayerId().ifPresent(payerIdStr -> {
                        try {
                            sdkCustomFeeLimit.setPayerId(AccountId.fromString(payerIdStr));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid payer ID: " + payerIdStr, e);
                        }
                    });

                    // Process fixed fees
                    customFeeLimitParam.getFixedFees().ifPresent(fixedFees -> {
                        List<CustomFixedFee> sdkFixedFees = new ArrayList<>();

                        for (com.hedera.hashgraph.tck.methods.sdk.param.CustomFee.FixedFee fixedFee : fixedFees) {
                            CustomFixedFee sdkFixedFee = new CustomFixedFee();

                            try {
                                sdkFixedFee.setAmount(Long.parseLong(fixedFee.getAmount()));
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(
                                        "Invalid fixed fee amount: " + fixedFee.getAmount(), e);
                            }

                            fixedFee.getDenominatingTokenId().ifPresent(tokenIdStr -> {
                                try {
                                    sdkFixedFee.setDenominatingTokenId(TokenId.fromString(tokenIdStr));
                                } catch (Exception e) {
                                    throw new IllegalArgumentException(
                                            "Invalid denominating token ID: " + tokenIdStr, e);
                                }
                            });

                            sdkFixedFees.add(sdkFixedFee);
                        }

                        sdkCustomFeeLimit.setCustomFees(sdkFixedFees);
                    });

                    transaction.addCustomFeeLimit(sdkCustomFeeLimit);
                }
            });

            return transaction;
        }

        public static TopicMessageSubmitTransaction buildSubmitMessage(Map<String, Object> params) {
            try {
                SubmitTopicMessageParams typedParams =
                        (SubmitTopicMessageParams) new SubmitTopicMessageParams().parse(params);
                return buildSubmitMessage(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse SubmitTopicMessageParams", e);
            }
        }
    }

    /**
     * File-related transaction builders
     */
    public static class FileBuilder {

        public static FileCreateTransaction buildCreate(FileCreateParams params) {
            FileCreateTransaction transaction = new FileCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            // Handle keys (optional)
            params.getKeys().ifPresent(keyStrings -> {
                try {
                    Key[] keys = new Key[keyStrings.size()];
                    for (int i = 0; i < keyStrings.size(); i++) {
                        keys[i] = KeyUtils.getKeyFromString(keyStrings.get(i));
                    }
                    transaction.setKeys(keys);
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException("Invalid key format", e);
                }
            });

            params.getContents().ifPresent(transaction::setContents);

            params.getExpirationTime().ifPresent(expirationTimeStr -> {
                transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTimeStr)));
            });

            params.getMemo().ifPresent(transaction::setFileMemo);

            return transaction;
        }

        public static FileCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                FileCreateParams typedParams = new FileCreateParams().parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileCreateParams", e);
            }
        }

        public static FileUpdateTransaction buildUpdate(FileUpdateParams params) {
            FileUpdateTransaction transaction = new FileUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getFileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            params.getKeys().ifPresent(keyStrings -> {
                try {
                    Key[] keys = new Key[keyStrings.size()];
                    for (int i = 0; i < keyStrings.size(); i++) {
                        keys[i] = KeyUtils.getKeyFromString(keyStrings.get(i));
                    }
                    transaction.setKeys(keys);
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException("Invalid key format", e);
                }
            });

            params.getContents().ifPresent(transaction::setContents);

            params.getExpirationTime().ifPresent(expirationTimeStr -> {
                transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTimeStr)));
            });

            params.getMemo().ifPresent(transaction::setFileMemo);

            return transaction;
        }

        public static FileUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                FileUpdateParams typedParams = new FileUpdateParams().parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileUpdateParams", e);
            }
        }

        public static FileDeleteTransaction buildDelete(FileDeleteParams params) {
            FileDeleteTransaction transaction = new FileDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getFileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            return transaction;
        }

        public static FileDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                FileDeleteParams typedParams = new FileDeleteParams().parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileDeleteParams", e);
            }
        }

        public static FileAppendTransaction buildAppend(FileAppendParams params) {
            FileAppendTransaction transaction = new FileAppendTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.getFileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            params.getContents().ifPresent(contents -> transaction.setContents(contents.getBytes()));

            params.getChunkSize().ifPresent(chunkSize -> {
                transaction.setChunkSize(chunkSize.intValue());
            });

            params.getMaxChunks().ifPresent(maxChunks -> {
                transaction.setMaxChunks(maxChunks.intValue());
            });

            return transaction;
        }

        public static FileAppendTransaction buildAppend(Map<String, Object> params) {
            try {
                FileAppendParams typedParams = new FileAppendParams().parse(params);
                return buildAppend(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileAppendParams", e);
            }
        }
    }
}
