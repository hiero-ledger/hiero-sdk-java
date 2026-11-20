// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import org.bouncycastle.util.encoders.Hex;

/**
 * Read-only endpoint participating in a CLPR service's syncs.
 * <p>
 * The TLS certificate and account ID stay opaque bytes. Byte values returned by this model are copies.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public final class ClprEndpoint {
    private final ClprServiceEndpoint serviceEndpoint;
    private final byte[] tlsCertificate;
    private final byte[] accountId;

    private ClprEndpoint(ClprServiceEndpoint serviceEndpoint, byte[] tlsCertificate, byte[] accountId) {
        this.serviceEndpoint = serviceEndpoint;
        this.tlsCertificate = tlsCertificate;
        this.accountId = accountId;
    }

    static ClprEndpoint fromProtobuf(com.hedera.hashgraph.sdk.proto.ClprEndpoint endpoint) {
        return new ClprEndpoint(
                ClprServiceEndpoint.fromProtobuf(endpoint.getServiceEndpoint()),
                endpoint.getTlsCertificate().toByteArray(),
                endpoint.getAccountId().toByteArray());
    }

    com.hedera.hashgraph.sdk.proto.ClprEndpoint toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.ClprEndpoint.newBuilder()
                .setServiceEndpoint(serviceEndpoint.toProtobuf())
                .setTlsCertificate(ByteString.copyFrom(tlsCertificate))
                .setAccountId(ByteString.copyFrom(accountId))
                .build();
    }

    /**
     * @return the network address and port
     */
    public ClprServiceEndpoint getServiceEndpoint() {
        return serviceEndpoint;
    }

    /**
     * @return a copy of the DER-encoded TLS listener certificate, preserved as opaque bytes
     */
    public byte[] getTlsCertificate() {
        return tlsCertificate.clone();
    }

    /**
     * @return a copy of the raw on-ledger account identifier associated with this endpoint
     */
    public byte[] getAccountId() {
        return accountId.clone();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceEndpoint", serviceEndpoint)
                .add("tlsCertificate", Hex.toHexString(tlsCertificate))
                .add("accountId", Hex.toHexString(accountId))
                .toString();
    }
}
