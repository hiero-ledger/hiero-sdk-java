// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.Objects;

abstract class CustomFeeBase<F extends CustomFeeBase<F>> extends CustomFee {

    /**
     * Assign the fee collector account id.
     *
     * @param feeCollectorAccountId     the account id of the fee collector
     * @return {@code this}
     */
    public F setFeeCollectorAccountId(AccountId feeCollectorAccountId) {
        this.feeCollectorAccountId = Objects.requireNonNull(feeCollectorAccountId);
        // noinspection unchecked
        return (F) this;
    }

    /**
     * If true, exempts all the token's fee collection accounts from this fee.
     * (The token's treasury and the above fee_collector_account_id will always
     * be exempt. Please see <a href="https://hips.hedera.com/hip/hip-573">HIP-573</a>
     * for details.)
     *
     * @param allCollectorsAreExempt whether all fee collectors are exempt from fees
     * @return {@code this}
     */
    public F setAllCollectorsAreExempt(boolean allCollectorsAreExempt) {
        this.allCollectorsAreExempt = allCollectorsAreExempt;
        // noinspection unchecked
        return (F) this;
    }

    abstract F deepCloneSubclass();

    /**
     * Finishes the deep clone by setting the fields of the {@link CustomFeeBase} class
     *
     * @param source    the source object
     * @return the cloned object
     */
    protected F finishDeepClone(CustomFeeBase<F> source) {
        feeCollectorAccountId = source.feeCollectorAccountId;
        allCollectorsAreExempt = source.getAllCollectorsAreExempt();

        // noinspection unchecked
        return (F) this;
    }

    @Override
    CustomFee deepClone() {
        return deepCloneSubclass();
    }
}
