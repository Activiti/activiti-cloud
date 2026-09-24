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
package org.activiti.cloud.services.rest.conf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet filter that enforces a maximum request body size for variable-related endpoints.
 * <p>
 * This filter acts as an application-level replacement for the AWS WAF rules that were
 * previously blocking oversized requests to process and task variable endpoints. The request's
 * input stream is wrapped in a {@link ByteCountingInputStream} via {@link SizeLimitedRequestWrapper}
 * that tracks actual bytes read. If the cumulative bytes exceed the configured limit during
 * deserialization, a {@link RequestBodyTooLargeException} is thrown, which Spring translates
 * into an HTTP 400 (Bad Request) response.
 * <p>
 * URL matching uses {@link AntPathMatcher} with {@code /**} prefixed patterns so that the
 * filter works regardless of any gateway or proxy path prefix.
 * <p>
 * Because actual bytes are counted rather than relying on the {@code Content-Length} header,
 * this filter is effective even when the header is missing, inaccurate, or spoofed.
 */
public class VariableRequestSizeLimitFilter extends OncePerRequestFilter {

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> VARIABLE_ENDPOINT_PATTERNS = List.of(
        "/**/v1/process-instances/*/variables",
        "/**/v1/process-instances/*/variables/**",
        "/**/v1/tasks/*/variables",
        "/**/v1/tasks/*/variables/**"
    );

    private final long maxContentLengthBytes;

    public VariableRequestSizeLimitFilter(long maxContentLengthBytes) {
        this.maxContentLengthBytes = maxContentLengthBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        HttpServletRequest wrappedRequest = new SizeLimitedRequestWrapper(request, maxContentLengthBytes);

        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"PUT".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        return VARIABLE_ENDPOINT_PATTERNS.stream().noneMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }
}
