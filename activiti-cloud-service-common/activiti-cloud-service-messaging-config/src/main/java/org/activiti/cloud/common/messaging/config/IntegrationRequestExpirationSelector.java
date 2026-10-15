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

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.util.StdDateFormat;

public class IntegrationRequestExpirationSelector implements MessageSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationRequestExpirationSelector.class);

    public static final String REQUEST_DATE = "requestDate";
    public static final String TTL_SECONDS = "ttlSeconds";
    public static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final StdDateFormat DATE_FORMAT = new StdDateFormat();

    private final String connectorName;
    private final Function<Object, byte[]> payloadSerializer;
    private final Clock clock;

    public IntegrationRequestExpirationSelector(
        String connectorName,
        Function<Object, byte[]> payloadSerializer,
        Clock clock
    ) {
        this.connectorName = connectorName;
        this.payloadSerializer = payloadSerializer;
        this.clock = clock;
    }

    @Override
    public boolean accept(Message<?> message) {
        Optional<Expiration> expiration = expiredAt(message);
        expiration.ifPresent(expired ->
            LOGGER.info(
                "Discarding expired integration request {} in connector binding {}: requestDate {} plus ttlSeconds {} expired at {}",
                message.getHeaders().get(INTEGRATION_CONTEXT_ID),
                connectorName,
                expired.requestDate(),
                expired.ttlSeconds(),
                expired.expiresAt()
            )
        );
        return expiration.isEmpty();
    }

    public boolean isExpired(Message<?> message) {
        return expiredAt(message).isPresent();
    }

    private Optional<Expiration> expiredAt(Message<?> message) {
        Instant now = clock.instant();
        return readExpiration(message.getPayload()).filter(expiration -> now.isAfter(expiration.expiresAt()));
    }

    private Optional<Expiration> readExpiration(Object payload) {
        try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), toJson(payload))) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return Optional.empty();
            }

            Instant requestDate = null;
            Long ttlSeconds = null;

            while ((requestDate == null || ttlSeconds == null) && parser.nextToken() == JsonToken.PROPERTY_NAME) {
                String propertyName = parser.currentName();
                JsonToken valueToken = parser.nextToken();

                if (REQUEST_DATE.equals(propertyName)) {
                    requestDate = readRequestDate(parser, valueToken);
                } else if (TTL_SECONDS.equals(propertyName)) {
                    ttlSeconds = readTtlSeconds(parser, valueToken);
                } else {
                    parser.skipChildren();
                }
            }

            if (requestDate == null || ttlSeconds == null || ttlSeconds <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Expiration(requestDate, ttlSeconds, requestDate.plusSeconds(ttlSeconds)));
        } catch (ParseException | RuntimeException e) {
            LOGGER.debug("Cannot read integration request expiration for connector {}", connectorName, e);
            return Optional.empty();
        }
    }

    private byte[] toJson(Object payload) {
        if (payload instanceof byte[] bytes) {
            return bytes;
        }
        if (payload instanceof String text) {
            return text.getBytes(StandardCharsets.UTF_8);
        }
        return payloadSerializer.apply(payload);
    }

    private static Instant readRequestDate(JsonParser parser, JsonToken valueToken) throws ParseException {
        return switch (valueToken) {
            case VALUE_STRING -> DATE_FORMAT.clone().parse(parser.getString()).toInstant();
            case VALUE_NUMBER_INT -> Instant.ofEpochMilli(parser.getLongValue());
            default -> {
                parser.skipChildren();
                yield null;
            }
        };
    }

    private static Long readTtlSeconds(JsonParser parser, JsonToken valueToken) {
        if (valueToken == JsonToken.VALUE_NUMBER_INT) {
            return parser.getLongValue();
        }
        parser.skipChildren();
        return null;
    }

    private record Expiration(Instant requestDate, long ttlSeconds, Instant expiresAt) {}
}
