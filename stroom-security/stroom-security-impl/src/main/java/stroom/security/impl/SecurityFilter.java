/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.security.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.authentication.api.OIDC;
import stroom.config.common.UriFactory;
import stroom.security.api.ProcessingUserIdentityProvider;
import stroom.security.api.SecurityContext;
import stroom.security.api.UserIdentity;
import stroom.security.impl.session.UserIdentitySessionUtil;
import stroom.security.shared.User;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ResourcePaths;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * Filter to avoid posts to the wrong place (e.g. the root of the app)
 * </p>
 */
@Singleton
class SecurityFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(SecurityFilter.class);

    private static final String NO_AUTH_PATH = ResourcePaths.buildUnauthenticatedServletPath("/");

    // E.g. /api/authentication/v1/noauth/exchange
    private static final String PUBLIC_API_PATH_REGEX = ResourcePaths.API_ROOT_PATH +
            ".*" + ResourcePaths.NO_AUTH + "/.*";
    private static final Set<String> STATIC_RESOURCE_EXTENSIONS = Set.of(
            ".js", ".css", ".htm", ".html", ".json", ".png", ".jpg", ".gif", ".ico", ".svg", ".woff", ".woff2");

    private final AuthenticationConfig authenticationConfig;
    private final UriFactory uriFactory;
    private final SecurityContext securityContext;
    private final Pattern publicApiPathPattern;
    private final OpenIdManager openIdManager;
    private final ProcessingUserIdentityProvider processingUserIdentityProvider;

    @Inject
    SecurityFilter(
            final AuthenticationConfig authenticationConfig,
            final UriFactory uriFactory,
            final SecurityContext securityContext,
            final OpenIdManager openIdManager,
            final ProcessingUserIdentityProvider processingUserIdentityProvider) {
        this.authenticationConfig = authenticationConfig;
        this.uriFactory = uriFactory;
        this.securityContext = securityContext;
        this.openIdManager = openIdManager;
        this.processingUserIdentityProvider = processingUserIdentityProvider;

        publicApiPathPattern = Pattern.compile(PUBLIC_API_PATH_REGEX);
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        LOGGER.debug("Initialising {}", this.getClass().getSimpleName());
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse)) {
            final String message = "Unexpected response type: " + response.getClass().getName();
            LOGGER.error(message);
            return;
        }
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (!(request instanceof HttpServletRequest)) {
            final String message = "Unexpected request type: " + request.getClass().getName();
            LOGGER.error(message);
            httpServletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
            return;
        }
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        filter(httpServletRequest, httpServletResponse, chain);
    }

    private void filter(final HttpServletRequest request,
                        final HttpServletResponse response,
                        final FilterChain chain)
            throws IOException, ServletException {
        LAMBDA_LOGGER.debug(() ->
                LogUtil.message("Filtering request uri: {},  servletPath: {}",
                        request.getRequestURI(), request.getServletPath()));

        final String servletPath = request.getServletPath().toLowerCase();
        final String fullPath = request.getRequestURI().toLowerCase();

        if (request.getMethod().toUpperCase().equals(HttpMethod.OPTIONS) ||
                (!isApiRequest(servletPath) && isStaticResource(request))) {
            // We need to allow CORS preflight requests
            LOGGER.debug("Passing on to next filter");
            chain.doFilter(request, response);
        } else {

            LAMBDA_LOGGER.debug(() -> LogUtil.message("Session ID {}, request URI {}",
                    Optional.ofNullable(request.getSession(false))
                            .map(HttpSession::getId)
                            .orElse("-"),
                    request.getRequestURI() + Optional.ofNullable(request.getQueryString())
                            .map(str -> "/" + str)
                            .orElse("")));

            // See if we have an authenticated session.
            final UserIdentity userIdentity = UserIdentitySessionUtil.get(request.getSession(false));
            if (userIdentity != null) {
                continueAsUser(request, response, chain, userIdentity);

            } else {
                // We need to distinguish between requests from an API client and from the UI.
                // - If a request is from the UI and fails authentication then we need to redirect to the login page.
                // - If a request is from an API client and fails authentication then we need to return HTTP 403 UNAUTHORIZED.
                if (isPublicApiRequest(fullPath)) {
                    authenticateAsProcUser(request, response, chain, false);
                } else if (isApiRequest(servletPath)) {
                    LOGGER.debug("API request");
                    if (!authenticationConfig.isAuthenticationRequired()) {
                        String propPath = authenticationConfig.getFullPath(AuthenticationConfig.PROP_NAME_AUTHENTICATION_REQUIRED);
                        LOGGER.warn("{} is false, authenticating as admin for {}", propPath, fullPath);
                        authenticateAsAdmin(request, response, chain, false);
                    } else {
                        // Authenticate requests to the API.
                        final UserIdentity token = loginAPI(request, response);
                        continueAsUser(request, response, chain, token);
                    }
                } else if (shouldBypassAuthentication(servletPath, fullPath)) {
                    // Some servlet requests need to bypass authentication -- this happens if the servlet class
                    // is annotated with @Unauthenticated. E.g. the status servlet doesn't require authentication.
                    authenticateAsProcUser(request, response, chain, false);
                } else {
                    // We assume all other requests are from the UI, and instigate an OpenID authentication flow
                    // like the good relying party we are.

                    if (!authenticationConfig.isAuthenticationRequired()) {
                        String propPath = authenticationConfig.getFullPath(AuthenticationConfig.PROP_NAME_AUTHENTICATION_REQUIRED);
                        LOGGER.warn("{} is false, authenticating as admin for {}", propPath, fullPath);
                        authenticateAsAdmin(request, response, chain, true);
                    } else {
                        // If the session doesn't have a user ref then attempt login.
                        try {
                            final String postAuthRedirectUri = getPostAuthRedirectUri(request);

                            LOGGER.debug("Using postAuthRedirectUri: {}", postAuthRedirectUri);

                            String redirectUri = null;

                            // If we have completed the front channel flow then we will have a state id.
                            final String code = UrlUtils.getLastParam(request, OIDC.CODE);
                            final String stateId = UrlUtils.getLastParam(request, OIDC.STATE);
                            if (code != null && stateId != null) {
                                redirectUri = openIdManager.backChannelOIDC(request, code, stateId, postAuthRedirectUri);
                            }

                            if (redirectUri == null) {
                                redirectUri = openIdManager.frontChannelOIDC(request, postAuthRedirectUri);
                            }

                            response.sendRedirect(redirectUri);

                        } catch (final RuntimeException e) {
                            LOGGER.error(e.getMessage(), e);
                            throw e;
                        }
                    }
                }
            }
        }
    }

    private String getPostAuthRedirectUri(final HttpServletRequest request) {
        // We have a a new request so we're going to redirect with an AuthenticationRequest.
        // Get the redirect URL for the auth service from the current request.
        String originalPath = request.getRequestURI() + Optional.ofNullable(request.getQueryString())
                .map(queryStr -> "?" + queryStr)
                .orElse("");

        // Dropwiz is likely sat behind Nginx with requests reverse proxied to it
        // so we need to append just the path/query part to the public URI defined in config
        // rather than using the full url of the request
        final String postAuthRedirectUri = uriFactory.publicUri(originalPath).toString();

        // When the auth service has performed authentication it will redirect
        // back to the current URL with some additional parameters (e.g.
        // `state` and `accessCode`). It is important that these parameters are
        // not provided by our redirect URL else the redirect URL that the
        // authentication service redirects back to may end up with multiple
        // copies of these parameters which will confuse Stroom as it will not
        // know which one of the param values to use (i.e. which were on the
        // original redirect request and which have been added by the
        // authentication service). For this reason we will cleanse the URL of
        // any reserved parameters here. The authentication service should do
        // the same to the redirect URL before adding its additional
        // parameters.
        LOGGER.debug("postAuthRedirectUri before param removal: {}", postAuthRedirectUri);
        return OIDC.removeOIDCParams(postAuthRedirectUri);
    }

    private boolean isStaticResource(final HttpServletRequest request) {
        final String url = request.getRequestURL().toString();

        if (url.contains("/s/") ||
                url.contains("/static/") ||
                url.endsWith("manifest.json")) { // New UI - For some reason this is requested without a session cookie
            return true;
        }

        int index = url.lastIndexOf(".");
        if (index > 0) {
            final String extension = url.substring(index);
            return STATIC_RESOURCE_EXTENSIONS.contains(extension);
        }

        return false;
    }

    private boolean isPublicApiRequest(final String servletPath) {
        return publicApiPathPattern != null && publicApiPathPattern.matcher(servletPath).matches();
    }

    private boolean isApiRequest(String servletPath) {
        return servletPath.startsWith(ResourcePaths.API_ROOT_PATH);
    }

    private boolean shouldBypassAuthentication(final String servletPath, final String fullPath) {
        return servletPath.startsWith(NO_AUTH_PATH) || fullPath.startsWith(NO_AUTH_PATH);
//        return servletPath.startsWith(NO_AUTH_PATH);
    }

    private void authenticateAsAdmin(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final FilterChain chain,
                                     final boolean useSession) throws IOException, ServletException {

        bypassAuthentication(request, response, chain, useSession, securityContext.createIdentity(User.ADMIN_USER_NAME));
    }

    private void authenticateAsProcUser(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final FilterChain chain,
                                        final boolean useSession) throws IOException, ServletException {
        bypassAuthentication(request, response, chain, useSession, processingUserIdentityProvider.get());
    }

    private void bypassAuthentication(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final FilterChain chain,
                                      final boolean useSession,
                                      final UserIdentity userIdentity) throws IOException, ServletException {
        LAMBDA_LOGGER.debug(() ->
                LogUtil.message("Authenticating as user {} for request {}", userIdentity, request.getRequestURI()));
        if (useSession) {
            // Set the user ref in the session.
            UserIdentitySessionUtil.set(request.getSession(true), userIdentity);
        }
        continueAsUser(request, response, chain, userIdentity);
    }

    private void continueAsUser(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final FilterChain chain,
                                final UserIdentity userIdentity)
            throws IOException, ServletException {
        if (userIdentity != null) {
            // If the session already has a reference to a user then continue the chain as that user.
            try {
                CurrentUserState.push(userIdentity);

                chain.doFilter(request, response);
            } finally {
                CurrentUserState.pop();
            }
        }
    }

