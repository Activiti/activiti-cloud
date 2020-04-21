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
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ExposesResourceFor(ProcessModelEntity.class)
@RequestMapping("/admin/v1/process-definitions/{processDefinitionId}/model")
public class ProcessModelAdminController {

    private ProcessModelRepository processModelRepository;

    private EntityFinder entityFinder;

    public ProcessModelAdminController(ProcessModelRepository processModelRepository,
                                       EntityFinder entityFinder) {
        this.processModelRepository = processModelRepository;
        this.entityFinder = entityFinder;
    }

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getProcessModel(@PathVariable("processDefinitionId") String processDefinitionId) {
        return entityFinder.findById(processModelRepository,
                                     processDefinitionId,
                                     "Unable to find process model for the given id:'" + processDefinitionId + "`")
                .getProcessModelContent();
    }
}
