// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.Query;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;
import org.hiero.sdk.proto.TokenGetNftInfoQuery;
import org.hiero.sdk.proto.TokenServiceGrpc;

/**
 * A query that returns information about a non-fungible token (NFT).
 *
 * You request the info for an NFT by specifying the NFT ID.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/get-nft-token-info#methods">Hedera Documentation</a>
 */
public class TokenNftInfoQuery extends org.hiero.sdk.Query<List<TokenNftInfo>, TokenNftInfoQuery> {
    /**
     * The ID of the non-fungible token in x.y.z format.
     */
    @Nullable
    private NftId nftId = null;

    @Nullable
    private TokenId tokenId = null;
    /**
     * The account ID of the current owner of the NFT
     */
    @Nullable
    private AccountId accountId = null;

    private long start = 0;
    private long end = 0;

    /**
     * Constructor.
     */
    public TokenNftInfoQuery() {}

    /**
     * Sets the NFT ID for which information is requested.
     *
     * @deprecated use {@link TokenNftInfoQuery#setNftId(NftId)} instead
     * @param nftId The NftId to be set
     * @return {@code this}
     */
    @Deprecated
    public TokenNftInfoQuery byNftId(NftId nftId) {
        return setNftId(nftId);
    }

    /**
     * Sets the NFT ID for which information is requested.
     *
     * @param nftId The NftId to be set
     * @return {@code this}
     */
    public TokenNftInfoQuery setNftId(NftId nftId) {
        Objects.requireNonNull(nftId);
        this.nftId = nftId;
        return this;
    }

    /**
     * Extract the nft id.
     *
     * @return                          the nft id
     */
    @Nullable
    public NftId getNftId() {
        return nftId;
    }

    /**
     * Sets the Token ID and the index range for which information is requested.
     *
     * @deprecated with no replacement
     * @param tokenId The ID of the token for which information is requested
     * @return {@code this}
     */
    @Deprecated
    public TokenNftInfoQuery byTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the token id
     *
     * @return the tokenId
     */
    @Nullable
    @Deprecated
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * Sets the Account ID for which information is requested.
     *
     * @deprecated with no replacement
     * @param accountId The Account ID for which information is requested
     * @return {@code this}
     */
    @Deprecated
    public TokenNftInfoQuery byAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        this.accountId = accountId;
        return this;
    }

    /**
     * Get the Account ID for which information is requested
     *
     * @return the accountId
     */
    @Nullable
    @Deprecated
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Get the start of the index range for which information is requested
     *
     * @return the start
     */
    @Deprecated
    public long getStart() {
        return start;
    }

    /**
     * Sets the start of the index range for which information is requested.
     *
     * @deprecated with no replacement
     * @param start The start index (inclusive) of the range of NFTs to query for. Value must be in the range [0; ownedNFTs-1]
     * @return {@code this}
     */
    @Deprecated
    public TokenNftInfoQuery setStart(@Nonnegative long start) {
        this.start = start;
        return this;
    }

    /**
     * Get the end of the index range for which information is requested
     *
     * @return the end
     */
    @Deprecated
    public long getEnd() {
        return end;
    }

    /**
     * Sets the end of the index range for which information is requested.
     *
     * @deprecated with no replacement
     * @param end The end index (exclusive) of the range of NFTs to query for. Value must be in the range (start; ownedNFTs]
     * @return {@code this}
     */
    @Deprecated
    public TokenNftInfoQuery setEnd(@Nonnegative long end) {
        this.end = end;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (nftId != null) {
            nftId.tokenId.validateChecksum(client);
        }
    }

    @Override
    CompletableFuture<Void> onExecuteAsync(Client client) {
        int modesEnabled = (nftId != null ? 1 : 0) + (tokenId != null ? 1 : 0) + (accountId != null ? 1 : 0);
        if (modesEnabled > 1) {
            throw new IllegalStateException(
                    "TokenNftInfoQuery must be one of byNftId, byTokenId, or byAccountId, but multiple of these modes have been selected");
        } else if (modesEnabled == 0) {
            throw new IllegalStateException(
                    "TokenNftInfoQuery must be one of byNftId, byTokenId, or byAccountId, but none of these modes have been selected");
        }
        return super.onExecuteAsync(client);
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = TokenGetNftInfoQuery.newBuilder();
        if (nftId != null) {
            builder.setNftID(nftId.toProtobuf());
        }
        queryBuilder.setTokenGetNftInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getTokenGetNftInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getTokenGetInfo().getHeader();
    }

    @Override
    List<TokenNftInfo> mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return Collections.singletonList(
                TokenNftInfo.fromProtobuf(response.getTokenGetNftInfo().getNft()));
    }

    @Override
    MethodDescriptor<Query, Response> getMethodDescriptor() {
        return TokenServiceGrpc.getGetTokenNftInfoMethod();
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `Token_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }
}
