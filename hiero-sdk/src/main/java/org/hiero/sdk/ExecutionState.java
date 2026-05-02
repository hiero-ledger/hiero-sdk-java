// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Enum for the execution states.
 */
public enum ExecutionState {
    /**
     * Indicates that the execution was successful
     */
    SUCCESS,
    /**
     * Indicates that the call was successful but the operation did not complete. Retry with same/new node
     */
    RETRY,
    /**
     * Indicates that the receiver was bad node. Retry with new node
     */
    SERVER_ERROR,
    /**
     * Indicates that the request was incorrect
     */
    REQUEST_ERROR
}
