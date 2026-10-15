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

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class IntegrationRequestExpirationSelector implements MessageSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationRequestExpirationSelector.class);

    public static final String REQUEST_DATE = "requestDate";
    public static final String TTL_SECONDS = "ttlSeconds";
    public static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

    private final String connectorName;
    private final Clock clock;

    public IntegrationRequestExpirationSelector(String connectorName, Clock clock) {
        this.connectorName = connectorName;
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
        return readExpiration(message.getHeaders()).filter(expiration -> now.isAfter(expiration.expiresAt()));
    }

    private Optional<Expiration> readExpiration(MessageHeaders headers) {
        Long requestDate = toLong(headers.get(REQUEST_DATE));
        Long ttlSeconds = toLong(headers.get(TTL_SECONDS));

        if (requestDate == null || ttlSeconds == null || ttlSeconds <= 0) {
            return Optional.empty();
        }
        try {
            Instant requestInstant = Instant.ofEpochMilli(requestDate);
            return Optional.of(new Expiration(requestInstant, ttlSeconds, requestInstant.plusSeconds(ttlSeconds)));
        } catch (RuntimeException e) {
            LOGGER.debug("Cannot read integration request expiration for connector {}", connectorName, e);
            return Optional.empty();
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private record Expiration(Instant requestDate, long ttlSeconds, Instant expiresAt) {}
}
