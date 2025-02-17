package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import com.veertu.plugin.anka.AnkaMgmtCloud;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public class UakAuthenticator {

    private final int maxRetries = 3;
    private final List<String> mgmtURLs;
    private final boolean skipTLSVerification;
    private final String rootCA;
    private final String id;
    private PrivateKey key;

    public UakAuthenticator(List<String> mgmtURLs, boolean skipTLSVerification, String rootCA, String id, String pemKey) {
        this.mgmtURLs = mgmtURLs;
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
        this.id = id;

        try {
            String privateKeyPEM = pemKey
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            ASN1Primitive asn1Object = ASN1Primitive.fromByteArray(keyBytes);
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(asn1Object);

            // Convert PKCS#1 to PKCS#8
            PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
                    new AlgorithmIdentifier(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption),
                    rsaPrivateKey);
            byte[] pkcs8Bytes = privateKeyInfo.getEncoded();

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.key = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            AnkaMgmtCloud.Log("Failed to initialize RSA private key: " + e.getMessage());
        }
    }

    public NameValuePair getAuthorization() throws AnkaMgmtException, ClientException {
        if (key == null) {
            throw new AnkaMgmtException("Failed to initialize RSA private key for " + id);
        }

        String secret = TapHandRequest();
        String token = TapShakeRequest(secret);
        return new BasicNameValuePair("Authorization", String.format("Bearer %s", token));
    }

    private String TapHandRequest() throws AnkaMgmtException, ClientException {
        JSONObject handObj = new JSONObject();
        handObj.put("id", id);

        String responseText = postRequest("/tap/v1/hand", handObj.toString());
        byte[] encryptedBytes = Base64.getDecoder().decode(responseText);

        byte[] decryptedBytes;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            // Ensure OAEP uses SHA-256 and MGF1
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1",
                    new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);

            cipher.init(Cipher.DECRYPT_MODE, key, oaepParams);
            decryptedBytes = cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            throw new AnkaMgmtException("Failed to decrypt response: " + e.getMessage());
        }

        String secret = new String(decryptedBytes);

        return secret;
    }

    private String TapShakeRequest(String secret) throws AnkaMgmtException, ClientException {
        JSONObject shakeObj = new JSONObject();
        shakeObj.put("id", id);
        shakeObj.put("secret", secret);

        String responseText = postRequest("/tap/v1/shake", shakeObj.toString());

        JSONObject jsonResponse = new JSONObject(responseText);
        JSONObject authObj = jsonResponse.getJSONObject("data");

        String jsonString = authObj.toString();
        String token = Base64.getEncoder().encodeToString(jsonString.getBytes());

        return token;
    }

    private String postRequest(String endpoint, String jsonData) throws ClientException {
        int retries = 1;
        retryLoop:
        while (retries <= maxRetries && mgmtURLs.size() == 1) { // Only retry if there is a single endpoint
            for (String mgmtURL : mgmtURLs) {
                try (CloseableHttpClient client = createHttpClient(skipTLSVerification, rootCA)) {
                    HttpUriRequest request = RequestBuilder.post(mgmtURL + endpoint)
                            .setHeader("Content-Type", "application/json")
                            .setEntity(new StringEntity(jsonData, "UTF-8"))
                            .build();

                    try (CloseableHttpResponse response = client.execute(request)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        String responseText = EntityUtils.toString(response.getEntity());

                        if (statusCode == 200) {
                            return responseText;
                        }

                        AnkaMgmtCloud.Log("POST request to " + endpoint + " failed: " + statusCode + " - " + responseText);

                        if (statusCode >= 400 && statusCode < 500) {
                            break retryLoop;
                        }
                    }
                } catch (Exception e) {
                    AnkaMgmtCloud.Log("Failed to send request to: " + endpoint + " - " + e.getMessage());
                }
            }

            retries++;
        }

        throw new ClientException("Failed to send request to any of the endpoints");
    }

    private CloseableHttpClient createHttpClient(boolean skipTLSVerification, String rootCA) throws Exception {
        SSLConnectionSocketFactory sslSocketFactory;

        if (skipTLSVerification) {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
            sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        } else if (rootCA != null && !rootCA.isEmpty()) {
            sslSocketFactory = createCustomSSLSocketFactory(rootCA);

        } else {
            sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        }

        return HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
    }

    private SSLConnectionSocketFactory createCustomSSLSocketFactory(String rootCA) throws Exception {
        try (InputStream caInput = new ByteArrayInputStream(rootCA.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(caInput);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("custom-ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();

            return new SSLConnectionSocketFactory(sslContext);
        }
    }
}
