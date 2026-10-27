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

import static org.activiti.cloud.common.messaging.config.outbox.OutboxChannelInterceptor.OUTBOX_DESTINATION;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

public class OutboxMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxMessageHandler.class);

    private final String partitionKey;
    private final ObjectProvider<StreamOperations> streamBridgeProvider;

    public OutboxMessageHandler(String partitionKey, ObjectProvider<StreamOperations> streamBridgeProvider) {
        this.partitionKey = partitionKey;
        this.streamBridgeProvider = streamBridgeProvider;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        log.debug(
            "Outbox [Partition: {} Thread: {}] processing message: {}",
            partitionKey,
            Thread.currentThread().getName(),
            message
        );

        Optional.ofNullable(message.getHeaders().get(OUTBOX_DESTINATION, String.class)).ifPresentOrElse(
            outboxBinding -> streamBridge().send(outboxBinding, message),
            () -> log.warn("Outbox {} message {} dropped", partitionKey, message)
        );
    }

    private StreamOperations streamBridge() {
        return streamBridgeProvider.getObject();
    }
}
