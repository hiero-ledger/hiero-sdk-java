// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ResponseSerializationTestHelper {
    private ResponseSerializationTestHelper() {}

    /**
     * Helper to serialize a value using serialization path used by {@code process()}.
     */
    public static String serializeToJson(final Object obj) {
        var map = AbstractJSONRPC2Service.convertValue(obj);
        var response = new JSONRPC2Response(map, 1);
        return response.toJSONString();
    }
}
