package steps.runtime;

import feign.FeignException;
import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import rest.RuntimeDirtyContextHandler;
import rest.feign.EnableRuntimeFeignContext;
import services.runtime.ProcessRuntimeService;
import services.runtime.diagram.ProcessRuntimeDiagramService;

import java.util.Collection;

import static helper.SvgToPng.svgToPng;
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
    public CloudProcessInstance startProcess(String process) {

        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                                                                            .start()
                                                                            .withProcessDefinitionKey(process)
                                                                            .build()));
    }

    @Step
    public CloudProcessInstance startProcessWithNameAndBusinessKey(String processName, String processInstanceName, String businessKey) {

        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                                                                            .start()
                                                                            .withProcessDefinitionKey(processName)
                                                                            .withProcessInstanceName(processInstanceName)
                                                                            .withBusinessKey(businessKey)
                                                                            .build()));
    }

    @Step
    public CloudProcessInstance startProcessWithVariables(String processName) {

        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(processName)
                .withVariable("test-variable-name", "test-variable-value")
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
    public void activateProcessInstance(String processInstanceId) {
        processRuntimeService.activateProcess(processInstanceId);
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
