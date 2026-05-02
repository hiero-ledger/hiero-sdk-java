// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// A number of transactions take List<>s as inputs.
// If the list parameter is used directly/naively, it can break encapsulation.
// That is, if you call foo.setBar(bazList), later calling bazList.add(-1) will alter the list
// that would be returned by foo.getBar().

public class ListInputTest {
    @Test
    @DisplayName("TokenAssociateTransaction list input is insulated")
    void tokenAssociateListTest() {
        var tx = new TokenAssociateTransaction();
        var list = new ArrayList<TokenId>();
        list.add(TokenId.fromString("1.2.3"));
        tx.setTokenIds(list);
        var v1 = new ArrayList<>(tx.getTokenIds());
        list.add(TokenId.fromString("4.5.6"));
        var v2 = new ArrayList<>(tx.getTokenIds());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getTokenIds();
        list2.add(TokenId.fromString("7.8.9"));
        var v3 = tx.getTokenIds();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("nodeAccountIds list input is insulated")
    void nodeAccountIdsListTest() {
        var tx = new TokenAssociateTransaction();
        var list = new ArrayList<AccountId>();
        list.add(AccountId.fromString("1.2.3"));
        tx.setNodeAccountIds(list);
        var v1 = new ArrayList<>(Objects.requireNonNull(tx.getNodeAccountIds()));
        list.add(AccountId.fromString("4.5.6"));
        var v2 = new ArrayList<>(tx.getNodeAccountIds());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getNodeAccountIds();
        list2.add(AccountId.fromString("7.8.9"));
        var v3 = tx.getNodeAccountIds();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenBurnTransaction list input is insulated")
    void tokenBurnListTest() {
        var tx = new TokenBurnTransaction();
        var list = new ArrayList<Long>();
        list.add(0L);
        tx.setSerials(list);
        var v1 = new ArrayList<>(tx.getSerials());
        list.add(1L);
        var v2 = new ArrayList<>(tx.getSerials());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getSerials();
        list2.add(2L);
        var v3 = tx.getSerials();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenWipeTransaction list input is insulated")
    void tokenWipeListTest() {
        var tx = new TokenWipeTransaction();
        var list = new ArrayList<Long>();
        list.add(0L);
        tx.setSerials(list);
        var v1 = new ArrayList<>(tx.getSerials());
        list.add(1L);
        var v2 = new ArrayList<>(tx.getSerials());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getSerials();
        list2.add(2L);
        var v3 = tx.getSerials();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenMintTransaction list input is insulated")
    void tokenMintListTest() {
        var tx = new TokenMintTransaction();
        var list = new ArrayList<byte[]>();
        list.add(new byte[] {0});
        tx.setMetadata(list);
        var v1 = new ArrayList<>(tx.getMetadata());
        list.add(new byte[] {1});
        var v2 = new ArrayList<>(tx.getMetadata());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getMetadata();
        list2.add(new byte[] {2});
        var v3 = tx.getMetadata();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenDissociateTransaction list input is insulated")
    void tokenDissociateListTest() {
        var tx = new TokenDissociateTransaction();
        var list = new ArrayList<TokenId>();
        list.add(TokenId.fromString("1.2.3"));
        tx.setTokenIds(list);
        var v1 = new ArrayList<>(tx.getTokenIds());
        list.add(TokenId.fromString("4.5.6"));
        var v2 = new ArrayList<>(tx.getTokenIds());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getTokenIds();
        list2.add(TokenId.fromString("7.8.9"));
        var v3 = tx.getTokenIds();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenCreateTransaction list input is insulated")
    void tokenCreateListTest() {
        var tx = new TokenCreateTransaction();
        var list = new ArrayList<CustomFee>();
        list.add(new CustomFixedFee().setAmount(1));
        tx.setCustomFees(list);
        var v1 = new ArrayList<>(Objects.requireNonNull(tx.getCustomFees()));
        list.add(new CustomFixedFee().setAmount(2));
        var v2 = new ArrayList<>(tx.getCustomFees());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getCustomFees();
        list2.add(new CustomFixedFee().setAmount(3));
        var v3 = tx.getCustomFees();
        assertEquals(v1.toString(), v3.toString());
    }

    @Test
    @DisplayName("TokenFeeScheduleUpdateTransaction list input is insulated")
    void tokenFeeScheduleUpdateListTest() {
        var tx = new TokenFeeScheduleUpdateTransaction();
        var list = new ArrayList<CustomFee>();
        list.add(new CustomFixedFee().setAmount(1));
        tx.setCustomFees(list);
        var v1 = new ArrayList<>(tx.getCustomFees());
        list.add(new CustomFixedFee().setAmount(2));
        var v2 = new ArrayList<>(tx.getCustomFees());
        assertEquals(v1.toString(), v2.toString());

        var list2 = tx.getCustomFees();
        list2.add(new CustomFixedFee().setAmount(3));
        var v3 = tx.getCustomFees();
        assertEquals(v1.toString(), v3.toString());
    }
}
