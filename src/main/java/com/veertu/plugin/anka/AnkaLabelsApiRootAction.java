package com.veertu.plugin.anka;

import hudson.Extension;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

/**
 * Unauthenticated root URL segment for token-based node label updates.
 *
 * <p>POST {@code /anka-build-cloud/labels/<cloudName>} with JSON body (see README).
 *
 * <p><b>Security model:</b> Implements {@link hudson.model.UnprotectedRootAction} without {@link hudson.security.ACL}
 * checks; callers must present the matching per-cloud secret ({@link AnkaMgmtCloud#verifyLabelsApiToken}). CSRF for
 * this path is handled via a narrow {@link AnkaLabelsApiCrumbExclusion}; {@code POST}-only via {@link POST} below.
 * See <a href="https://www.jenkins.io/doc/developer/security/misc/">Automation / CSRF trade-offs</a>.
 */
@Extension
public class AnkaLabelsApiRootAction implements hudson.model.UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(AnkaLabelsApiRootAction.class.getName());

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
     * Stapler maps this to {@code /anka-build-cloud/labels/...}. The cloud name is the first path segment after {@code
     * labels/} (see {@link StaplerRequest2#getRestOfPath()}).
     *
     * <p>No {@code Permission} check here by design; authorization is the configured labels API token on the target
     * cloud.
     */
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
        if (cloud == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown cloud: " + cloudName);
            return;
        }
        if (!cloud.isLabelsApiEnabled()) {
            rsp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Labels API is not configured for this cloud");
            return;
        }
        String token = extractToken(req);
        if (!cloud.verifyLabelsApiToken(token)) {
            rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token");
            return;
        }
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
}
