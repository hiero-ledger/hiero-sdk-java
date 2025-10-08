// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HookCreationDetailsTest {

    @Test
    void constructorRejectsNullsWhereNotAllowed() {
        var lambda = new LambdaEvmHook(new ContractId(0, 0, 123));

        NullPointerException ex1 =
                assertThrows(NullPointerException.class, () -> new HookCreationDetails(null, 1L, lambda));
        assertTrue(ex1.getMessage().contains("extensionPoint cannot be null"));

        NullPointerException ex2 = assertThrows(
                NullPointerException.class,
                () -> new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, null));
        assertTrue(ex2.getMessage().contains("hook cannot be null"));
    }

    @Test
    void gettersAndHasAdminKeyWork() {
        var cid = new ContractId(0, 0, 77);
        var lambda = new LambdaEvmHook(cid);
        var admin = PrivateKey.generateED25519().getPublicKey();

        var withAdmin = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 9L, lambda, admin);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, withAdmin.getExtensionPoint());
        assertEquals(9L, withAdmin.getHookId());
        assertEquals(lambda, withAdmin.getHook());
        assertTrue(withAdmin.hasAdminKey());
        assertEquals(admin, withAdmin.getAdminKey());

        var withoutAdmin = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 10L, lambda);
        assertFalse(withoutAdmin.hasAdminKey());
        assertNull(withoutAdmin.getAdminKey());
    }

    @Test
    void protobufRoundTripPreservesValues() {
        var cid = new ContractId(0, 0, 1234);
        var lambda = new LambdaEvmHook(cid);
        var details = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                42L,
                lambda,
                PrivateKey.generateED25519().getPublicKey());

        var proto = details.toProtobuf();
        var restored = HookCreationDetails.fromProtobuf(proto);

        assertEquals(details.getExtensionPoint(), restored.getExtensionPoint());
        assertEquals(details.getHookId(), restored.getHookId());
        assertEquals(details.getHook(), restored.getHook());
        assertEquals(details.getAdminKey(), restored.getAdminKey());
        assertEquals(details, restored);
        assertEquals(details.hashCode(), restored.hashCode());
    }

    @Test
    void equalsAndHashCodeVaryByFields() {
        var lambda1 = new LambdaEvmHook(new ContractId(0, 0, 1));
        var lambda2 = new LambdaEvmHook(new ContractId(0, 0, 2));

        var a = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambda1);
        var b = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambda1);
        var c = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, lambda1);
        var d = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambda2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }

    @Test
    void toStringContainsKeyFields() {
        var lambda = new LambdaEvmHook(new ContractId(0, 0, 3));
        var details = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 7L, lambda);
        var s = details.toString();
        assertTrue(s.contains("extensionPoint"));
        assertTrue(s.contains("hookId"));
        assertTrue(s.contains("hook"));
    }
}
