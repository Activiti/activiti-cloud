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

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    // --- Byte-counting: actual body size checks ---

    @Test
    void should_rejectRequest_when_actualBodyExceedsLimit_withNoContentLengthHeader() throws Exception {
        byte[] oversizedBody = new byte[(int) MAX_SIZE_BYTES + 100];
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(oversizedBody);
        // MockHttpServletRequest auto-sets Content-Length from setContent,
        // so we remove it to simulate a missing header
        request.removeHeader("Content-Length");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate what the downstream filter chain does: read the input stream
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest wrappedReq = invocation.getArgument(0);
            wrappedReq.getInputStream().readAllBytes();
            return null;
        })
            .when(filterChain)
            .doFilter(Mockito.any(), Mockito.any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getContentAsString()).contains("Payload Too Large");
    }

    @Test
    void should_allowRequest_when_actualBodyWithinLimit() throws Exception {
        byte[] smallBody = "{\"var1\":\"value1\"}".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/v1/process-instances/123/variables");
        request.setContentType("application/json");
        request.setContent(smallBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest wrappedReq = invocation.getArgument(0);
            wrappedReq.getInputStream().readAllBytes();
            return null;
        })
            .when(filterChain)
            .doFilter(Mockito.any(), Mockito.any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
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

    // --- Endpoint coverage ---

    @Test
    void should_rejectRequest_forTaskVariableEndpoint() throws Exception {
        byte[] oversizedBody = new byte[(int) MAX_SIZE_BYTES + 100];
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/tasks/456/variables");
        request.setContentType("application/json");
        request.setContent(oversizedBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest wrappedReq = invocation.getArgument(0);
            wrappedReq.getInputStream().readAllBytes();
            return null;
        })
            .when(filterChain)
            .doFilter(Mockito.any(), Mockito.any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }

    @Test
    void should_rejectRequest_forAdminVariableEndpoint() throws Exception {
        byte[] oversizedBody = new byte[(int) MAX_SIZE_BYTES + 100];
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin/v1/process-instances/789/variables");
        request.setContentType("application/json");
        request.setContent(oversizedBody);

        MockHttpServletResponse response = new MockHttpServletResponse();

        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest wrappedReq = invocation.getArgument(0);
            wrappedReq.getInputStream().readAllBytes();
            return null;
        })
            .when(filterChain)
            .doFilter(Mockito.any(), Mockito.any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }
}
