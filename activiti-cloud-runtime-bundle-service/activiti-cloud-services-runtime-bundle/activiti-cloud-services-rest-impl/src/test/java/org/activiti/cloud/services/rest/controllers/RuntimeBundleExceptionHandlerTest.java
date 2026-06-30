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
package org.activiti.cloud.services.rest.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RuntimeBundleExceptionHandlerTest {

    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> listAppender;
    private final RuntimeBundleExceptionHandler handler = new RuntimeBundleExceptionHandler();

    @BeforeEach
    void setUp() {
        handlerLogger = (Logger) LoggerFactory.getLogger(RuntimeBundleExceptionHandler.class);
        handlerLogger.setAdditive(false);
        handlerLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        handlerLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(listAppender);
    }

    @Test
    void logFailedRequestLogsInfoWithMethodAndUri() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/v1/tasks/task-1/complete");
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.handleAppException(new IllegalStateException("task already completed"), request, response);

        assertThat(listAppender.list)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).isEqualTo("POST /v1/tasks/task-1/complete failed");
            });
    }
}
