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

import java.security.Principal;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;

public class TaskAuditServiceInfoAppender {

    private final SecurityContextPrincipalProvider securityContextPrincipalProvider;

    public TaskAuditServiceInfoAppender(SecurityContextPrincipalProvider securityContextPrincipalProvider) {
        this.securityContextPrincipalProvider = securityContextPrincipalProvider;
    }

    public CloudRuntimeEventImpl<Task, TaskRuntimeEvent.TaskEvents> appendAuditServiceInfoTo(
        CloudTaskCompletedEventImpl cloudRuntimeEvent
    ) {
        securityContextPrincipalProvider
            .getCurrentPrincipal()
            .map(Principal::getName)
            .ifPresent(cloudRuntimeEvent::setActor);

        return cloudRuntimeEvent;
    }
}
