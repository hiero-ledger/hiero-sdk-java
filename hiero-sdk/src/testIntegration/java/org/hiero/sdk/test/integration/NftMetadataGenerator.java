// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NftMetadataGenerator {
    private NftMetadataGenerator() {}

    public static List<byte[]> generate(byte metadataCount) {
        List<byte[]> metadatas = new ArrayList<>();
        for (byte i = 0; i < metadataCount; i++) {
            byte[] md = {i};
            metadatas.add(md);
        }
        return metadatas;
    }

    public static List<byte[]> generate(byte[] metadata, int count) {
        return IntStream.range(0, count).mapToObj(i -> metadata.clone()).collect(Collectors.toList());
    }

    public static List<byte[]> generateOneLarge() {
        return Collections.singletonList(new byte[101]);
    }
}
