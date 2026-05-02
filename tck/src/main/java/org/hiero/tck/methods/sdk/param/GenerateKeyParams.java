// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.KeyUtils.KeyType;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GenerateKeyParams extends JSONRPC2Param {
    private KeyType type;
    private Optional<String> fromKey;
    private Optional<Long> threshold;
    private Optional<List<GenerateKeyParams>> keys;

    @Override
    public GenerateKeyParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedType = (String) jrpcParams.get("type");
        var parsedFromKey = Optional.ofNullable((String) jrpcParams.get("fromKey"));
        var parsedThreshold = Optional.ofNullable((Long) jrpcParams.get("threshold"));

        Optional<List<GenerateKeyParams>> parsedKeys = Optional.empty();
        if (jrpcParams.containsKey("keys")) {
            JSONArray jsonArray = (JSONArray) jrpcParams.get("keys");
            List<GenerateKeyParams> keyList = new ArrayList<>();
            for (Object o : jsonArray) {
                JSONObject jsonObject = (JSONObject) o;
                GenerateKeyParams keyParam = new GenerateKeyParams().parse(jsonObject);
                keyList.add(keyParam);
            }
            parsedKeys = Optional.of(keyList);
        }

        return new GenerateKeyParams(KeyType.fromString(parsedType), parsedFromKey, parsedThreshold, parsedKeys);
    }
}
