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
package org.activiti.cloud.security.authorization;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Matches an {@link HttpServletRequest} whose URI is covered by any of the
 * patterns supplied at construction time (typically a subset of the patterns
 * declared in {@link AuthorizationProperties}).
 *
 * <p>Callers decide which flavour of "public" they need:
 * <ul>
 *   <li>{@link AuthorizationConfigurer#getPublicUrlPatterns()} — every pattern
 *   declared without a role/permission. Suitable for HTTP-level opt-outs such
 *   as disabling CSRF protection.</li>
 *   <li>{@link AuthorizationConfigurer#getStrictlyPublicUrlPatterns()} — only
 *   patterns that are also not shadowed by any restricted constraint. Suitable
 *   when the goal is to bypass authentication entirely (for example, skipping
 *   bearer-token extraction), so that "public overrides" backed by
 *   method-level security (like {@code @PreAuthorize}) are not accidentally
 *   stripped of the authorities they need.</li>
 * </ul>
 */
public class PublicUrlMatcher implements RequestMatcher {

    private final List<String> publicUrls;

    private final PathMatcher matcher;

    public PublicUrlMatcher(List<String> publicUrls) {
        this.publicUrls = publicUrls;
        this.matcher = new AntPathMatcher();
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return publicUrls.stream().anyMatch(url -> matcher.match(url, request.getRequestURI()));
    }
}
