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
package org.activiti.cloud.services.query.rest;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessVariableHistoryRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/admin/v1/process-instances/{processInstanceId}/variables/history",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class ProcessInstanceVariableHistoryAdminController {

    private final ProcessVariableHistoryRepository historyRepository;

    private final ProcessVariableHistoryRepresentationModelAssembler historyRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<ProcessVariableHistoryEntity> pagedModelAssembler;

    @Autowired
    public ProcessInstanceVariableHistoryAdminController(
        ProcessVariableHistoryRepository historyRepository,
        ProcessVariableHistoryRepresentationModelAssembler historyRepresentationModelAssembler,
        AlfrescoPagedModelAssembler<ProcessVariableHistoryEntity> pagedModelAssembler
    ) {
        this.historyRepository = historyRepository;
        this.historyRepresentationModelAssembler = historyRepresentationModelAssembler;
        this.pagedModelAssembler = pagedModelAssembler;
    }

    @GetMapping
    public PagedModel<EntityModel<ProcessVariableHistoryEntity>> getVariableHistory(
        @PathVariable String processInstanceId,
        Pageable pageable
    ) {
        Page<ProcessVariableHistoryEntity> history =
            historyRepository.findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(
                processInstanceId,
                pageable
            );

        return pagedModelAssembler.toModel(pageable, history, historyRepresentationModelAssembler);
    }
}
