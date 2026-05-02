// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Possible Token Types (IWA Compatibility).
 * <p>
 * Apart from fungible and non-fungible, Tokens can have either a common or
 * unique representation. This distinction might seem subtle, but it is
 * important when considering how tokens can be traced and if they can have
 * isolated and unique properties.
 * <p>
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/tokentype">Hedera Documentation</a>
 */
public enum TokenType {
    /**
     * Interchangeable value with one another, where any quantity of them has the same value as another equal quantity if they are in the same class.
     * Share a single set of properties, not distinct from one another. Simply represented as a balance or quantity to a given Hedera account.
     */
    FUNGIBLE_COMMON(org.hiero.sdk.proto.TokenType.FUNGIBLE_COMMON),
    /**
     * Unique, not interchangeable with other tokens of the same type as they typically have different values.
     * Individually traced and can carry unique properties (e.g. serial number).
     */
    NON_FUNGIBLE_UNIQUE(org.hiero.sdk.proto.TokenType.NON_FUNGIBLE_UNIQUE);

    final org.hiero.sdk.proto.TokenType code;

    /**
     * Constructor.
     *
     * @param code the token type
     */
    TokenType(org.hiero.sdk.proto.TokenType code) {
        this.code = code;
    }

    /**
     * What type are we.
     *
     * @param code the token type in question
     * @return the token type
     */
    static TokenType valueOf(org.hiero.sdk.proto.TokenType code) {
        return switch (code) {
            case FUNGIBLE_COMMON -> FUNGIBLE_COMMON;
            case NON_FUNGIBLE_UNIQUE -> NON_FUNGIBLE_UNIQUE;
            default -> throw new IllegalStateException("(BUG) unhandled TokenType");
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case FUNGIBLE_COMMON -> "FUNGIBLE_COMMON";
            case NON_FUNGIBLE_UNIQUE -> "NON_FUNGIBLE_UNIQUE";
        };
    }

    public org.hiero.sdk.proto.TokenType toProtobuf() {
        return this.code;
    }
}
