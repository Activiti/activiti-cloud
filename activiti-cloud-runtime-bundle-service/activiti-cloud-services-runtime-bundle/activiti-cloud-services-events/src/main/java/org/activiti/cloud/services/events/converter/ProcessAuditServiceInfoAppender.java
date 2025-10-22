/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.activiti.api.model.shared.model.IdentityLink;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.engine.impl.interceptor.CommandContext;

public class ProcessAuditServiceInfoAppender {

    private final Supplier<CommandContext> commandContext;

    public ProcessAuditServiceInfoAppender(Supplier<CommandContext> commandContext) {
        this.commandContext = commandContext;
    }

    public CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> appendAuditServiceInfoTo(
        CloudRuntimeEventImpl cloudRuntimeEvent
    ) {
        // First try to get identity links from the event entity
        Optional<String> actorFromEvent = getActorFromEvent(cloudRuntimeEvent);

        // If not found in event entity, fall back to database lookup
        if (actorFromEvent.isEmpty()) {
            getActorFromDb(cloudRuntimeEvent);
        } else {
            clearIdentityLinksIfPresent(cloudRuntimeEvent);
            actorFromEvent.ifPresent(cloudRuntimeEvent::setActor);
        }

        return cloudRuntimeEvent;
    }

    private static Optional<String> extractActorFromIdentityLinks(Stream<?> identityLinksStream) {
        return identityLinksStream
            .filter(link -> link instanceof IdentityLink || link instanceof org.activiti.engine.task.IdentityLink)
            .filter(link ->
                ActorConstants.ACTOR_TYPE.equalsIgnoreCase(
                    link instanceof IdentityLink
                        ? ((IdentityLink) link).getType()
                        : ((org.activiti.engine.task.IdentityLink) link).getType()
                )
            )
            .map(link ->
                link instanceof IdentityLink
                    ? ((IdentityLink) link).getDetails()
                    : ((org.activiti.engine.task.IdentityLink) link).getDetails()
            )
            .map(String::new)
            .findFirst();
    }

    private void getActorFromDb(CloudRuntimeEventImpl cloudRuntimeEvent) {
        Optional
            .ofNullable(commandContext)
            .map(Supplier::get)
            .map(CommandContext::getExecutionEntityManager)
            .map(it -> it.findById(cloudRuntimeEvent.getProcessInstanceId()))
            .flatMap(processInstance -> extractActorFromIdentityLinks(processInstance.getIdentityLinks().stream()))
            .ifPresent(cloudRuntimeEvent::setActor);
    }

    private static Optional<String> getActorFromEvent(CloudRuntimeEventImpl cloudRuntimeEvent) {
        return Optional
            .ofNullable(cloudRuntimeEvent.getEntity())
            .filter(entity -> entity instanceof ProcessInstanceImpl)
            .map(entity -> (ProcessInstanceImpl) entity)
            .filter(processInstance ->
                processInstance.getId() != null &&
                processInstance.getId().equals(cloudRuntimeEvent.getProcessInstanceId())
            )
            .flatMap(processInstance ->
                Optional
                    .ofNullable(processInstance.getIdentityLinks())
                    .filter(identityLinks -> !identityLinks.isEmpty())
                    .flatMap(identityLinks -> extractActorFromIdentityLinks(identityLinks.stream()))
            );
    }

    private void clearIdentityLinksIfPresent(CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> event) {
        if (event.getEntity() instanceof ProcessInstanceImpl) {
            ((ProcessInstanceImpl) event.getEntity()).setIdentityLinks(null);
        }
    }
}
