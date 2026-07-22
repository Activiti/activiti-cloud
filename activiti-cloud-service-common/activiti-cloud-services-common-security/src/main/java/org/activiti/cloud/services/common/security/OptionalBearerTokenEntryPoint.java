/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * {@link AuthenticationEntryPoint} used with {@code oauth2ResourceServer} to make bearer-token
 * authentication <em>optional</em> on paths declared as public.
 *
 * <p>When the {@code Authorization} header contains a bearer token that fails to decode/validate
 * (malformed, expired, wrong signature, …) Spring Security normally responds with {@code 401}
 * through {@link BearerTokenAuthenticationEntryPoint}. On public endpoints this would prevent
 * legitimate anonymous access whenever the client sends a stale token in the header.
 *
 * <p>This entry point delegates to the default bearer entry point for protected paths, but on
 * public paths it clears the {@link SecurityContextHolder} and lets the filter chain proceed:
 * the downstream {@code AnonymousAuthenticationFilter} will then populate an anonymous
 * authentication, so {@code permitAll()} and method-level security annotations that allow
 * anonymous access keep working while genuinely protected endpoints still return {@code 401}.
 *
 * <p>Note: for valid tokens the resource server converter still populates a
 * {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken},
 * so annotation-based checks (e.g. {@code @PreAuthorize}) that rely on roles/permissions extracted
 * from the JWT continue to work on public endpoints too.
 */
public class OptionalBearerTokenEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint delegate;
    private final RequestMatcher publicPaths;

    public OptionalBearerTokenEntryPoint(RequestMatcher publicPaths) {
        this(publicPaths, new BearerTokenAuthenticationEntryPoint());
    }

    OptionalBearerTokenEntryPoint(RequestMatcher publicPaths, AuthenticationEntryPoint delegate) {
        this.publicPaths = publicPaths;
        this.delegate = delegate;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        if (publicPaths.matches(request)) {
            SecurityContextHolder.clearContext();
            return;
        }
        delegate.commence(request, response, authException);
    }
}
