package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnAuthenticatedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnauthorizedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class AnkaMgmtOpenIdCommunicator extends AnkaMgmtCommunicator {

    private final OpenIdConnectAuthenticator authenticator;

    public AnkaMgmtOpenIdCommunicator(String mgmtUrl, String clientId, String clientSecret) {
        super(mgmtUrl);
        authenticator = new OpenIdConnectAuthenticator(mgmtUrl, clientId, clientSecret);
    }

    public AnkaMgmtOpenIdCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key) {
        super(mgmtUrl, skipTLSVerification);
        authenticator = new OpenIdConnectAuthenticator(mgmtUrl, client, key);

    }

    public AnkaMgmtOpenIdCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtUrl, skipTLSVerification, rootCA);
        authenticator = new OpenIdConnectAuthenticator(mgmtUrl, client, key);
    }

    protected JSONObject doRequest(AnkaMgmtCommunicator.RequestMethod method, String url, JSONObject requestBody) throws IOException, AnkaMgmtException {
        int retry = 0;
        while (true){
            try {
                CloseableHttpClient httpClient = makeHttpClient();
                HttpRequestBase request;
                try {
                    switch (method) {
                        case POST:
                            HttpPost postRequest = new HttpPost(url);
                            request = setBody(postRequest, requestBody);
                            break;
                        case DELETE:
                            HttpDeleteWithBody delRequest = new HttpDeleteWithBody(url);
                            request = setBody(delRequest, requestBody);
                            break;
                        case GET:
                            request = new HttpGet(url);
                            break;
                        default:
                            request = new HttpGet(url);
                            break;
                    }

                    NameValuePair authHeader = authenticator.getAuthorization();
                    request.setHeader(authHeader.getName(), authHeader.getValue());

                    HttpResponse response = httpClient.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 401) {
                        throw new AnkaUnAuthenticatedRequestException("Authentication Required");
                    }
                    if (responseCode == 403) {
                        throw new AnkaUnauthorizedRequestException("Not authorized to perform this request");
                    }

                    if (responseCode >= 400) {
                        throw new ClientException(request.getMethod() + request.getURI().toString() + "Bad Request");
                    }

                    if (responseCode != 200) {
                        return null;
                    }
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(entity.getContent()));
                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        JSONObject jsonResponse = new JSONObject(result.toString());
                        return jsonResponse;
                    }

                } catch (ClientException e) {
                    // don't retry on client exception
                    throw e;
                } catch (HttpHostConnectException | ConnectTimeoutException e) {
                    throw new AnkaMgmtException(e);
                } catch (SSLException e) {
                    throw e;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AnkaMgmtException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AnkaMgmtException(e);
                } finally {
                    httpClient.close();
                }
                return null;
            } catch (ClientException e) {
                // don't retry on client exception
                throw new AnkaMgmtException(e);
            } catch (Exception e) {
                if (retry >= maxRetries) {
                    continue;
                }
                throw new AnkaMgmtException(e);
            }
        }

    }
}
