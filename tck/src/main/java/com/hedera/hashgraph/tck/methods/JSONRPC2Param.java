// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

/**
 * Marker interface for JSON-RPC parameter types.
 * Implementations represent the parameters accepted by JSON-RPC methods. This interface
 * intentionally does not define any methods. Each parameter implementation
 * provides its own type-specific parsing logic.
 *
 * <p>
 * IMPORTANT:
 * all inheriting classes should include the following method signature:
 * <pre>public static JSONRPC2Param parse(Map<String, Object> params){}</pre>
 */
public interface JSONRPC2Param {}
