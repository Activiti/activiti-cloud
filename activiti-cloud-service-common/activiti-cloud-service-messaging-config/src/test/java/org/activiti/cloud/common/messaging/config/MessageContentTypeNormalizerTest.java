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

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

class MessageContentTypeNormalizerTest {

    private final MessageContentTypeNormalizer normalizer = new MessageContentTypeNormalizer();

    @Test
    void shouldSetJsonWhenContentTypeHeaderIsMissing() {
        Message<byte[]> message = new GenericMessage<>("payload".getBytes());

        Message<?> normalized = normalizer.normalizeToJsonFallback(message);

        assertThat(normalized.getHeaders().get(MessageHeaders.CONTENT_TYPE))
            .hasToString(MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    @Test
    void shouldOverrideOctetStreamWithJson() {
        Message<byte[]> message = MessageBuilder
            .withPayload("payload".getBytes())
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
            .build();

        Message<?> normalized = normalizer.normalizeToJsonFallback(message);

        assertThat(normalized.getHeaders().get(MessageHeaders.CONTENT_TYPE))
            .hasToString(MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    @Test
    void shouldReturnSameInstanceWhenContentTypeIsJson() {
        Message<byte[]> message = MessageBuilder
            .withPayload("payload".getBytes())
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .build();

        Message<?> normalized = normalizer.normalizeToJsonFallback(message);

        assertThat(normalized).isSameAs(message);
    }

    @Test
    void shouldPreserveNonJsonContentType() {
        Message<byte[]> message = MessageBuilder
            .withPayload("payload".getBytes())
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();

        Message<?> normalized = normalizer.normalizeToJsonFallback(message);

        assertThat(normalized).isSameAs(message);
        assertThat(normalized.getHeaders().get(MessageHeaders.CONTENT_TYPE))
            .hasToString(MimeTypeUtils.TEXT_PLAIN_VALUE);
    }
}
