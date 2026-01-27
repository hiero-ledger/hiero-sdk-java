// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HookEntityIdTest {
    @Test
    void accountVariantToFromProto() {
        var acct = new AccountId(0, 0, 1234);
        var id = new HookEntityId(acct);
        assertThat(id.isAccount()).isTrue();
        assertThat(id.isContract()).isFalse();
        assertThat(id.getAccountId()).isEqualTo(acct);
        assertThat(id.getContractId()).isNull();

        var proto = id.toProtobuf();
        var parsed = HookEntityId.fromProtobuf(proto);
        assertThat(parsed).isEqualTo(id);
        assertThat(parsed.hashCode()).isEqualTo(id.hashCode());
    }

    @Test
    void contractVariantToFromProto() {
        var contract = new ContractId(0, 0, 5678);
        var id = new HookEntityId(contract);
        assertThat(id.isAccount()).isFalse();
        assertThat(id.isContract()).isTrue();
        assertThat(id.getAccountId()).isNull();
        assertThat(id.getContractId()).isEqualTo(contract);

        var proto = id.toProtobuf();
        var parsed = HookEntityId.fromProtobuf(proto);
        assertThat(parsed).isEqualTo(id);
    }
}
