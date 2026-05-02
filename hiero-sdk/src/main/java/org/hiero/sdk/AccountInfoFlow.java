// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Account Info Flow object.
 */
public class AccountInfoFlow {

    private static PublicKey getAccountPublicKey(Client client, AccountId accountId)
            throws PrecheckStatusException, TimeoutException {
        return requirePublicKey(
                accountId, new AccountInfoQuery().setAccountId(accountId).execute(client).key);
    }

    private static CompletableFuture<PublicKey> getAccountPublicKeyAsync(Client client, AccountId accountId) {
        return new AccountInfoQuery()
                .setAccountId(accountId)
                .executeAsync(client)
                .thenApply(accountInfo -> {
                    return requirePublicKey(accountId, accountInfo.key);
                });
    }

    private static PublicKey requirePublicKey(AccountId accountId, Key key) {
        if (key instanceof PublicKey k) {
            return k;
        }
        throw new UnsupportedOperationException("Account " + accountId + " has a KeyList key, which is not supported");
    }

    /**
     * Is the signature valid.
     *
     * @param client    the client
     * @param accountId the account id
     * @param message   the message
     * @param signature the signature
     * @return is the signature valid
     * @throws PrecheckStatusException when the precheck fails
     * @throws TimeoutException        when the transaction times out
     */
    public static boolean verifySignature(Client client, AccountId accountId, byte[] message, byte[] signature)
            throws PrecheckStatusException, TimeoutException {
        return getAccountPublicKey(client, accountId).verify(message, signature);
    }

    /**
     * Is the transaction signature valid.
     *
     * @param client      the client
     * @param accountId   the account id
     * @param transaction the signed transaction
     * @return is the transaction signature valid
     * @throws PrecheckStatusException when the precheck fails
     * @throws TimeoutException        when the transaction times out
     */
    public static boolean verifyTransactionSignature(Client client, AccountId accountId, Transaction<?> transaction)
            throws PrecheckStatusException, TimeoutException {
        return getAccountPublicKey(client, accountId).verifyTransaction(transaction);
    }

    /**
     * Asynchronously determine if the signature is valid.
     *
     * @param client    the client
     * @param accountId the account id
     * @param message   the message
     * @param signature the signature
     * @return is the signature valid
     */
    public static CompletableFuture<Boolean> verifySignatureAsync(
            Client client, AccountId accountId, byte[] message, byte[] signature) {
        return getAccountPublicKeyAsync(client, accountId).thenApply(pubKey -> pubKey.verify(message, signature));
    }

    /**
     * Asynchronously determine if the signature is valid.
     *
     * @param client      the client
     * @param accountId   the account id
     * @param transaction the signed transaction
     * @return is the signature valid
     */
    public static CompletableFuture<Boolean> verifyTransactionSignatureAsync(
            Client client, AccountId accountId, Transaction<?> transaction) {
        return getAccountPublicKeyAsync(client, accountId).thenApply(pubKey -> pubKey.verifyTransaction(transaction));
    }
}
