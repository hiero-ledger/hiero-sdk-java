// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeAccountBalanceQuery;
import com.hedera.hashgraph.sdk.MirrorNodeTokenBalance;
import com.hedera.hashgraph.sdk.MirrorNodeTokenBalanceQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assumptions;

public class IntegrationTestEnv implements AutoCloseable {
    static final String LOCAL_CONSENSUS_NODE_ENDPOINT = "127.0.0.1:35211";
    // Local mirror REST port is 8084; 5600 is gRPC and rejects HTTP/1.1 requests.
    public static final String LOCAL_MIRROR_NODE_GRPC_ENDPOINT = "127.0.0.1:5600";
    static final AccountId LOCAL_CONSENSUS_NODE_ACCOUNT_ID = new AccountId(0, 0, 3);
    private final Client originalClient;
    public Client client;
    public PublicKey operatorKey;
    public AccountId operatorId;
    public boolean isLocalNode = false;
    private static ExecutorService clientExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public IntegrationTestEnv() throws Exception {
        this(0);
    }

    @SuppressWarnings("EmptyCatch")
    public IntegrationTestEnv(int maxNodesPerTransaction) throws Exception {
        client = createTestEnvClient();

        if (maxNodesPerTransaction == 0) {
            maxNodesPerTransaction = client.getNetwork().size();
        }

        client.setMaxNodesPerTransaction(maxNodesPerTransaction);
        originalClient = client;

        try {
            var operatorPrivateKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY"));
            operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID"));
            operatorKey = operatorPrivateKey.getPublicKey();

            client.setOperator(operatorId, operatorPrivateKey);
        } catch (RuntimeException ignored) {
        }

        operatorKey = client.getOperatorPublicKey();
        operatorId = client.getOperatorAccountId();

        assertThat(client.getOperatorAccountId()).isNotNull();
        assertThat(client.getOperatorPublicKey()).isNotNull();

        if (client.getNetwork().size() > 0 && (client.getNetwork().containsKey(LOCAL_CONSENSUS_NODE_ENDPOINT))) {
            isLocalNode = true;
        }

        var nodeGetter = new TestEnvNodeGetter(client);
        var network = new HashMap<String, AccountId>();

        var nodeCount = Math.min(client.getNetwork().size(), maxNodesPerTransaction);
        for (int i = 0; i < nodeCount; i++) {
            nodeGetter.nextNode(network);
        }
        client.setNetwork(network);
    }

    @SuppressWarnings("EmptyCatch")
    private static Client createTestEnvClient() throws Exception {
        if (System.getProperty("HEDERA_NETWORK").equals("previewnet")) {
            return Client.forPreviewnet();
        } else if (System.getProperty("HEDERA_NETWORK").equals("testnet")) {
            return Client.forTestnet();
        } else if (System.getProperty("HEDERA_NETWORK").equals("localhost")) {
            var network = new HashMap<String, AccountId>();
            network.put(LOCAL_CONSENSUS_NODE_ENDPOINT, LOCAL_CONSENSUS_NODE_ACCOUNT_ID);

            return Client.forNetwork(network, clientExecutor)
                    .setMirrorNetwork(List.of(LOCAL_MIRROR_NODE_GRPC_ENDPOINT));
        } else if (!System.getProperty("CONFIG_FILE").equals("")) {
            try {
                return Client.fromConfigFile(System.getProperty("CONFIG_FILE"));
            } catch (Exception configFileException) {
                configFileException.printStackTrace();
            }
        }
        throw new IllegalStateException("Failed to construct client for IntegrationTestEnv");
    }

    public IntegrationTestEnv useThrowawayAccount(Hbar initialBalance) throws Exception {
        var key = PrivateKey.generateED25519();
        operatorKey = key.getPublicKey();
        operatorId = new AccountCreateTransaction()
                .setInitialBalance(initialBalance)
                .setKeyWithoutAlias(key)
                .execute(client)
                .getReceipt(client)
                .accountId;

        client = Client.forNetwork(originalClient.getNetwork());
        client.setMirrorNetwork(originalClient.getMirrorNetwork());
        client.setOperator(Objects.requireNonNull(operatorId), key);
        client.setLedgerId(originalClient.getLedgerId());
        client.setMaxAttempts(15);
        return this;
    }

