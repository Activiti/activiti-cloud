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

import java.io.IOException;
import java.io.OutputStream;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

class OutboxMessageSerializer implements Serializer<Object> {

    private final EmbeddedHeadersJsonMessageMapper mapper;

    public OutboxMessageSerializer(EmbeddedHeadersJsonMessageMapper mapper) {
        Assert.notNull(mapper, "mapper must not be null");
        this.mapper = mapper;
    }

    @Override
    public void serialize(Object object, OutputStream outputStream) throws IOException {
        Assert.notNull(object, "Object to serialize cannot be null");
        Assert.notNull(outputStream, "OutputStream cannot be null");

        // 1. Guard Clause: Enforce that the object is a Spring Message
        if (!(object instanceof Message<?> message)) {
            throw new IllegalArgumentException(
                "This serializer only supports instances of " +
                    Message.class.getName() +
                    ". Received: " +
                    object.getClass().getName()
            );
        }

        // 2. Guard Clause: Enforce that the payload is strictly a byte array
        if (!(message.getPayload() instanceof byte[])) {
            throw new IllegalArgumentException(
                "Message payload must be a byte[] array. " +
                    "Received payload type: " +
                    message.getPayload().getClass().getName()
            );
        }

        try {
            // 3. Serialize message via mapper into the [headersLen][headersJSON][payloadLen][rawBytes] frame
            byte[] serializedBytes = this.mapper.fromMessage(message);

            // 4. Write the raw binary packet straight to the output stream
            outputStream.write(serializedBytes);
            outputStream.flush();
        } catch (Exception e) {
            throw new IOException("Failed to serialize Message via EmbeddedHeadersJsonMessageMapper", e);
        }
    }
}
