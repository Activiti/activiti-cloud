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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ProcessInstanceDiagramControllerBase {

    public static final String IMAGE_SVG_XML = "image/svg+xml";

    protected final ProcessModelRepository processModelRepository;

    protected final BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    protected final EntityFinder entityFinder;

    protected final ProcessInstanceRepository processInstanceRepository;

    protected final BPMNActivityRepository bpmnActivityRepository;

    protected final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @Autowired
    public ProcessInstanceDiagramControllerBase(
        ProcessModelRepository processModelRepository,
        BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
        ProcessDiagramGeneratorWrapper processDiagramGenerator,
        ProcessInstanceRepository processInstanceRepository,
        BPMNActivityRepository bpmnActivityRepository,
        EntityFinder entityFinder
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processModelRepository = processModelRepository;
        this.entityFinder = entityFinder;
        this.processDiagramGenerator = processDiagramGenerator;
        this.bpmnActivityRepository = bpmnActivityRepository;
        this.bpmnSequenceFlowRepository = bpmnSequenceFlowRepository;
    }

    public String generateDiagram(String processInstanceId) {
        String processDefinitionId = resolveProcessDefinitionId(processInstanceId);
        BpmnModel bpmnModel = getBpmnModel(processDefinitionId);

        if (!bpmnModel.hasDiagramInterchangeInfo()) new BpmnAutoLayout(bpmnModel).execute();

        List<String> highLightedActivities = resolveCompletedActivitiesIds(processInstanceId);
        List<String> highLightedFlows = resolveCompletedFlows(bpmnModel, processInstanceId);
        List<String> currentActivities = resolveStartedActivitiesIds(processInstanceId);
        List<String> erroredActivities = resolveErroredActivitiesIds(processInstanceId);

        return new String(
            processDiagramGenerator.generateDiagram(
                bpmnModel,
                highLightedActivities,
                highLightedFlows,
                currentActivities,
                erroredActivities
            ),
            StandardCharsets.UTF_8
        );
    }

    protected List<String> resolveCompletedFlows(BpmnModel bpmnModel, String processInstanceId) {
        List<String> completedFlows = bpmnSequenceFlowRepository
            .findByProcessInstanceId(processInstanceId)
            .stream()
            .map(BPMNSequenceFlowEntity::getElementId)
            .distinct()
            .collect(Collectors.toList());
        return completedFlows;
    }

    protected List<String> resolveStartedActivitiesIds(String processInstanceId) {
        return bpmnActivityRepository
            .findByProcessInstanceIdAndStatus(processInstanceId, CloudBPMNActivity.BPMNActivityStatus.STARTED)
            .stream()
            .map(BPMNActivityEntity::getElementId)
            .distinct()
            .collect(Collectors.toList());
    }

    protected List<String> resolveCompletedActivitiesIds(String processInstanceId) {
        return bpmnActivityRepository
            .findByProcessInstanceIdAndStatus(processInstanceId, BPMNActivityStatus.COMPLETED)
            .stream()
            .map(BPMNActivityEntity::getElementId)
            .distinct()
            .collect(Collectors.toList());
    }

    protected List<String> resolveErroredActivitiesIds(String processInstanceId) {
        return bpmnActivityRepository
            .findByProcessInstanceIdAndStatus(processInstanceId, BPMNActivityStatus.ERROR)
            .stream()
            .map(BPMNActivityEntity::getElementId)
            .distinct()
            .collect(Collectors.toList());
    }

    protected String resolveProcessDefinitionId(String processInstanceId) {
        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(
            processInstanceRepository,
            processInstanceId,
            "Unable to find process instance for the given id:'" + processInstanceId + "'"
        );

        return processInstanceEntity.getProcessDefinitionId();
    }

    protected BpmnModel getBpmnModel(String processDefinitionId) {
        ProcessModelEntity processModelEntity = entityFinder.findById(
            processModelRepository,
            processDefinitionId,
            "Unable to find process model for the given id:'" + processDefinitionId + "`"
        );

        String processModelContent = processModelEntity.getProcessModelContent();

        return processDiagramGenerator.parseBpmnModelXml(new ByteArrayInputStream(processModelContent.getBytes()));
    }
}
