package com.hedera.hashgraph.tck.methods.sdk.response;

import lombok.Data;

import java.util.List;

@Data
public class FileInfoResponse {
    private final String fileId;
    private final String size;
    private final  String expirationTime;
    private final boolean isDeleted;
    private final String fileMemo;
    private final String ledgerId;
    private final List<String> keys;
}
