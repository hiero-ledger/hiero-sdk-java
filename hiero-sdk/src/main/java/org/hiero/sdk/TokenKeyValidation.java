// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Types of validation strategies for token keys.
 *
 */
public enum TokenKeyValidation {
    /**
     * Currently the default behaviour. It will perform all token key validations.
     */
    FULL_VALIDATION(org.hiero.sdk.proto.TokenKeyValidation.FULL_VALIDATION),

    /**
     * Perform no validations at all for all passed token keys.
     */
    NO_VALIDATION(org.hiero.sdk.proto.TokenKeyValidation.NO_VALIDATION);

    final org.hiero.sdk.proto.TokenKeyValidation code;

    /**
     * Constructor.
     *
     * @param code the token key validation
     */
    TokenKeyValidation(org.hiero.sdk.proto.TokenKeyValidation code) {
        this.code = code;
    }

    static TokenKeyValidation valueOf(org.hiero.sdk.proto.TokenKeyValidation code) {
        return switch (code) {
            case FULL_VALIDATION -> FULL_VALIDATION;
            case NO_VALIDATION -> NO_VALIDATION;
            default -> throw new IllegalStateException("(BUG) unhandled TokenKeyValidation");
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case FULL_VALIDATION -> "FULL_VALIDATION";
            case NO_VALIDATION -> "NO_VALIDATION";
        };
    }

    public org.hiero.sdk.proto.TokenKeyValidation toProtobuf() {
        return this.code;
    }
}
