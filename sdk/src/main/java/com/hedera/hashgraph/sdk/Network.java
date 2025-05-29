// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Internal utility class.
 */
class Network extends BaseNetwork<Network, AccountId, Node> {
    @Nullable
    private Integer maxNodesPerRequest;

    /**
     * The protobuf address book converted into a map of node account IDs to NodeAddress
     *
     * This variable is package private so tests can use it
     */
    @Nullable
    Map<AccountId, NodeAddress> addressBook;

    private boolean verifyCertificates = true;

    private Network(ExecutorService executor, Map<String, AccountId> network) {
        super(executor);

        try {
            setNetwork(network);
        } catch (InterruptedException | TimeoutException e) {
            // This should never occur. The network is empty.
        }
    }

    /**
     * Create a network.
     *
     * @param executor                  the executor service
     * @param network                   the network records
     * @return                          the new network
     */
    static Network forNetwork(ExecutorService executor, Map<String, AccountId> network) {
        return new Network(executor, network);
    }

    /**
     * Create a mainnet network.
     *
     * @param executor                  the executor service
     * @return                          the new mainnet network
     */
    static Network forMainnet(ExecutorService executor) {
        var addressBook = getAddressBookForLedger(LedgerId.MAINNET);
        HashMap<String, AccountId> network =
                addressBookToNetwork(Objects.requireNonNull(addressBook).values());
        return new Network(executor, network).setLedgerIdInternal(LedgerId.MAINNET, addressBook);
    }

    /**
     * Create a testnet network.
     *
     * @param executor                  the executor service
     * @return                          the new testnet network
     */
    static Network forTestnet(ExecutorService executor) {
        var addressBook = getAddressBookForLedger(LedgerId.TESTNET);
        HashMap<String, AccountId> network =
                addressBookToNetwork(Objects.requireNonNull(addressBook).values());
        return new Network(executor, network).setLedgerIdInternal(LedgerId.TESTNET, addressBook);
    }

    /**
     * Create a previewnet network.
     *
     * @param executor                  the executor service
     * @return                          the new previewnet network
     */
    static Network forPreviewnet(ExecutorService executor) {
        var addressBook = getAddressBookForLedger(LedgerId.PREVIEWNET);
        HashMap<String, AccountId> network =
                addressBookToNetwork(Objects.requireNonNull(addressBook).values());
        return new Network(executor, network).setLedgerIdInternal(LedgerId.PREVIEWNET, addressBook);
    }

    /**
     * Are certificates being verified?
     *
     * @return                          are certificates being verified
     */
    boolean isVerifyCertificates() {
        return verifyCertificates;
    }

    /**
     * Assign the desired verify certificate status.
     *
     * @param verifyCertificates        the desired status
     * @return {@code this}
     */
    synchronized Network setVerifyCertificates(boolean verifyCertificates) {
        this.verifyCertificates = verifyCertificates;

        for (var node : nodes) {
            node.setVerifyCertificates(verifyCertificates);
        }

        return this;
    }

    @Override
    synchronized Network setLedgerId(@Nullable LedgerId ledgerId) {
        return setLedgerIdInternal(ledgerId, getAddressBookForLedger(ledgerId));
    }

    private Network setLedgerIdInternal(
            @Nullable LedgerId ledgerId, @Nullable Map<AccountId, NodeAddress> addressBook) {
        super.setLedgerId(ledgerId);

        this.addressBook = addressBook;
        for (var node : nodes) {
            node.setAddressBookEntry(addressBook == null ? null : addressBook.get(node.getAccountId()));
        }

        return this;
    }

    void setAddressBook(NodeAddressBook addressBook) {
        Map<AccountId, NodeAddress> newAddressBook = addressBook.getNodeAddresses().stream()
                .filter(nodeAddress -> Objects.nonNull(nodeAddress.getAccountId()))
                .collect(Collectors.toMap(
                        NodeAddress::getAccountId,
                        Function.identity(),
                        /*
                         * Here we index by AccountId ignoring any subsequent entries with the same AccountId.
                         *
                         * Currently, this seems to be needed when reloading predefined address book for testnet which contains
                         * multiple entries with the same AccountId.
                         *
                         * If it becomes necessary to better handle such cases, either the one-to-one mapping from AccountId to
                         * single NodeAddress should be abandoned or NodeAddresses with the same AccountId may need to be merged.
                         * */
                        (a, b) -> a));
        /*
         * Here we preserve the certificate hash in the case where one is previously defined and no new one is provided.
         *
         * Currently, this seems to be needed since the downloaded address book lacks the certificate hash. However,
         * it is expected the certificate hash will be provided in the future in which case this workaround will no
         * longer be necessary.
         * */
        if (null != this.addressBook) {
            for (Map.Entry<AccountId, NodeAddress> entry : newAddressBook.entrySet()) {
                NodeAddress previous = this.addressBook.get(entry.getKey());
                if (null != previous) {
                    ByteString certHash = entry.getValue().getCertHash();
                    if (null == certHash || certHash.isEmpty()) {
                        entry.getValue().setCertHash(previous.certHash);
                    }
                }
            }
        }
        this.addressBook = newAddressBook;
        for (var node : nodes) {
            node.setAddressBookEntry(this.addressBook.get(node.getAccountId()));
        }
    }

