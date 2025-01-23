package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.List;

public class AnkaMgmtUakCommunicator extends AnkaMgmtCommunicator {

    private final UakAuthenticator authenticator;

    public AnkaMgmtUakCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String rootCA, String id, String uakKeyPEM) {
        super(mgmtURLS, skipTLSVerification, rootCA);
        authenticator = new UakAuthenticator(mgmtURLS, id, uakKeyPEM);
    }

    @Override
    protected void addHeaders(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        NameValuePair authHeader = this.authenticator.getAuthorization();
        request.setHeader(authHeader.getName(), authHeader.getValue());
    }
}
