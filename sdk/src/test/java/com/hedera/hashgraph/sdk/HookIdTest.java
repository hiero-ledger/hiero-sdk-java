// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HookIdTest {
    @Test
    void toFromProtoAndEquality() {
        var acct = new AccountId(0, 0, 1001);
        var entity = new HookEntityId(acct);
        var hookId = new HookId(entity, 42L);

        var proto = hookId.toProtobuf();
        var parsed = HookId.fromProtobuf(proto);
        assertThat(parsed).isEqualTo(hookId);
        assertThat(parsed.hashCode()).isEqualTo(hookId.hashCode());
        assertThat(parsed.getEntityId()).isEqualTo(entity);
        assertThat(parsed.getHookId()).isEqualTo(42L);
    }
}
