// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeeAssessmentMethodTest {
    @Test
    void feeAssessmentMethodToString() {
        assertThat(FeeAssessmentMethod.valueOf(true)).hasToString(FeeAssessmentMethod.EXCLUSIVE.toString());
        assertThat(FeeAssessmentMethod.valueOf(false)).hasToString(FeeAssessmentMethod.INCLUSIVE.toString());
    }
}
