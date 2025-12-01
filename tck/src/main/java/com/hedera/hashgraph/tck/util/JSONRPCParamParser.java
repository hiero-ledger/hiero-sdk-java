// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.util;

import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import com.hedera.hashgraph.tck.methods.sdk.param.account.AllowanceParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class JSONRPCParamParser {

    public static Optional<CommonTransactionParams> parseCommonTransactionParams(Map<String, Object> jrpcParams)
            throws Exception {
        return parseJsonObject(jrpcParams, "commonTransactionParams", CommonTransactionParams::parse);
    }

    public static String parseSessionId(Map<String, Object> jrpcParams) {
        Object sessionId = jrpcParams.get("sessionId");
        if (sessionId instanceof String stringSessionId && !stringSessionId.isBlank()) {
            return stringSessionId;
        }
        throw new IllegalArgumentException("sessionId is required and must be a non-empty string");
    }

    public static Optional<List<AllowanceParams>> parseAllowances(Map<String, Object> jrpcParams) throws Exception {
        return parseJsonArray(jrpcParams, "allowances", AllowanceParams::parse);
    }

    public static Optional<List<CustomFee>> parseCustomFees(Map<String, Object> jrpcParams) throws Exception {
        return parseJsonArray(jrpcParams, "customFees", jsonObj -> new CustomFee().parse(jsonObj));
    }

    private static <T> Optional<T> parseJsonObject(
            Map<String, Object> params, String key, ThrowingFunction<JSONObject, T> parser) throws Exception {
        if (!params.containsKey(key)) {
            return Optional.empty();
        }
        JSONObject jsonObject = (JSONObject) params.get(key);
        return Optional.of(parser.apply(jsonObject));
    }

    private static <T> Optional<List<T>> parseJsonArray(
            Map<String, Object> params, String key, ThrowingFunction<JSONObject, T> elementParser) throws Exception {
        if (!params.containsKey(key)) {
            return Optional.empty();
        }
        JSONArray jsonArray = (JSONArray) params.get(key);
        List<T> results = new ArrayList<>();

        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            results.add(elementParser.apply(jsonObject));
        }
        return Optional.of(results);
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
}
