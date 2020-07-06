package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import java.util.List;

public class AnkaMgmtOpenIdCommunicator extends AnkaMgmtCommunicator {

    private final OpenIdConnectAuthenticator authenticator;

    public AnkaMgmtOpenIdCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtUrl, skipTLSVerification, rootCA);
        authenticator = new OpenIdConnectAuthenticator(mgmtUrl, client, key);
    }

    public AnkaMgmtOpenIdCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtURLS, skipTLSVerification, rootCA);
        authenticator = new OpenIdConnectAuthenticator(mgmtURLS.get(0), client, key);
    }

    @Override
    protected void addHeaders(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        NameValuePair authHeader = this.authenticator.getAuthorization();
        request.setHeader(authHeader.getName(), authHeader.getValue());
    }
}
