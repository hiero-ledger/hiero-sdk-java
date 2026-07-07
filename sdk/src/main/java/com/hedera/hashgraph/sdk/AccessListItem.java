// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * An <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2930.md">EIP-2930</a> access list entry: an
 * Ethereum address together with the storage keys of that account the transaction intends to access.
 */
public class AccessListItem {

    private byte[] address = new byte[] {};

    private List<byte[]> storageKeys = new ArrayList<>();

    /**
     * Constructor.
     */
    public AccessListItem() {}

    /**
     * @return the address this entry refers to
     */
    public byte[] getAddress() {
        return Arrays.copyOf(address, address.length);
    }

    /**
     * @param address the address this entry refers to
     * @return {@code this}
     */
    public AccessListItem setAddress(byte[] address) {
        this.address = Arrays.copyOf(address, address.length);
        return this;
    }

    /**
     * @return the storage keys this entry refers to
     */
    public List<byte[]> getStorageKeys() {
        List<byte[]> copy = new ArrayList<>(storageKeys.size());
        for (byte[] key : storageKeys) {
            copy.add(Arrays.copyOf(key, key.length));
        }
        return copy;
    }

    /**
     * @param storageKeys the storage keys this entry refers to
     * @return {@code this}
     */
    public AccessListItem setStorageKeys(List<byte[]> storageKeys) {
        this.storageKeys = new ArrayList<>(storageKeys.size());
        for (byte[] key : storageKeys) {
            this.storageKeys.add(Arrays.copyOf(key, key.length));
        }
        return this;
    }

    /**
     * Append a single storage key.
     *
     * @param storageKey the storage key to add
     * @return {@code this}
     */
    public AccessListItem addStorageKey(byte[] storageKey) {
        this.storageKeys.add(Arrays.copyOf(storageKey, storageKey.length));
        return this;
    }

    /**
     * Build the nested RLP object representation of this item: {@code [address, [storageKeys...]]}. The returned value
     * is suitable for inclusion in the object list passed to {@code RLPEncoder.list}/{@code RLPEncoder.sequence}.
     */
    Object toRlpObject() {
        List<Object> keys = new ArrayList<>(storageKeys.size());
        for (byte[] key : storageKeys) {
            keys.add(key);
        }
        return Arrays.asList(address, keys);
    }

    /**
     * Build the nested RLP object representation of an entire access list.
     *
     * @param items the access list items (may be null)
     * @return a list of nested RLP objects, one per item
     */
    static List<Object> accessListItemsToRlpObject(List<AccessListItem> items) {
        List<Object> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (AccessListItem item : items) {
            result.add(item.toRlpObject());
        }
        return result;
    }

    /**
     * Decode a single access list entry from its RLP representation.
     *
     * @param item the RLP item for a single entry
     * @return the decoded item, or null if the entry is malformed
     */
    static AccessListItem fromRlp(RLPItem item) {
        if (item == null || !item.isList()) {
            return null;
        }
        List<RLPItem> elements = item.asRLPList().elements();
        if (elements.size() != 2 || !elements.get(1).isList()) {
            return null;
        }
        AccessListItem result = new AccessListItem();
        result.setAddress(elements.get(0).data());
        List<byte[]> keys = new ArrayList<>();
        for (RLPItem keyItem : elements.get(1).asRLPList().elements()) {
            keys.add(keyItem.data());
        }
        result.setStorageKeys(keys);
        return result;
    }

    /**
     * Decode an entire access list from the RLP item holding the list. Malformed entries are skipped.
     *
     * @param accessListItem the RLP item representing the whole access list
     * @return the decoded access list items
     */
    static List<AccessListItem> accessListItemsFromRlp(RLPItem accessListItem) {
        List<AccessListItem> result = new ArrayList<>();
        if (accessListItem == null || !accessListItem.isList()) {
            return result;
        }
        for (RLPItem entry : accessListItem.asRLPList().elements()) {
            AccessListItem item = fromRlp(entry);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Encode a list of access list items to their canonical, self-contained RLP list bytes. An empty (or null) list
     * encodes to an empty byte array so that the stored {@code accessList} field stays {@code ""} for the common
     * empty case.
     *
     * @param items the access list items
     * @return the RLP-encoded access list, or an empty byte array when there are no items
     */
    static byte[] encodeAccessList(List<AccessListItem> items) {
        if (items == null || items.isEmpty()) {
            return new byte[] {};
        }
        return RLPEncoder.list(accessListItemsToRlpObject(items).toArray());
    }

    /**
     * Decode the stored {@code accessList} bytes (either empty or a self-contained RLP list) back into items.
     *
     * @param accessList the stored access list bytes
     * @return the decoded items (empty when there are none)
     */
    static List<AccessListItem> decodeAccessList(byte[] accessList) {
        if (accessList == null || accessList.length == 0) {
            return new ArrayList<>();
        }
        return accessListItemsFromRlp(RLPDecoder.RLP_STRICT.wrap(accessList));
    }

    @Override
    public String toString() {
        List<String> keys = new ArrayList<>(storageKeys.size());
        for (byte[] key : storageKeys) {
            keys.add(Hex.toHexString(key));
        }
        return MoreObjects.toStringHelper(this)
                .add("address", Hex.toHexString(address))
                .add("storageKeys", keys)
                .toString();
    }
}
