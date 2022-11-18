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
package org.activiti.cloud.services.events.message;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class CloudRuntimeEventMessageBuilderAppender implements MessageBuilderAppender {

    private final CloudRuntimeEvent<?, ?> event;

    public CloudRuntimeEventMessageBuilderAppender(CloudRuntimeEvent<?, ?> event) {
        Assert.notNull(event, "event must not be null");

        this.event = event;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");

        return request
            .setHeader(CloudRuntimeEventMessageHeaders.EVENT_TYPE, event.getEventType().name())
            .setHeader(CloudRuntimeEventMessageHeaders.BUSINESS_KEY, event.getBusinessKey())
            .setHeader(CloudRuntimeEventMessageHeaders.PROCESS_DEFINITION_KEY, event.getProcessDefinitionKey())
            .setHeader(CloudRuntimeEventMessageHeaders.PROCESS_DEFINITION_VERSION, event.getProcessDefinitionVersion())
            .setHeader(CloudRuntimeEventMessageHeaders.PROCESS_INSTANCE_ID, event.getProcessInstanceId())
            .setHeader(CloudRuntimeEventMessageHeaders.PARENT_PROCESS_INSTANCE_ID, event.getParentProcessInstanceId());
    }
}
