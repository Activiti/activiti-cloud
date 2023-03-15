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

import org.activiti.api.process.model.BPMNMessage;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.cloud.services.events.message.RuntimeBundleInfoMessageBuilderAppender;

public class BpmnMessageEventMessageBuilderFactory {

    private final RuntimeBundleProperties properties;

    public BpmnMessageEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        this.properties = properties;
    }

    public MessageBuilderAppenderChain create(BPMNMessage bpmnMessage) {
        return new MessageBuilderAppenderChain()
            .chain(new RuntimeBundleInfoMessageBuilderAppender(properties))
            .chain(new MessageEventPayloadMessageBuilderAppender(bpmnMessage.getMessagePayload()));
    }
}
