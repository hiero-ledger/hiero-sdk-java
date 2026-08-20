// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractJSONRPCServiceTest {
    @Test
    void shouldSerializeRecordWithAllFields() {
        var record = new MockRecord(
                "0.0.1", true, false, List.of(1, 2), Map.of("T1", 1), new MockRecord.InnerMockRecord("101"));
        var json = serializeToJson(record);

        var expectedJson =
                "{\"result\":{\"accountId\":\"0.0.1\",\"isDeleted\":true,\"received\":false,\"serials\":[1,2],\"tokens\":{\"T1\":1},\"innerRecord\":{\"id\":\"101\"}},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    void shouldSerializeRecordWithEmptyCollections() {
        var record = new MockRecord("0.0.1", true, false, List.of(), Map.of(), new MockRecord.InnerMockRecord("101"));
        var json = serializeToJson(record);

        var expectedJson =
                "{\"result\":{\"accountId\":\"0.0.1\",\"isDeleted\":true,\"received\":false,\"serials\":[],\"tokens\":{},\"innerRecord\":{\"id\":\"101\"}},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    void shouldSerializeRecordWithNullFields() {
        var record = new MockRecord(null, null, false, null, null, null);
        var json = serializeToJson(record);

        var expectedJson =
                "{\"result\":{\"accountId\":null,\"isDeleted\":null,\"received\":false,\"serials\":null,\"tokens\":null,\"innerRecord\":null},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    void shouldSerializeNonNullRecordWithAllFields() {
        var record = new NonNullMockRecord(
                "0.0.1", true, false, List.of(1, 2), Map.of("T1", 1), new NonNullMockRecord.InnerMockRecord("101"));
        var json = serializeToJson(record);

        var expectedJson =
                "{\"result\":{\"accountId\":\"0.0.1\",\"isDeleted\":true,\"received\":false,\"serials\":[1,2],\"tokens\":{\"T1\":1},\"innerRecord\":{\"id\":\"101\"}},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    void shouldSerializeNonNullRecordWithEmptyCollections() {
        var record = new NonNullMockRecord(
                "0.0.1", true, false, List.of(), Map.of(), new NonNullMockRecord.InnerMockRecord("101"));
        var json = serializeToJson(record);

        var expectedJson =
                "{\"result\":{\"accountId\":\"0.0.1\",\"isDeleted\":true,\"received\":false,\"serials\":[],\"tokens\":{},\"innerRecord\":{\"id\":\"101\"}},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    void shouldSerializeNonNullRecordWithNullFields() {
        var record = new NonNullMockRecord(null, null, false, null, null, null);
        var json = serializeToJson(record);

        var expectedJson = "{\"result\":{\"received\":false},\"id\":1,\"jsonrpc\":\"2.0\"}";
        Assertions.assertEquals(expectedJson, json);
    }

    record MockRecord(
            String accountId,
            Boolean isDeleted,
            boolean received,
            List<Integer> serials,
            Map<String, Integer> tokens,
            InnerMockRecord innerRecord) {
        record InnerMockRecord(String id) {}
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record NonNullMockRecord(
            String accountId,
            Boolean isDeleted,
            boolean received,
            List<Integer> serials,
            Map<String, Integer> tokens,
            InnerMockRecord innerRecord) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        record InnerMockRecord(String id) {}
    }
}
