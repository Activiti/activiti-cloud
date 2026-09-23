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
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
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
 * {@link ServletInputStream} is wrapped in a byte-counting stream that tracks actual bytes
 * read. If the cumulative bytes exceed the configured limit during deserialization, a
 * {@link RequestBodyTooLargeException} is thrown and converted to an HTTP 413 response.
 * <p>
 * Because actual bytes are counted rather than relying on the {@code Content-Length} header,
 * this filter is effective even when the header is missing, inaccurate, or spoofed.
 */
public class VariableRequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(VariableRequestSizeLimitFilter.class);

    private final long maxContentLengthBytes;

    public VariableRequestSizeLimitFilter(long maxContentLengthBytes) {
        this.maxContentLengthBytes = maxContentLengthBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // Wrap the request with a byte-counting input stream that tracks actual bytes
        // read and throws RequestBodyTooLargeException if the limit is exceeded
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

        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response
            .getWriter()
            .write(
                "{\"status\":413,\"error\":\"Payload Too Large\"," +
                    "\"message\":\"Request body size exceeds the maximum allowed size of " +
                    maxContentLengthBytes +
                    " bytes\"}"
            );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        // Only filter write operations (PUT, POST) — GETs and DELETEs with small/no bodies are fine
        return !"PUT".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method);
    }

    /**
     * Exception thrown when the actual bytes read from the request body exceed the allowed limit.
     */
    static class RequestBodyTooLargeException extends RuntimeException {

        private final long bytesRead;

        RequestBodyTooLargeException(long bytesRead, long maxAllowed) {
            super(
                "Request body of " + bytesRead + " bytes exceeds the maximum allowed size of " + maxAllowed + " bytes"
            );
            this.bytesRead = bytesRead;
        }

        long getBytesRead() {
            return bytesRead;
        }
    }

    /**
     * An {@link HttpServletRequestWrapper} that replaces the input stream with a
     * byte-counting wrapper. When the cumulative bytes read exceed the configured
     * limit, a {@link RequestBodyTooLargeException} is thrown.
     */
    private static class SizeLimitedRequestWrapper extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream wrappedStream;

        SizeLimitedRequestWrapper(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (wrappedStream == null) {
                wrappedStream = new ByteCountingInputStream(super.getInputStream(), maxBytes);
            }
            return wrappedStream;
        }
    }

    /**
     * A {@link ServletInputStream} decorator that counts every byte read and throws
     * {@link RequestBodyTooLargeException} if the total exceeds the allowed maximum.
     */
    private static class ByteCountingInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long bytesRead = 0;

        ByteCountingInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                bytesRead++;
                checkLimit();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = delegate.read(b, off, len);
            if (count > 0) {
                bytesRead += count;
                checkLimit();
            }
            return count;
        }

        private void checkLimit() {
            if (bytesRead > maxBytes) {
                throw new RequestBodyTooLargeException(bytesRead, maxBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
