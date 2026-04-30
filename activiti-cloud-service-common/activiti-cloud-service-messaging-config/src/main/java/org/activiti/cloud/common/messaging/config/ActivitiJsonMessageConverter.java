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

import org.springframework.cloud.function.cloudevent.CloudEventMessageUtils;
import org.springframework.cloud.function.context.config.JsonMessageConverter;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

public class ActivitiJsonMessageConverter extends JsonMessageConverter {

    public ActivitiJsonMessageConverter(JsonMapper jsonMapper) {
        super(
            jsonMapper,
            new MimeType("application", "json"),
            new MimeType(
                CloudEventMessageUtils.APPLICATION_CLOUDEVENTS.getType(),
                CloudEventMessageUtils.APPLICATION_CLOUDEVENTS.getSubtype() + "+json"
            ),
            new MimeType("application", "octet-stream")
        );
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        var payload = message.getPayload();
        if (payload instanceof byte[] bytes && !looksLikeJson(bytes)) {
            return null;
        }
        return super.convertFromInternal(message, targetClass, conversionHint);
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
        if ((headers != null && isNotJson(headers)) || payload instanceof byte[] || payload instanceof String) {
            return null;
        }
        return super.convertToInternal(payload, headers, conversionHint);
    }

    private boolean isNotJson(MessageHeaders headers) {
        Object contentType = headers.get(MessageHeaders.CONTENT_TYPE);
        return contentType != null && !contentType.toString().equals("application/json");
    }

    private boolean looksLikeJson(byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        int start = 0;
        int end = bytes.length - 1;
        while (start < bytes.length && Character.isWhitespace(bytes[start])) {
            start++;
        }
        while (end > start && Character.isWhitespace(bytes[end])) {
            end--;
        }
        if (start >= bytes.length) {
            return false;
        }
        return (bytes[start] == '{' && bytes[end] == '}') || (bytes[start] == '[' && bytes[end] == ']');
    }
}
