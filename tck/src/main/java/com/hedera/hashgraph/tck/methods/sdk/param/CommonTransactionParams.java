// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.JSONArray;

/**
 * CommonTransactionParams
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommonTransactionParams {
    private Optional<String> transactionId;
    private Optional<Long> maxTransactionFee;
    private Optional<Long> validTransactionDuration;
    private Optional<String> memo;
    private Optional<Boolean> regenerateTransactionId;
    private Optional<List<String>> signers;

    public static CommonTransactionParams parse(Map<String, Object> jrpcParams) throws ClassCastException {
        var parsedTransactionId = Optional.ofNullable((String) jrpcParams.get("transactionId"));
        var parsedMaxTransactionFee = Optional.ofNullable((Long) jrpcParams.get("maxTransactionFee"));
        var parsedValidTransactionDuration = Optional.ofNullable((Long) jrpcParams.get("validTransactionDuration"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedRegenerateTransactionId = Optional.ofNullable((Boolean) jrpcParams.get("regenerateTransactionId"));

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
        signers.ifPresent(s -> {
            transaction.freezeWith(client);
            s.forEach(signer -> {
                var pk = PrivateKey.fromString(signer);
                transaction.sign(pk);
            });
        });
    }

    public void fillOutQuery(final Query<?, ?> transaction) {
        maxTransactionFee.ifPresent(maxFee -> transaction.setMaxQueryPayment(Hbar.fromTinybars(maxFee)));
    }
}