    @Nullable
    private static Map<AccountId, NodeAddress> getAddressBookForLedger(@Nullable LedgerId ledgerId) {
        return (ledgerId == null || !ledgerId.isKnownNetwork())
                ? null
                : readAddressBookResource("addressbook/" + ledgerId + ".pb");
    }

    static HashMap<String, AccountId> addressBookToNetwork(Collection<NodeAddress> addressBook) {
        var network = new HashMap<String, AccountId>();
        for (var nodeAddress : addressBook) {
            for (var endpoint : nodeAddress.addresses) {
                network.put(endpoint.toString(), nodeAddress.accountId);
            }
        }
        return network;
    }

    /**
     * Import an address book.
     *
     * @param fileName                  the file name
     * @return                          the list of address book records
     */
    static Map<AccountId, NodeAddress> readAddressBookResource(String fileName) {
        try (var inputStream = Objects.requireNonNull(Network.class.getResource("/" + fileName))
                .openStream()) {
            var contents = ByteStreams.toByteArray(inputStream);
            var nodeAddressBook = NodeAddressBook.fromBytes(ByteString.copyFrom(contents));
            var map = new HashMap<AccountId, NodeAddress>();

            for (var nodeAddress : nodeAddressBook.nodeAddresses) {
                if (nodeAddress.accountId == null) {
                    continue;
                }

                map.put(nodeAddress.accountId, nodeAddress);
            }

            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract the of network records.
     *
     * @return                          list of network records
     */
    synchronized Map<String, AccountId> getNetwork() {
        Map<String, AccountId> returnMap = new HashMap<>();
        for (var node : nodes) {
            returnMap.put(node.address.toString(), node.getAccountId());
        }
        return returnMap;
    }

    @Override
    protected Node createNodeFromNetworkEntry(Map.Entry<String, AccountId> entry) {
        var addressBookEntry = addressBook != null ? addressBook.get(entry.getValue()) : null;
        return new Node(entry.getValue(), entry.getKey(), executor)
                .setAddressBookEntry(addressBookEntry)
                .setVerifyCertificates(verifyCertificates);
    }

    /**
     * Pick 1/3 of the nodes sorted by health and expected delay from the network.
     * This is used by Query and Transaction for selecting node AccountId's.
     *
     * @return {@link java.util.List<com.hedera.hashgraph.sdk.AccountId>}
     */
    synchronized List<AccountId> getNodeAccountIdsForExecute() throws InterruptedException {
        var nodes = getNumberOfMostHealthyNodes(getNumberOfNodesForRequest());
        var nodeAccountIds = new ArrayList<AccountId>(nodes.size());

        for (var node : nodes) {
            nodeAccountIds.add(node.getAccountId());
        }

        return nodeAccountIds;
    }

    /**
     * Assign the maximum nodes to be returned for each request.
     *
     * @param maxNodesPerRequest        the desired number of nodes
     * @return {@code this}
     */
    Network setMaxNodesPerRequest(int maxNodesPerRequest) {
        this.maxNodesPerRequest = maxNodesPerRequest;
        return this;
    }

    /**
     * Extract the number of nodes for each request.
     *
     * @return                          the number of nodes for each request
     */
    int getNumberOfNodesForRequest() {
        if (maxNodesPerRequest != null) {
            return Math.min(maxNodesPerRequest, network.size());
        } else {
            return (network.size() + 3 - 1) / 3;
        }
    }

    private List<Node> getNodesForKey(AccountId key) {
        if (network.containsKey(key)) {
            return network.get(key);
        } else {
            var newList = new ArrayList<Node>();
            network.put(key, newList);
            return newList;
        }
    }

    /**
     * Enable or disable transport security (TLS).
     *
     * @param transportSecurity         should transport security be enabled
     * @return {@code this}
     * @throws InterruptedException     when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    synchronized Network setTransportSecurity(boolean transportSecurity) throws InterruptedException {
        if (this.transportSecurity != transportSecurity) {
            network.clear();

            for (int i = 0; i < nodes.size(); i++) {
                var node = nodes.get(i);
                node.close(closeTimeout);

                node = transportSecurity ? node.toSecure() : node.toInsecure();

                nodes.set(i, node);
                getNodesForKey(node.getKey()).add(node);
            }
        }

        healthyNodes = new ArrayList<>(nodes);

        this.transportSecurity = transportSecurity;

        return this;
    }
}
