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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class OutboxMessageSerializerTest {

    private EmbeddedHeadersJsonMessageMapper mapper;
    private OutboxMessageSerializer serializer;

    @BeforeEach
    void setUp() {
        mapper = new EmbeddedHeadersJsonMessageMapper("*");
        serializer = new OutboxMessageSerializer(mapper);
    }

    @Test
    void constructor_whenMapperIsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new OutboxMessageSerializer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("mapper must not be null");
    }

    @Test
    void serialize_whenObjectIsNull_throwsIllegalArgumentException() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThatThrownBy(() -> serializer.serialize(null, outputStream))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Object to serialize cannot be null");
    }

    @Test
    void serialize_whenOutputStreamIsNull_throwsIllegalArgumentException() {
        Message<byte[]> message = MessageBuilder.withPayload("test".getBytes(StandardCharsets.UTF_8)).build();

        assertThatThrownBy(() -> serializer.serialize(message, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("OutputStream cannot be null");
    }

    @Test
    void serialize_whenObjectIsNotMessage_throwsIllegalArgumentException() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String invalidObject = "plain-string-not-a-message";

        assertThatThrownBy(() -> serializer.serialize(invalidObject, outputStream))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "This serializer only supports instances of " +
                    Message.class.getName() +
                    ". Received: " +
                    String.class.getName()
            );
    }

    @Test
    void serialize_whenPayloadIsNotByteArray_throwsIllegalArgumentException() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Message<String> invalidPayloadMessage = MessageBuilder.withPayload("string-payload").build();

        assertThatThrownBy(() -> serializer.serialize(invalidPayloadMessage, outputStream))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "Message payload must be a byte[] array. " + "Received payload type: " + String.class.getName()
            );
    }

    @Test
    void serialize_whenValidByteArrayMessage_writesFramedBytesAndFlushesStream() throws Exception {
        byte[] payload = "{\"id\":42,\"action\":\"CREATED\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
            .setHeader("eventType", "ItemCreated")
            .setHeader("source", "outbox-service")
            .build();

        TrackingOutputStream outputStream = new TrackingOutputStream();

        serializer.serialize(message, outputStream);

        byte[] writtenBytes = outputStream.toByteArray();
        assertThat(writtenBytes).isNotEmpty();
        assertThat(outputStream.isFlushed()).isTrue();

        Message<?> roundTrippedMessage = mapper.toMessage(writtenBytes);
        assertThat(roundTrippedMessage.getPayload()).isEqualTo(payload);
        assertThat(roundTrippedMessage.getHeaders())
            .containsEntry("eventType", "ItemCreated")
            .containsEntry("source", "outbox-service");
    }

    @Test
    void serialize_whenOutputStreamThrowsIOExceptionOnWrite_wrapsInIOException() {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.withPayload(payload).build();
        IOException writeFailure = new IOException("Disk full or broken pipe");

        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw writeFailure;
            }
        };

        assertThatThrownBy(() -> serializer.serialize(message, failingStream))
            .isInstanceOf(IOException.class)
            .hasMessage("Failed to serialize Message via EmbeddedHeadersJsonMessageMapper")
            .hasCause(writeFailure);
    }

    @Test
    void serialize_whenOutputStreamThrowsIOExceptionOnFlush_wrapsInIOException() {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.withPayload(payload).build();
        IOException flushFailure = new IOException("Flush failed");

        OutputStream failingFlushStream = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                throw flushFailure;
            }
        };

        assertThatThrownBy(() -> serializer.serialize(message, failingFlushStream))
            .isInstanceOf(IOException.class)
            .hasMessage("Failed to serialize Message via EmbeddedHeadersJsonMessageMapper")
            .hasCause(flushFailure);
    }

    private static class TrackingOutputStream extends ByteArrayOutputStream {

        private boolean flushed = false;

        @Override
        public void flush() throws IOException {
            super.flush();
            this.flushed = true;
        }

        public boolean isFlushed() {
            return flushed;
        }
    }
}
