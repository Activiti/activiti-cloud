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

package org.activiti.cloud.services.messages.core.support;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_TYPE;

import java.util.Optional;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

public class MessageSelectors {

    public static class MessageEventTypeSelector implements MessageSelector {
        
        private final Enum<?> type;
        
        public MessageEventTypeSelector(Enum<?> type) {
            this.type = type;
        }

        @Override
        public boolean accept(Message<?> message) {
            return Optional.ofNullable(message.getHeaders()
                                              .get(MESSAGE_EVENT_TYPE))
                           .filter(type.name()::equals)
                           .isPresent();
        }
        
    }
}
