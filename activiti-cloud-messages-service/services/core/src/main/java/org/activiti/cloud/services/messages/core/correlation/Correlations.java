/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.messages.core.correlation;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_CORRELATION_KEY;
import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_NAME;
import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.SERVICE_FULL_NAME;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class Correlations {
    
    public static String getCorrelationId(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        String serviceFullName = headers.get(SERVICE_FULL_NAME, String.class);
        String messageEventName = headers.get(MESSAGE_EVENT_NAME, String.class);
        String messageCorrelationKey = headers.get(MESSAGE_EVENT_CORRELATION_KEY, String.class);
        
        StringBuilder builder = new StringBuilder();
        builder.append(serviceFullName)
               .append(":")
               .append(messageEventName);
               
        if (messageCorrelationKey != null) {
            builder.append(":")
                   .append(messageCorrelationKey);
        }
        
        return builder.toString();        
    }
}
