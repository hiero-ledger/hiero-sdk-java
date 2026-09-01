// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

/**
 * Represent fileContent query response.
 *
 * @param contents the contents of the file
 */
public record FileContentsResponse(String contents) {}
