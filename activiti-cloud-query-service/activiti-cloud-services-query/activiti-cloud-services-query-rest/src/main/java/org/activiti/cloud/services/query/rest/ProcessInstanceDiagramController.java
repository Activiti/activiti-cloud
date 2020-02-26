package org.activiti.cloud.services.query.rest;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/process-instances/{processInstanceId}/diagram")
public class ProcessInstanceDiagramController extends ProcessInstanceDiagramControllerBase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceDiagramController.class);

    private final SecurityPoliciesManager securityPoliciesManager;
    
    private final SecurityManager securityManager;
    
    @Autowired
    public ProcessInstanceDiagramController(ProcessModelRepository processModelRepository,
                                            BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
                                            ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                            ProcessInstanceRepository processInstanceRepository,
                                            BPMNActivityRepository bpmnActivityRepository,
                                            EntityFinder entityFinder,
                                            SecurityPoliciesManager securityPoliciesManager,
                                            SecurityManager securityManager) {
        super(processModelRepository,
              bpmnSequenceFlowRepository,
              processDiagramGenerator,
              processInstanceRepository,
              bpmnActivityRepository,
              entityFinder);
        
        this.securityPoliciesManager = securityPoliciesManager;
        this.securityManager = securityManager;
    }

    @GetMapping(produces = IMAGE_SVG_XML)
    @ResponseBody
    public String getProcessDiagram(@PathVariable String processInstanceId) {
        
        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find process for the given id:'" + processInstanceId + "'");

        if (securityPoliciesManager.arePoliciesDefined() && !securityPoliciesManager.canRead(processInstanceEntity.getProcessDefinitionKey(),
                                                                                             processInstanceEntity.getServiceName())) {
            LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access definition " + processInstanceEntity.getProcessDefinitionKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processInstanceEntity.getProcessDefinitionKey());
        }

        return generateDiagram(processInstanceId);
    }

}
