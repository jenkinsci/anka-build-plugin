package com.veertu.plugin.anka;

import hudson.Extension;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.verb.POST;

/**
 * Unauthenticated root URL segment for token-based node label updates.
 *
 * <p>POST {@code /anka-build-cloud/labels/<cloudName>} with JSON body (see README).
 *
 * <p><b>Security model:</b> Implements {@link hudson.model.UnprotectedRootAction} without {@link hudson.security.ACL}
 * checks; callers must present the matching per-cloud secret ({@link AnkaMgmtCloud#verifyLabelsApiToken}). {@link
 * StaplerProxy#getTarget()} returns {@code null} when no cloud has a Labels API token configured, or when the target
 * cloud is missing or has no token, so Stapler does not route the request to {@link #doLabels}. CSRF for enabled paths
 * is handled via a narrow {@link AnkaLabelsApiCrumbExclusion}; {@code POST}-only via {@link POST} below. See
 * <a href="https://www.jenkins.io/doc/developer/security/misc/">Automation / CSRF trade-offs</a>.
 */
@Extension
public class AnkaLabelsApiRootAction implements hudson.model.UnprotectedRootAction, StaplerProxy {

    private static final Logger LOGGER = Logger.getLogger(AnkaLabelsApiRootAction.class.getName());
    private static final String LABELS_PATH_PREFIX = "/anka-build-cloud/labels/";

    /** Maximum Labels API request body size (1 MiB). */
    static final int MAX_REQUEST_BODY_BYTES = 1_048_576;

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "anka-build-cloud";
    }

    /**
     * Hide this action from Stapler unless the request targets {@code /labels/<cloudName>} for a cloud with a configured
     * Labels API token.
     */
    @Override
    public Object getTarget() {
        if (!AnkaMgmtCloud.isAnyLabelsApiEnabled()) {
            return null;
        }
        StaplerRequest2 req = Stapler.getCurrentRequest2();
        if (req == null) {
            return null;
        }
        String cloudName = cloudNameFromLabelsRequest(req.getPathInfo());
        if (cloudName == null) {
            return null;
        }
        AnkaMgmtCloud cloud = AnkaMgmtCloud.get(cloudName);
        if (cloud == null || !cloud.isLabelsApiEnabled()) {
            return null;
        }
        return this;
    }

    /**
     * Stapler maps this to {@code /anka-build-cloud/labels/...}. The cloud name is the first path segment after {@code
     * labels/} (see {@link StaplerRequest2#getRestOfPath()}).
     *
     * <p>Only reachable when {@link #getTarget()} returns {@code this}.
     *
     * @see hudson.model.UnprotectedRootAction
     */
    @SuppressWarnings("lgtm[jenkins/no-permission-check]") // Otherwise protected: per-cloud secret before any side effect; see class Javadoc
    @POST
    public void doLabels(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        String rest = req.getRestOfPath();
        if (rest == null) {
            rest = "";
        }
        if (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        if (rest.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "Cloud name required in URL path after /anka-build-cloud/labels/");
            return;
        }
        int slash = rest.indexOf('/');
        String cloudName = slash < 0 ? rest : rest.substring(0, slash);

        AnkaMgmtCloud cloud = AnkaMgmtCloud.get(cloudName);
        String token = extractToken(req);
        if (!cloud.verifyLabelsApiToken(token)) {
            rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token");
            return;
        }
        String body;
        try {
            body = readBoundedRequestBody(req.getContentLengthLong(), req.getInputStream(), MAX_REQUEST_BODY_BYTES);
        } catch (RequestBodyTooLargeException e) {
            rsp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, e.getMessage());
            return;
        }
        try {
            AnkaLabelsTemplateService.Result result = AnkaLabelsTemplateService.apply(cloud, body);
            rsp.setStatus(HttpServletResponse.SC_OK);
            rsp.setContentType("application/json;charset=UTF-8");
            rsp.getWriter().write(result.toJson());
        } catch (IllegalArgumentException e) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Labels API failed for cloud " + cloudName, e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    static String cloudNameFromLabelsRequest(String pathInfo) {
        if (pathInfo == null || !pathInfo.startsWith(LABELS_PATH_PREFIX)) {
            return null;
        }
        String rest = pathInfo.substring(LABELS_PATH_PREFIX.length());
        int slash = rest.indexOf('/');
        String encodedCloudName = slash < 0 ? rest : rest.substring(0, slash);
        if (encodedCloudName.isEmpty()) {
            return null;
        }
        return URLDecoder.decode(encodedCloudName, StandardCharsets.UTF_8);
    }

    private static String extractToken(StaplerRequest2 req) {
        String h = req.getHeader("X-Anka-Labels-Token");
        if (h != null && !h.isEmpty()) {
            return h.trim();
        }
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    static String readBoundedRequestBody(long contentLength, InputStream in, int maxBytes) throws IOException {
        if (contentLength > maxBytes) {
            throw new RequestBodyTooLargeException(maxBytes);
        }
        try (InputStream input = in) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new RequestBodyTooLargeException(maxBytes);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static final class RequestBodyTooLargeException extends IOException {
        private static final long serialVersionUID = 1L;

        RequestBodyTooLargeException(int maxBytes) {
            super("Request body exceeds " + maxBytes + " bytes");
        }
    }
}
