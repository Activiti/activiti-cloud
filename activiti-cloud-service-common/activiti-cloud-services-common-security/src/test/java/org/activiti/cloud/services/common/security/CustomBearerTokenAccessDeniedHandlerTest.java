package org.activiti.cloud.services.common.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;

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
