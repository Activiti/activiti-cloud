package org.activiti.cloud.acc.core.steps.runtime;

import feign.FeignException;
import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessRuntimeService;
import org.activiti.cloud.acc.core.services.runtime.diagram.ProcessRuntimeDiagramService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import java.util.Collection;
import java.util.Map;

import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnableRuntimeFeignContext
public class ProcessRuntimeBundleSteps {

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private ProcessRuntimeService processRuntimeService;

    @Autowired
    private ProcessRuntimeDiagramService processRuntimeDiagramService;

    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance startProcess(String process, boolean variables ) {

        StartProcessPayloadBuilder payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(process)
                .withProcessInstanceName("processInstanceName")
                .withBusinessKey("businessKey");

        if(variables){
            payload.withVariable("test-variable-name", "test-variable-value");
        }

        return dirtyContextHandler.dirty(processRuntimeService
                .startProcess(payload.build()));
    }

    @Step
    public CloudProcessInstance startProcess(String process) {

        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(process)
                .build()));
    }

    @Step
    public CloudProcessInstance startProcessWithVariables(String process, Map<String,Object> variables) {

        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(process)
                .withVariables(variables)
                .build()));
    }

    @Step
    public void deleteProcessInstance(String id) {
        processRuntimeService.deleteProcess(id);
    }

    @Step
    public void checkProcessInstanceNotFound(String processInstanceId) {
        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> processRuntimeService.getProcessInstance(processInstanceId)
        ).withMessageContaining("Unable to find process instance for the given id:");
    }

    @Step
    public String openProcessInstanceDiagram(String id) {
        return processRuntimeDiagramService.getProcessInstanceModel(id);
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

    @Step
    public void suspendProcessInstance(String processInstanceId) {
        processRuntimeService.suspendProcess(processInstanceId);
    }

    @Step
    public void resumeProcessInstance(String processInstanceId) {
        processRuntimeService.resumeProcess(processInstanceId);
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstances(){
        return processRuntimeService.getAllProcessInstances();
    }

    @Step
    public void checkProcessInstanceIsNotPresent(String id){
        try{
            processRuntimeService.getProcessInstance(id);

        }catch (FeignException exception) {
            assertThat(exception.getMessage()).contains("Unable to find process instance for the given id:'" + id + "'");
        }
    }

    @Step
    public void getProcessInstanceById(String id){
        processRuntimeService.getProcessInstance(id);
    }

    @Step
    public PagedResources<ProcessDefinition> getProcessDefinitions(){
        return processRuntimeService.getProcessDefinitions();
    }

    @Step
    public ProcessDefinition getProcessDefinitionByKey(String key){
        return processRuntimeService.getProcessDefinitionByKey(key);
    }

    @Step
    public Collection<CloudTask> getTaskByProcessInstanceId(String processInstanceId) {
        return processRuntimeService
                .getProcessInstanceTasks(processInstanceId).getContent();
    }
}
