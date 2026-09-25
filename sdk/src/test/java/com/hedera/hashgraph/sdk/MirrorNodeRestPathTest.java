// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeRestPathTest {

    @Test
    @DisplayName("An absolute URL cannot be expressed as a path")
    void rejectsAbsoluteUrl() {
        assertThatThrownBy(() -> MirrorNodeRestPath.of("https://evil.example/api/v1/accounts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must begin with '/'");
    }

    @Test
    @DisplayName("A protocol-relative reference cannot be expressed as a path")
    void rejectsProtocolRelativeReference() {
        assertThatThrownBy(() -> MirrorNodeRestPath.of("//evil.example/steal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protocol-relative");
    }

    @Test
    void rejectsDotDotSegments() {
        assertThatThrownBy(() -> MirrorNodeRestPath.of("/accounts/../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'..'");
    }

    @Test
    void rejectsWhitespace() {
        assertThatThrownBy(() -> MirrorNodeRestPath.of("/accounts/0.0.1 2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whitespace");
    }

    @Test
    @DisplayName("A '..' inside the query string is not a path segment")
    void allowsDotDotInsideQueryString() {
        assertThat(MirrorNodeRestPath.of("/accounts?order=..").getValue()).isEqualTo("/accounts?order=..");
    }

    @Test
    @DisplayName("Both spellings of a trailing slash on the base URL resolve identically")
    void resolutionIsConcatenation() {
        var path = MirrorNodeRestPath.of("/accounts/0.0.5/tokens");

        assertThat(path.resolveAgainst("https://mirror/api/v1"))
                .isEqualTo("https://mirror/api/v1/accounts/0.0.5/tokens");
        assertThat(path.resolveAgainst("https://mirror/api/v1/"))
                .isEqualTo("https://mirror/api/v1/accounts/0.0.5/tokens");
    }

    @Test
    void nextLinkStripsTheApiPrefixExactlyOnce() {
        assertThat(MirrorNodeRestPath.fromNextLink("/api/v1/network/nodes?limit=100&order=asc")
                        .getValue())
                .isEqualTo("/network/nodes?limit=100&order=asc");

        // The prefix appears twice in the cursor; only the leading one belongs to the base URL.
        assertThat(MirrorNodeRestPath.fromNextLink("/api/v1/api/v1/x").getValue())
                .isEqualTo("/api/v1/x");
    }

    @Test
    void nextLinkRejectsAnotherHost() {
        assertThatThrownBy(() -> MirrorNodeRestPath.fromNextLink("https://evil.example/api/v1/network/nodes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not name a host");
    }
}
