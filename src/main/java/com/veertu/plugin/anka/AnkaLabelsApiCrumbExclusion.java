package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Allows POST to the labels API without a CSRF crumb (token auth replaces crumb). Applies to {@code
 * /anka-build-cloud/labels/...} so unconfigured paths reach Stapler and are not routed (404) instead of failing CSRF
 * (403).
 */
@Extension
public class AnkaLabelsApiCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/anka-build-cloud/labels/")) {
            chain.doFilter(request, response);
            return true;
        }
        return false;
    }
}
