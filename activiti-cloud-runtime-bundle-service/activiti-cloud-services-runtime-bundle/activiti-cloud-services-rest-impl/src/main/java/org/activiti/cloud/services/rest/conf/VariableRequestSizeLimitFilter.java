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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet filter that enforces a maximum request body size for variable-related endpoints.
 * <p>
 * This filter acts as an application-level replacement for the AWS WAF rules that were
 * previously blocking oversized requests to process and task variable endpoints.
 * <p>
 * Enforcement happens in two stages:
 * <ol>
 *   <li>If the {@code Content-Length} header is present and exceeds the limit, the request
 *       is rejected immediately without reading the body.</li>
 *   <li>Otherwise the body is drained into a bounded buffer (up to {@code maxBytes + 1}).
 *       If the buffer exceeds the limit the request is rejected; otherwise the buffered body
 *       is passed downstream via {@link SizeLimitedRequestWrapper}.</li>
 * </ol>
 * This ensures the <em>entire</em> body is validated regardless of how much the downstream
 * deserializer actually consumes.
 * <p>
 * URL matching uses {@link AntPathMatcher} with {@code /**} prefixed patterns so that the
 * filter works regardless of any gateway or proxy path prefix.
 */
public class VariableRequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableRequestSizeLimitFilter.class);
    public static final String POST = "POST";
    public static final String PUT = "PUT";

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> VARIABLE_ENDPOINT_PATTERNS = List.of(
        "/**/v1/process-instances",
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
        rejectIfContentLengthExceedsLimit(request);
        byte[] body = readAndValidateBody(request);
        filterChain.doFilter(new SizeLimitedRequestWrapper(request, body), response);
    }

    /**
     * Stage 1: reject immediately if the Content-Length header is present and exceeds the limit.
     * This avoids buffering a known-oversized body into memory.
     */
    private void rejectIfContentLengthExceedsLimit(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxContentLengthBytes) {
            LOGGER.warn(
                "{} {} rejected: Content-Length {} exceeds the maximum allowed size of {} bytes",
                request.getMethod(),
                request.getRequestURI(),
                contentLength,
                maxContentLengthBytes
            );
            throw new RequestBodyTooLargeException(maxContentLengthBytes);
        }
    }

    /**
     * Stage 2: drain the body into a bounded buffer (up to {@code maxBytes + 1}) to enforce
     * the limit regardless of how much the downstream deserializer consumes.
     */
    private byte[] readAndValidateBody(HttpServletRequest request) throws IOException {
        int readLimit = (int) Math.min(maxContentLengthBytes + 1, Integer.MAX_VALUE);
        byte[] body = request.getInputStream().readNBytes(readLimit);

        if (body.length > maxContentLengthBytes) {
            LOGGER.warn(
                "{} {} rejected: request body of {} bytes exceeds the maximum allowed size of {} bytes",
                request.getMethod(),
                request.getRequestURI(),
                body.length,
                maxContentLengthBytes
            );
            throw new RequestBodyTooLargeException(maxContentLengthBytes);
        }
        return body;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!PUT.equalsIgnoreCase(method) && !POST.equalsIgnoreCase(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        return VARIABLE_ENDPOINT_PATTERNS.stream().noneMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }
}
