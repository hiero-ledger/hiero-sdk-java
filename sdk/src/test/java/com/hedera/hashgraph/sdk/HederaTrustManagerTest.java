// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class HederaTrustManagerTest {
    public static final String PREVIEWNET_CERT_NODE_3_STRING =
            "-----BEGIN CERTIFICATE-----\n" + "MIICnzCCAiWgAwIBAgIUenyqJ4UaFBbwokatcUqAwW3o3rswCgYIKoZIzj0EAwMw\n"
                    + "gYQxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJUWDETMBEGA1UEBwwKUmljaGFyZHNv\n"
                    + "bjEPMA0GA1UECgwGSGVkZXJhMQ8wDQYDVQQLDAZIZWRlcmExEDAOBgNVBAMMBzAw\n"
                    + "MDAwMDAxHzAdBgkqhkiG9w0BCQEWEGFkbWluQGhlZGVyYS5jb20wIBcNMjEwODIz\n"
                    + "MjIyMTU4WhgPMjI5NTA2MDcyMjIxNThaMIGEMQswCQYDVQQGEwJVUzELMAkGA1UE\n"
                    + "CAwCVFgxEzARBgNVBAcMClJpY2hhcmRzb24xDzANBgNVBAoMBkhlZGVyYTEPMA0G\n"
                    + "A1UECwwGSGVkZXJhMRAwDgYDVQQDDAcwMDAwMDAwMR8wHQYJKoZIhvcNAQkBFhBh\n"
                    + "ZG1pbkBoZWRlcmEuY29tMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEm5b1+oG9R0qt\n"
                    + "zM7UZnS5l/xxUNHIHq5+NAvtlviCpJL19jrW9+/UOy00Qqc6vS6tS1hS+dNJmpiZ\n"
                    + "FN0EHew4VDR7ACnL4LDJKmIHWjQ0iwvZo5kCpO0r9BtPN5FvaSxyo1QwUjAPBgNV\n"
                    + "HREECDAGhwR/AAABMAsGA1UdDwQEAwIEsDATBgNVHSUEDDAKBggrBgEFBQcDATAd\n"
                    + "BgNVHQ4EFgQUeciBviJtjeuue0GPf1xllNw7qvYwCgYIKoZIzj0EAwMDaAAwZQIw\n"
                    + "JeG0H2HdsI1VhOYmJmYlNeKCNgAk+LMorzPmsIInVBO2HK2IrKfpReWDS/m5j51V\n"
                    + "AjEAxKBxDezJDqAZHTkTXCg+X9Q9V6J6M5yDy5IS90aCWEo+W8C1Hc6hkn2/NrvT\n"
                    + "PhwK\n"
                    + "-----END CERTIFICATE-----\n";

    public static final ByteArrayInputStream PREVIEWNET_CERT_NODE_3_BYTES =
            new ByteArrayInputStream(PREVIEWNET_CERT_NODE_3_STRING.getBytes(StandardCharsets.UTF_8));

    public static final CertificateFactory CERTIFICATE_FACTORY;

    static {
        try {
            CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static final X509Certificate PREVIEWNET_CERT_NODE_3;

    static {
        try {
            PREVIEWNET_CERT_NODE_3 =
                    (X509Certificate) CERTIFICATE_FACTORY.generateCertificate(PREVIEWNET_CERT_NODE_3_BYTES);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    static final X509Certificate[] CERTIFICATE_CHAIN = new X509Certificate[] {PREVIEWNET_CERT_NODE_3};

    @Test
    void skipsCheckIfVerificationIsDisabled() throws CertificateException {
        new HederaTrustManager(ByteString.EMPTY, false).checkServerTrusted(CERTIFICATE_CHAIN, "");
    }

    @Test
    void skipsCheckIfCertificateIsNotProvided() throws CertificateException {
        new HederaTrustManager(null, false).checkServerTrusted(CERTIFICATE_CHAIN, "");
    }

    @Test
    void throwsErrorIfCertificateIsNotProvidedButVerificationIsRequired() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new HederaTrustManager(null, true));
    }

    @Test
    void properlyChecksCertificateAgainstCurrentNetworkAddressBook() throws InterruptedException, CertificateException {
        var client = Client.forNetwork(Map.of("0.previewnet.hedera.com:50211", new AccountId(0, 0, 3)))
                .setTransportSecurity(true)
                .setVerifyCertificates(true)
                .setLedgerId(LedgerId.PREVIEWNET);

        var nodeAddress = Objects.requireNonNull(
                Objects.requireNonNull(client.network.addressBook).get(new AccountId(0, 0, 3)));
        new HederaTrustManager(nodeAddress.getCertHash(), client.isVerifyCertificates())
                .checkServerTrusted(CERTIFICATE_CHAIN, "");
    }

    @Test
    void certificateCheckFailWhenHashMismatches() throws InterruptedException, CertificateException {
        var client = Client.forNetwork(Map.of("0.previewnet.hedera.com:50211", new AccountId(0, 0, 3)))
                .setTransportSecurity(true)
                .setVerifyCertificates(true)
                .setLedgerId(LedgerId.PREVIEWNET);

        var nodeAddress = Objects.requireNonNull(
                Objects.requireNonNull(client.network.addressBook).get(new AccountId(0, 0, 4)));
        assertThatExceptionOfType(CertificateException.class)
                .isThrownBy(() -> new HederaTrustManager(nodeAddress.getCertHash(), client.isVerifyCertificates())
                        .checkServerTrusted(CERTIFICATE_CHAIN, ""));
    }
}
