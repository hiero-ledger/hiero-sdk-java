// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.SubType;

/**
 * Enum for the fee data types.
 */
public enum FeeDataType {
    /**
     * The resource cost for the transaction type has no additional attributes
     */
    DEFAULT(SubType.DEFAULT),

    /**
     * The resource cost for the transaction type includes an operation on a
     * fungible/common token
     */
    TOKEN_FUNGIBLE_COMMON(SubType.TOKEN_FUNGIBLE_COMMON),

    /**
     * The resource cost for the transaction type includes an operation on
     * a non-fungible/unique token
     */
    TOKEN_NON_FUNGIBLE_UNIQUE(SubType.TOKEN_NON_FUNGIBLE_UNIQUE),

    /**
     * The resource cost for the transaction type includes an operation on a
     * fungible/common token with a custom fee schedule
     */
    TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES(SubType.TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES),

    /**
     * The resource cost for the transaction type includes an operation on a
     * non-fungible/unique token with a custom fee schedule
     */
    TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES(SubType.TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES),

    /**
     * The resource cost for the transaction type includes a ScheduleCreate
     * containing a ContractCall.
     */
    SCHEDULE_CREATE_CONTRACT_CALL(SubType.SCHEDULE_CREATE_CONTRACT_CALL),

    /**
     * The resource cost for the transaction type includes a TopicCreate
     * with custom fees.
     */
    TOPIC_CREATE_WITH_CUSTOM_FEES(SubType.TOPIC_CREATE_WITH_CUSTOM_FEES),

    /**
     * The resource cost for the transaction type includes a ConsensusSubmitMessage
     * for a topic with custom fees.
     */
    SUBMIT_MESSAGE_WITH_CUSTOM_FEES(SubType.SUBMIT_MESSAGE_WITH_CUSTOM_FEES);

    final SubType code;

    FeeDataType(SubType code) {
        this.code = code;
    }

    static FeeDataType valueOf(SubType code) {
        return switch (code) {
            case DEFAULT -> DEFAULT;
            case TOKEN_FUNGIBLE_COMMON -> TOKEN_FUNGIBLE_COMMON;
            case TOKEN_NON_FUNGIBLE_UNIQUE -> TOKEN_NON_FUNGIBLE_UNIQUE;
            case TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES -> TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES;
            case TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES -> TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES;
            case SCHEDULE_CREATE_CONTRACT_CALL -> SCHEDULE_CREATE_CONTRACT_CALL;
            case TOPIC_CREATE_WITH_CUSTOM_FEES -> TOPIC_CREATE_WITH_CUSTOM_FEES;
            case SUBMIT_MESSAGE_WITH_CUSTOM_FEES -> SUBMIT_MESSAGE_WITH_CUSTOM_FEES;
            default -> throw new IllegalStateException("(BUG) unhandled SubType (FeeDataType)");
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case DEFAULT -> "DEFAULT";
            case TOKEN_FUNGIBLE_COMMON -> "TOKEN_FUNGIBLE_COMMON";
            case TOKEN_NON_FUNGIBLE_UNIQUE -> "TOKEN_NON_FUNGIBLE_UNIQUE";
            case TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES -> "TOKEN_FUNGIBLE_COMMON_WITH_CUSTOM_FEES";
            case TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES -> "TOKEN_NON_FUNGIBLE_UNIQUE_WITH_CUSTOM_FEES";
            case SCHEDULE_CREATE_CONTRACT_CALL -> "SCHEDULE_CREATE_CONTRACT_CALL";
            case TOPIC_CREATE_WITH_CUSTOM_FEES -> "TOPIC_CREATE_WITH_CUSTOM_FEES";
            case SUBMIT_MESSAGE_WITH_CUSTOM_FEES -> "SUBMIT_MESSAGE_WITH_CUSTOM_FEES";
        };
    }
}
