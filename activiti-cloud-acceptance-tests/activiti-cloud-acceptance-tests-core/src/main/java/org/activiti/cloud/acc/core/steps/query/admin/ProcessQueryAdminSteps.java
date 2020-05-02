package org.activiti.cloud.acc.core.steps.query.admin;

import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.admin.ProcessModelQueryAdminService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminDiagramService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminService;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;

@EnableRuntimeFeignContext
public class ProcessQueryAdminSteps {

    @Autowired
    private ProcessQueryAdminService processQueryAdminService;

    @Autowired
    private ProcessModelQueryAdminService processModelQueryAdminService;

    @Autowired
    private ProcessQueryAdminDiagramService processQueryAdminDiagramService;

    @Step
    public void checkServicesHealth() {
        assertThat(processQueryAdminService.isServiceUp()).isTrue();
    }

    @Step
    public PagedModel<CloudProcessInstance> getAllProcessInstancesAdmin(){
        return processQueryAdminService.getProcessInstances();
    }

    @Step
    public PagedModel<CloudProcessDefinition> getAllProcessDefinitions(){
        return processQueryAdminService.getProcessDefinitions();
    }

    @Step
    public String getProcessModel(String processDefinitionId){
        return processModelQueryAdminService.getProcessModel(processDefinitionId);
    }

    @Step
    public PagedModel<CloudProcessDefinition> getProcessDefinitions(){
        return processQueryAdminService.getProcessDefinitions();
    }

    @Step
    public CollectionModel<EntityModel<CloudProcessInstance>> deleteProcessInstances(){
        return processQueryAdminService.deleteProcessInstances();
    }
    @Step
    public String getProcessInstanceDiagram(String id) {
        return processQueryAdminDiagramService.getProcessInstanceDiagram(id);
    }
    @Step
    public void checkProcessInstanceDiagram(String diagram) throws Exception {
        assertThat(diagram).isNotEmpty();
        assertThat(svgToPng(diagram.getBytes())).isNotEmpty();
    }
    @Step
    public void checkProcessInstanceNoDiagram(String diagram) {
        assertThat(diagram).isEmpty();
    }
}
