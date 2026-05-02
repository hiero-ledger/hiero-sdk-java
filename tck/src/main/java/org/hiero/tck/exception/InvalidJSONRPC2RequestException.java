// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.exception;

/**
 * Thrown when the server cannot process the request
 */
public class InvalidJSONRPC2RequestException extends Exception {

    public InvalidJSONRPC2RequestException(String message) {
        super(message);
    }
}
