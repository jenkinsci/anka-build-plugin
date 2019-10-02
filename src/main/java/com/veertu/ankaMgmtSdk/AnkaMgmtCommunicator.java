package com.veertu.ankaMgmtSdk;

import com.veertu.RoundRobin;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnAuthenticatedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnauthorizedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import com.veertu.plugin.anka.AnkaMgmtCloud;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.utils.URIBuilder;

import java.io.*;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by asafgur on 09/05/2017.
 */
public class AnkaMgmtCommunicator {

    protected URL mgmtUrl;
    protected final int timeout = 30000;
    protected final int maxRetries = 10;
    protected boolean skipTLSVerification;
    protected String rootCA;
    protected RoundRobin roundRobin;


    public AnkaMgmtCommunicator(String url) {
        try {
            URL tmpUrl = new URL(url);
            URIBuilder b = new URIBuilder();
            b.setScheme(tmpUrl.getProtocol());
            b.setHost( tmpUrl.getHost());
            b.setPort(tmpUrl.getPort());
            mgmtUrl = b.build().toURL();

        } catch (IOException | URISyntaxException e) {

            e.printStackTrace();
        }
    }

    public AnkaMgmtCommunicator(String mgmtURL, boolean skipTLSVerification) {
        this(mgmtURL);
        this.skipTLSVerification = skipTLSVerification;
    }

    public AnkaMgmtCommunicator(String mgmtUrl, String rootCA) {
        this(mgmtUrl);
        this.rootCA = rootCA;
    }

    public AnkaMgmtCommunicator(String mgmtUrl, boolean skipTLSVerification, String rootCA) {
        this(mgmtUrl);
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
    }

