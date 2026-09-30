// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;

/**
 * Turns a mirror node error response into something worth putting in an exception message.
 *
 * <p>Prefers the node's own envelope — {@code _status.messages[].detail}, then
 * {@code _status.messages[].message} — over dumping the body, and truncates whatever is left.
 */
final class MirrorNodeErrorMessage {
    private static final int MAX_RAW_BODY_CHARACTERS = 512;

    private MirrorNodeErrorMessage() {}

    /**
     * Describe an error response.
     *
     * @param response the response the mirror node sent
     * @return a short description, never null and never empty
     */
    static String describe(HttpResponse response) {
        var raw = new String(response.getBody(), StandardCharsets.UTF_8).trim();

        try {
            var root = JsonParser.parseString(raw).getAsJsonObject();
            var status = root.getAsJsonObject("_status");
            var messages = status.getAsJsonArray("messages");

            for (var element : messages) {
                var message = element.getAsJsonObject();

                for (var key : new String[] {"detail", "message"}) {
                    if (message.has(key) && !message.get(key).isJsonNull()) {
                        var value = message.get(key).getAsString();
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Not the mirror node's error envelope, so fall through to the raw body.
        }

        if (raw.isEmpty()) {
            return "no response body";
        }

        return raw.length() <= MAX_RAW_BODY_CHARACTERS ? raw : raw.substring(0, MAX_RAW_BODY_CHARACTERS) + "...";
    }
}