//    private boolean loginUI(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
//        final String stateId = UrlUtils.getLastParam(request, OIDC.STATE);
//        if (stateId != null) {
//            String redirectUri = openIdManager.backChannelOIDC(request, stateId, openIdConfig.getRedirectUri());
//            response.sendRedirect(redirectUri);
//            return true;
//        }
//        return false;
//    }

//    private String redirectToAuthService(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
//        // We have a a new request so we're going to redirect with an AuthenticationRequest.
//        // Get the redirect URL for the auth service from the current request.
//        String oidcRedirectUri = UrlUtils.getFullUrl(request);
//
//        // When the auth service has performed authentication it will redirect
//        // back to the current URL with some additional parameters (e.g.
//        // `state` and `accessCode`). It is important that these parameters are
//        // not provided by our redirect URL else the redirect URL that the
//        // authentication service redirects back to may end up with multiple
//        // copies of these parameters which will confuse Stroom as it will not
//        // know which one of the param values to use (i.e. which were on the
//        // original redirect request and which have been added by the
//        // authentication service). For this reason we will cleanse the URL of
//        // any reserved parameters here. The authentication service should do
//        // the same to the redirect URL before adding its additional
//        // parameters.
//        oidcRedirectUri = OIDC.removeOIDCParams(oidcRedirectUri);
//
////        if (uiConfig.getUrl() != null && uiConfig.getUrl().getUi() != null && uiConfig.getUrl().getUi().trim().length() > 0) {
////            LOGGER.debug("Using the advertised URL as the OpenID redirect URL");
////            final URI uri = UriBuilder.fromUri(postLoginUrl).build();
////            final UriBuilder builder = UriBuilder.fromUri(uiConfig.getUrl().getUi());
////            if (uri.getPath() != null) {
////                builder.path(uri.getPath());
////            }
////            if (uri.getFragment() != null) {
////                builder.fragment(uri.getFragment());
////            }
////            if (uri.getQuery() != null) {
////                builder.replaceQuery(uri.getQuery());
////            }
////            postLoginUrl = builder.build().toString();
////        }
//
//        return openIdManager.frontChannelOIDC(request, oidcRedirectUri);//openIdConfig.getRedirectUri());
//    }

    private UserIdentity loginAPI(final HttpServletRequest request, final HttpServletResponse response) {
        // Authenticate requests from an API client
        final UserIdentity userIdentity = openIdManager.createAPIToken(request);

        if (userIdentity == null) {
            LOGGER.debug("API request is unauthorised.");
            response.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
        }

        return userIdentity;
    }

    @Override
    public void destroy() {
    }
}
