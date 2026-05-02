// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ThresholdKey;

/**
 * A list of keys that are required to sign in unison, with an optional threshold controlling how many keys of
 * the list are required.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/key">Hedera Documentation</a>
 */
public final class KeyList extends Key implements Collection<Key> {
    /**
     * The list of keys.
     */
    private final List<Key> keys = new ArrayList<>();

    /**
     * The minimum number of keys that must sign.
     */
    @Nullable
    public Integer threshold;

    /**
     * Create a new key list where all keys that are added will be required to sign.
     */
    public KeyList() {
        this.threshold = null;
    }

    /**
     * Number of keys that need to sign.
     *
     * @param threshold                 the minimum number of keys that must sign
     */
    private KeyList(int threshold) {
        this.threshold = threshold;
    }

    /**
     * List of keys in the key.
     *
     * @param keys                      the key / key list
     * @return                          a list of the keys
     */
    public static KeyList of(Key... keys) {
        var list = new KeyList();

        for (var key : keys) {
            list.add(key);
        }

        return list;
    }

    /**
     * Create a new key list where at least {@code threshold} keys must sign.
     *
     * @param threshold the minimum number of keys that must sign
     * @return KeyList
     */
    public static KeyList withThreshold(int threshold) {
        return new KeyList(threshold);
    }

    /**
     * Create key list from protobuf.
     *
     * @param keyList                   the key list
     * @param threshold                 the minimum number of keys that must sign
     * @return                          the key list
     */
    static KeyList fromProtobuf(org.hiero.sdk.proto.KeyList keyList, @Nullable Integer threshold) {
        var keys = (threshold != null ? new KeyList(threshold) : new KeyList());
        for (var i = 0; i < keyList.getKeysCount(); ++i) {
            keys.add(Key.fromProtobufKey(keyList.getKeys(i)));
        }

        return keys;
    }

    /**
     * Get the threshold for the KeyList.
     *
     * @return int
     */
    @Nullable
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Set a threshold for the KeyList.
     *
     * @param threshold the minimum number of keys that must sign
     * @return KeyList
     */
    public KeyList setThreshold(int threshold) {
        this.threshold = threshold;
        return this;
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return keys.contains(o);
    }

    @Override
    public Iterator<Key> iterator() {
        return keys.iterator();
    }

    @Override
    public Object[] toArray() {
        return keys.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        // noinspection unchecked,SuspiciousToArrayCall
        return (T[]) keys.toArray((Key[]) ts);
    }

    @Override
    public boolean add(Key key) {
        return keys.add(key);
    }

    @Override
    public boolean remove(Object o) {
        return keys.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return keys.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends Key> collection) {
        return keys.addAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return keys.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return keys.retainAll(collection);
    }

    @Override
    public void clear() {
        keys.clear();
    }

    @Override
    org.hiero.sdk.proto.Key toProtobufKey() {
        var protoKeyList = org.hiero.sdk.proto.KeyList.newBuilder();
        for (var key : keys) {
            protoKeyList.addKeys(key.toProtobufKey());
        }

        if (threshold != null) {
            return org.hiero.sdk.proto.Key.newBuilder()
                    .setThresholdKey(
                            ThresholdKey.newBuilder().setThreshold(threshold).setKeys(protoKeyList))
                    .build();
        }

        return org.hiero.sdk.proto.Key.newBuilder().setKeyList(protoKeyList).build();
    }

    /**
     * Convert into protobuf representation.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.KeyList toProtobuf() {
        var keyList = org.hiero.sdk.proto.KeyList.newBuilder();

        for (Key key : keys) {
            keyList.addKeys(key.toProtobufKey());
        }

        return keyList.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("threshold", threshold)
                .add("keys", keys)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof KeyList)) {
            return false;
        }

        KeyList keyList = (KeyList) o;

        if (keyList.size() != size()) {
            return false;
        }

        for (int i = 0; i < keyList.size(); i++) {
            if (!Arrays.equals(keyList.keys.get(i).toBytes(), keys.get(i).toBytes())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keys.hashCode(), threshold != null ? threshold : -1);
    }
}
