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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that performs <em>optional</em> JWT authentication.
 *
 * <p>Unlike {@code BearerTokenAuthenticationFilter}, this filter never fails the request:
 * <ul>
 *     <li>if the {@code Authorization: Bearer …} header is missing, it just continues the chain
 *         so the downstream {@code AnonymousAuthenticationFilter} can populate an anonymous
 *         authentication;</li>
 *     <li>if the header is present and the token can be decoded/validated, the resulting
 *         {@link org.springframework.security.core.Authentication} is placed in the
 *         {@link SecurityContextHolder}, so method-level security annotations
 *         (e.g. {@code @PreAuthorize}) can evaluate roles/permissions extracted from the JWT;</li>
 *     <li>if the token cannot be decoded (malformed, expired, wrong signature, …), the error is
 *         swallowed and the request continues as anonymous — no {@code 401} is emitted.</li>
 * </ul>
 *
 * <p>Intended to be used on a dedicated {@code SecurityFilterChain} that matches only paths
 * declared as public (no {@code authRoles} nor {@code authPermissions}), so that on protected
 * paths the standard {@code oauth2ResourceServer} pipeline keeps rejecting invalid tokens with
 * {@code 401}.
 */
public class OptionalJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionalJwtAuthenticationFilter.class);

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;
    private final BearerTokenResolver bearerTokenResolver;

    public OptionalJwtAuthenticationFilter(
        JwtDecoder jwtDecoder,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) {
        this(jwtDecoder, jwtAuthenticationConverter, new DefaultBearerTokenResolver());
    }

    OptionalJwtAuthenticationFilter(
        JwtDecoder jwtDecoder,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
        BearerTokenResolver bearerTokenResolver
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Optional-authentication by design: any failure (invalid token, decoder error,
                // buggy JwtAdapter, NPE inside the converter, …) must not break a public endpoint.
                // The request proceeds as anonymous and the downstream AnonymousAuthenticationFilter
                // populates the SecurityContext.
                LOGGER.debug("Ignoring bearer token on public endpoint: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        try {
            return bearerTokenResolver.resolve(request);
        } catch (AuthenticationException ex) {
            LOGGER.debug("Ignoring malformed Authorization header on public endpoint: {}", ex.getMessage());
            return null;
        }
    }
}
