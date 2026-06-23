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
package org.activiti.cloud.services.query.rest.assembler;

import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.query.rest.dto.ProcessVariableHistoryEntry;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ProcessVariableHistoryRepresentationModelAssembler
    implements RepresentationModelAssembler<ProcessVariableHistoryEntity, EntityModel<ProcessVariableHistoryEntry>> {

    @Override
    public EntityModel<ProcessVariableHistoryEntry> toModel(ProcessVariableHistoryEntity entity) {
        return EntityModel.of(
            new ProcessVariableHistoryEntry(
                entity.getId(),
                entity.getProcessInstanceId(),
                entity.getVariableName(),
                entity.getType(),
                entity.getValue(),
                entity.isDeleted(),
                entity.getEventTime(),
                entity.getRecordCreateTime(),
                entity.getMessageId(),
                entity.getCommandId(),
                entity.getSequenceNumber()
            )
        );
    }
}
