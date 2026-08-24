// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class AccessListItemTest {

    private static final byte[] ADDRESS = Hex.decode("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
    private static final byte[] KEY_1 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001");
    private static final byte[] KEY_2 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000002");

    @Test
    public void gettersSettersAndChaining() {
        var item = new AccessListItem().setAddress(ADDRESS).addStorageKey(KEY_1).addStorageKey(KEY_2);

        assertThat(Hex.toHexString(item.getAddress())).isEqualTo(Hex.toHexString(ADDRESS));
        assertThat(item.getStorageKeys()).hasSize(2);
        assertThat(Hex.toHexString(item.getStorageKeys().get(0))).isEqualTo(Hex.toHexString(KEY_1));
        assertThat(Hex.toHexString(item.getStorageKeys().get(1))).isEqualTo(Hex.toHexString(KEY_2));
    }

    @Test
    public void settersCopyDefensively() {
        byte[] mutable = ADDRESS.clone();
        var item = new AccessListItem().setAddress(mutable);
        mutable[0] = 0;
        assertThat(Hex.toHexString(item.getAddress())).isEqualTo(Hex.toHexString(ADDRESS));
    }

    @Test
    public void rlpRoundTripThroughEip2930() {
        var item = new AccessListItem().setAddress(ADDRESS).addStorageKey(KEY_1).addStorageKey(KEY_2);

        var tx =
                new EthereumTransactionDataEip2930().setChainId(298).setNonce(1).addAccessListItem(item);
        // sign to produce serializable bytes (r/s populated)
        tx.sign(PrivateKey.generateECDSA());

        var decoded = (EthereumTransactionDataEip2930) EthereumTransactionData.fromBytes(tx.toBytes());
        List<AccessListItem> decodedItems = decoded.getAccessListItems();

        assertThat(decodedItems).hasSize(1);
        assertThat(Hex.toHexString(decodedItems.get(0).getAddress())).isEqualTo(Hex.toHexString(ADDRESS));
        assertThat(decodedItems.get(0).getStorageKeys()).hasSize(2);
        assertThat(Hex.toHexString(decodedItems.get(0).getStorageKeys().get(0))).isEqualTo(Hex.toHexString(KEY_1));
        assertThat(Hex.toHexString(decodedItems.get(0).getStorageKeys().get(1))).isEqualTo(Hex.toHexString(KEY_2));
    }
}
