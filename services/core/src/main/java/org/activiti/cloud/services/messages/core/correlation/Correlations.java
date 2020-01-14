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
