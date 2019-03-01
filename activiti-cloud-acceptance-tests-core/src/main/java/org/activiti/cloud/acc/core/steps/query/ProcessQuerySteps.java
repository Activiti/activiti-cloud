package org.activiti.cloud.acc.core.steps.query;

import java.util.Collection;

import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.ProcessModelQueryService;
import org.activiti.cloud.acc.core.services.query.ProcessQueryService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnableRuntimeFeignContext
public class ProcessQuerySteps {

    @Autowired
    private ProcessQueryService processQueryService;

    @Autowired
    private ProcessModelQueryService processModelQueryService;

    @Step
    public void checkServicesHealth() {
        assertThat(processQueryService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance getProcessInstance(String processInstanceId){
        return processQueryService.getProcessInstance(processInstanceId);
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstances(){
        return processQueryService.getProcessInstances();
    }

    @Step
    public void checkProcessInstanceStatus(String processInstanceId,
                                           ProcessInstance.ProcessInstanceStatus expectedStatus) {

        await().untilAsserted(() -> {

            assertThat(expectedStatus).isNotNull();
            CloudProcessInstance processInstance = getProcessInstance(processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getStatus()).isEqualTo(expectedStatus);
            assertThat(processInstance.getServiceName()).isNotEmpty();
            assertThat(processInstance.getServiceFullName()).isNotEmpty();

        });
    }

    @Step
    public void checkProcessInstanceHasVariable(String processInstanceId, String variableName) {

        await().untilAsserted(() -> {
            assertThat(variableName).isNotNull();
            final Collection<CloudVariableInstance> variableInstances = processQueryService.getProcessInstanceVariables(processInstanceId).getContent();
            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();
            //one of the variables should have name matching variableName
            assertThat(variableInstances).extracting(VariableInstance::getName).contains(variableName);
        });
    }

    @Step
    public void checkProcessInstanceName(String processInstanceId,
                                         String processInstanceName) {
        await().untilAsserted(() -> assertThat(processQueryService.getProcessInstance(processInstanceId).getName())
                .isNotNull()
                .isEqualTo(processInstanceName));
    }

    @Step
    public PagedResources<ProcessDefinition> getProcessDefinitions(){
        return processQueryService.getProcessDefinitions();
    }

    @Step
    public String getProcessModel(String processDefinitionId){
        return processModelQueryService.getProcessModel(processDefinitionId);
    }

    @Step
    public PagedResources<CloudProcessInstance> getProcessInstancesByName(String processName){
        return processQueryService.getProcessInstancesByName(processName);
    }

}
