// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GenerateKeyResponse {
    private String key;
    private List<String> privateKeys = new ArrayList<>();
}
