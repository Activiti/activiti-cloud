package org.activiti.cloud.services.query.rest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity.BPMNActivityStatus;
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
    public ProcessInstanceDiagramControllerBase(ProcessModelRepository processModelRepository,
                                            BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
                                            ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                            ProcessInstanceRepository processInstanceRepository,
                                            BPMNActivityRepository bpmnActivityRepository,
                                            EntityFinder entityFinder) {
        
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

        if(!bpmnModel.hasDiagramInterchangeInfo())
            new BpmnAutoLayout(bpmnModel).execute();
        
        List<String> highLightedActivities = resolveStartedActivitiesIds(processInstanceId);
        List<String> highLightedFlows = resolveCompletedFlows(bpmnModel, processInstanceId);

        return new String(processDiagramGenerator.generateDiagram(bpmnModel,
                                                                  highLightedActivities,
                                                                  highLightedFlows),
                          StandardCharsets.UTF_8);
    }

    protected List<String> resolveCompletedFlows(BpmnModel bpmnModel, String processInstanceId) {
        List<String> completedFlows = bpmnSequenceFlowRepository.findByProcessInstanceId(processInstanceId)
                                                                .stream()
                                                                .map(BPMNSequenceFlowEntity::getElementId)
                                                                .distinct()
                                                                .collect(Collectors.toList());
        return completedFlows;
    }

    protected List<String> resolveStartedActivitiesIds(String processInstanceId) {
        return bpmnActivityRepository.findByProcessInstanceIdAndStatus(processInstanceId, BPMNActivityStatus.STARTED)
                                     .stream()
                                     .map(BPMNActivityEntity::getElementId)
                                     .distinct()
                                     .collect(Collectors.toList());
    }

    protected String resolveProcessDefinitionId(String processInstanceId) {

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find process instance for the given id:'" + processInstanceId + "'");

        return processInstanceEntity.getProcessDefinitionId();
    }

    protected BpmnModel getBpmnModel(String processDefinitionId) {
        ProcessModelEntity processModelEntity = entityFinder.findById(processModelRepository,
                                                                      processDefinitionId,
                                                                      "Unable to find process model for the given id:'" + processDefinitionId + "`");

        String processModelContent = processModelEntity.getProcessModelContent();

        return processDiagramGenerator.parseBpmnModelXml(new ByteArrayInputStream(processModelContent.getBytes()));
    }

}
