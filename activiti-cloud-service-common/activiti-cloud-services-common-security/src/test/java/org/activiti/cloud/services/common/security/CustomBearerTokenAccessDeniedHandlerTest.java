/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@ExtendWith(MockitoExtension.class)
class CustomBearerTokenAccessDeniedHandlerTest {

    @Mock
    private AccessDeniedHandler delegated;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AccessDeniedException exception;

    @Test
    public void should_sendErrorWithMessage_when_ErrorIs403() throws ServletException, IOException {
        CustomBearerTokenAccessDeniedHandler accessDeniedHandler = new CustomBearerTokenAccessDeniedHandler(delegated);

        when(response.getStatus()).thenReturn(403);

        accessDeniedHandler.handle(request, response, exception);

        verify(response).sendError(eq(403), eq("Forbidden"));
        verify(delegated).handle(eq(request), eq(response), eq(exception));
    }

    @Test
    public void should_NotSendErrorWithMessage_when_ErrorIs401() throws ServletException, IOException {
        CustomBearerTokenAccessDeniedHandler accessDeniedHandler = new CustomBearerTokenAccessDeniedHandler(delegated);

        when(response.getStatus()).thenReturn(401);

        accessDeniedHandler.handle(request, response, exception);

        verify(response, never()).sendError(anyInt(), anyString());
        verify(delegated).handle(eq(request), eq(response), eq(exception));
    }
}
