package org.activiti.cloud.acc.core.steps.runtime;

import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

@EnableRuntimeFeignContext
public class ProcessRuntimeBundleSteps {

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private ProcessRuntimeService processRuntimeService;

    @Autowired
    private ProcessRuntimeDiagramService processRuntimeDiagramService;

    @Autowired
    private ObjectMapper objectMapper;

    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance startProcess(String process, boolean variables ) throws IOException {

        StartProcessPayloadBuilder payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(process)
                .withName("processInstanceName")
                .withBusinessKey("businessKey");

        if(variables){
            payload.withVariable("test-variable-name", "test-variable-value");
            payload.withVariable("test-int-variable-name", 7);
            payload.withVariable("test-bool-variable-name", true);
            payload.withVariable("test-json-variable-name",objectMapper.readTree("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}"));
            payload.withVariable("test-long-json-variable-name",objectMapper.readTree("{ \"verylongjson\":\""+ StringUtils.repeat("a", 4000)+"\"}"));
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
    public CloudProcessInstance getProcessInstanceById(String id){
        return processRuntimeService.getProcessInstance(id);
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

    @Step
    public CloudProcessInstance startProcessWithProcessInstanceName(String process,
                                                                    String processName) {
        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                .start()
                .withName(processName)
                .withProcessDefinitionKey(process)
                .build()));
    }

    @Step
    public void checkProcessInstanceName(String processInstanceId,
                                         String processInstanceName) {
        assertThat(processRuntimeService.getProcessInstance(processInstanceId).getName()).isEqualTo(processInstanceName);
    }    

  
    @Step
    public CloudProcessInstance setProcessName(String processInstanceId, String processInstanceName){
        return processRuntimeService.updateProcess(
                processInstanceId,
                ProcessPayloadBuilder.update().withName(processInstanceName).build());

    }
}
