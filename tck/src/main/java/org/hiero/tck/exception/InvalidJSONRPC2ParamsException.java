// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.exception;

/**
 * Thrown when the server cannot parse the given parameters.
 * This error should be thrown from the param parser
 */
public class InvalidJSONRPC2ParamsException extends Exception {

    public InvalidJSONRPC2ParamsException() {
        super();
    }

    public InvalidJSONRPC2ParamsException(String message) {
        super(message);
    }

    public InvalidJSONRPC2ParamsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJSONRPC2ParamsException(Throwable cause) {
        super(cause);
    }
}
