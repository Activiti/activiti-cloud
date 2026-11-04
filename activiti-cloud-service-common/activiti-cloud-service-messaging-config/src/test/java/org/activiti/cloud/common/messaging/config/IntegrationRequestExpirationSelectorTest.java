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
package org.activiti.cloud.common.messaging.config;

import static org.activiti.cloud.common.messaging.config.IntegrationRequestExpirationSelector.REQUEST_DATE;
import static org.activiti.cloud.common.messaging.config.IntegrationRequestExpirationSelector.TTL_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class IntegrationRequestExpirationSelectorTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");
    private static final long ONE_HOUR_AGO = NOW.minusSeconds(3600).toEpochMilli();

    private final IntegrationRequestExpirationSelector selector = new IntegrationRequestExpirationSelector(
        "testConnector",
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    private final Logger selectorLogger = (Logger) LoggerFactory.getLogger(IntegrationRequestExpirationSelector.class);
    private final ListAppender<ILoggingEvent> logEvents = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void attachLogAppender() {
        originalLevel = selectorLogger.getLevel();
        selectorLogger.setLevel(Level.DEBUG);
        logEvents.start();
        selectorLogger.addAppender(logEvents);
    }

    @AfterEach
    void detachLogAppender() {
        selectorLogger.detachAppender(logEvents);
        selectorLogger.setLevel(originalLevel);
    }

    @Test
    void shouldLogDiscardAtInfo() {
        selector.accept(message(ONE_HOUR_AGO, 60));

        assertThat(logEvents.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.INFO);
    }

    @Test
    void shouldReportExpirationWithoutLogging() {
        assertThat(selector.isExpired(message(ONE_HOUR_AGO, 60))).isTrue();
        assertThat(selector.isExpired(message(ONE_HOUR_AGO, 10800))).isFalse();
        assertThat(logEvents.list).isEmpty();
    }

    @Test
    void shouldRejectRequestWhenTtlHasElapsed() {
        assertThat(selector.accept(message(ONE_HOUR_AGO, 60))).isFalse();
    }

    @Test
    void shouldAcceptRequestWhenTtlHasNotElapsed() {
        assertThat(selector.accept(message(ONE_HOUR_AGO, 10800))).isTrue();
    }

    @Test
    void shouldAcceptRequestExactlyAtExpiration() {
        assertThat(selector.accept(message(ONE_HOUR_AGO, 3600))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithoutExpirationHeaders() {
        assertThat(selector.accept(MessageBuilder.withPayload("{}").build())).isTrue();
    }

    @Test
    void shouldAcceptRequestWithoutRequestDate() {
        assertThat(selector.accept(MessageBuilder.withPayload("{}").setHeader(TTL_SECONDS, 60).build())).isTrue();
    }

    @Test
    void shouldAcceptRequestWithoutTtlSeconds() {
        assertThat(
            selector.accept(MessageBuilder.withPayload("{}").setHeader(REQUEST_DATE, ONE_HOUR_AGO).build())
        ).isTrue();
    }

    @Test
    void shouldAcceptRequestWithNonPositiveTtlSeconds() {
        assertThat(selector.accept(message(ONE_HOUR_AGO, 0))).isTrue();
        assertThat(selector.accept(message(ONE_HOUR_AGO, -1))).isTrue();
    }

    @Test
    void shouldReadHeadersSentAsStrings() {
        Message<String> message = MessageBuilder.withPayload("{}")
            .setHeader(REQUEST_DATE, String.valueOf(ONE_HOUR_AGO))
            .setHeader(TTL_SECONDS, "60")
            .build();

        assertThat(selector.accept(message)).isFalse();
    }

    @Test
    void shouldAcceptRequestWithUnparsableHeaders() {
        Message<String> message = MessageBuilder.withPayload("{}")
            .setHeader(REQUEST_DATE, "yesterday")
            .setHeader(TTL_SECONDS, 60)
            .build();

        assertThat(selector.accept(message)).isTrue();
    }

    @Test
    void shouldAcceptRequestWithUnexpectedHeaderTypes() {
        Message<String> message = MessageBuilder.withPayload("{}")
            .setHeader(REQUEST_DATE, new Object())
            .setHeader(TTL_SECONDS, 60)
            .build();

        assertThat(selector.accept(message)).isTrue();
    }

    @Test
    void shouldAcceptRequestWithOutOfRangeRequestDate() {
        assertThat(selector.accept(message(Long.MAX_VALUE, 60))).isTrue();
    }

    @Test
    void shouldIgnorePayload() {
        Message<String> message = MessageBuilder.withPayload(
            "{\"requestDate\":\"2026-10-09T09:00:00.000+0000\",\"ttlSeconds\":60}"
        ).build();

        assertThat(selector.accept(message)).isTrue();
    }

    private static Message<byte[]> message(long requestDate, long ttlSeconds) {
        return MessageBuilder.withPayload(new byte[0])
            .setHeader(REQUEST_DATE, requestDate)
            .setHeader(TTL_SECONDS, ttlSeconds)
            .build();
    }
}
