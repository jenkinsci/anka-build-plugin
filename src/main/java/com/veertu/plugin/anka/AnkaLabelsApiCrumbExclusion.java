package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows POST to the labels API without a CSRF crumb (token auth replaces crumb).
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
