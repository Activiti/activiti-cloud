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
 * Matches an {@link HttpServletRequest} whose URI is covered by a public
 * URL pattern declared in {@link AuthorizationProperties}. A security
 * constraint is considered public when it does not declare any required
 * role or permission.
 *
 * <p>Used both to disable CSRF protection and to skip bearer-token
 * extraction on public endpoints.
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
