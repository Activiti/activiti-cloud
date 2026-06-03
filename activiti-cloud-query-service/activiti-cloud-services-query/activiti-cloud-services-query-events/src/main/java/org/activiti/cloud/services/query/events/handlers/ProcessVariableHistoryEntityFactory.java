/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.events.handlers;

import java.util.Date;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;

public class ProcessVariableHistoryEntityFactory {

    private ProcessVariableHistoryEntityFactory() {}

    public static ProcessVariableHistoryEntity forCreate(CloudVariableCreatedEvent event) {
        ProcessVariableHistoryEntity history = buildBase(event);
        history.setValue(event.getEntity().getValue());
        return history;
    }

    public static ProcessVariableHistoryEntity forUpdate(CloudVariableUpdatedEvent event) {
        ProcessVariableHistoryEntity history = buildBase(event);
        history.setValue(event.getEntity().getValue());
        return history;
    }

    public static ProcessVariableHistoryEntity forDelete(CloudVariableDeletedEvent event) {
        ProcessVariableHistoryEntity history = buildBase(event);
        history.setDeleted(true);
        return history;
    }

    private static ProcessVariableHistoryEntity buildBase(CloudVariableEvent event) {
        ProcessVariableHistoryEntity history = new ProcessVariableHistoryEntity();
        history.setProcessInstanceId(event.getEntity().getProcessInstanceId());
        history.setVariableName(event.getEntity().getName());
        history.setType(event.getEntity().getType());
        history.setCreateTime(new Date(event.getTimestamp()));
        history.setMessageId(event.getMessageId());
        history.setSequenceNumber(event.getSequenceNumber());
        return history;
    }
}
