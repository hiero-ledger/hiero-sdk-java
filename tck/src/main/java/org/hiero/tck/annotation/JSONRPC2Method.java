// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks methods as JSON-RPC methods.
 * Methods marked with this annotation will be registered
 * as handlers for JSON-RPC requests with the specified method name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSONRPC2Method {
    /**
     * Specifies the name of the JSON-RPC method.
     * the params need to be of instance JSONRPC2Param because if not - will blow up at runtime
     * @return The name of the JSON-RPC method.
     */
    String value();
}
