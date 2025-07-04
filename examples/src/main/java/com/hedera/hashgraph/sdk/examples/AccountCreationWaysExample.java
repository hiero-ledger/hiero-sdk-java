// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;

/**
 * How to create a Hedera account in different ways.
 * <p>
 * In Hedera there are 4 different account representations:
 * <ul>
 *   <li>an account can have an account ID in shard.realm.accountNumber format (0.0.10);</li>
 *   <li>an account can have a public key alias in 0.0.CIQNOWUYAGBLCCVX2VF75U6JMQDTUDXBOLZ5VJRDEWXQEGTI64DVCGQ format;</li>
 *   <li>an account can have an AccountId that is represented in 0x000000000000000000000000000000000000000a (for account ID 0.0.10) long zero format;</li>
 *   <li>an account can be represented by an Ethereum public address 0xb794f5ea0ba39494ce839613fffba74279579268.</li>
 * </ul>
 */
class AccountCreationWaysExample {

    public static void main(String[] args) {
        System.out.println("Account Creation Ways Example Start!");

        /*
         * Account ID:
         * shard.realm.number format, i.e. 0.0.10 with the corresponding 0x000000000000000000000000000000000000000a ethereum address
         */
        AccountId hederaFormat = AccountId.fromString("0.0.10");
        System.out.println("Account ID: " + hederaFormat);
        System.out.println("Account \"0.0.10\" corresponding long-zero address: " + hederaFormat.toEvmAddress());

        /*
         * Hedera Long-Form Account ID:
         * 0.0.aliasPublicKey, i.e. 0.0.CIQNOWUYAGBLCCVX2VF75U6JMQDTUDXBOLZ5VJRDEWXQEGTI64DVCGQ
         */
        PrivateKey privateKey = PrivateKey.generateECDSA();
        PublicKey publicKey = privateKey.getPublicKey();

        // Assuming that the target shard and realm are known.
        // For now, they are virtually always 0.
        AccountId aliasAccountId = publicKey.toAccountId(0, 0);
        System.out.println("Hedera long-form account ID: " + aliasAccountId.toString());

        /*
         * Hedera Account Long-Zero address:
         * 0x000000000000000000000000000000000000000a (for accountId 0.0.10)
         */
        AccountId longZeroAddress = AccountId.fromString("0x000000000000000000000000000000000000000a");
        System.out.println("Hedera account long-zero address: " + longZeroAddress);

        /*
         * Ethereum Account Address or public-address:
         * 0xb794f5ea0ba39494ce839613fffba74279579268
         */
        AccountId evmAddress = AccountId.fromString("0xb794f5ea0ba39494ce839613fffba74279579268");
        System.out.println("Ethereum account address or public address: " + evmAddress);

        System.out.println("Account Creation Ways Example Complete!");
    }
}
