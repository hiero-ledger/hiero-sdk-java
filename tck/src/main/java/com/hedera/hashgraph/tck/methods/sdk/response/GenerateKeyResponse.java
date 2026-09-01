// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;

/**
 * Represent generated key response.
 *
 * @param key the generated key
 * @param privateKeys the list of generated private keys
 */
public record GenerateKeyResponse(String key, List<String> privateKeys) {}
