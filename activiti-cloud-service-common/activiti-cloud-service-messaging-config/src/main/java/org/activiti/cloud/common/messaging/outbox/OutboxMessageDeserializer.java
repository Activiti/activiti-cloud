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
import java.io.InputStream;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.util.Assert;

class OutboxMessageDeserializer implements Deserializer<Object> {

    private final EmbeddedHeadersJsonMessageMapper mapper;

    public OutboxMessageDeserializer(EmbeddedHeadersJsonMessageMapper mapper) {
        Assert.notNull(mapper, "EmbeddedHeadersJsonMessageMapper must not be null");
        this.mapper = mapper;
    }

    @Override
    public Object deserialize(InputStream inputStream) throws IOException {
        Assert.notNull(inputStream, "InputStream cannot be null");

        try {
            // Read all available bytes from the stream representing the framed message
            byte[] allBytes = inputStream.readAllBytes();
            if (allBytes.length == 0) {
                throw new IOException("Expected a non empty byte[] array");
            }

            // Reconstruct the Message object (payload automatically resolves back into a raw byte[])
            return this.mapper.toMessage(allBytes);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize Message via EmbeddedHeadersJsonMessageMapper", e);
        }
    }
}
