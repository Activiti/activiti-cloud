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

package org.activiti.cloud.services.events.converter;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.task.IdentityLink;

public class ProcessAuditServiceInfoAppender {

    private final RuntimeService runtimeService;

    public ProcessAuditServiceInfoAppender(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> appendAuditServiceInfoTo(
        CloudRuntimeEventImpl cloudRuntimeEvent
    ) {
        var identityLinks = runtimeService.getIdentityLinksForProcessInstance(cloudRuntimeEvent.getProcessInstanceId());

        identityLinks
            .stream()
            .filter(identityLink -> ActorConstants.ACTOR_TYPE.equalsIgnoreCase(identityLink.getType()))
            .map(IdentityLink::getDetails)
            .map(String::new)
            .findFirst()
            .ifPresent(cloudRuntimeEvent::setActor);

        return cloudRuntimeEvent;
    }
}
