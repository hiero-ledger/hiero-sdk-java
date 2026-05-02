// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Custom exception for when there are issues with the mnemonic.
 */
public class BadMnemonicException extends Exception {
    /**
     * The mnemonic that failed validation.
     */
    public final Mnemonic mnemonic;

    /**
     * The reason for which the mnemonic failed validation.
     */
    public final BadMnemonicReason reason;

    /**
     * If not null, these are the indices in the mnemonic that were not found in the
     * BIP-39 standard English word list.
     * <p>
     * If {@code reason == BadMnemonicReason.UnknownWords} then this will be not null.
     */
    @Nullable
    public final List<Integer> unknownWordIndices;

    /**
     * Constructor.
     *
     * @param mnemonic                  the mnemonic
     * @param reason                    the reason
     * @param unknownWordIndices        the indices
     */
    BadMnemonicException(Mnemonic mnemonic, BadMnemonicReason reason, List<Integer> unknownWordIndices) {
        this.mnemonic = mnemonic;
        this.reason = reason;
        this.unknownWordIndices = unknownWordIndices;
    }

    /**
     * Constructor.
     *
     * @param mnemonic                  the mnemonic
     * @param reason                    the reason
     */
    BadMnemonicException(Mnemonic mnemonic, BadMnemonicReason reason) {
        this.mnemonic = mnemonic;
        this.reason = reason;
        this.unknownWordIndices = null;
    }
}
