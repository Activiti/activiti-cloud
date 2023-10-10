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

import java.util.Optional;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;

public class TaskAuditServiceInfoAppender {

    public TaskAuditServiceInfoAppender() {}

    public CloudRuntimeEventImpl<?, ?> appendAuditServiceInfoTo(CloudTaskCompletedEventImpl cloudRuntimeEvent) {
        Optional
            .<String>ofNullable(getCommandContext().getGenericAttribute("actor"))
            .ifPresent(cloudRuntimeEvent::setActor);

        return cloudRuntimeEvent;
    }

    public CommandContext getCommandContext() {
        return Context.getCommandContext();
    }
}
