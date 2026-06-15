package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Runtime HTTP client for OIDC authentication. Not a Jenkins persisted model and never written to config.xml.
 */
public class OpenIdConnectAuthenticator {

    private static final String OPENID_WELL_KNOWN_PATH = ".well-known/openid-configuration";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String GRANT_TYPE_REFRESH = "refresh_token";

    private final String mgmtUrl;
    private final String clientId;
    private final Secret clientSecret;

    private String userNameField;
    private String groupsField;
    private String providerUrl;
    private String displayName;

    private final transient RuntimeOidcSession runtimeSession = new RuntimeOidcSession();

    private int timeout = 100;
    private int maxRetries = 20;

    public OpenIdConnectAuthenticator(String mgmtUrl, String clientId, String clientSecret) {
        this.mgmtUrl = mgmtUrl;
        this.clientId = clientId;
        this.clientSecret = Secret.fromString(clientSecret);
    }

    public void getControllerConfig() throws AnkaMgmtException, ClientException {
        String url = String.format("%s/config/v1/auth", mgmtUrl);

        String response = doGetRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("status") && jsonResponse.getString("status").equals("OK")) {
            JSONObject body = jsonResponse.getJSONObject("body");
            if (body.has("oidc")) {
                JSONObject oidcConfig = body.getJSONObject("oidc");
                try {
                    userNameField = oidcConfig.getString("user_name_field");
                    groupsField = oidcConfig.getString("groups_field");
                    providerUrl = oidcConfig.getString("provider_url");
                    displayName = oidcConfig.getString("display_name");
                } catch (JSONException e) {
                    throw new AnkaMgmtException(e);
                }
            } else {
                throw new AnkaMgmtException("no oidc configuration in controller");
            }
        } else {
            String message = "";
            if (jsonResponse.has("message") && jsonResponse.getString("message") != null) {
                message = jsonResponse.getString("message");
            }
            throw new AnkaMgmtException(message);
        }
    }

    public void discoverOpenIdProvider() throws AnkaMgmtException, ClientException {
        String url = String.format("%s/%s", providerUrl, OPENID_WELL_KNOWN_PATH);
        String response = doGetRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("token_endpoint")) {
            runtimeSession.providerAuthEndpoint = URI.create(jsonResponse.getString("token_endpoint"));
        } else {
            throw new AnkaMgmtException("no token endpoint on openid provider");
        }
    }

    public String authorizeWithProvider() throws AnkaMgmtException, ClientException {

        List<NameValuePair> headers = new ArrayList<>();
        headers.add(makeAuthorization());
        headers.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded"));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS));

        List<String> scopes = new ArrayList<>();
        if (isClaimInProfile(userNameField) || isClaimInProfile(groupsField)) {
            scopes.add("profile");
        }
        if (!isClaimInProfile(userNameField)) {
            scopes.add(userNameField);
        }
        if (!isClaimInProfile(groupsField)) {
            scopes.add(groupsField);
        }
        if (!scopes.isEmpty()) {
            String scope = StringUtils.join(scopes, " ");
            params.add(new BasicNameValuePair("scope", scope));
        }

        String response = doPostRequest(requireAuthEndpoint(), params, headers);
        return processResponse(response);
    }

    public NameValuePair getAuthorization() throws AnkaMgmtException, ClientException {
        if (providerUrl == null || providerUrl.isEmpty()) {
            getControllerConfig();
        }
        if (runtimeSession.providerAuthEndpoint == null) {
            discoverOpenIdProvider();
        }
        if (runtimeSession.cachedAccessCredential == null
                || runtimeSession.cachedAccessCredential.getPlainText().isEmpty()) {
            authorizeWithProvider();
        } else {
            long timePassed = timeNow() - runtimeSession.requestTime;
            if (timePassed > runtimeSession.expireIn) {
                if (timePassed < runtimeSession.refreshExpires
                        && runtimeSession.cachedRefreshCredential != null
                        && !runtimeSession.cachedRefreshCredential.getPlainText().isEmpty()) {
                    try {
                        refreshWithRefreshCredential();
                    } catch (Exception e) {
                        authorizeWithProvider();
                    }
                } else {
                    authorizeWithProvider();
                }
            }
        }
        return credentialToAuthorizationHeader(runtimeSession.cachedAccessCredential);
    }

    private NameValuePair credentialToAuthorizationHeader(Secret accessCredential) {
        return new BasicNameValuePair(
                "Authorization",
                String.format("Bearer %s", accessCredential.getPlainText()));
    }

    public String refreshWithRefreshCredential() throws AnkaMgmtException, ClientException {
        List<NameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded"));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", GRANT_TYPE_REFRESH));
        params.add(new BasicNameValuePair(
                "refresh_token",
                runtimeSession.cachedRefreshCredential.getPlainText()));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret.getPlainText()));

        String response = doPostRequest(requireAuthEndpoint(), params, headers);
        return processResponse(response);
    }

    private String requireAuthEndpoint() throws AnkaMgmtException, ClientException {
        if (runtimeSession.providerAuthEndpoint == null) {
            discoverOpenIdProvider();
        }
        return runtimeSession.providerAuthEndpoint.toString();
    }

    private NameValuePair makeAuthorization() {
        String authorizationPair = String.format("%s:%s", clientId, clientSecret.getPlainText());
        String encoded = Base64.getEncoder().encodeToString(authorizationPair.getBytes());
        return new BasicNameValuePair("Authorization", String.format("Basic %s", encoded));
    }

    public String doPostRequest(String url, Iterable<NameValuePair> params, Iterable<NameValuePair> headers)
            throws AnkaMgmtException, ClientException {

        RequestBuilder builder = RequestBuilder.post();
        builder.setUri(url);
        for (NameValuePair pair : headers) {
            builder.setHeader(pair.getName(), pair.getValue());
        }
        HttpEntity body = new UrlEncodedFormEntity(params);
        builder.setEntity(body);

        HttpUriRequest request = builder.build();
        return doRequest((HttpRequestBase) request);
    }

    protected String doGetRequest(String url) throws AnkaMgmtException, ClientException {
        HttpRequestBase request = new HttpGet(url);
        return doRequest(request);
    }

    protected String doRequest(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        int retry = 0;
        while (true) {
            try {
                retry++;
                System.out.println("getUri: " + request.getMethod());
                System.out.println("getUri: " + request.getRequestLine().toString());
                System.out.println("getUri: " + request.getURI().toString());
                CloseableHttpClient httpClient = makeHttpClient();
                HttpResponse response = httpClient.execute(request);
                if (checkIfNeedsContinue(response)) {
                    continue;
                }

                return readResponse(response);

            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                e.printStackTrace();
                throw new AnkaMgmtException(e);
            } catch (HttpResponseException e) {
                throw new ClientException(request.getMethod() + request.getURI().toString() + "Bad Request");
            } catch (Exception e) {
                if (retry >= maxRetries) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    continue;
                }
                throw new AnkaMgmtException(e);
            }
        }

    }

    private CloseableHttpClient makeHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(timeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);
        HttpClientBuilder builder = HttpClientBuilder.create();

        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, utils.strategyLambda()).build();
        builder.setSSLContext(sslContext);
        return builder.setDefaultRequestConfig(requestBuilder.build()).build();
    }

    private boolean checkIfNeedsContinue(HttpResponse response) throws HttpResponseException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode >= 400 && responseCode < 500) {
            throw new HttpResponseException(responseCode, response.getStatusLine().getReasonPhrase());
        }
        return responseCode >= 500;
    }

    private String readResponse(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(entity.getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        }
        return null;
    }

    private boolean isClaimInProfile(String claimName) {
        List<String> profileClaims = Arrays.asList(
                "name", "family_name", "given_name", "middle_name", "nickname", "preferred_username", "profile",
                "picture", "website", "gender", "birthdate", "zoneinfo", "locale", "updated_at");
        return profileClaims.contains(claimName);
    }

    private long timeNow() {
        return System.currentTimeMillis() / 1000;
    }

    private String processResponse(String response) {
        runtimeSession.requestTime = timeNow();
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("access_token")) {
            runtimeSession.cachedAccessCredential = Secret.fromString(jsonResponse.getString("access_token"));
        }
        if (jsonResponse.has("refresh_token")) {
            runtimeSession.cachedRefreshCredential = Secret.fromString(jsonResponse.getString("refresh_token"));
        }
        if (jsonResponse.has("refresh_expires_in")) {
            runtimeSession.refreshExpires = jsonResponse.getLong("refresh_expires_in");
        }
        if (jsonResponse.has("expires_in")) {
            runtimeSession.expireIn = jsonResponse.getLong("expires_in");
        }
        return runtimeSession.cachedAccessCredential.getPlainText();
    }

    private static final class RuntimeOidcSession {
        private URI providerAuthEndpoint;
        private Secret cachedAccessCredential;
        private Secret cachedRefreshCredential;
        private long refreshExpires;
        private long expireIn;
        private long requestTime;
    }
}
