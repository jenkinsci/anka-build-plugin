package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import com.veertu.plugin.anka.AnkaMgmtCloud;
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
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
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
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.net.ssl.SSLContext;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
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

    /**
     * Parses a UAK credential string and returns the RSA private key.
     * UAK can be either a PEM formatted private key or just a concatenated string without header and footer.
     *
     * @param key the key string to parse
     * @return the RSA private key object
     * @throws IllegalArgumentException if the key is null, empty, or not a valid RSA private key
     */
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

        String secret = TapHandRequest();
        String token = TapShakeRequest(secret);
        return new BasicNameValuePair("Authorization", String.format("Bearer %s", token));
    }

    /**
     * Sends a POST request to the Anka Management API to handshake with the UAK.
     *
     * @return the secret
     * @throws AnkaMgmtException if the request fails
     * @throws ClientException   if the request fails
     */
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

    /**
     * Sends a POST request to the Anka Management API to shake hands with the UAK.
     *
     * @param secret the secret to send
     * @return the token
     * @throws AnkaMgmtException if the request fails
     * @throws ClientException   if the request fails
     */
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

    /**
     * Sends a POST request to the Anka Management API.
     *
     * @param endpoint the API endpoint
     * @param jsonData the JSON data to send
     * @return the response text
     * @throws ClientException if the request fails
     */
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
        PEMParser reader;
        BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
        reader = new PEMParser(new StringReader(rootCA));
        X509CertificateHolder holder = (X509CertificateHolder) reader.readObject();
        Certificate certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(holder);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        keyStore.setCertificateEntry("rootCA", certificate);


        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
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
