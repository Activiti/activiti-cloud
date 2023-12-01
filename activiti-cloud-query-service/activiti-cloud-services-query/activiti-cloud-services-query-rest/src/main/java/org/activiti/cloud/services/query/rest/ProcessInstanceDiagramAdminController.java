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

import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/process-instances/{processInstanceId}/diagram")
public class ProcessInstanceDiagramAdminController extends ProcessInstanceDiagramControllerBase {

    @Autowired
    public ProcessInstanceDiagramAdminController(
        ProcessModelRepository processModelRepository,
        BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
        ProcessDiagramGeneratorWrapper processDiagramGenerator,
        ProcessInstanceRepository processInstanceRepository,
        BPMNActivityRepository bpmnActivityRepository,
        EntityFinder entityFinder
    ) {
        super(
            processModelRepository,
            bpmnSequenceFlowRepository,
            processDiagramGenerator,
            processInstanceRepository,
            bpmnActivityRepository,
            entityFinder
        );
    }

    @GetMapping(produces = IMAGE_SVG_XML)
    @ResponseBody
    public String getProcessDiagramAdmin(@PathVariable String processInstanceId) {
        return generateDiagram(processInstanceId);
    }
}
