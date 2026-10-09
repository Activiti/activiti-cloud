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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class IntegrationRequestExpirationSelectorTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");
    private static final String ONE_HOUR_AGO = "2026-10-09T09:00:00.000+0000";
    private final Function<Object, byte[]> failingSerializer = payload -> {
        throw new IllegalStateException("Cannot serialize " + payload);
    };

    private final IntegrationRequestExpirationSelector selector = new IntegrationRequestExpirationSelector(
        "testConnector",
        failingSerializer,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldRejectRequestWhenTtlHasElapsed() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\",\"ttlSeconds\":60}", ONE_HOUR_AGO))).isFalse();
    }

    @Test
    void shouldAcceptRequestWhenTtlHasNotElapsed() {
        assertThat(
            selector.accept(jsonMessage("{\"requestDate\":\"%s\",\"ttlSeconds\":10800}", ONE_HOUR_AGO))
        ).isTrue();
    }

    @Test
    void shouldAcceptRequestExactlyAtExpiration() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\",\"ttlSeconds\":3600}", ONE_HOUR_AGO))).isTrue();
    }

    @Test
    void shouldRejectRequestWhenFieldsFollowLargeNestedProperties() {
        String payload =
            "{\"integrationContext\":{\"inBoundVariables\":{\"list\":[1,2,{\"requestDate\":\"2099-01-01T00:00:00Z\"}]}}," +
            "\"ttlSeconds\":60,\"resultDestination\":\"results\",\"requestDate\":\"" +
            ONE_HOUR_AGO +
            "\"}";

        assertThat(
            selector.accept(MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build())
        ).isFalse();
    }

    @Test
    void shouldIgnoreNestedFieldsWhenTopLevelFieldsAreMissing() {
        String payload = "{\"integrationContext\":{\"requestDate\":\"" + ONE_HOUR_AGO + "\",\"ttlSeconds\":60}}";

        assertThat(
            selector.accept(MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build())
        ).isTrue();
    }

    @Test
    void shouldAcceptRequestWithoutRequestDate() {
        assertThat(selector.accept(jsonMessage("{\"ttlSeconds\":60}"))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithoutTtlSeconds() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\"}", ONE_HOUR_AGO))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithNullFields() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":null,\"ttlSeconds\":null}"))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithNonPositiveTtlSeconds() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\",\"ttlSeconds\":0}", ONE_HOUR_AGO))).isTrue();
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\",\"ttlSeconds\":-1}", ONE_HOUR_AGO))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithUnparsableRequestDate() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"yesterday\",\"ttlSeconds\":60}"))).isTrue();
    }

    @Test
    void shouldAcceptRequestWithUnexpectedFieldTypes() {
        assertThat(selector.accept(jsonMessage("{\"requestDate\":{\"date\":1},\"ttlSeconds\":\"60\"}"))).isTrue();
    }

    @Test
    void shouldRejectRequestWithRequestDateInIsoFormatWithColonInOffset() {
        assertThat(
            selector.accept(jsonMessage("{\"requestDate\":\"2026-10-09T11:00:00.000+02:00\",\"ttlSeconds\":60}"))
        ).isFalse();
    }

    @Test
    void shouldRejectRequestWithRequestDateInEpochMillis() {
        long oneHourAgo = NOW.minusSeconds(3600).toEpochMilli();

        assertThat(selector.accept(jsonMessage("{\"requestDate\":%d,\"ttlSeconds\":60}", oneHourAgo))).isFalse();
    }

    @Test
    void shouldReadStringPayload() {
        String payload = "{\"requestDate\":\"" + ONE_HOUR_AGO + "\",\"ttlSeconds\":60}";

        assertThat(selector.accept(MessageBuilder.withPayload(payload).build())).isFalse();
    }

    @Test
    void shouldAcceptNonJsonPayloads() {
        assertThat(selector.accept(MessageBuilder.withPayload("GetRequest").build())).isTrue();
        assertThat(selector.accept(MessageBuilder.withPayload("GetRequest".getBytes()).build())).isTrue();
        assertThat(selector.accept(jsonMessage("[{\"requestDate\":\"%s\",\"ttlSeconds\":60}]", ONE_HOUR_AGO))).isTrue();
        assertThat(selector.accept(jsonMessage("{\"requestDate\":\"%s\",", ONE_HOUR_AGO))).isTrue();
    }

    @Test
    void shouldSerializeObjectPayloads() {
        AtomicReference<Object> serializedPayload = new AtomicReference<>();
        Map<String, Object> payload = Map.of("requestDate", ONE_HOUR_AGO, "ttlSeconds", 60);
        IntegrationRequestExpirationSelector objectSelector = new IntegrationRequestExpirationSelector(
            "testConnector",
            value -> {
                serializedPayload.set(value);
                return ("{\"requestDate\":\"" + ONE_HOUR_AGO + "\",\"ttlSeconds\":60}").getBytes(
                    StandardCharsets.UTF_8
                );
            },
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThat(objectSelector.accept(MessageBuilder.withPayload(payload).build())).isFalse();
        assertThat(serializedPayload.get()).isSameAs(payload);
    }

    @Test
    void shouldAcceptRequestWhenSerializationFails() {
        assertThat(selector.accept(MessageBuilder.withPayload(Map.of("ttlSeconds", 60)).build())).isTrue();
    }

    private static Message<byte[]> jsonMessage(String template, Object... args) {
        return MessageBuilder.withPayload(String.format(template, args).getBytes(StandardCharsets.UTF_8)).build();
    }
}
