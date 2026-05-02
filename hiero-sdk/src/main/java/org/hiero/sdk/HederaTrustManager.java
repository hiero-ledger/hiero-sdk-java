// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.annotation.Nullable;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class used by node.
 */
class HederaTrustManager implements X509TrustManager {
    private static final String CERTIFICATE = "CERTIFICATE";
    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
    private static final String PEM_FOOTER = "-----END CERTIFICATE-----\n";
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Nullable
    private final String certHash;

    /**
     * Constructor.
     *
     * @param certHash                  a byte string of the certificate hash
     * @param verifyCertificate         should be verified
     */
    HederaTrustManager(@Nullable ByteString certHash, boolean verifyCertificate) {
        if (certHash == null || certHash.isEmpty()) {
            if (verifyCertificate) {
                throw new IllegalStateException(
                        "transport security and certificate verification are enabled, but no applicable address book was found");
            }

            logger.warn("skipping certificate check since no cert hash was found");
            this.certHash = null;
        } else {
            this.certHash = new String(certHash.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        throw new UnsupportedOperationException(
                "Attempted to use HederaTrustManager to verify a client, but this trust manager is for verifying server only");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (certHash == null) {
            return;
        }

        for (var cert : chain) {
            byte[] pem;

            try (var outputStream = new ByteArrayOutputStream();
                    var pemWriter = new PemWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                pemWriter.writeObject(new PemObject(CERTIFICATE, cert.getEncoded()));
                pemWriter.flush();

                pem = outputStream.toByteArray();
            } catch (IOException e) {
                logger.warn("Failed to write PEM to byte array: ", e);
                continue;
            }

            var certHashBytes = new byte[0];

            try {
                certHashBytes = MessageDigest.getInstance("SHA-384").digest(pem);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to find SHA-384 digest for certificate hashing", e);
            }

            if (this.certHash.equals(Hex.toHexString(certHashBytes))) {
                return;
            }
        }

        throw new CertificateException("Failed to confirm the server's certificate from a known address book");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
