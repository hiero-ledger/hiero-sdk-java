// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Custom exception thrown by the entity helper validate method when the account id and checksum are invalid.
 */
public class BadEntityIdException extends Exception {
    /**
     * the shard portion of the account id
     */
    public final long shard;
    /**
     * the realm portion of the account id
     */
    public final long realm;
    /**
     * the num portion of the account id
     */
    public final long num;
    /**
     * the user supplied checksum
     */
    public final String presentChecksum;
    /**
     * the calculated checksum
     */
    public final String expectedChecksum;

    /**
     * Constructor.
     *
     * @param shard                     the shard portion of the account id
     * @param realm                     the realm portion of the account id
     * @param num                       the num portion of the account id
     * @param presentChecksum           the user supplied checksum
     * @param expectedChecksum          the calculated checksum
     */
    BadEntityIdException(long shard, long realm, long num, String presentChecksum, String expectedChecksum) {
        super(String.format("Entity ID %d.%d.%d-%s was incorrect.", shard, realm, num, presentChecksum));
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.presentChecksum = presentChecksum;
        this.expectedChecksum = expectedChecksum;
    }
}
