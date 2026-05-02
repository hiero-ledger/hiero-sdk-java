// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.security.SecureRandom;

/**
 * Internal utility class.
 */
final class ThreadLocalSecureRandom {
    @SuppressWarnings("AnonymousHasLambdaAlternative")
    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    /**
     * Constructor.
     */
    private ThreadLocalSecureRandom() {}

    /**
     * Extract seme randomness.
     *
     * @return                          some randomness
     */
    static SecureRandom current() {
        return secureRandom.get();
    }
}
