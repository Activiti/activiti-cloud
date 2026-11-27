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

package org.activiti.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

class OutboxMessageDeserializerTest {

    private EmbeddedHeadersJsonMessageMapper mapper;
    private OutboxMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        mapper = new EmbeddedHeadersJsonMessageMapper("*");
        deserializer = new OutboxMessageDeserializer(mapper);
    }

    @Test
    void constructor_whenMapperIsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new OutboxMessageDeserializer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("EmbeddedHeadersJsonMessageMapper must not be null");
    }

    @Test
    void deserialize_whenInputStreamIsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> deserializer.deserialize(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("InputStream cannot be null");
    }

    @Test
    void deserialize_whenInputStreamIsEmpty_throwsIOException() {
        InputStream emptyStream = InputStream.nullInputStream();

        assertThatThrownBy(() -> deserializer.deserialize(emptyStream))
            .isInstanceOf(IOException.class)
            .hasMessage("Failed to deserialize Message via EmbeddedHeadersJsonMessageMapper")
            .hasRootCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("Expected a non empty byte[] array");
    }

    @Test
    void deserialize_whenValidFramedMessage_returnsMessageWithPayloadAndHeaders() throws Exception {
        byte[] originalPayload = "{\"orderId\":123,\"status\":\"CONFIRMED\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> originalMessage = MessageBuilder.withPayload(originalPayload)
            .setHeader("eventType", "OrderConfirmed")
            .setHeader("aggregateId", "order-123")
            .build();

        byte[] framedBytes = mapper.fromMessage(originalMessage);
        InputStream inputStream = new ByteArrayInputStream(framedBytes);

        Object result = deserializer.deserialize(inputStream);

        Message<?> deserializedMessage = assertThat(result)
            .asInstanceOf(InstanceOfAssertFactories.type(GenericMessage.class))
            .actual();

        assertThat(deserializedMessage.getPayload()).isEqualTo(originalPayload);
        assertThat(deserializedMessage.getHeaders())
            .containsEntry("eventType", "OrderConfirmed")
            .containsEntry("aggregateId", "order-123");
    }

    @Test
    void deserialize_whenPayloadIsMalformed_wrapsMapperPayloadToGenericMessage() throws IOException {
        byte[] invalidBytes = "not-a-valid-embedded-header-frame".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(invalidBytes);

        Object result = deserializer.deserialize(inputStream);

        assertThat(result)
            .asInstanceOf(InstanceOfAssertFactories.type(GenericMessage.class))
            .extracting(Message::getPayload)
            .isEqualTo(invalidBytes);
    }

    @Test
    void deserialize_whenInputStreamFailsOnRead_wrapsStreamExceptionInIOException() {
        IOException readFailure = new IOException("Simulated stream read error");
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw readFailure;
            }
        };

        assertThatThrownBy(() -> deserializer.deserialize(failingStream))
            .isInstanceOf(IOException.class)
            .hasMessage("Failed to deserialize Message via EmbeddedHeadersJsonMessageMapper")
            .hasCause(readFailure);
    }
}
