// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Enum for the fee assessment method.
 * <p>
 * The terminology here (exclusive vs inclusive) is borrowed from tax assessment.
 */
public enum FeeAssessmentMethod {

    /**
     * If Alice is paying Bob, and an <b>inclusive</b> fractional fee is collected to be sent to Charlie,
     * the amount Alice declares she will pay in the transfer transaction <b>includes</b> the fee amount.
     * <p>
     * In other words, Bob receives the amount that Alice intended to send, minus the fee.
     */
    INCLUSIVE(false),
    /**
     * If Alice is paying Bob, and an <b>exclusive</b> fractional fee is collected to be sent to Charlie,
     * the amount Alice declares she will pay in the transfer transaction <b>does not</b> include the fee amount.
     * <p>
     * In other words, Alice is charged the fee <b>in addition to</b> the amount she intended to send to Bob.
     */
    EXCLUSIVE(true);

    final boolean code;

    FeeAssessmentMethod(boolean code) {
        this.code = code;
    }

    static FeeAssessmentMethod valueOf(boolean code) {
        return code ? EXCLUSIVE : INCLUSIVE;
    }

    @Override
    public String toString() {
        return code ? "EXCLUSIVE" : "INCLUSIVE";
    }
}
