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
package org.activiti.cloud.api.process.model.events;

import org.activiti.api.process.model.StartMessageDeploymentDefinition;
import org.activiti.api.process.model.events.MessageDefinitionEvent;
import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

public interface CloudStartMessageDeployedEvent
    extends
        CloudRuntimeEvent<StartMessageDeploymentDefinition, MessageDefinitionEvent.MessageDefinitionEvents>,
        StartMessageDeployedEvent {
    @Override
    default MessageDefinitionEvents getEventType() {
        return MessageDefinitionEvents.START_MESSAGE_DEPLOYED;
    }
}
