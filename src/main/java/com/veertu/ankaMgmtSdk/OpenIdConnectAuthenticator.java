package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import org.apache.commons.lang.StringUtils;
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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenIdConnectAuthenticator {

    private final String mgmtUrl;
    private final String clientId;
    private final String clientSecret;

    private String userNameField;
    private String groupsField;
    private String providerUrl;
    private String displayName;

    private String refreshToken;
    private String accessToken;
    private long refreshExpires;
    private long expireIn;
    private long requestTime;


    private int timeout = 100;
    private int maxRetries = 20;
    private String wellKnownPath = ".well-known/openid-configuration";
    private String tokenUrl;


    private final String grantTypeClientCredentials = "client_credentials";
    private final String grantTypeRefreshToken = "refresh_token";


    public OpenIdConnectAuthenticator(String mgmtUrl, String clientId, String clientSecret) {
        this.mgmtUrl = mgmtUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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

    public void doDiscovery() throws AnkaMgmtException, ClientException {
        String url = String.format("%s/%s", providerUrl, wellKnownPath);
        String response = doGetRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("token_endpoint")) {
            tokenUrl = jsonResponse.getString("token_endpoint");
        } else {
            throw new AnkaMgmtException("no token endpoint on openid provider");
        }
    }

    public String authorizeWithProvider() throws AnkaMgmtException, ClientException {

        List<NameValuePair> headers = new ArrayList<>();
        headers.add(makeAuthorization());
        // Content-Type: application/x-www-form-urlencoded
        headers.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded"));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", grantTypeClientCredentials));

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
        if (!scopes.isEmpty()){
            String scope = StringUtils.join(scopes, " ");
            params.add(new BasicNameValuePair("scope", scope));
        }


        String response = doPostRequest(tokenUrl, params, headers);
        return processResponse(response);
    }

    public NameValuePair getAuthorization() throws AnkaMgmtException, ClientException {
        if (providerUrl == null || providerUrl.isEmpty()) { // lazy get config
            getControllerConfig();
        }
        if (tokenUrl == null || tokenUrl.isEmpty()) { // lazy oidc discovery
            doDiscovery();
        }
        if (accessToken == null || accessToken.isEmpty()) { // means this is the first request
            authorizeWithProvider();
        } else { // this is not the first request, check if we need to refresh the token
            long timePassed = timeNow() - requestTime;
            if (timePassed > expireIn) { // token expired, needs refresh
                if (timePassed < refreshExpires && refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        refreshWithRefreshToken(); // use refresh token to get a new token
                    } catch (Exception e) { // if we has some bad luck, fall back to provider
                        authorizeWithProvider();
                    }
                } else {
                    authorizeWithProvider(); // re-authenticate
                }
            }
        }
        return tokenToValuePair(accessToken);

    }

    private NameValuePair tokenToValuePair(String accessToken) {
        return new BasicNameValuePair("Authorization", String.format("Bearer %s", accessToken));
    }

    public String refreshWithRefreshToken() throws AnkaMgmtException, ClientException {
        List<NameValuePair> headers = new ArrayList<>();
//        headers.add(makeAuthorization());
        headers.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded"));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", grantTypeRefreshToken));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));

        String response = doPostRequest(tokenUrl, params, headers);
        return processResponse(response);
    }

    private NameValuePair makeAuthorization() {
        String authorizationPair = String.format("%s:%s", clientId, clientSecret);
        String encoded = DatatypeConverter.printBase64Binary(authorizationPair.getBytes());
        return new BasicNameValuePair("Authorization", String.format("Basic %s", encoded));
    }

    public String doPostRequest(String url, Iterable<NameValuePair> params, Iterable<NameValuePair> headers) throws AnkaMgmtException, ClientException {

        RequestBuilder builder = RequestBuilder.post();
        builder.setUri(url);
        for (NameValuePair pair: headers) {
            builder.setHeader(pair.getName(), pair.getValue());
        }
        HttpEntity body = new UrlEncodedFormEntity(params);
        builder.setEntity(body);

//        for (NameValuePair pair: params) {
//            builder.addParameter(pair);
//        }
        HttpUriRequest request = builder.build();
        return doRequest((HttpRequestBase) request);
    }

    protected String doGetRequest(String url) throws AnkaMgmtException, ClientException {
        HttpRequestBase request = new HttpGet(url);
        return doRequest(request);
    }

    protected String doRequest(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        int retry = 0;
        while (true){
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
                // no retry on client exception
                throw new ClientException(request.getMethod() + request.getURI().toString() + "Bad Request");
            }
            catch (Exception e) {
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
//        setTLSVerificationIfDefined(sslContext, builder);
        // TODO: add support for self signed certs
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(requestBuilder.build()).build();
        return httpClient;
    }

    private boolean checkIfNeedsContinue(HttpResponse response) throws HttpResponseException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode >= 400 && responseCode < 500) {
            throw new HttpResponseException(responseCode, response.getStatusLine().getReasonPhrase());
        }
        if (responseCode >= 500) {
            return true;
        }
        return false;
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
        List<String> profileClaims = Arrays.asList("name", "family_name", "given_name", "middle_name", "nickname", "preferred_username", "profile", "picture", "website", "gender", "birthdate", "zoneinfo", "locale", "updated_at");
        return profileClaims.contains(claimName);
    }

    private long timeNow() {
        return  System.currentTimeMillis() / 1000;
    }

    private String processResponse(String response) {
        requestTime = timeNow();
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("access_token")) {
            accessToken = jsonResponse.getString("access_token");
        }
        if (jsonResponse.has("refresh_token")) {
            refreshToken = jsonResponse.getString("refresh_token");
        }
        if (jsonResponse.has("refresh_expires_in")) {
            refreshExpires = jsonResponse.getLong("refresh_expires_in");
        }
        if (jsonResponse.has("expires_in")) {
            expireIn = jsonResponse.getLong("expires_in");
        }
        return accessToken;
    }
}
