/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import java.util.List;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.springframework.messaging.support.MessageBuilder;

public class MessageProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String PROCESS_ENGINE_EVENTS = "processEngineEvents";

    private final ProcessEngineChannels producer;

    public MessageProducerCommandContextCloseListener(ProcessEngineChannels producer) {
        this.producer = producer;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<CloudRuntimeEvent<?, ?>> events = commandContext.getGenericAttribute(PROCESS_ENGINE_EVENTS);
        if (events != null && !events.isEmpty()) {
            producer.auditProducer().send(MessageBuilder.withPayload(
                    events.toArray(new CloudRuntimeEvent<?, ?>[0]))
                                                  .build());
        }
    }

    @Override
    public void closing(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        // No need to implement this method in this class
    }
}
