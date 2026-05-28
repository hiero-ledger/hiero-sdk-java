// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import lombok.Data;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;
import org.jspecify.annotations.Nullable;

@Data
public class TransactionReceiptResponse implements JSONAware {
    private final String status;

    @Nullable
    private final String accountId;

    @Nullable
    private final String fileId;

    @Nullable
    private final String contractId;

    @Nullable
    private final String topicId;

    @Nullable
    private final String tokenId;

    @Nullable
    private final String scheduleId;

    @Nullable
    private final ExchangeRate exchangeRate;

    @Nullable
    private final String topicSequenceNumber;

    @Nullable
    private final String topicRunningHash;

    @Nullable
    private final String totalSupply;

    @Nullable
    private final String scheduledTransactionId;

    private final List<Long> serials;
    private final List<TransactionReceiptResponse> duplicates;
    private final List<TransactionReceiptResponse> children;

    @Nullable
    private final String nodeId;

    private static void putIfNotNull(JSONObject json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }

    @Override
    public String toJSONString() {
        JSONObject json = new JSONObject();

        putIfNotNull(json, "status", status);
        putIfNotNull(json, "accountId", accountId);
        putIfNotNull(json, "fileId", fileId);
        putIfNotNull(json, "contractId", contractId);
        putIfNotNull(json, "topicId", topicId);
        putIfNotNull(json, "tokenId", tokenId);
        putIfNotNull(json, "scheduleId", scheduleId);
        putIfNotNull(json, "exchangeRate", exchangeRate);
        putIfNotNull(json, "topicSequenceNumber", topicSequenceNumber);
        putIfNotNull(json, "topicRunningHash", topicRunningHash);
        putIfNotNull(json, "totalSupply", totalSupply);
        putIfNotNull(json, "scheduledTransactionId", scheduledTransactionId);
        putIfNotNull(json, "serials", serials);
        putIfNotNull(json, "duplicates", duplicates);
        putIfNotNull(json, "children", children);
        putIfNotNull(json, "nodeId", nodeId);

        return json.toJSONString();
    }

    @Data
    public static class ExchangeRate implements JSONAware {
        private final Long hbars;
        private final Long cents;
        private final String expirationTime;

        @Override
        public String toJSONString() {
            JSONObject json = new JSONObject();

            putIfNotNull(json, "hbars", hbars);
            putIfNotNull(json, "cents", cents);
            putIfNotNull(json, "expirationTime", expirationTime);

            return json.toJSONString();
        }
    }
}
