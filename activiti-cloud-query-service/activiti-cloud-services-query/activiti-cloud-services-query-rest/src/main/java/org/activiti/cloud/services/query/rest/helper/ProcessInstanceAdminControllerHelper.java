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
package org.activiti.cloud.services.query.rest.helper;

import com.querydsl.core.types.Predicate;
import java.util.List;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.ProcessInstanceAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class ProcessInstanceAdminControllerHelper {

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceAdminService processInstanceAdminService;
    private final ProcessInstanceControllerHelper processInstanceControllerHelper;

    public ProcessInstanceAdminControllerHelper(
        ProcessInstanceRepository processInstanceRepository,
        ProcessInstanceAdminService processInstanceAdminService,
        ProcessInstanceControllerHelper processInstanceControllerHelper
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceAdminService = processInstanceAdminService;
        this.processInstanceControllerHelper = processInstanceControllerHelper;
    }

    public Page<ProcessInstanceEntity> findAllProcessInstanceAdmin(Predicate predicate, Pageable pageable) {
        Page<ProcessInstanceEntity> processInstances = processInstanceAdminService.findAll(predicate, pageable);
        return processInstanceControllerHelper.mapAllSubprocesses(processInstances, pageable);
    }

    public Page<ProcessInstanceEntity> findAllProcessInstanceAdminWithVariables(
        Predicate predicate,
        List<String> variableKeys,
        Pageable pageable
    ) {
        Page<ProcessInstanceEntity> processInstances = processInstanceAdminService.findAllWithVariables(
            predicate,
            variableKeys,
            pageable
        );
        return processInstanceControllerHelper.mapAllSubprocesses(processInstances, pageable);
    }

    public ProcessInstanceEntity findByIdProcessAdmin(String processInstanceId) {
        ProcessInstanceEntity processInstance = processInstanceAdminService.findById(processInstanceId);
        return processInstanceRepository.mapSubprocesses(processInstance);
    }
}
