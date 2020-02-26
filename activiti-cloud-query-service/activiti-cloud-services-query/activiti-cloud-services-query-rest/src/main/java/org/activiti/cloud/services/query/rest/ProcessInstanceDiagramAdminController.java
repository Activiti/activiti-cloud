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
    public ProcessInstanceDiagramAdminController(ProcessModelRepository processModelRepository,
                                            BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
                                            ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                            ProcessInstanceRepository processInstanceRepository,
                                            BPMNActivityRepository bpmnActivityRepository,
                                            EntityFinder entityFinder) {
        super(processModelRepository,
              bpmnSequenceFlowRepository,
              processDiagramGenerator,
              processInstanceRepository,
              bpmnActivityRepository,
              entityFinder);
    }

    @GetMapping(produces = IMAGE_SVG_XML)
    @ResponseBody
    public String getProcessDiagram(@PathVariable String processInstanceId) {
        return generateDiagram(processInstanceId);
    }

}