    public IntegrationTestEnv useThrowawayAccount() throws Exception {
        return useThrowawayAccount(new Hbar(100));
    }

    /**
     * How long to let the mirror node catch up before reading a balance that is expected <i>not</i>
     * to have changed. Such assertions cannot poll for a change, so they must wait instead.
     */
    public static final Duration MIRROR_NODE_SYNC_DELAY = Duration.ofSeconds(5);

    /**
     * Read the HBAR balance from the mirror node after giving it {@link #MIRROR_NODE_SYNC_DELAY} to
     * catch up.
     *
     * <p>Use this for assertions that a balance did <i>not</i> change — {@link #awaitMirrorBalance}
     * would be satisfied by a stale pre-transaction read and prove nothing.
     *
     * @param client the client to query with
     * @param accountId the account whose balance is being read
     * @return the HBAR balance the mirror node reports after the delay
     */
    public static Hbar mirrorBalanceAfterSync(Client client, AccountId accountId) throws Exception {
        Thread.sleep(MIRROR_NODE_SYNC_DELAY.toMillis());
        return new MirrorNodeAccountBalanceQuery().setAccountId(accountId).execute(client).hbars;
    }

    /**
     * Poll the mirror node until the reported HBAR balance satisfies {@code condition}, then return it.
     *
     * <p>{@link MirrorNodeAccountBalanceQuery} reads eventually-consistent mirror node state, so a
     * balance queried immediately after a transaction may still be the pre-transaction value. Tests
     * that assert on a balance change must wait for the mirror node to catch up rather than reading
     * once.
     *
     * @param client the client to query with
     * @param accountId the account whose balance is being polled
     * @param condition the condition the balance must satisfy
     * @return the first balance satisfying {@code condition}
     * @throws AssertionError if the condition is not met before the timeout
     */
    public static Hbar awaitMirrorBalance(Client client, AccountId accountId, Predicate<Hbar> condition)
            throws Exception {
        var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        Hbar balance = null;

        while (System.nanoTime() < deadline) {
            balance =
                    new MirrorNodeAccountBalanceQuery().setAccountId(accountId).execute(client).hbars;

            if (condition.test(balance)) {
                return balance;
            }

            Thread.sleep(1000);
        }

        throw new AssertionError("Mirror node did not report a balance matching the expected condition for " + accountId
                + " within 30s; last observed balance was " + balance);
    }

    /**
     * Read a token balance from the mirror node after giving it {@link #MIRROR_NODE_SYNC_DELAY} to catch
     * up.
     *
     * <p>Use this for assertions that a token balance did <i>not</i> change, and in particular for
     * "the account was never associated with this token" — {@link #awaitMirrorTokenBalance} would be
     * satisfied by a stale pre-transaction read and prove nothing.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is being read
     * @param tokenId the token to read
     * @return the token balance the mirror node reports after the delay
     */
    public static MirrorNodeTokenBalance mirrorTokenBalanceAfterSync(
            Client client, AccountId accountId, TokenId tokenId) throws Exception {
        Thread.sleep(MIRROR_NODE_SYNC_DELAY.toMillis());
        return new MirrorNodeTokenBalanceQuery()
                .setAccountId(accountId)
                .setTokenId(tokenId)
                .execute(client);
    }

    /**
     * Poll the mirror node until the reported token balance satisfies {@code condition}, then return it.
     *
     * <p>{@link MirrorNodeTokenBalanceQuery} reads eventually-consistent mirror node state, so a balance
     * queried immediately after a transaction may still be the pre-transaction value. Tests that assert
     * on a token balance change must wait for the mirror node to catch up rather than reading once.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is being polled
     * @param tokenId the token to poll
     * @param condition the condition the balance must satisfy
     * @return the first token balance satisfying {@code condition}
     * @throws AssertionError if the condition is not met before the timeout
     */
    public static MirrorNodeTokenBalance awaitMirrorTokenBalance(
            Client client, AccountId accountId, TokenId tokenId, Predicate<MirrorNodeTokenBalance> condition)
            throws Exception {
        var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        MirrorNodeTokenBalance balance = null;

        while (System.nanoTime() < deadline) {
            try {
                balance = new MirrorNodeTokenBalanceQuery()
                    .setAccountId(accountId)
                    .setTokenId(tokenId)
                    .execute(client);

                if (condition.test(balance)) {
                    return balance;
                }
            } catch (Exception illegalArgumentException) {
                Thread.sleep(1000);
            }
        }

        throw new AssertionError("Mirror node did not report a token balance matching the expected condition for "
                + accountId + " and token " + tokenId + " within 30s; last observed balance was " + balance);
    }

