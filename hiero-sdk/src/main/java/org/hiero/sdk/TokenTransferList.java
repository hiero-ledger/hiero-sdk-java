// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.UInt32Value;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.AccountAmount;
import org.hiero.sdk.proto.NftTransfer;

class TokenTransferList {
    final TokenId tokenId;

    @Nullable
    final Integer expectDecimals;

    List<TokenTransfer> transfers = new ArrayList<>();
    List<TokenNftTransfer> nftTransfers = new ArrayList<>();

    TokenTransferList(
            TokenId tokenId,
            @Nullable Integer expectDecimals,
            @Nullable TokenTransfer transfer,
            @Nullable TokenNftTransfer nftTransfer) {
        this.tokenId = tokenId;
        this.expectDecimals = expectDecimals;

        if (transfer != null) {
            this.transfers.add(transfer);
        }

        if (nftTransfer != null) {
            this.nftTransfers.add(nftTransfer);
        }
    }

    org.hiero.sdk.proto.TokenTransferList toProtobuf() {
        var transfers = new ArrayList<AccountAmount>();
        var nftTransfers = new ArrayList<NftTransfer>();

        for (var transfer : this.transfers) {
            transfers.add(transfer.toProtobuf());
        }

        for (var transfer : this.nftTransfers) {
            nftTransfers.add(transfer.toProtobuf());
        }

        var builder = org.hiero.sdk.proto.TokenTransferList.newBuilder()
                .setToken(tokenId.toProtobuf())
                .addAllTransfers(transfers)
                .addAllNftTransfers(nftTransfers);

        if (expectDecimals != null) {
            builder.setExpectedDecimals(
                    UInt32Value.newBuilder().setValue(expectDecimals).build());
        }

        return builder.build();
    }
}
