// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minidev.json.JSONArray;

/**
 * CommonTransactionParams
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record CommonTransactionParams(
        Optional<String> transactionId,
        Optional<Long> maxTransactionFee,
        Optional<Long> validTransactionDuration,
        Optional<String> memo,
        Optional<Boolean> regenerateTransactionId,
        Optional<Boolean> highVolume,
        Optional<List<String>> signers) {
    public static CommonTransactionParams parse(Map<String, Object> jrpcParams) throws ClassCastException {
        var parsedTransactionId = Optional.ofNullable((String) jrpcParams.get("transactionId"));
        var parsedMaxTransactionFee = Optional.ofNullable((Long) jrpcParams.get("maxTransactionFee"));
        var parsedValidTransactionDuration = Optional.ofNullable((Long) jrpcParams.get("validTransactionDuration"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedRegenerateTransactionId = Optional.ofNullable((Boolean) jrpcParams.get("regenerateTransactionId"));
        var parsedHighVolume = Optional.ofNullable((Boolean) jrpcParams.get("highVolume"));

        Optional<List<String>> signers = Optional.empty();
        if (jrpcParams.containsKey("signers")) {
            JSONArray jsonArray = (JSONArray) jrpcParams.get("signers");
            List<String> signersList = jsonArray.stream().map(Objects::toString).toList();
            signers = Optional.of(signersList);
        }

        return new CommonTransactionParams(
                parsedTransactionId,
                parsedMaxTransactionFee,
                parsedValidTransactionDuration,
                parsedMemo,
                parsedRegenerateTransactionId,
                parsedHighVolume,
                signers);
    }

    public void fillOutTransaction(final Transaction<?> transaction, final Client client) {
        transactionId.ifPresent(payerID -> {
            transaction.setTransactionId(TransactionId.generate(AccountId.fromString(payerID)));
        });
        maxTransactionFee.ifPresent(maxFee -> transaction.setMaxTransactionFee(Hbar.fromTinybars(maxFee)));
        validTransactionDuration.ifPresent(
                validDuration -> transaction.setTransactionValidDuration(Duration.ofSeconds(validDuration)));
        memo.ifPresent(transaction::setTransactionMemo);
        regenerateTransactionId.ifPresent(transaction::setRegenerateTransactionId);
        highVolume.ifPresent(transaction::setHighVolume);
        signers.ifPresent(s -> {
            transaction.freezeWith(client);
            s.forEach(signer -> {
                var pk = PrivateKey.fromString(signer);
                transaction.sign(pk);
            });
        });
    }
}
