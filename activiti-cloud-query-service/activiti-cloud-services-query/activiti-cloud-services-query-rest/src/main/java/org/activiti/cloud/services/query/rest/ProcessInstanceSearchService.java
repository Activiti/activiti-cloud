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
package org.activiti.cloud.services.query.rest;

import java.util.Set;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class ProcessInstanceSearchService {

    private final ProcessInstanceRepository processInstanceRepository;

    private final ProcessVariableService processVariableService;

    private final SecurityManager securityManager;

    public ProcessInstanceSearchService(
        ProcessInstanceRepository processInstanceRepository,
        ProcessVariableService processVariableService,
        SecurityManager securityManager
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processVariableService = processVariableService;
        this.securityManager = securityManager;
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchRestricted(ProcessInstanceSearchRequest searchRequest, Pageable pageable) {
        return search(
            searchRequest.processVariableKeys(),
            pageable,
            ProcessInstanceSpecification.restricted(searchRequest, securityManager.getAuthenticatedUserId())
        );
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchUnrestricted(
        ProcessInstanceSearchRequest searchRequest,
        Pageable pageable
    ) {
        return search(
            searchRequest.processVariableKeys(),
            pageable,
            ProcessInstanceSpecification.unrestricted(searchRequest)
        );
    }

    /**
     * @param processVariableKeys the process variables to fetch for each process instance, each represented by process definition key and variable name
     * @param pageable the page request. N.B. the sort contained in this pageable will be ignored and the sort from the search request will be used instead
     * @param specification the specification to use for the search. It includes the sorting parameter.
     * @return the page of process instances
     */
    private Page<ProcessInstanceEntity> search(
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable,
        ProcessInstanceSpecification specification
    ) {
        Page<ProcessInstanceEntity> processInstances = processInstanceRepository.findAll(specification, pageable);
        processVariableService.fetchProcessVariablesForProcessInstances(
            processInstances.getContent(),
            processVariableKeys
        );
        return processInstances;
    }
}