    /**
     * Assert that the account's balance in {@code tokenId} reaches {@code expected}, polling the mirror
     * node until it does.
     *
     * <p>The poll <i>is</i> the assertion: it fails with an {@link AssertionError} naming the last
     * observed balance if the expected value never arrives. This is the shape almost every token
     * balance assertion needs, because the mirror node lags the network.
     *
     * @param client the client to query with
     * @param accountId the account whose token balance is being asserted
     * @param tokenId the token to check
     * @param expected the balance to wait for
     * @throws AssertionError if the balance does not reach {@code expected} before the timeout
     */
    public static void assertTokenBalance(Client client, AccountId accountId, TokenId tokenId, long expected)
            throws Exception {
        awaitMirrorTokenBalance(client, accountId, tokenId, b -> b.isAssociated() && b.balance == expected);
    }

    /**
     * Assert that the account holds no relationship to {@code tokenId} at all.
     *
     * <p>Waits {@link #MIRROR_NODE_SYNC_DELAY} first: this is a "nothing happened" assertion, so
     * polling would be satisfied by a stale read and prove nothing.
     *
     * @param client the client to query with
     * @param accountId the account to check
     * @param tokenId the token the account must not be associated with
     * @throws AssertionError if the account is associated with the token
     */
    public static void assertTokenNotAssociated(Client client, AccountId accountId, TokenId tokenId) throws Exception {
        var balance = mirrorTokenBalanceAfterSync(client, accountId, tokenId);

        assertThat(balance.isAssociated())
                .as("account %s should not be associated with token %s, but reported %s", accountId, tokenId, balance)
                .isFalse();
    }

    // Note: this is a temporary workaround.
    // The assumption should be removed once the local node is supporting multiple nodes.
    public void assumeNotLocalNode() throws Exception {
        // first clean up the current IntegrationTestEnv...
        if (isLocalNode) {
            close();
        }

        // then skip the current test
        Assumptions.assumeFalse(isLocalNode);
    }

    @Override
    public void close() throws Exception {
        if (!operatorId.equals(originalClient.getOperatorAccountId())) {
            try {
                // Deliberately a consensus-node read: the whole balance is swept back to the
                // operator, and a stale mirror node value would overshoot the real balance and
                // fail the transfer.
                var hbarsBalance =
                        new AccountInfoQuery().setAccountId(operatorId).execute(originalClient).balance;
                new TransferTransaction()
                        .addHbarTransfer(operatorId, hbarsBalance.negated())
                        .addHbarTransfer(Objects.requireNonNull(originalClient.getOperatorAccountId()), hbarsBalance)
                        .freezeWith(originalClient)
                        .signWithOperator(client)
                        .execute(originalClient)
                        .getReceipt(originalClient);

            } catch (Exception e) {
                client.close();
            }
        }
        originalClient.close();
    }

    private static class TestEnvNodeGetter {
        private final Client client;
        private final List<Map.Entry<String, AccountId>> nodes;
        private int index = 0;

        public TestEnvNodeGetter(Client client) {
            this.client = client;
            nodes = new ArrayList<>(client.getNetwork().entrySet());
            Collections.shuffle(nodes);
        }

        public void nextNode(Map<String, AccountId> outMap) throws Exception {
            if (nodes.isEmpty()) {
                throw new IllegalStateException(
                        "IntegrationTestEnv needs another node, but there aren't enough nodes in client network");
            }
            for (; index < nodes.size(); index++) {
                var node = nodes.get(index);
                try {
                    new TransferTransaction()
                            .setNodeAccountIds(Collections.singletonList(node.getValue()))
                            .setMaxAttempts(1)
                            .addHbarTransfer(
                                    client.getOperatorAccountId(),
                                    Hbar.fromTinybars(1).negated())
                            .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1))
                            .execute(client)
                            .getReceipt(client);
                    nodes.remove(index);
                    outMap.put(node.getKey(), node.getValue());
                    return;
                } catch (Throwable err) {
                    System.err.println(err);
                }
            }
            throw new Exception("Failed to find working node in " + nodes + " for IntegrationTestEnv");
        }
    }
}
