// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CryptoGetAccountBalanceResponse;
import com.hedera.hashgraph.sdk.proto.TokenBalance;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnegative;

/**
 * This class represents the account balance object
 */
public class AccountBalance {
    /**
     * The Hbar balance of the account
     */
    @Nonnegative
    public final Hbar hbars;

    /**
     * <b>Deprecated.</b> This field is deprecated and will be removed in a future release.<br>
     * <p>
     * Please use {@link #tokens} instead for accessing token balances.
     * </p>
     *
     * @deprecated Use {@link #tokens} instead.
     */
    @Deprecated
    @Nonnegative
    public final Map<TokenId, Long> token = new HashMap<>();

    public final Map<TokenId, Long> tokens;

    @Nonnegative
    public final Map<TokenId, Integer> tokenDecimals;

    AccountBalance(Hbar hbars, Map<TokenId, Long> token, Map<TokenId, Integer> decimal) {
        this.hbars = hbars;
        this.tokens = token;
        this.tokenDecimals = decimal;
    }

    /**
     * Convert the protobuf object to an account balance object.
     *
     * @param protobuf                  protobuf response object
     * @return                          the converted account balance object
     */
    static AccountBalance fromProtobuf(CryptoGetAccountBalanceResponse protobuf) {
        var balanceList = protobuf.getTokenBalancesList();
        Map<TokenId, Long> map = new HashMap<>();
        Map<TokenId, Integer> decimalMap = new HashMap<>();
        for (int i = 0; i < protobuf.getTokenBalancesCount(); i++) {
            map.put(
                    TokenId.fromProtobuf(balanceList.get(i).getTokenId()),
                    balanceList.get(i).getBalance());
            decimalMap.put(
                    TokenId.fromProtobuf(balanceList.get(i).getTokenId()),
                    balanceList.get(i).getDecimals());
        }

        return new AccountBalance(Hbar.fromTinybars(protobuf.getBalance()), map, decimalMap);
    }

    /**
     * Convert a byte array to an account balance object.
     *
     * @param data                      the byte array
     * @return                          the converted account balance object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static AccountBalance fromBytes(byte[] data) throws InvalidProtocolBufferException {
        return fromProtobuf(CryptoGetAccountBalanceResponse.parseFrom(data));
    }

    /**
     * Convert an account balance object into a protobuf.
     *
     * @return                          the protobuf object
     */
    CryptoGetAccountBalanceResponse toProtobuf() {
        var protobuf = CryptoGetAccountBalanceResponse.newBuilder().setBalance(hbars.toTinybars());

        for (var entry : tokens.entrySet()) {
            protobuf.addTokenBalances(TokenBalance.newBuilder()
                    .setTokenId(entry.getKey().toProtobuf())
                    .setBalance(entry.getValue())
                    .setDecimals(Objects.requireNonNull(tokenDecimals.get(entry.getKey()))));
        }

        return protobuf.build();
    }

    /**
     * Convert the account balance object to a byte array.
     *
     * @return                          the converted account balance object
     */
    public ByteString toBytes() {
        return toProtobuf().toByteString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hbars", hbars)
                .add("tokens", tokens)
                .toString();
    }
}
