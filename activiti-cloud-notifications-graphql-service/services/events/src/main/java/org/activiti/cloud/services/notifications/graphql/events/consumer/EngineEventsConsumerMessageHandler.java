/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.notifications.graphql.events.consumer;

import java.util.List;
import java.util.Map;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class EngineEventsConsumerMessageHandler {

    private static Logger logger = LoggerFactory.getLogger(EngineEventsConsumerMessageHandler.class);

    private final Transformer transformer;

    public EngineEventsConsumerMessageHandler(Transformer transformer) {
        this.transformer = transformer;
    }

    @org.springframework.integration.annotation.Transformer
    public Message<List<EngineEvent>> receive(Message<List<Map<String, Object>>> message) {
        List<Map<String, Object>> events = message.getPayload();
        String routingKey = (String) message.getHeaders().get("routingKey");

        logger.debug("Recieved source message {} with routingKey: {}", message, routingKey);

        return MessageBuilder.<List<EngineEvent>>createMessage(transformer.transform(events), message.getHeaders());
    }
}
