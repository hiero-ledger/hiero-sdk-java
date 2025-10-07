// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusTest {
    @Test
    @DisplayName("Status can be constructed from any ResponseCode")
    void statusToResponseCode() {
        for (ResponseCodeEnum code : ResponseCodeEnum.values()) {
            // not an actual value we want to handle
            // this is what we're given if an unexpected value was decoded
            if (code == ResponseCodeEnum.UNRECOGNIZED) {
                continue;
            }

            Status status = Status.valueOf(code);

            assertThat(code.getNumber()).isEqualTo(status.code.getNumber());
        }
    }

    @Test
    @DisplayName("Status throws on Unrecognized")
    void statusUnrecognized() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Status.valueOf(ResponseCodeEnum.UNRECOGNIZED))
                .withMessage("network returned unrecognized response code; your SDK may be out of date");
    }

    @Test
    @DisplayName("New hook- and lambda-related Status codes round-trip and map correctly")
    void newHookAndLambdaStatusesRoundTrip() {
        Object[][] pairs = new Object[][] {
            {
                Status.INVALID_SERIALIZED_TX_MESSAGE_HASH_ALGORITHM,
                ResponseCodeEnum.INVALID_SERIALIZED_TX_MESSAGE_HASH_ALGORITHM
            },
            {Status.EVM_HOOK_GAS_THROTTLED, ResponseCodeEnum.EVM_HOOK_GAS_THROTTLED},
            {Status.HOOK_ID_IN_USE, ResponseCodeEnum.HOOK_ID_IN_USE},
            {Status.BAD_HOOK_REQUEST, ResponseCodeEnum.BAD_HOOK_REQUEST},
            {Status.REJECTED_BY_ACCOUNT_ALLOWANCE_HOOK, ResponseCodeEnum.REJECTED_BY_ACCOUNT_ALLOWANCE_HOOK},
            {Status.HOOK_NOT_FOUND, ResponseCodeEnum.HOOK_NOT_FOUND},
            {Status.LAMBDA_STORAGE_UPDATE_BYTES_TOO_LONG, ResponseCodeEnum.LAMBDA_STORAGE_UPDATE_BYTES_TOO_LONG},
            {
                Status.LAMBDA_STORAGE_UPDATE_BYTES_MUST_USE_MINIMAL_REPRESENTATION,
                ResponseCodeEnum.LAMBDA_STORAGE_UPDATE_BYTES_MUST_USE_MINIMAL_REPRESENTATION
            },
            {Status.INVALID_HOOK_ID, ResponseCodeEnum.INVALID_HOOK_ID},
            {Status.EMPTY_LAMBDA_STORAGE_UPDATE, ResponseCodeEnum.EMPTY_LAMBDA_STORAGE_UPDATE},
            {Status.HOOK_ID_REPEATED_IN_CREATION_DETAILS, ResponseCodeEnum.HOOK_ID_REPEATED_IN_CREATION_DETAILS},
            {Status.HOOKS_NOT_ENABLED, ResponseCodeEnum.HOOKS_NOT_ENABLED},
            {Status.HOOK_IS_NOT_A_LAMBDA, ResponseCodeEnum.HOOK_IS_NOT_A_LAMBDA},
            {Status.HOOK_DELETED, ResponseCodeEnum.HOOK_DELETED},
            {Status.TOO_MANY_LAMBDA_STORAGE_UPDATES, ResponseCodeEnum.TOO_MANY_LAMBDA_STORAGE_UPDATES},
            {
                Status.HOOK_CREATION_BYTES_MUST_USE_MINIMAL_REPRESENTATION,
                ResponseCodeEnum.HOOK_CREATION_BYTES_MUST_USE_MINIMAL_REPRESENTATION
            },
            {Status.HOOK_CREATION_BYTES_TOO_LONG, ResponseCodeEnum.HOOK_CREATION_BYTES_TOO_LONG},
            {Status.INVALID_HOOK_CREATION_SPEC, ResponseCodeEnum.INVALID_HOOK_CREATION_SPEC},
            {Status.HOOK_EXTENSION_EMPTY, ResponseCodeEnum.HOOK_EXTENSION_EMPTY},
            {Status.INVALID_HOOK_ADMIN_KEY, ResponseCodeEnum.INVALID_HOOK_ADMIN_KEY},
            {
                Status.HOOK_DELETION_REQUIRES_ZERO_STORAGE_SLOTS,
                ResponseCodeEnum.HOOK_DELETION_REQUIRES_ZERO_STORAGE_SLOTS
            },
            {Status.CANNOT_SET_HOOKS_AND_APPROVAL, ResponseCodeEnum.CANNOT_SET_HOOKS_AND_APPROVAL},
            {Status.TRANSACTION_REQUIRES_ZERO_HOOKS, ResponseCodeEnum.TRANSACTION_REQUIRES_ZERO_HOOKS},
            {Status.INVALID_HOOK_CALL, ResponseCodeEnum.INVALID_HOOK_CALL},
            {Status.HOOKS_ARE_NOT_SUPPORTED_IN_AIRDROPS, ResponseCodeEnum.HOOKS_ARE_NOT_SUPPORTED_IN_AIRDROPS},
        };

        for (Object[] pair : pairs) {
            Status status = (Status) pair[0];
            ResponseCodeEnum code = (ResponseCodeEnum) pair[1];

            // enum holds the exact ResponseCodeEnum
            assertThat(status.code).isEqualTo(code);
            // valueOf maps ResponseCodeEnum back to Status
            assertThat(Status.valueOf(code)).isEqualTo(status);
            // toResponseCode preserves numeric value
            assertThat(status.toResponseCode()).isEqualTo(code.getNumber());
            // toString mirrors code.name()
            assertThat(status.toString()).isEqualTo(code.name());
        }
    }
}
