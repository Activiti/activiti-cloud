/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.rest;

import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@ExposesResourceFor(ProcessModelEntity.class)
@RequestMapping("/v1/process-definitions/{processDefinitionId}/model")
public class ProcessModelController {

    private ProcessModelRepository processModelRepository;

    private EntityFinder entityFinder;

    private SecurityPoliciesManager securityPoliciesManager;

    public ProcessModelController(ProcessModelRepository processModelRepository,
                                  EntityFinder entityFinder,
                                  SecurityPoliciesManager securityPoliciesManager) {
        this.processModelRepository = processModelRepository;
        this.entityFinder = entityFinder;
        this.securityPoliciesManager = securityPoliciesManager;
    }

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getProcessModel(@PathVariable("processDefinitionId") String processDefinitionId) {
        ProcessModelEntity processModelEntity = entityFinder.findById(processModelRepository,
                                                                      processDefinitionId,
                                                                      "Unable to find process model for the given id:'" + processDefinitionId + "`");
        if (securityPoliciesManager.arePoliciesDefined() && !securityPoliciesManager.canRead(processModelEntity.getProcessDefinition().getKey(),
                                                                                             processModelEntity.getProcessDefinition().getServiceName())) {
            throw new ActivitiForbiddenException("Operation not permitted for " + processModelEntity.getProcessDefinition().getKey());
        }
        return processModelEntity
                .getProcessModelContent();
    }
}
