// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Information about a single account that is proxy staking.
 */
public final class ProxyStaker {
    /**
     * The Account ID that is proxy staking.
     */
    public final AccountId accountId;

    /**
     * The number of hbars that are currently proxy staked.
     */
    public final Hbar amount;

    /**
     * Constructor.
     *
     * @param accountId                 the account id
     * @param amount                    the amount
     */
    private ProxyStaker(AccountId accountId, long amount) {
        this.accountId = accountId;
        this.amount = Hbar.fromTinybars(amount);
    }

    /**
     * Create a proxy staker object from a protobuf.
     *
     * @param proxyStaker               the protobuf
     * @return                          the new proxy staker object
     */
    static ProxyStaker fromProtobuf(org.hiero.sdk.proto.ProxyStaker proxyStaker) {
        return new ProxyStaker(AccountId.fromProtobuf(proxyStaker.getAccountID()), proxyStaker.getAmount());
    }

    @Override
    public String toString() {
        return "ProxyStaker{" + "accountId=" + accountId + ", amount=" + amount + '}';
    }
}
