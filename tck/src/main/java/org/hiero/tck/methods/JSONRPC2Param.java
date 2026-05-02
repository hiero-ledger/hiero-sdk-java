// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods;

import java.util.Map;

/**
 * Abstract base class for JSON-RPC parameters. Every parameter POJO should extend this class
 * and implement the {@link #parse} method. This method assists JSON-RPC services in creating
 * parameters for their JSON-RPC methods from the request.
 *
 * IMPORTANT:
 * all inheriting classes should include the following Lombok annotations:
 * {@code @Getter}, {@code @AllArgsConstructor}, and {@code @NoArgsConstructor}.
 * These annotations are needed for the instance creation via reflection.
 *
 */
public abstract class JSONRPC2Param {
    public abstract JSONRPC2Param parse(final Map<String, Object> jrpcParams) throws Exception;
}
