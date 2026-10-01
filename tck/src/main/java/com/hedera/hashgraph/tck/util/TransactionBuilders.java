// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction;
import com.hedera.hashgraph.sdk.AccountAllowanceDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountUpdateTransaction;
import com.hedera.hashgraph.sdk.CustomFixedFee;
import com.hedera.hashgraph.sdk.EthereumTransaction;
import com.hedera.hashgraph.sdk.FileAppendTransaction;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HbarUnit;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.NftId;
import com.hedera.hashgraph.sdk.PendingAirdropId;
import com.hedera.hashgraph.sdk.TokenAirdropTransaction;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenBurnTransaction;
import com.hedera.hashgraph.sdk.TokenCancelAirdropTransaction;
import com.hedera.hashgraph.sdk.TokenClaimAirdropTransaction;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TokenDissociateTransaction;
import com.hedera.hashgraph.sdk.TokenFeeScheduleUpdateTransaction;
import com.hedera.hashgraph.sdk.TokenFreezeTransaction;
import com.hedera.hashgraph.sdk.TokenGrantKycTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TokenPauseTransaction;
import com.hedera.hashgraph.sdk.TokenRejectTransaction;
import com.hedera.hashgraph.sdk.TokenRevokeKycTransaction;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.sdk.TokenUnfreezeTransaction;
import com.hedera.hashgraph.sdk.TokenUnpauseTransaction;
import com.hedera.hashgraph.sdk.TokenUpdateTransaction;
import com.hedera.hashgraph.sdk.TokenWipeTransaction;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicDeleteTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TopicUpdateTransaction;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.tck.methods.sdk.AccountService;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountAllowanceParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AccountUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.ethereum.EthereumTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileAppendParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.AssociateDisassociateTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.BurnTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.FreezeUnfreezeTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.GrantRevokeTokenKycParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.MintTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.PauseUnpauseTokenParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.PendingAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenAirdropCancelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenClaimAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenRejectAirdropParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenUpdateFeeScheduleParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.token.TokenWipeParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.CreateTopicParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.CustomFeeLimit;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.DeleteTopicParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.SubmitTopicMessageParams;
import com.hedera.hashgraph.tck.methods.sdk.param.topic.UpdateTopicParams;
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

            params.key().ifPresent(key -> {
                try {
                    transaction.setKeyWithoutAlias(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.initialBalance()
                    .ifPresent(initialBalanceTinybars -> transaction.setInitialBalance(
                            Hbar.from(Long.parseLong(initialBalanceTinybars), HbarUnit.TINYBAR)));

            params.receiverSignatureRequired().ifPresent(transaction::setReceiverSignatureRequired);

            params.autoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.memo().ifPresent(transaction::setAccountMemo);

            params.maxAutoTokenAssociations()
                    .ifPresent(autoAssociations ->
                            transaction.setMaxAutomaticTokenAssociations(autoAssociations.intValue()));

            params.stakedAccountId()
                    .ifPresent(
                            stakedAccountId -> transaction.setStakedAccountId(AccountId.fromString(stakedAccountId)));

            params.stakedNodeId().ifPresent(stakedNodeId -> transaction.setStakedNodeId(Long.parseLong(stakedNodeId)));

            params.declineStakingReward().ifPresent(transaction::setDeclineStakingReward);

            params.alias().ifPresent(transaction::setAlias);

            return transaction;
        }

        public static AccountCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                AccountCreateParams typedParams = AccountCreateParams.parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountCreateParams", e);
            }
        }

        public static AccountUpdateTransaction buildUpdate(AccountUpdateParams params) {
            AccountUpdateTransaction transaction =
                    new AccountUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            params.key().ifPresent(key -> {
                try {
                    transaction.setKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.receiverSignatureRequired().ifPresent(transaction::setReceiverSignatureRequired);

            params.autoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.memo().ifPresent(transaction::setAccountMemo);

            params.expirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.maxAutoTokenAssociations()
                    .ifPresent(autoAssociations ->
                            transaction.setMaxAutomaticTokenAssociations(autoAssociations.intValue()));

            params.stakedAccountId()
                    .ifPresent(
                            stakedAccountId -> transaction.setStakedAccountId(AccountId.fromString(stakedAccountId)));

            params.stakedNodeId().ifPresent(stakedNodeId -> transaction.setStakedNodeId(Long.parseLong(stakedNodeId)));

            params.declineStakingReward().ifPresent(transaction::setDeclineStakingReward);

            return transaction;
        }

        public static AccountUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                AccountUpdateParams typedParams = AccountUpdateParams.parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountUpdateParams", e);
            }
        }

        public static AccountDeleteTransaction buildDelete(AccountDeleteParams params) {
            AccountDeleteTransaction transaction =
                    new AccountDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.deleteAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            params.transferAccountId()
                    .ifPresent(accountId -> transaction.setTransferAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static AccountDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                AccountDeleteParams typedParams = AccountDeleteParams.parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountDeleteParams", e);
            }
        }

        public static AccountAllowanceApproveTransaction buildApproveAllowance(AccountAllowanceParams params) {
            AccountAllowanceApproveTransaction transaction =
                    new AccountAllowanceApproveTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.allowances()
                    .ifPresent(allowances -> allowances.forEach(allowance -> approve(transaction, allowance)));

            return transaction;
        }

        public static AccountAllowanceApproveTransaction buildApproveAllowance(Map<String, Object> params) {
            try {
                AccountAllowanceParams typedParams = AccountAllowanceParams.parse(params);
                return buildApproveAllowance(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountAllowanceParams", e);
            }
        }

        public static AccountAllowanceDeleteTransaction buildDeleteAllowance(AccountAllowanceParams params) {
            AccountAllowanceDeleteTransaction transaction =
                    new AccountAllowanceDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.allowances()
                    .ifPresent(allowances -> allowances.forEach(allowance -> delete(transaction, allowance)));

            return transaction;
        }

        public static AccountAllowanceDeleteTransaction buildDeleteAllowance(Map<String, Object> params) {
            try {
                AccountAllowanceParams typedParams = AccountAllowanceParams.parse(params);
                return buildDeleteAllowance(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AccountAllowanceParams", e);
            }
        }

        private static void approve(
                AccountAllowanceApproveTransaction tx,
                com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams allowance) {
            AccountId owner = AccountId.fromString(allowance.ownerAccountId().orElseThrow());
            AccountId spender =
                    AccountId.fromString(allowance.spenderAccountId().orElseThrow());

            allowance
                    .hbar()
                    .ifPresent(hbar ->
                            tx.approveHbarAllowance(owner, spender, Hbar.fromTinybars(Long.parseLong(hbar.amount()))));

            allowance
                    .token()
                    .ifPresent(token -> tx.approveTokenAllowance(
                            TokenId.fromString(token.tokenId()), owner, spender, token.amount()));

            allowance.nft().ifPresent(nft -> approveNFT(tx, owner, spender, nft));
        }

        private static void delete(
                AccountAllowanceDeleteTransaction tx,
                com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams allowance) {
            var owner = AccountId.fromString(allowance.ownerAccountId().orElseThrow());
            var tokenId = allowance.tokenId().orElseThrow();

            if (allowance.serialNumbers().isPresent()) {
                allowance.serialNumbers().get().forEach(serialNumber -> {
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
            TokenId tokenId = TokenId.fromString(nft.tokenId());
            Optional<String> delegateSpender = Optional.ofNullable(nft.delegatingSpender());

            if (!nft.serialNumbers().isEmpty()) {
                nft.serialNumbers().forEach(serial -> {
                    NftId nftId = new NftId(tokenId, serial);
                    delegateSpender.ifPresentOrElse(
                            ds -> tx.approveTokenNftAllowance(nftId, owner, spender, AccountId.fromString(ds)),
                            () -> tx.approveTokenNftAllowance(nftId, owner, spender));
                });
            } else if (nft.allSerials()) {
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

            params.transfers()
                    .ifPresent(transfers ->
                            transfers.forEach(txParams -> AccountService.processTransfer(transaction, txParams)));

            return transaction;
        }

        public static TransferTransaction buildTransfer(Map<String, Object> params) {
            try {
                TransferCryptoParams typedParams = TransferCryptoParams.parse(params);
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

            setKey(params.adminKey(), transaction::setAdminKey, "admin");
            setKey(params.kycKey(), transaction::setKycKey, "kyc");
            setKey(params.freezeKey(), transaction::setFreezeKey, "freeze");
            setKey(params.wipeKey(), transaction::setWipeKey, "wipe");
            setKey(params.supplyKey(), transaction::setSupplyKey, "supply");
            setKey(params.feeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");
            setKey(params.pauseKey(), transaction::setPauseKey, "pause");
            setKey(params.metadataKey(), transaction::setMetadataKey, "metadata");

            params.name().ifPresent(transaction::setTokenName);
            params.symbol().ifPresent(transaction::setTokenSymbol);
            params.decimals().ifPresent(decimals -> transaction.setDecimals(decimals.intValue()));
            params.initialSupply()
                    .ifPresent(initialSupply -> transaction.setInitialSupply(Long.parseLong(initialSupply)));

            params.treasuryAccountId()
                    .ifPresent(treasuryAccountId ->
                            transaction.setTreasuryAccountId(AccountId.fromString(treasuryAccountId)));

            params.freezeDefault().ifPresent(transaction::setFreezeDefault);

            params.expirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.autoRenewAccountId()
                    .ifPresent(autoRenewAccountId ->
                            transaction.setAutoRenewAccountId(AccountId.fromString(autoRenewAccountId)));

            params.autoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.memo().ifPresent(transaction::setTokenMemo);
            params.metadata().ifPresent(metadata -> transaction.setTokenMetadata(metadata.getBytes()));
            params.tokenType().ifPresent(tokenType -> transaction.setTokenType(parseTokenType(tokenType)));

            params.supplyType().ifPresent(supplyType -> transaction.setSupplyType(parseSupplyType(supplyType)));

            params.maxSupply().ifPresent(maxSupply -> transaction.setMaxSupply(Long.parseLong(maxSupply)));

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
            params.customFees().ifPresent(customFees -> {
                if (!customFees.isEmpty()) {
                    List<com.hedera.hashgraph.sdk.CustomFee> sdkCustomFees =
                            customFees.get(0).fillOutCustomFees(customFees);
                    transaction.setCustomFees(sdkCustomFees);
                }
            });
        }

        public static TokenCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                TokenCreateParams typedParams = TokenCreateParams.parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenCreateParams", e);
            }
        }

        public static TokenUpdateTransaction buildUpdate(TokenUpdateParams params) {
            TokenUpdateTransaction transaction = new TokenUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.adminKey().ifPresent(key -> {
                try {
                    transaction.setAdminKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.kycKey().ifPresent(key -> {
                try {
                    transaction.setKycKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.freezeKey().ifPresent(key -> {
                try {
                    transaction.setFreezeKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.wipeKey().ifPresent(key -> {
                try {
                    transaction.setWipeKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.supplyKey().ifPresent(key -> {
                try {
                    transaction.setSupplyKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.feeScheduleKey().ifPresent(key -> {
                try {
                    transaction.setFeeScheduleKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.pauseKey().ifPresent(key -> {
                try {
                    transaction.setPauseKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.metadataKey().ifPresent(key -> {
                try {
                    transaction.setMetadataKey(KeyUtils.getKeyFromString(key));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException(e);
                }
            });

            params.name().ifPresent(transaction::setTokenName);
            params.symbol().ifPresent(transaction::setTokenSymbol);
            params.memo().ifPresent(transaction::setTokenMemo);

            params.treasuryAccountId()
                    .ifPresent(treasuryAccountId ->
                            transaction.setTreasuryAccountId(AccountId.fromString(treasuryAccountId)));

            params.autoRenewAccountId()
                    .ifPresent(autoRenewAccountId ->
                            transaction.setAutoRenewAccountId(AccountId.fromString(autoRenewAccountId)));

            params.autoRenewPeriod()
                    .ifPresent(autoRenewPeriodSeconds ->
                            transaction.setAutoRenewPeriod(Duration.ofSeconds(Long.parseLong(autoRenewPeriodSeconds))));

            params.expirationTime()
                    .ifPresent(expirationTime ->
                            transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTime))));

            params.metadata().ifPresent(metadata -> transaction.setTokenMetadata(metadata.getBytes()));

            return transaction;
        }

        public static TokenUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                TokenUpdateParams typedParams = TokenUpdateParams.parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenUpdateParams", e);
            }
        }

        public static TokenDeleteTransaction buildDelete(TokenDeleteParams params) {
            TokenDeleteTransaction transaction = new TokenDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                TokenDeleteParams typedParams = TokenDeleteParams.parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenDeleteParams", e);
            }
        }

        public static TokenMintTransaction buildMint(MintTokenParams params) {
            TokenMintTransaction transaction = new TokenMintTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            try {
                params.amount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.metadata()
                    .ifPresent(metadata -> transaction.setMetadata(
                            metadata.stream().map(Hex::decode).toList()));

            return transaction;
        }

        public static TokenMintTransaction buildMint(Map<String, Object> params) {
            try {
                MintTokenParams typedParams = MintTokenParams.parse(params);
                return buildMint(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse MintTokenParams", e);
            }
        }

        public static TokenBurnTransaction buildBurn(BurnTokenParams params) {
            TokenBurnTransaction transaction = new TokenBurnTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            try {
                params.amount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.serialNumbers().ifPresent(serialNumbers -> {
                List<Long> tokenIdList =
                        serialNumbers.stream().map(Long::parseLong).collect(Collectors.toList());
                transaction.setSerials(tokenIdList);
            });

            return transaction;
        }

        public static TokenBurnTransaction buildBurn(Map<String, Object> params) {
            try {
                BurnTokenParams typedParams = BurnTokenParams.parse(params);
                return buildBurn(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse BurnTokenParams", e);
            }
        }

        public static TokenWipeTransaction buildWipe(TokenWipeParams params) {
            TokenWipeTransaction transaction = new TokenWipeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            try {
                params.amount().ifPresent(amount -> transaction.setAmount(Long.parseLong(amount)));
            } catch (NumberFormatException e) {
                transaction.setAmount(-1L);
            }

            params.serialNumbers().ifPresent(serialNumbers -> {
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
                TokenWipeParams typedParams = TokenWipeParams.parse(params);
                return buildWipe(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenWipeParams", e);
            }
        }

        // Additional token operations for schedule support
        public static TokenAssociateTransaction buildAssociate(Map<String, Object> params) {
            try {
                AssociateDisassociateTokenParams typedParams = AssociateDisassociateTokenParams.parse(params);
                return buildAssociate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AssociateDisassociateTokenParams", e);
            }
        }

        public static TokenAssociateTransaction buildAssociate(AssociateDisassociateTokenParams params) {
            TokenAssociateTransaction transaction =
                    new TokenAssociateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));
            params.tokenIds().ifPresent(tokenIds -> {
                List<TokenId> tokenIdList =
                        tokenIds.stream().map(TokenId::fromString).collect(Collectors.toList());
                transaction.setTokenIds(tokenIdList);
            });

            return transaction;
        }

        public static TokenDissociateTransaction buildDissociate(Map<String, Object> params) {
            try {
                AssociateDisassociateTokenParams typedParams = AssociateDisassociateTokenParams.parse(params);
                return buildDissociate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse AssociateDisassociateTokenParams", e);
            }
        }

        public static TokenDissociateTransaction buildDissociate(AssociateDisassociateTokenParams params) {
            TokenDissociateTransaction transaction =
                    new TokenDissociateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));
            params.tokenIds().ifPresent(tokenIds -> {
                List<TokenId> tokenIdList =
                        tokenIds.stream().map(TokenId::fromString).collect(Collectors.toList());
                transaction.setTokenIds(tokenIdList);
            });

            return transaction;
        }

        public static TokenFreezeTransaction buildFreeze(Map<String, Object> params) {
            try {
                FreezeUnfreezeTokenParams typedParams = FreezeUnfreezeTokenParams.parse(params);
                return buildFreeze(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FreezeUnfreezeTokenParams", e);
            }
        }

        public static TokenFreezeTransaction buildFreeze(FreezeUnfreezeTokenParams params) {
            TokenFreezeTransaction transaction = new TokenFreezeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenUnfreezeTransaction buildUnfreeze(Map<String, Object> params) {
            try {
                FreezeUnfreezeTokenParams typedParams = FreezeUnfreezeTokenParams.parse(params);
                return buildUnfreeze(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FreezeUnfreezeTokenParams", e);
            }
        }

        public static TokenUnfreezeTransaction buildUnfreeze(FreezeUnfreezeTokenParams params) {
            TokenUnfreezeTransaction transaction =
                    new TokenUnfreezeTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenGrantKycTransaction buildGrantKyc(Map<String, Object> params) {
            try {
                GrantRevokeTokenKycParams typedParams = GrantRevokeTokenKycParams.parse(params);
                return buildGrantKyc(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse GrantRevokeTokenKycParams", e);
            }
        }

        public static TokenGrantKycTransaction buildGrantKyc(GrantRevokeTokenKycParams params) {
            TokenGrantKycTransaction transaction =
                    new TokenGrantKycTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenRevokeKycTransaction buildRevokeKyc(Map<String, Object> params) {
            try {
                GrantRevokeTokenKycParams typedParams = GrantRevokeTokenKycParams.parse(params);
                return buildRevokeKyc(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse GrantRevokeTokenKycParams", e);
            }
        }

        public static TokenRevokeKycTransaction buildRevokeKyc(GrantRevokeTokenKycParams params) {
            TokenRevokeKycTransaction transaction =
                    new TokenRevokeKycTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));
            params.accountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));

            return transaction;
        }

        public static TokenPauseTransaction buildPause(Map<String, Object> params) {
            try {
                PauseUnpauseTokenParams typedParams = PauseUnpauseTokenParams.parse(params);
                return buildPause(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse PauseUnpauseTokenParams", e);
            }
        }

        public static TokenPauseTransaction buildPause(PauseUnpauseTokenParams params) {
            TokenPauseTransaction transaction = new TokenPauseTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenUnpauseTransaction buildUnpause(Map<String, Object> params) {
            try {
                PauseUnpauseTokenParams typedParams = PauseUnpauseTokenParams.parse(params);
                return buildUnpause(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse PauseUnpauseTokenParams", e);
            }
        }

        public static TokenUnpauseTransaction buildUnpause(PauseUnpauseTokenParams params) {
            TokenUnpauseTransaction transaction = new TokenUnpauseTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            return transaction;
        }

        public static TokenFeeScheduleUpdateTransaction buildUpdateFeeSchedule(TokenUpdateFeeScheduleParams params) {
            TokenFeeScheduleUpdateTransaction transaction =
                    new TokenFeeScheduleUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenId().ifPresent(tokenId -> transaction.setTokenId(TokenId.fromString(tokenId)));

            params.customFees().ifPresent(customFees -> {
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
                TokenUpdateFeeScheduleParams typedParams = TokenUpdateFeeScheduleParams.parse(params);
                return buildUpdateFeeSchedule(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenUpdateFeeScheduleParams", e);
            }
        }

        public static TokenAirdropTransaction buildAirdrop(TokenAirdropParams params) {
            TokenAirdropTransaction transaction = new TokenAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.tokenTransfers().ifPresent(transferParams -> {
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
                TokenAirdropParams typedParams = TokenAirdropParams.parse(params);
                return buildAirdrop(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenAirdropParams", e);
            }
        }

        public static TokenCancelAirdropTransaction buildCancelAirdrop(TokenAirdropCancelParams params) {
            TokenCancelAirdropTransaction transaction =
                    new TokenCancelAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.pendingAirdrops().ifPresent(pendingAirdrops -> {
                for (PendingAirdropParams pendingAirdrop : pendingAirdrops) {
                    String tokenId = pendingAirdrop.tokenId().orElseThrow();
                    String senderAccountId = pendingAirdrop.senderAccountId().orElseThrow();
                    String receiverAccountId =
                            pendingAirdrop.receiverAccountId().orElseThrow();

                    // NFT token cancellation
                    if (pendingAirdrop.serialNumbers().isPresent()
                            && !pendingAirdrop.serialNumbers().get().isEmpty()) {
                        List<String> serialNumbers =
                                pendingAirdrop.serialNumbers().get();
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
                TokenAirdropCancelParams typedParams = TokenAirdropCancelParams.parse(params);
                return buildCancelAirdrop(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse TokenAirdropCancelParams", e);
            }
        }

        public static TokenClaimAirdropTransaction buildClaimAirdrop(TokenClaimAirdropParams params) {
            TokenClaimAirdropTransaction transaction =
                    new TokenClaimAirdropTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            String senderAccountId = params.senderAccountId().orElseThrow();
            String receiverAccountId = params.receiverAccountId().orElseThrow();
            String tokenId = params.tokenId().orElseThrow();

            // NFT token claiming
            if (params.serialNumbers().isPresent()
                    && !params.serialNumbers().get().isEmpty()) {
                List<String> serialNumbers = params.serialNumbers().get();
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

        public static TokenRejectTransaction buildRejectAirdrop(TokenRejectAirdropParams params) {
            TokenRejectTransaction transaction = new TokenRejectTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            String ownerAccountId = params.ownerAccountId().orElseThrow();
            transaction.setOwnerId(AccountId.fromString(ownerAccountId));

            if (params.serialNumbers().isPresent()
                    && !params.serialNumbers().get().isEmpty()) {
                List<String> serialNumbers = params.serialNumbers().get();
                for (String serialNumber : serialNumbers) {
                    transaction.addNftId(new NftId(
                            TokenId.fromString(params.tokenIds().get().getFirst()), Long.parseLong(serialNumber)));
                }
            } else if (params.tokenIds().isPresent() && !params.tokenIds().get().isEmpty()) {
                List<String> tokenIds = params.tokenIds().get();
                for (String id : tokenIds) {
                    transaction.addTokenId(TokenId.fromString(id));
                }
            }

            return transaction;
        }

        public static TokenClaimAirdropTransaction buildClaimAirdrop(Map<String, Object> params) {
            try {
                TokenClaimAirdropParams typedParams = TokenClaimAirdropParams.parse(params);
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

            params.memo().ifPresent(transaction::setTopicMemo);

            setTopicKey(params.adminKey(), transaction::setAdminKey, "admin");
            setTopicKey(params.submitKey(), transaction::setSubmitKey, "submit");
            setTopicKey(params.feeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");

            setFeeExemptKeys(params, transaction);

            params.autoRenewPeriod()
                    .ifPresent(
                            periodStr -> transaction.setAutoRenewPeriod(parseDuration(periodStr, "auto renew period")));

            setAccountId(params.autoRenewAccountId(), transaction::setAutoRenewAccountId, "auto renew account ID");

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
            params.feeExemptKeys().ifPresent(keyStrings -> {
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
            params.feeExemptKeys().ifPresent(keyStrings -> {
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
            params.customFees().ifPresent(customFees -> {
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
            params.customFees().ifPresent(customFees -> {
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
                CreateTopicParams typedParams = CreateTopicParams.parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse CreateTopicParams", e);
            }
        }

        public static TopicUpdateTransaction buildUpdate(UpdateTopicParams params) {
            TopicUpdateTransaction transaction = new TopicUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            setTopicId(params.topicId(), transaction::setTopicId);

            params.memo().ifPresent(transaction::setTopicMemo);

            setTopicKey(params.adminKey(), transaction::setAdminKey, "admin");
            setTopicKey(params.submitKey(), transaction::setSubmitKey, "submit");
            setTopicKey(params.feeScheduleKey(), transaction::setFeeScheduleKey, "fee schedule");

            setFeeExemptKeys(params, transaction);

            params.autoRenewPeriod()
                    .ifPresent(
                            periodStr -> transaction.setAutoRenewPeriod(parseDuration(periodStr, "auto renew period")));

            setAccountId(params.autoRenewAccountId(), transaction::setAutoRenewAccountId, "auto renew account ID");

            params.expirationTime()
                    .ifPresent(expirationTimeStr ->
                            transaction.setExpirationTime(parseDuration(expirationTimeStr, "expiration time")));

            setTopicCustomFees(params, transaction);

            return transaction;
        }

        public static TopicUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                UpdateTopicParams typedParams = UpdateTopicParams.parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse UpdateTopicParams", e);
            }
        }

        public static TopicDeleteTransaction buildDelete(DeleteTopicParams params) {
            TopicDeleteTransaction transaction = new TopicDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.topicId().ifPresent(topicIdStr -> {
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
                DeleteTopicParams typedParams = DeleteTopicParams.parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse DeleteTopicParams", e);
            }
        }

        public static TopicMessageSubmitTransaction buildSubmitMessage(SubmitTopicMessageParams params) {
            TopicMessageSubmitTransaction transaction =
                    new TopicMessageSubmitTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.topicId().ifPresent(topicIdStr -> {
                try {
                    transaction.setTopicId(TopicId.fromString(topicIdStr));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid topic ID: " + topicIdStr, e);
                }
            });

            if (params.message().isEmpty()) {
                throw new IllegalArgumentException("Message is required");
            } else {
                String message = params.message().get();
                transaction.setMessage(message.getBytes());
            }

            params.maxChunks().ifPresent(maxChunks -> {
                transaction.setMaxChunks(maxChunks.intValue());
            });

            params.chunkSize().ifPresent(chunkSize -> {
                transaction.setChunkSize(chunkSize.intValue());
            });

            params.customFeeLimits().ifPresent(customFeeLimits -> {
                for (CustomFeeLimit customFeeLimitParam : customFeeLimits) {
                    com.hedera.hashgraph.sdk.CustomFeeLimit sdkCustomFeeLimit =
                            new com.hedera.hashgraph.sdk.CustomFeeLimit();

                    // Set payer ID if present
                    customFeeLimitParam.payerId().ifPresent(payerIdStr -> {
                        try {
                            sdkCustomFeeLimit.setPayerId(AccountId.fromString(payerIdStr));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid payer ID: " + payerIdStr, e);
                        }
                    });

                    // Process fixed fees
                    customFeeLimitParam.fixedFees().ifPresent(fixedFees -> {
                        List<CustomFixedFee> sdkFixedFees = new ArrayList<>();

                        for (com.hedera.hashgraph.tck.methods.sdk.param.CustomFee.FixedFee fixedFee : fixedFees) {
                            CustomFixedFee sdkFixedFee = new CustomFixedFee();

                            try {
                                sdkFixedFee.setAmount(Long.parseLong(fixedFee.amount()));
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid fixed fee amount: " + fixedFee.amount(), e);
                            }

                            fixedFee.denominatingTokenId().ifPresent(tokenIdStr -> {
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
                SubmitTopicMessageParams typedParams = SubmitTopicMessageParams.parse(params);
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
            params.keys().ifPresent(keyStrings -> {
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

            params.contents().ifPresent(transaction::setContents);

            params.expirationTime().ifPresent(expirationTimeStr -> {
                transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTimeStr)));
            });

            params.memo().ifPresent(transaction::setFileMemo);

            return transaction;
        }

        public static FileCreateTransaction buildCreate(Map<String, Object> params) {
            try {
                FileCreateParams typedParams = FileCreateParams.parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileCreateParams", e);
            }
        }

        public static FileUpdateTransaction buildUpdate(FileUpdateParams params) {
            FileUpdateTransaction transaction = new FileUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.fileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            params.keys().ifPresent(keyStrings -> {
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

            params.contents().ifPresent(transaction::setContents);

            params.expirationTime().ifPresent(expirationTimeStr -> {
                transaction.setExpirationTime(Duration.ofSeconds(Long.parseLong(expirationTimeStr)));
            });

            params.memo().ifPresent(transaction::setFileMemo);

            return transaction;
        }

        public static FileUpdateTransaction buildUpdate(Map<String, Object> params) {
            try {
                FileUpdateParams typedParams = FileUpdateParams.parse(params);
                return buildUpdate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileUpdateParams", e);
            }
        }

        public static FileDeleteTransaction buildDelete(FileDeleteParams params) {
            FileDeleteTransaction transaction = new FileDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.fileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            return transaction;
        }

        public static FileDeleteTransaction buildDelete(Map<String, Object> params) {
            try {
                FileDeleteParams typedParams = FileDeleteParams.parse(params);
                return buildDelete(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileDeleteParams", e);
            }
        }

        public static FileAppendTransaction buildAppend(FileAppendParams params) {
            FileAppendTransaction transaction = new FileAppendTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            params.fileId().ifPresent(fileId -> transaction.setFileId(FileId.fromString(fileId)));

            params.contents().ifPresent(contents -> transaction.setContents(contents.getBytes()));

            params.chunkSize().ifPresent(chunkSize -> {
                transaction.setChunkSize(chunkSize.intValue());
            });

            params.maxChunks().ifPresent(maxChunks -> {
                transaction.setMaxChunks(maxChunks.intValue());
            });

            return transaction;
        }

        public static FileAppendTransaction buildAppend(Map<String, Object> params) {
            try {
                FileAppendParams typedParams = FileAppendParams.parse(params);
                return buildAppend(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse FileAppendParams", e);
            }
        }
    }

    /**
     * Ethereum-related transaction builder
     */
    public static class EthereumBuilder {
        public static EthereumTransaction buildCreate(Map<String, Object> params) {
            try {
                EthereumTransactionParams typedParams = EthereumTransactionParams.parse(params);
                return buildCreate(typedParams);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse EthereumTransactionParam", e);
            }
        }

        public static EthereumTransaction buildCreate(EthereumTransactionParams params) {
            EthereumTransaction transaction = new EthereumTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

            if (params.ethereumData() != null) {
                byte[] bytes = Hex.decode(params.ethereumData());
                transaction.setEthereumData(bytes);
            }

            if (params.callDataFileId() != null) {
                transaction.setCallDataFileId(FileId.fromString(params.callDataFileId()));
            }

            if (params.maxGasAllowance() != null) {
                transaction.setMaxGasAllowanceHbar(Hbar.fromTinybars(Long.parseLong(params.maxGasAllowance())));
            }

            return transaction;
        }
    }
}
