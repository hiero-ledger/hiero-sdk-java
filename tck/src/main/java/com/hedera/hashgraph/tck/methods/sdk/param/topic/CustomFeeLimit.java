// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CustomFeeLimit for topic message submit method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CustomFeeLimit extends JSONRPC2Param {
    private Optional<String> payerId;
    private Optional<List<CustomFee.FixedFee>> fixedFees;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedPayerId = Optional.ofNullable((String) jrpcParams.get("payerId"));

        @SuppressWarnings("unchecked")
        var fixedFeesList = (List<Map<String, Object>>) jrpcParams.get("fixedFees");
        Optional<List<CustomFee.FixedFee>> parsedFixedFees = Optional.empty();

        if (fixedFeesList != null) {
            var fixedFees = fixedFeesList.stream()
                    .map(fixedFeeMap -> {
                        try {
                            return CustomFee.FixedFee.parse(fixedFeeMap);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse fixed fee", e);
                        }
                    })
                    .toList();
            parsedFixedFees = Optional.of(fixedFees);
        }

        return new CustomFeeLimit(parsedPayerId, parsedFixedFees);
    }
}