    public AnkaMgmtCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String rootCA) {
        this.roundRobin = new RoundRobin(mgmtURLS);
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
    }

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        List<AnkaVmTemplate> templates = new ArrayList<AnkaVmTemplate>();
        String url = "/api/v1/registry/vm";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String vmId = jsonObj.getString("id");
                    String name = jsonObj.getString("name");
                    AnkaVmTemplate vm = new AnkaVmTemplate(vmId, name);
                    templates.add(vm);
                }
            }
        } catch (IOException e) {
            return templates;
        }
        return templates;
    }


    public List<String> getTemplateTags(String templateId) throws AnkaMgmtException {
        List<String> tags = new ArrayList<String>();
        String url = String.format("/api/v1/registry/vm?id=%s", templateId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject templateVm = jsonResponse.getJSONObject("body");
                JSONArray vmsJson = templateVm.getJSONArray("versions");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String tag = jsonObj.getString("tag");
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            AnkaMgmtCloud.Log("Exception trying to access: '%s'", url);
        } catch (org.json.JSONException e) {
            AnkaMgmtCloud.Log("Exception trying to parse response: '%s'", url);
        }
        return tags;
    }


    public List<NodeGroup> getNodeGroups() throws AnkaMgmtException {
        List<NodeGroup> groups = new ArrayList<>();
        String url = "/api/v1/group";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResponse = jsonResponse.getString("status");
            if (logicalResponse.equals("OK")) {
                JSONArray groupsJson = jsonResponse.getJSONArray("body");
                for (int i = 0; i < groupsJson.length(); i++) {
                    JSONObject groupJsonObject = groupsJson.getJSONObject(i);
                    NodeGroup nodeGroup = new NodeGroup(groupJsonObject);
                    groups.add(nodeGroup);
                }
            } else {
                throw new AnkaMgmtException(jsonResponse.getString("message"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnkaMgmtException(e);
        } catch (JSONException e) {
            return groups;
        }
        return groups;
    }

    public String startVm(String templateId, String tag, String nameTemplate, String startUpScript, String groupId, int priority) throws AnkaMgmtException {
        String url = "/api/v1/vm";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("vmid", templateId);
        if (tag != null)
            jsonObject.put("tag", tag);
        if (nameTemplate != null)
            jsonObject.put("name_template", nameTemplate);
        if (startUpScript != null) {
            String b64Script = Base64.encodeBase64String(startUpScript.getBytes());
            jsonObject.put("startup_script", b64Script);
        }
        if (groupId != null) {
            jsonObject.put("group_id", groupId);
        }
        if (priority > 0) {
            jsonObject.put("priority", priority);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String logicalResult = jsonResponse.getString("status");
        if (logicalResult.equals("OK")) {
            JSONArray uuidsJson = jsonResponse.getJSONArray("body");
            if (uuidsJson.length() >= 1 ){
                return uuidsJson.getString(0);
            }
        }
        if (tag != null && !tag.isEmpty()) {
            String message = jsonResponse.getString("message");
            if (message.equals("No such tag "+ tag)) {
                AnkaMgmtCloud.Log("Tag " + tag + " not found. starting vm with latest tag");
                return startVm(templateId, null, nameTemplate, startUpScript, groupId, priority);
            }
        }

        throw new AnkaMgmtException(jsonResponse.getString("message"));
    }

    public AnkaVmSession showVm(String sessionId) throws AnkaMgmtException {
        String url = String.format("/api/v1/vm?id=%s", sessionId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject body = jsonResponse.getJSONObject("body");
                return new AnkaVmSession(sessionId, body);
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean terminateVm(String sessionId) throws AnkaMgmtException {
        String url = "%s/api/v1/vm";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", sessionId);
            JSONObject jsonResponse = this.doRequest(RequestMethod.DELETE, url, jsonObject);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<AnkaVmSession> list() throws AnkaMgmtException {
        List<AnkaVmSession> vms = new ArrayList<>();
        String url = "/api/v1/vm";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (int i = 0; i < vmsJson.length(); i++) {
                    JSONObject vmJson = vmsJson.getJSONObject(i);
                    String instanceId = vmJson.getString("instance_id");
                    JSONObject vm = vmJson.getJSONObject("vm");
                    vm.put("instance_id", instanceId);
                    vm.put("cr_time", vm.getString("cr_time"));
                    AnkaVmSession ankaVmSession = AnkaVmSession.makeAnkaVmSessionFromJson(vmJson);
                    vms.add(ankaVmSession);
                }
            }
            return vms;
        } catch (IOException e) {
            return vms;
        }
    }

    public AnkaCloudStatus status() throws AnkaMgmtException {
        String url = "%s/api/v1/status";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject statusJson = jsonResponse.getJSONObject("body");
                return AnkaCloudStatus.fromJson(statusJson);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public void saveImage(String instanceId, String targetVMId, String newTemplateName, String tag,
                          String description, Boolean suspend, String shutdownScript,
                          Boolean revertBeforePush,
                          String revertTag,
                          Boolean doSuspendTest
    ) throws AnkaMgmtException {
        String url = "/api/v1/image";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", instanceId);
        if (targetVMId != null) {
            jsonObject.put("target_vm_id", targetVMId);
        }
        if (newTemplateName != null) {
            jsonObject.put("new_template_name", newTemplateName);
        }
        jsonObject.put("tag", tag);
        jsonObject.put("description", description);
        jsonObject.put("suspend", suspend);
        if (shutdownScript != null && !shutdownScript.isEmpty()) {
            String b64Script = Base64.encodeBase64String(shutdownScript.getBytes());
            jsonObject.put("script", b64Script);
        }
        if (revertBeforePush) {
            jsonObject.put("revert_before_push", true);
        }
        if (doSuspendTest) {
            jsonObject.put("do_suspend_sanity_test", true);
        }
        if (revertTag != null && !revertTag.isEmpty()) {
            jsonObject.put("revert_tag", revertTag);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonResponse == null ) {
            return;
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "error saving image"));
        }

    }

    public void revertRegistryVM(String templateID) throws AnkaMgmtException {
        String url = String.format("/api/v1/registry/revert?id=%s", templateID);
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.DELETE, url);
        } catch (IOException e) {
            throw new AnkaMgmtException(e);
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "error reverting template " + templateID));
        }

    }

    public List<JSONObject> getImageRequests() throws AnkaMgmtException {
        String url = "/api/v1/image";
        List<JSONObject> imageRequests = new ArrayList<>();
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.GET, url);

        } catch (AnkaMgmtException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClientException) {
                throw new AnkaNotFoundException("not found");
            }
        } catch (IOException e) {
            throw new AnkaMgmtException(e);
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "could not get image requests"));
        }
        JSONArray jsonArray = jsonResponse.optJSONArray("body");
        for (int i = 0; i < jsonArray.length(); i++) {
            imageRequests.add(jsonArray.getJSONObject(i));
        }
        return imageRequests;
    }

    protected enum RequestMethod {
        GET, POST, DELETE
    }

    private JSONObject doRequest(RequestMethod method, String url) throws IOException, AnkaMgmtException {
        return doRequest(method, url, null);
    }

    protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody) throws IOException, AnkaMgmtException {
        int retry = 0;
        while (true){
            try {
                retry++;

                CloseableHttpClient httpClient = makeHttpClient();
                HttpRequestBase request;
                try {
                    String host = "";
                    if (roundRobin != null) {
                        host = roundRobin.next();
                    } else {
                        host = mgmtUrl.toString();
                    }

                    String url = host + path;
                    switch (method) {
                        case POST:
                            HttpPost postRequest = new HttpPost(url);
                            request = setBody(postRequest, requestBody);
                            break;
                        case DELETE:
                            if (requestBody != null) {
                                HttpDeleteWithBody delRequest = new HttpDeleteWithBody(url);
                                request = setBody(delRequest, requestBody);
                            } else {
                                request = new HttpDelete(url);
                            }
                            break;
                        case GET:
                            request = new HttpGet(url);
                            break;
                        default:
                            request = new HttpGet(url);
                            break;
                    }
                    HttpResponse response ;
                    try {
                        long startTime = System.currentTimeMillis();
                        response = httpClient.execute(request);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (roundRobin != null) {
                            roundRobin.update(host, (int) elapsedTime, false);
                        }
                    } catch (HttpHostConnectException | ConnectTimeoutException e) {
                        if (roundRobin != null) {
                            roundRobin.update(host, 0, true);
                        }
                        throw e;
                    }
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
                        AnkaMgmtCloud.Log(String.format("url: %s response: %s", url, response.toString()));
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

                } catch (ClientException | SSLException e) {
                    // don't retry on client exception
                    throw e;
                } catch (HttpHostConnectException | ConnectTimeoutException e) {
                    throw new AnkaMgmtException(e);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
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
                if (retry < maxRetries) {
                    continue;
                }

                throw new AnkaMgmtException(e);
            }
        }

    }

    protected CloseableHttpClient makeHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException, UnrecoverableKeyException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(timeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);
        requestBuilder.setSocketTimeout(timeout);

        HttpClientBuilder builder = HttpClientBuilder.create();
        KeyStore keystore = null;
        if (rootCA != null) {
            PEMParser reader;
            BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
            reader = new PEMParser(new StringReader(rootCA));
            X509CertificateHolder holder = (X509CertificateHolder)reader.readObject();
            Certificate certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(holder);
            keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("rootCA", certificate);
        }

        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(keystore, getTrustStartegy()).build();
        builder.setSSLContext(sslContext);
        setTLSVerificationIfDefined(sslContext, builder);
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(requestBuilder.build()).build();
        return httpClient;

    }

    protected void setTLSVerificationIfDefined(SSLContext sslContext, HttpClientBuilder builder) {
        if (skipTLSVerification) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier()));
        }
    }

    protected TrustStrategy getTrustStartegy() {
        if (skipTLSVerification) {
            return utils.strategyLambda();
        }
        return null;
    }

    protected HttpRequestBase setBody(HttpEntityEnclosingRequestBase request, JSONObject requestBody) throws UnsupportedEncodingException {
        request.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(requestBody.toString());
        request.setEntity(body);
        return request;
    }

    class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }


}
