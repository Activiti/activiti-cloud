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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class VariableRequestSizeLimitFilterTest {

    private static final long MAX_SIZE_BYTES = 256; // small limit for testing

    private VariableRequestSizeLimitFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new VariableRequestSizeLimitFilter(MAX_SIZE_BYTES);
    }

    // --- Content-Length up-front rejection ---

    @Test
    void should_throwException_when_contentLengthExceedsLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        // MockHttpServletRequest derives getContentLengthLong() from setContent(),
        // so we must provide an oversized array for stage 1 to see a Content-Length > limit
        request.setContent(new byte[(int) MAX_SIZE_BYTES + 1]);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isInstanceOf(RequestBodyTooLargeException.class)
            .hasMessageContaining("exceeds the maximum allowed size of 256 bytes");
    }

    @Test
    void should_allowRequest_when_contentLengthWithinLimit() throws Exception {
        byte[] body = "{\"var1\":\"value1\"}".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(body);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        Mockito.verify(filterChain).doFilter(Mockito.any(), Mockito.any());
    }

    // --- Body draining: actual body size checks ---

    @Test
    void should_throwException_when_actualBodyExceedsLimit() {
        byte[] oversizedBody = new byte[(int) MAX_SIZE_BYTES + 100];
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(oversizedBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isInstanceOf(RequestBodyTooLargeException.class)
            .hasMessageContaining("exceeds the maximum allowed size of 256 bytes");
    }

    @Test
    void should_allowRequest_when_actualBodyWithinLimit() throws Exception {
        byte[] smallBody = "{\"var1\":\"value1\"}".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(smallBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void should_allowRequest_when_bodyExactlyAtLimit() throws Exception {
        byte[] exactBody = new byte[(int) MAX_SIZE_BYTES];
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(exactBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // --- Buffered body is passed downstream ---

    @Test
    void should_passBufferedBodyToDownstream() throws Exception {
        byte[] body = "{\"var1\":\"value1\"}".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(body);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilter(request, response, filterChain);

        Mockito.verify(filterChain).doFilter(captor.capture(), Mockito.any());
        HttpServletRequest wrappedRequest = captor.getValue();

        assertThat(wrappedRequest).isInstanceOf(SizeLimitedRequestWrapper.class);
        assertThat(wrappedRequest.getContentLengthLong()).isEqualTo(body.length);

        byte[] downstreamBody = wrappedRequest.getInputStream().readAllBytes();
        assertThat(downstreamBody).isEqualTo(body);
    }

    // --- Method filtering ---

    @Test
    void should_skipFilter_forGetRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/process-instances/123/variables");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void should_skipFilter_forDeleteRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "DELETE",
            "/admin/v1/process-instances/123/variables"
        );
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void should_applyFilter_forPutRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void should_applyFilter_forPostRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/tasks/456/variables");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // --- URL pattern matching (with gateway prefixes) ---

    @Test
    void should_applyFilter_forPrefixedVariableEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "PUT",
            "/any-prefix/rb/v1/process-instances/123/variables"
        );
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void should_applyFilter_forPrefixedAdminEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "PUT",
            "/some-prefix/rb/admin/v1/process-instances/123/variables"
        );
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void should_skipFilter_forNonVariableEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/status");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    // --- Endpoint coverage ---

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "POST, /v1/tasks/456/variables",
        "POST, /v1/process-instances",
        "PUT, /admin/v1/process-instances/789/variables",
    })
    void should_throwException_forOversizedBody(String method, String path) {
        byte[] oversizedBody = new byte[(int) MAX_SIZE_BYTES + 100];
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContentType("application/json");
        request.setContent(oversizedBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain)).isInstanceOf(
            RequestBodyTooLargeException.class
        );
    }

    @Test
    void should_applyFilter_forStartProcessEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/process-instances");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void should_applyFilter_forAdminStartProcessEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/v1/process-instances");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void should_applyFilter_forPrefixedStartProcessEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/gateway-prefix/rb/v1/process-instances");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // --- SizeLimitedRequestWrapper: getInputStream() returns same instance ---

    @Test
    void should_returnSameInputStream_onMultipleCalls() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent("{}".getBytes());

        SizeLimitedRequestWrapper wrapper = new SizeLimitedRequestWrapper(request, "{}".getBytes());

        ServletInputStream first = wrapper.getInputStream();
        ServletInputStream second = wrapper.getInputStream();

        assertThat(first).isSameAs(second);
    }

    // --- SizeLimitedRequestWrapper: delegate methods ---

    @Test
    void should_delegateIsFinishedAndIsReady() throws Exception {
        byte[] body = "{}".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");

        SizeLimitedRequestWrapper wrapper = new SizeLimitedRequestWrapper(request, body);
        ServletInputStream is = wrapper.getInputStream();

        assertThat(is.isFinished()).isFalse();
        assertThat(is.isReady()).isTrue();

        is.readAllBytes();

        assertThat(is.isFinished()).isTrue();
    }

    // --- RequestBodyTooLargeException ---

    @Test
    void should_containMaxSizeInMessage() {
        RequestBodyTooLargeException ex = new RequestBodyTooLargeException(5242880);
        assertThat(ex.getMessage()).isEqualTo("Request body exceeds the maximum allowed size of 5242880 bytes");
    }
}
