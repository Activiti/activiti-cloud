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

import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.rest.dto.BpmnDiagramActivityEntry;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class BPMNActivityRepresentationModelAssembler
    implements RepresentationModelAssembler<BPMNActivityEntity, EntityModel<BpmnDiagramActivityEntry>>
{

    @Override
    public EntityModel<BpmnDiagramActivityEntry> toModel(BPMNActivityEntity entity) {
        return EntityModel.of(
            new BpmnDiagramActivityEntry(
                entity.getId(),
                entity.getElementId(),
                entity.getActivityType(),
                entity.getStatus(),
                entity.getExecutionId(),
                entity.getStartedDate(),
                entity.getCompletedDate(),
                entity.getCancelledDate()
            )
        );
    }
}
