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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class TaskControllerLoggingHelperTest {

    private Logger testLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        testLogger = (Logger) LoggerFactory.getLogger("test." + TaskControllerLoggingHelperTest.class.getName());
        testLogger.setAdditive(false);
        listAppender = new ListAppender<>();
        listAppender.start();
        testLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        testLogger.detachAppender(listAppender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void logTaskAttemptWithoutTaskIdLogsCorrectMessageWhenDebugEnabled() {
        testLogger.setLevel(Level.DEBUG);
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user-42");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertDebugLog(
            () -> TaskControllerLoggingHelper.logTaskAttempt(testLogger, "create a task"),
            "User user-42 wants to create a task"
        );
    }

    @Test
    void logTaskAttemptWithTaskIdLogsCorrectMessageWhenDebugEnabled() {
        testLogger.setLevel(Level.DEBUG);
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user-42");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertDebugLog(
            () -> TaskControllerLoggingHelper.logTaskAttempt(testLogger, "claim", "task-99"),
            "User user-42 wants to claim task task-99"
        );
    }

    @Test
    void logTaskAttemptWithoutTaskIdUsesUnknownWhenAuthenticationIsNull() {
        testLogger.setLevel(Level.DEBUG);

        assertDebugLog(
            () -> TaskControllerLoggingHelper.logTaskAttempt(testLogger, "create a task"),
            "User unknown wants to create a task"
        );
    }

    @Test
    void logTaskAttemptWithTaskIdUsesUnknownWhenAuthenticationIsNull() {
        testLogger.setLevel(Level.DEBUG);

        assertDebugLog(
            () -> TaskControllerLoggingHelper.logTaskAttempt(testLogger, "complete", "task-77"),
            "User unknown wants to complete task task-77"
        );
    }

    @Test
    void logTaskAttemptWithoutTaskIdDoesNotLogWhenDebugDisabled() {
        testLogger.setLevel(Level.INFO);

        TaskControllerLoggingHelper.logTaskAttempt(testLogger, "create a task");

        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void logTaskAttemptWithTaskIdDoesNotLogWhenDebugDisabled() {
        testLogger.setLevel(Level.INFO);

        TaskControllerLoggingHelper.logTaskAttempt(testLogger, "claim", "task-99");

        assertThat(listAppender.list).isEmpty();
    }

    private void assertDebugLog(Runnable call, String expectedMessage) {
        listAppender.list.clear();
        call.run();
        assertThat(listAppender.list)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(event.getFormattedMessage()).isEqualTo(expectedMessage);
            });
    }
}
