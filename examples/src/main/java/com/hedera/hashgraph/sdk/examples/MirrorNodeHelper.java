// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeAccountBalanceQuery;
import com.hedera.hashgraph.sdk.MirrorNodeTokenBalance;
import com.hedera.hashgraph.sdk.MirrorNodeTokenBalanceQuery;
import com.hedera.hashgraph.sdk.TokenId;
import java.time.Duration;
import java.util.function.Predicate;

/**
 * Helpers for reading eventually-consistent mirror node state in the examples.
 *
 * <p>The mirror node reflects network state with a lag of a few seconds, so an example that reads a
 * balance immediately after the transaction that changed it may still see the old value. Reading once
 * would make the examples intermittently print — or throw on — stale numbers.
 */
public final class MirrorNodeHelper {

    /**
     * How long to wait before reading a value that is expected <i>not</i> to have changed.
     */
    private static final Duration SYNC_DELAY = Duration.ofSeconds(5);

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private MirrorNodeHelper() {}

    /**
     * Poll the mirror node until the account's HBAR balance equals {@code expected}, then return it.
     *
     * <p>Prefer this over {@link #awaitHbarBalance(Client, AccountId, Predicate)} wherever the figure is
     * predictable — the initial balance of an account the example just created, or the result of a
     * transfer whose fee another account paid. An exact target is the strongest condition available: a
     * stale pre-transaction read cannot satisfy it.
     *
     * @param client the client to query with
     * @param accountId the account whose HBAR balance is wanted
     * @param expected the balance to wait for
     * @return the balance the mirror node reports, equal to {@code expected}
     * @throws IllegalStateException if the expected balance does not arrive before the timeout
     */
    public static Hbar awaitHbarBalance(Client client, AccountId accountId, Hbar expected) throws Exception {
        return awaitHbarBalance(client, accountId, expected::equals);
    }

    /**
     * Poll the mirror node until the account's HBAR balance satisfies {@code condition}.
     *
     * <p>Use this where the exact figure is not predictable, typically because the account paid the
     * transaction fee. Choose a condition a stale read cannot satisfy — a comparison against a balance
     * captured <i>before</i> the transaction, rather than a bare bound the old value already met.
     *
     * @param client the client to query with
     * @param accountId the account whose HBAR balance is wanted
     * @param condition the condition the balance must satisfy
     * @return the first balance satisfying {@code condition}
     * @throws IllegalStateException if the condition is not met before the timeout
     */
    public static Hbar awaitHbarBalance(Client client, AccountId accountId, Predicate<Hbar> condition)
            throws Exception {
        var deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
        Hbar balance = null;

        while (System.nanoTime() < deadline) {
            balance = hbarBalance(client, accountId);

            if (condition.test(balance)) {
                return balance;
            }

            Thread.sleep(POLL_INTERVAL.toMillis());
        }

        throw new IllegalStateException("The mirror node did not report the expected HBAR balance for account "
                + accountId + " within " + POLL_TIMEOUT.toSeconds() + "s; last observed " + balance);
    }

    /**
     * Read the account's HBAR balance from the mirror node once, without waiting.
     *
     * @param client the client to query with
     * @param accountId the account whose HBAR balance is wanted
     * @return the HBAR balance the mirror node currently reports
     */
    public static Hbar hbarBalance(Client client, AccountId accountId) throws Exception {
        return new MirrorNodeAccountBalanceQuery().setAccountId(accountId).execute(client).hbars;
    }

    /**
     * Read the account's HBAR balance after giving the mirror node time to catch up.
     *
     * <p>Use this where the value is expected <i>not</i> to have changed — polling for a change that
     * should never arrive would simply time out, and polling for the unchanged value would be satisfied
     * by a stale read and prove nothing.
     *
     * @param client the client to query with
     * @param accountId the account whose HBAR balance is wanted
     * @return the HBAR balance the mirror node reports after the delay
     */
    public static Hbar hbarBalanceAfterSync(Client client, AccountId accountId) throws Exception {
        Thread.sleep(SYNC_DELAY.toMillis());
        return hbarBalance(client, accountId);
    }

    /**
     * Poll the mirror node until the account's balance in {@code tokenId} equals {@code expected}, then
     * return it.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is wanted
     * @param tokenId the token to read
     * @param expected the balance to wait for
     * @return the balance the mirror node reports, in the token's smallest denomination or as an NFT count
     * @throws IllegalStateException if the expected balance does not arrive before the timeout
     */
    public static long awaitTokenBalance(Client client, AccountId accountId, TokenId tokenId, long expected)
            throws Exception {
        return awaitTokenBalance(client, accountId, tokenId, b -> b.isAssociated() && b.balance == expected).balance;
    }

    /**
     * Poll the mirror node until the account's balance in {@code tokenId} satisfies {@code condition}.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is wanted
     * @param tokenId the token to read
     * @param condition the condition the balance must satisfy
     * @return the first balance satisfying {@code condition}
     * @throws IllegalStateException if the condition is not met before the timeout
     */
    public static MirrorNodeTokenBalance awaitTokenBalance(
            Client client, AccountId accountId, TokenId tokenId, Predicate<MirrorNodeTokenBalance> condition)
            throws Exception {
        var deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
        MirrorNodeTokenBalance balance = null;

        while (System.nanoTime() < deadline) {
                balance = tokenBalance(client, accountId, tokenId);

                if (condition.test(balance)) {
                    return balance;
                }

                Thread.sleep(POLL_INTERVAL.toMillis());
        }

        throw new IllegalStateException("The mirror node did not report the expected balance for account " + accountId
                + " and token " + tokenId + " within " + POLL_TIMEOUT.toSeconds() + "s; last observed " + balance);
    }

    /**
     * Read the account's balance in {@code tokenId} from the mirror node once, without waiting.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is wanted
     * @param tokenId the token to read
     * @return the token balance the mirror node currently reports
     */
    public static MirrorNodeTokenBalance tokenBalance(Client client, AccountId accountId, TokenId tokenId)
            throws Exception {
        return new MirrorNodeTokenBalanceQuery()
                .setAccountId(accountId)
                .setTokenId(tokenId)
                .execute(client);
    }

    /**
     * Read the account's balance in {@code tokenId} after giving the mirror node time to catch up.
     *
     * <p>Use this where the value is expected <i>not</i> to have changed — polling for a change that
     * should never arrive would simply time out.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is wanted
     * @param tokenId the token to read
     * @return the token balance the mirror node reports after the delay
     */
    public static MirrorNodeTokenBalance tokenBalanceAfterSync(Client client, AccountId accountId, TokenId tokenId)
            throws Exception {
        Thread.sleep(SYNC_DELAY.toMillis());
        return tokenBalance(client, accountId, tokenId);
    }
}
