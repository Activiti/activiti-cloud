package org.activiti.cloud.acc.core.steps.query;

import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.ProcessModelQueryService;
import org.activiti.cloud.acc.core.services.query.ProcessQueryDiagramService;
import org.activiti.cloud.acc.core.services.query.ProcessQueryService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;

@EnableRuntimeFeignContext
public class ProcessQuerySteps {

    @Autowired
    private ProcessQueryService processQueryService;

    @Autowired
    private ProcessModelQueryService processModelQueryService;

    @Autowired
    private ProcessQueryDiagramService processQueryDiagramService;

    @Step
    public void checkServicesHealth() {
        assertThat(processQueryService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance getProcessInstance(String processInstanceId) {
        await().untilAsserted(() ->
            assertThat(
                catchThrowable(() -> processQueryService.getProcessInstance(processInstanceId))
            ).isNull());
        return processQueryService.getProcessInstance(processInstanceId);
    }

    @Step
    public PagedModel<CloudProcessInstance> getAllProcessInstances() {
        return processQueryService.getProcessInstances();
    }

    @Step
    public void checkProcessInstanceStatus(String processInstanceId,
        ProcessInstance.ProcessInstanceStatus expectedStatus) {
        assertThat(expectedStatus).isNotNull();

        await().untilAsserted(() -> {
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
            final Collection<CloudVariableInstance> variableInstances = processQueryService
                .getProcessInstanceVariables(processInstanceId).getContent();
            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();
            //one of the variables should have name matching variableName
            assertThat(variableInstances).extracting(VariableInstance::getName)
                .contains(variableName);
        });
    }

    @Step
    public void checkProcessInstanceHasVariableValue(String processInstanceId, String variableName,
        Object variableValue) {

        await().untilAsserted(() -> {
            assertThat(variableName).isNotNull();
            final Collection<CloudVariableInstance> variableInstances = processQueryService
                .getProcessInstanceVariables(processInstanceId).getContent();
            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();
            //one of the variables should have name matching variableName and value
            assertThat(variableInstances)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .contains(tuple(variableName, variableValue));
        });
    }

    @Step
    public void checkProcessInstanceName(String processInstanceId,
        String processInstanceName) {
        await().untilAsserted(
            () -> assertThat(processQueryService.getProcessInstance(processInstanceId).getName())
                .isNotNull()
                .isEqualTo(processInstanceName));
    }

    @Step
    public PagedModel<ProcessDefinition> getProcessDefinitions() {
        return processQueryService.getProcessDefinitions();
    }

    @Step
    public String getProcessModel(String processDefinitionId) {
        return processModelQueryService.getProcessModel(processDefinitionId);
    }

    @Step
    public PagedModel<CloudProcessInstance> getProcessInstancesByName(String processName) {
        return processQueryService.getProcessInstancesByName(processName);
    }

    @Step
    public PagedModel<CloudProcessInstance> getProcessInstancesByProcessDefinitionKey(
        String processDefinitionKey) {
        return processQueryService.getProcessInstancesByProcessDefinitionKey(processDefinitionKey);
    }

    @Step
    public String getProcessInstanceDiagram(String id) {
        await().untilAsserted(() ->
            assertThat(
                catchThrowable(() ->
                    processQueryDiagramService.getProcessInstanceDiagram(id)
                )
            ).isNull());
        return processQueryDiagramService.getProcessInstanceDiagram(id);
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
