// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minidev.json.JSONObject;

public record CustomFee(
        String feeCollectorAccountId,
        Boolean feeCollectorsExempt,
        Optional<FixedFee> fixedFee,
        Optional<FractionalFee> fractionalFee,
        Optional<RoyaltyFee> royaltyFee)
        implements JSONRPC2Param {
    public static CustomFee parse(Map<String, Object> jrpcParams) throws Exception {
        var feeCollectorAccountIdParsed = (String) jrpcParams.get("feeCollectorAccountId");
        var feeCollectorsExemptParsed = (Boolean) jrpcParams.get("feeCollectorsExempt");

        Optional<FixedFee> fixedFeeParsed = Optional.empty();
        if (jrpcParams.containsKey("fixedFee")) {
            JSONObject jsonObject = (JSONObject) jrpcParams.get("fixedFee");
            fixedFeeParsed = Optional.of(FixedFee.parse(jsonObject));
        }

        Optional<FractionalFee> fractionalFeeParsed = Optional.empty();
        if (jrpcParams.containsKey("fractionalFee")) {
            JSONObject jsonObject = (JSONObject) jrpcParams.get("fractionalFee");
            fractionalFeeParsed = Optional.of(FractionalFee.parse(jsonObject));
        }

        Optional<RoyaltyFee> royaltyFeeParsed = Optional.empty();
        if (jrpcParams.containsKey("royaltyFee")) {
            JSONObject jsonObject = (JSONObject) jrpcParams.get("royaltyFee");
            royaltyFeeParsed = Optional.of(RoyaltyFee.parse(jsonObject));
        }

        return new CustomFee(
                feeCollectorAccountIdParsed,
                feeCollectorsExemptParsed,
                fixedFeeParsed,
                fractionalFeeParsed,
                royaltyFeeParsed);
    }

    public record FixedFee(String amount, Optional<String> denominatingTokenId) {
        public static FixedFee parse(Map<String, Object> jrpcParams) throws Exception {
            var amountParsed = (String) jrpcParams.get("amount");
            var denominatingTokenIdParsed = Optional.ofNullable((String) jrpcParams.get("denominatingTokenId"));
            return new FixedFee(amountParsed, denominatingTokenIdParsed);
        }
    }

    public record FractionalFee(
            String numerator, String denominator, String minimumAmount, String maximumAmount, String assessmentMethod) {
        public static FractionalFee parse(Map<String, Object> jrpcParams) throws Exception {
            var numeratorParsed = (String) jrpcParams.get("numerator");
            var denominatorParsed = (String) jrpcParams.get("denominator");
            var minimumAmountParsed = (String) jrpcParams.get("minimumAmount");
            var maximumAmountParsed = (String) jrpcParams.get("maximumAmount");
            var assessmentMethodParsed = (String) jrpcParams.get("assessmentMethod");
            return new FractionalFee(
                    numeratorParsed,
                    denominatorParsed,
                    minimumAmountParsed,
                    maximumAmountParsed,
                    assessmentMethodParsed);
        }
    }

    public record RoyaltyFee(String numerator, String denominator, Optional<FixedFee> fallbackFee) {
        public static RoyaltyFee parse(Map<String, Object> jrpcParams) throws Exception {
            var numeratorParsed = (String) jrpcParams.get("numerator");
            var denominatorParsed = (String) jrpcParams.get("denominator");

            Optional<FixedFee> fallbackFeeParsed = Optional.empty();
            if (jrpcParams.containsKey("fallbackFee")) {
                JSONObject jsonObject = (JSONObject) jrpcParams.get("fallbackFee");
                fallbackFeeParsed = Optional.of(FixedFee.parse(jsonObject));
            }

            return new RoyaltyFee(numeratorParsed, denominatorParsed, fallbackFeeParsed);
        }
    }

    public List<com.hedera.hashgraph.sdk.CustomFee> fillOutCustomFees(List<CustomFee> customFees) {
        Objects.requireNonNull(customFees, "customFees must not be null");
        List<com.hedera.hashgraph.sdk.CustomFee> customFeeList = new ArrayList<>();

        for (var customFee : customFees) {
            customFee.fixedFee().ifPresent(fixedFee -> {
                var sdkFixedFee = new CustomFixedFee()
                        .setAmount(Long.parseLong(fixedFee.amount()))
                        .setFeeCollectorAccountId(AccountId.fromString(customFee.feeCollectorAccountId()))
                        .setAllCollectorsAreExempt(customFee.feeCollectorsExempt());

                fixedFee.denominatingTokenId()
                        .ifPresent(tokenId -> sdkFixedFee.setDenominatingTokenId(TokenId.fromString(tokenId)));

                customFeeList.add(sdkFixedFee);
            });

            customFee.fractionalFee().ifPresent(fractionalFee -> {
                var sdkFractionalFee = new CustomFractionalFee()
                        .setNumerator(Long.parseLong(fractionalFee.numerator()))
                        .setDenominator(Long.parseLong(fractionalFee.denominator()))
                        .setMin(Long.parseLong(fractionalFee.minimumAmount()))
                        .setMax(Long.parseLong(fractionalFee.maximumAmount()))
                        .setFeeCollectorAccountId(AccountId.fromString(customFee.feeCollectorAccountId()))
                        .setAllCollectorsAreExempt(customFee.feeCollectorsExempt())
                        .setAssessmentMethod(
                                "inclusive".equalsIgnoreCase(fractionalFee.assessmentMethod())
                                        ? FeeAssessmentMethod.INCLUSIVE
                                        : FeeAssessmentMethod.EXCLUSIVE);

                customFeeList.add(sdkFractionalFee);
            });

            customFee.royaltyFee().ifPresent(royaltyFee -> {
                var sdkRoyaltyFee = new CustomRoyaltyFee()
                        .setDenominator(Long.parseLong(royaltyFee.denominator()))
                        .setNumerator(Long.parseLong(royaltyFee.numerator()))
                        .setFeeCollectorAccountId(AccountId.fromString(customFee.feeCollectorAccountId()))
                        .setAllCollectorsAreExempt(customFee.feeCollectorsExempt());

                royaltyFee.fallbackFee().ifPresent(fallbackFee -> {
                    var fixedFallback = new CustomFixedFee().setAmount(Long.parseLong(fallbackFee.amount()));

                    fallbackFee
                            .denominatingTokenId()
                            .ifPresent(tokenId -> fixedFallback.setDenominatingTokenId(TokenId.fromString(tokenId)));

                    sdkRoyaltyFee.setFallbackFee(fixedFallback);
                });

                customFeeList.add(sdkRoyaltyFee);
            });
        }

        return customFeeList;
    }
}
