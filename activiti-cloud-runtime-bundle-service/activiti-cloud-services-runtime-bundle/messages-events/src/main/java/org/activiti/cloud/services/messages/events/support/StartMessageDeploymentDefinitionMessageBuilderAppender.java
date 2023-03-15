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
package org.activiti.cloud.services.messages.events.support;

import static org.activiti.cloud.services.messages.events.MessageEventHeaders.MESSAGE_EVENT_ID;
import static org.activiti.cloud.services.messages.events.MessageEventHeaders.MESSAGE_EVENT_NAME;

import org.activiti.api.process.model.StartMessageDeploymentDefinition;
import org.activiti.api.process.model.StartMessageSubscription;
import org.activiti.cloud.services.events.message.MessageBuilderAppender;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class StartMessageDeploymentDefinitionMessageBuilderAppender implements MessageBuilderAppender {

    private final StartMessageDeploymentDefinition startMessageDeploymentDefinition;

    public StartMessageDeploymentDefinitionMessageBuilderAppender(
        StartMessageDeploymentDefinition startMessageDeploymentDefinition
    ) {
        Assert.notNull(startMessageDeploymentDefinition, "startMessageDeploymentDefinition must not be null");

        this.startMessageDeploymentDefinition = startMessageDeploymentDefinition;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");

        StartMessageSubscription messageEventSubscription = startMessageDeploymentDefinition.getMessageSubscription();

        // TODO Add more headers
        return request
            .setHeader(MESSAGE_EVENT_NAME, messageEventSubscription.getEventName())
            .setHeader(MESSAGE_EVENT_ID, messageEventSubscription.getId());
    }
}
