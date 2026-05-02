// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import javax.annotation.Nullable;

/**
 * Subscribe to a topic ID's messages from a mirror node. You will receive
 * all messages for the specified topic or within the defined start and end
 * time.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/consensus/get-topic-message">Hedera Documentation</a>
 */
public final class SubscriptionHandle {
    @Nullable
    private Runnable onUnsubscribe;

    /**
     * Constructor.
     */
    SubscriptionHandle() {}

    /**
     * Assign the callback method.
     *
     * @param onUnsubscribe             the callback method
     */
    void setOnUnsubscribe(Runnable onUnsubscribe) {
        this.onUnsubscribe = onUnsubscribe;
    }

    /**
     * Call the callback.
     */
    public void unsubscribe() {
        var unsubscribe = this.onUnsubscribe;

        // Set onUnsubscribe back to null to make sure it is run just once.
        this.onUnsubscribe = null;

        if (unsubscribe != null) {
            unsubscribe.run();
        }
    }
}
