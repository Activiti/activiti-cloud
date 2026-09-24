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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet filter that enforces a maximum request body size for variable-related endpoints.
 * <p>
 * This filter acts as an application-level replacement for the AWS WAF rules that were
 * previously blocking oversized requests to process and task variable endpoints. The request's
 * input stream is wrapped in a {@link ByteCountingInputStream} via {@link SizeLimitedRequestWrapper}
 * that tracks actual bytes read. If the cumulative bytes exceed the configured limit during
 * deserialization, a {@link RequestBodyTooLargeException} is thrown and converted to an HTTP 413 response.
 * <p>
 * Because actual bytes are counted rather than relying on the {@code Content-Length} header,
 * this filter is effective even when the header is missing, inaccurate, or spoofed.
 */
public class VariableRequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(VariableRequestSizeLimitFilter.class);

    private static final int ERROR_STATUS = HttpStatus.PAYLOAD_TOO_LARGE.value();
    private static final String ERROR_NAME = "Payload Too Large";
    private static final String ERROR_MESSAGE_TEMPLATE =
        "Request body size exceeds the maximum allowed size of %d bytes";

    private static final String PROCESS_VARIABLES_PATH = "/v1/process-instances/";
    private static final String TASK_VARIABLES_PATH = "/v1/tasks/";
    private static final String VARIABLES_SEGMENT = "/variables";

    private final long maxContentLengthBytes;

    public VariableRequestSizeLimitFilter(long maxContentLengthBytes) {
        this.maxContentLengthBytes = maxContentLengthBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        HttpServletRequest wrappedRequest = new SizeLimitedRequestWrapper(request, maxContentLengthBytes);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } catch (RequestBodyTooLargeException e) {
            if (!response.isCommitted()) {
                rejectRequest(request, response, e.getBytesRead());
            }
        }
    }

    private void rejectRequest(HttpServletRequest request, HttpServletResponse response, long size) throws IOException {
        log.warn(
            "Rejected request to {}: body size {} exceeds maximum allowed size of {} bytes",
            request.getRequestURI(),
            size,
            maxContentLengthBytes
        );

        String errorMessage = String.format(ERROR_MESSAGE_TEMPLATE, maxContentLengthBytes);

        response.setStatus(ERROR_STATUS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response
            .getWriter()
            .write(
                String.format(
                    "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                    ERROR_STATUS,
                    ERROR_NAME,
                    errorMessage
                )
            );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"PUT".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            return true;
        }
        return !isVariableEndpoint(request.getRequestURI());
    }

    /**
     * Checks whether the URI targets a variable endpoint, regardless of any gateway prefix.
     * Matches paths containing /v1/process-instances/{id}/variables or /v1/tasks/{id}/variables,
     * including /admin/ variants.
     */
    private static boolean isVariableEndpoint(String uri) {
        int variablesIdx = uri.indexOf(VARIABLES_SEGMENT);
        if (variablesIdx < 0) {
            return false;
        }
        String beforeVariables = uri.substring(0, variablesIdx);
        return beforeVariables.contains(PROCESS_VARIABLES_PATH) || beforeVariables.contains(TASK_VARIABLES_PATH);
    }
}
