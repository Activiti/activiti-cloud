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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

public class MessageContentTypeNormalizer {

    public Message<?> normalizeToJsonFallback(Message<?> message) {
        if (!requiresJsonFallback(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))) {
            return message;
        }
        return MessageBuilder
            .fromMessage(message)
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .build();
    }

    private boolean requiresJsonFallback(Object currentContentType) {
        if (currentContentType == null) {
            return true;
        }
        return MimeTypeUtils.APPLICATION_OCTET_STREAM.equalsTypeAndSubtype(
            MimeType.valueOf(currentContentType.toString())
        );
    }
}
