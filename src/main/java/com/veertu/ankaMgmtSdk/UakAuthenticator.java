package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import com.veertu.plugin.anka.AnkaMgmtCloud;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.nio.charset.StandardCharsets;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public class UakAuthenticator {

    private static final int maxRetries = 3;
    private final List<String> mgmtURLs;
    private final boolean skipTLSVerification;
    private final String rootCA;
    private final String id;
    private PrivateKey key;
    private String cloudName;

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    private boolean controllerUsesHttps() {
        for (String mgmtURL : mgmtURLs) {
            if (mgmtURL != null && mgmtURL.regionMatches(true, 0, "https://", 0, 8)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a UAK credential string and returns the RSA private key.
     * UAK can be either a PEM formatted private key or just a concatenated string without header and footer.
     *
     * @param key the key string to parse
     * @return the RSA private key object
     * @throws IllegalArgumentException if the key is null, empty, or not a valid RSA private key
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "Intentional catch-all: any parsing/decoding failure is reported as an invalid key.")
    public static RSAPrivateKey getRSAPrivateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        try {
            String privateKeyPEM = key
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            ASN1Primitive asn1Object = ASN1Primitive.fromByteArray(keyBytes);
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(asn1Object);

            if (rsaPrivateKey == null) {
                throw new IllegalArgumentException("Failed to parse RSA private key");
            }

            return rsaPrivateKey;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA private key: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes a new instance of the UakAuthenticator class.
     *
     * @param mgmtURLs            the Anka Management API URLs
     * @param skipTLSVerification whether to skip TLS verification
     * @param rootCA              the root CA certificate in PEM format
     * @param id                  the UAK ID
     * @param pemKey              the RSA private key in PEM format
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "Intentional catch-all: key init failures are logged and leave key null for later handling.")
    public UakAuthenticator(List<String> mgmtURLs, boolean skipTLSVerification, String rootCA, String id, String pemKey) {
        this.mgmtURLs = mgmtURLs;
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
        this.id = id;

        try {
            // Parse and validate the UAK credential
            RSAPrivateKey rsaPrivateKey = getRSAPrivateKey(pemKey);

            // Convert PKCS#1 to PKCS#8
            PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
                    new AlgorithmIdentifier(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption),
                    rsaPrivateKey);
            byte[] pkcs8Bytes = privateKeyInfo.getEncoded();

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.key = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            AnkaMgmtCloud.Log("Failed to initialize RSA private key for id " + id + ": " + e.getMessage());
        }
    }

    /**
     * Gets the Authorization header for the Anka Management API.
     *
     * @return the Authorization header
     * @throws AnkaMgmtException if the request fails
     * @throws ClientException   if the request fails
     */
    public NameValuePair getAuthorization() throws AnkaMgmtException, ClientException {
        if (key == null) {
            throw new AnkaMgmtException("Failed to initialize RSA private key for " + id);
        }

        String secret = tapHandRequest();
        String token = tapShakeRequest(secret);
        return new BasicNameValuePair("Authorization", String.format("Bearer %s", token));
    }

    /**
     * Sends a POST request to the Anka Management API to handshake with the UAK.
     *
     * @return the secret
     * @throws AnkaMgmtException if the request fails
     * @throws ClientException   if the request fails
     */
    private String tapHandRequest() throws AnkaMgmtException, ClientException {
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
        } catch (GeneralSecurityException e) {
            throw new AnkaMgmtException("Failed to decrypt response: " + e.getMessage());
        }

        String secret = new String(decryptedBytes, StandardCharsets.UTF_8);

        return secret;
    }

    /**
     * Sends a POST request to the Anka Management API to shake hands with the UAK.
     *
     * @param secret the secret to send
     * @return the token
     * @throws AnkaMgmtException if the request fails
     * @throws ClientException   if the request fails
     */
    private String tapShakeRequest(String secret) throws AnkaMgmtException, ClientException {
        JSONObject shakeObj = new JSONObject();
        shakeObj.put("id", id);
        shakeObj.put("secret", secret);

        String responseText = postRequest("/tap/v1/shake", shakeObj.toString());

        JSONObject jsonResponse = new JSONObject(responseText);
        JSONObject authObj = jsonResponse.getJSONObject("data");

        String jsonString = authObj.toString();
        String token = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));

        return token;
    }

    /**
     * Sends a POST request to the Anka Management API.
     *
     * @param endpoint the API endpoint
     * @param jsonData the JSON data to send
     * @return the response text
     * @throws ClientException if the request fails
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "createHttpClient declares throws Exception; catching it drives endpoint retry/failover.")
    private String postRequest(String endpoint, String jsonData) throws ClientException {
        int retries = 1;
        Exception lastFailure = null;
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

                        AnkaMgmtCloud.Log(AnkaSdkLog.cloudLabel(cloudName) + ": POST request to " + endpoint
                                + " failed: " + statusCode + " - " + responseText);

                        if (statusCode >= 400 && statusCode < 500) {
                            break retryLoop;
                        }
                    }
                } catch (Exception e) {
                    AnkaMgmtCloud.Log(AnkaSdkLog.cloudLabel(cloudName) + ": Failed to send request to: " + endpoint
                            + " - " + e.getMessage());
                    lastFailure = e;
                }
            }

            retries++;
        }

        if (lastFailure != null) {
            throw new ClientException(lastFailure);
        }
        throw new ClientException("Failed to send request to any of the endpoints");
    }

    /**
     * Creates an HttpClient with the provided configuration.
     *
     * @param skipTLSVerification whether to skip TLS verification
     * @param rootCA              the root CA certificate in PEM format
     * @return the HttpClient
     * @throws Exception if an error occurs while creating the HttpClient
     */
    private CloseableHttpClient createHttpClient(boolean skipTLSVerification, String rootCA) throws Exception {
        SSLConnectionSocketFactory sslSocketFactory;
        boolean usesHttps = controllerUsesHttps();

        if (skipTLSVerification) {
            if (usesHttps) {
                AnkaMgmtCloud.Log("%s: UAK auth: TLS verification DISABLED (Skip TLS Verification is on). "
                        + "The controller certificate is NOT validated; this is insecure and intended for testing only.",
                        AnkaSdkLog.cloudLabel(cloudName));
            }
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
            sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        } else if (usesHttps && rootCA != null && !rootCA.isEmpty()) {
            AnkaMgmtCloud.Log("%s: UAK auth: validating the controller certificate against the configured Root CA "
                    + "via standard PKIX path validation.",
                    AnkaSdkLog.cloudLabel(cloudName));
            sslSocketFactory = createCustomSSLSocketFactory(rootCA);

        } else if (usesHttps) {
            AnkaMgmtCloud.Log("%s: UAK auth: validating the controller certificate against the JVM default trust store "
                    + "(no Root CA configured).",
                    AnkaSdkLog.cloudLabel(cloudName));
            sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();

        } else {
            sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        }

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory);

        // Apply Jenkins proxy configuration using the extracted method
        httpClientBuilder = applyJenkinsProxy(httpClientBuilder);

        return httpClientBuilder.build();
    }

    /**
     * Creates a custom SSLConnectionSocketFactory using the provided root CA certificate.
     *
     * @param rootCA the root CA certificate in PEM format
     * @return the custom SSLConnectionSocketFactory
     * @throws Exception if an error occurs while creating the SSLConnectionSocketFactory
     */
    private SSLConnectionSocketFactory createCustomSSLSocketFactory(String rootCA) throws Exception {
        Certificate certificate = RootCaCertificateParser.parsePemToCertificate(rootCA);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        keyStore.setCertificateEntry("rootCA", certificate);

        // Validate the controller certificate against the configured Root CA using standard PKIX.
        // Passing a null TrustStrategy (instead of TrustSelfSignedStrategy) keeps the keystore-backed
        // trust manager in effect; TrustSelfSignedStrategy would trust ANY single-certificate chain and
        // bypass the configured Root CA for self-signed controller certificates.
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(keyStore, null)
                .build();

        return new SSLConnectionSocketFactory(sslContext);
    }

    /**
     * Applies the Jenkins proxy configuration to the provided HttpClientBuilder.
     *
     * @param builder the HttpClientBuilder to configure
     * @return the updated HttpClientBuilder with proxy settings applied (if available)
     */
    private HttpClientBuilder applyJenkinsProxy(HttpClientBuilder builder) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            hudson.ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null && proxyConfig.name != null && !proxyConfig.name.isEmpty()) {
                HttpHost proxyHost = new HttpHost(proxyConfig.name, proxyConfig.port);
                builder.setProxy(proxyHost);

                if (proxyConfig.getUserName() != null && !proxyConfig.getUserName().isEmpty()) {
                    org.apache.http.client.CredentialsProvider credsProvider =
                            new org.apache.http.impl.client.BasicCredentialsProvider();
                    credsProvider.setCredentials(
                            new AuthScope(proxyConfig.name, proxyConfig.port),
                            new UsernamePasswordCredentials(
                                    proxyConfig.getUserName(),
                                    proxyConfig.getPassword()
                            )
                    );
                    builder.setDefaultCredentialsProvider(credsProvider);
                }
            }
        }
        return builder;
    }
}
