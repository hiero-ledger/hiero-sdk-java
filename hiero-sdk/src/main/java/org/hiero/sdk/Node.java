// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.ChannelCredentials;
import io.grpc.TlsChannelCredentials;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;

/**
 * Internal utility class.
 */
class Node extends BaseNode<Node, AccountId> {
    private final AccountId accountId;

    // This kind of shadows the address field inherited from BaseNode.
    // This is only needed for the cert hash
    @Nullable
    private NodeAddress addressBookEntry;

    private boolean verifyCertificates;

    /**
     * Constructor.
     *
     * @param accountId                 the account id
     * @param address                   the address as a managed node address
     * @param executor                  the executor service
     */
    Node(AccountId accountId, BaseNodeAddress address, ExecutorService executor) {
        super(address, executor);

        this.accountId = accountId;
    }

    /**
     * Constructor.
     * @param accountId                 the account id
     * @param address                   the address as a string
     * @param executor                  the executor service
     */
    Node(AccountId accountId, String address, ExecutorService executor) {
        this(accountId, BaseNodeAddress.fromString(address), executor);
    }

    /**
     * Constructor for a node that verifies certificates.
     *
     * @param node                      the node
     * @param address                   the address as a managed node address
     */
    Node(Node node, BaseNodeAddress address) {
        super(node, address);

        this.accountId = node.accountId;
        this.verifyCertificates = node.verifyCertificates;
        this.addressBookEntry = node.addressBookEntry;
    }

    /**
     * Create an insecure version of this node
     *
     * @return                          the insecure version of the node
     */
    Node toInsecure() {
        return new Node(this, address.toInsecure());
    }

    /**
     * Create a secure version of this node
     *
     * @return                          the secure version of the node
     */
    Node toSecure() {
        return new Node(this, address.toSecure());
    }

    @Override
    AccountId getKey() {
        return accountId;
    }

    /**
     * Extract the account id.
     *
     * @return                          the account id
     */
    AccountId getAccountId() {
        return accountId;
    }

    /**
     * Extract the address book.
     *
     * @return                          the address book
     */
    NodeAddress getAddressBookEntry() {
        return addressBookEntry;
    }

    /**
     * Assign the address book.
     *
     * @param addressBookEntry               the address book
     * @return {@code this}
     */
    Node setAddressBookEntry(@Nullable NodeAddress addressBookEntry) {
        this.addressBookEntry = addressBookEntry;
        return this;
    }

    /**
     * Are the certificates being verified?
     *
     * @return                          are the certificates being verified
     */
    boolean isVerifyCertificates() {
        return verifyCertificates;
    }

    /**
     * Assign the certificate status.
     *
     * @param verifyCertificates        should certificates be verified
     * @return {@code this}
     */
    Node setVerifyCertificates(boolean verifyCertificates) {
        this.verifyCertificates = verifyCertificates;
        return this;
    }

    @Override
    ChannelCredentials getChannelCredentials() {
        return TlsChannelCredentials.newBuilder()
                .trustManager(new HederaTrustManager(
                        addressBookEntry == null ? null : addressBookEntry.certHash, verifyCertificates))
                .build();
    }

    @Override
    public String toString() {
        return address.toString() + "->" + accountId.toString();
    }
}
