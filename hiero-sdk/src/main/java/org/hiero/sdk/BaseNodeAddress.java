// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Internal utility class.
 */
class BaseNodeAddress {
    private static final Pattern HOST_AND_PORT = Pattern.compile("^(\\S+):(\\d+)$");
    private static final Pattern IN_PROCESS = Pattern.compile("^in-process:(\\S+)$");
    static final int PORT_MIRROR_TLS = 443;
    static final int PORT_NODE_PLAIN = 50211;
    static final int PORT_NODE_TLS = 50212;

    // If address is `in-process:.*` this will contain the right side of the `:`
    @Nullable
    private final String name;

    @Nullable
    private final String address;

    private final int port;
    private final boolean secure;

    /**
     * Constructor.
     *
     * @param name                      the name part
     * @param address                   the address part
     * @param port                      the port part
     */
    public BaseNodeAddress(@Nullable String name, @Nullable String address, int port) {
        this(name, address, port, false);
    }

    /**
     * Constructor.
     *
     * @param name                      the name part
     * @param address                   the address part
     * @param port                      the port part
     * @param secure                    secure transport
     */
    public BaseNodeAddress(@Nullable String name, @Nullable String address, int port, boolean secure) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.secure = secure;
    }

    /**
     * Create a managed node address fom a string.
     *
     * @param string                    the string representation
     * @return                          the new managed node address
     */
    public static BaseNodeAddress fromString(String string) {
        var hostAndPortMatcher = HOST_AND_PORT.matcher(string);
        var inProcessMatcher = IN_PROCESS.matcher(string);

        if (hostAndPortMatcher.matches() && hostAndPortMatcher.groupCount() == 2) {
            var address = hostAndPortMatcher.group(1);
            var port = hostAndPortMatcher.group(2);

            return new BaseNodeAddress(null, address, Integer.parseInt(port));
        } else if (inProcessMatcher.matches() && inProcessMatcher.groupCount() == 1) {
            return new BaseNodeAddress(inProcessMatcher.group(1), null, 0);
        } else {
            throw new IllegalStateException("failed to parse node address");
        }
    }

    /**
     * Extract the name.
     *
     * @return                          the name
     */
    public String getName() {
        return name;
    }

    /**
     * Extract the address.
     *
     * @return                          the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Extract the port.
     *
     * @return                          the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Are we in process?
     *
     * @return                          are we in process
     */
    public boolean isInProcess() {
        return name != null;
    }

    /**
     * Are we secure?
     *
     * @return                          are we secure
     */
    public boolean isTransportSecurity() {
        return port == PORT_NODE_TLS || port == PORT_MIRROR_TLS || secure;
    }

    /**
     * Create a new insecure managed node.
     *
     * @return                          the insecure managed node address
     */
    public BaseNodeAddress toInsecure() {
        var newPort = (this.port == PORT_NODE_TLS) ? PORT_NODE_PLAIN : this.port;
        return new BaseNodeAddress(name, address, newPort, false);
    }

    /**
     * Create a new managed node.
     *
     * @return                          the secure managed node address
     */
    public BaseNodeAddress toSecure() {
        var newPort = (this.port == PORT_NODE_PLAIN) ? PORT_NODE_TLS : this.port;
        return new BaseNodeAddress(name, address, newPort, true);
    }

    @Override
    public String toString() {
        if (name != null) {
            return name;
        }

        return address + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseNodeAddress that = (BaseNodeAddress) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getAddress(), that.getAddress())
                && port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getAddress(), getPort());
    }
}
