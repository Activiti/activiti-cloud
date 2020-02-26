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


package org.activiti.cloud.services.messages.core.transformer;

import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;

public class ReceiveMessagePayloadTransformer extends AbstractPayloadTransformer<MessageEventPayload, ReceiveMessagePayload> {
    
    private static final ReceiveMessagePayloadTransformer INSTANCE = new ReceiveMessagePayloadTransformer();
    
    public static ReceiveMessagePayload from(Message<?> message) {
        try {
            return INSTANCE.doTransform(message);
        } catch (Exception cause) {
            throw new MessageConversionException(message, cause.getMessage());
        }
    }

    @Override
    protected ReceiveMessagePayload transformPayload(MessageEventPayload eventPayload) throws Exception {
        return MessagePayloadBuilder.receive(eventPayload.getName())
                                    .withCorrelationKey(eventPayload.getCorrelationKey())
                                    .withVariables(eventPayload.getVariables())
                                    .build();
    }
}
