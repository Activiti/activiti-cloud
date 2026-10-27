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
package org.activiti.cloud.common.messaging.config.outbox;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamOperations;
import org.springframework.util.MimeType;

public class OutboxStreamBridge implements StreamOperations {

    private final ObjectProvider<StreamBridge> streamBridge;

    public OutboxStreamBridge(ObjectProvider<StreamBridge> streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean send(String bindingName, Object data) {
        return streamBridge().send(bindingName, data);
    }

    @Override
    public boolean send(String bindingName, Object data, MimeType outputContentType) {
        return streamBridge().send(bindingName, data, outputContentType);
    }

    @Override
    public boolean send(String bindingName, @Nullable String binderName, Object data) {
        return streamBridge().send(bindingName, binderName, data);
    }

    @Override
    public boolean send(String bindingName, @Nullable String binderName, Object data, MimeType outputContentType) {
        return streamBridge().send(bindingName, binderName, data, outputContentType);
    }

    private StreamOperations streamBridge() {
        return streamBridge.getObject();
    }
}
