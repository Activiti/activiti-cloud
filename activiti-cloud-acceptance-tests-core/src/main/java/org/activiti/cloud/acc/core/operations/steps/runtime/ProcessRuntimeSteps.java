package org.activiti.cloud.acc.core.operations.steps.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessRuntimeService;
import org.activiti.cloud.acc.core.services.runtime.diagram.ProcessRuntimeDiagramService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

@EnableRuntimeFeignContext
public class ProcessRuntimeSteps {

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
    public CloudProcessInstance startProcess(String processDefinitionName){

        StartProcessPayloadBuilder payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(processDefinitionName)
                .withName("processInstanceName")
                .withBusinessKey("businessKey");

        if(Serenity.sessionVariableCalled("variables") != null){
            payload.withVariable("test-variable-name", "test-variable-value");
        }

        return dirtyContextHandler.dirty(processRuntimeService
                .startProcess(payload.build()));
    }

    @Step
    public void deleteProcessInstance(String processInstanceId) {
        processRuntimeService.deleteProcess(processInstanceId);
    }

    @Step
    public void suspendProcessInstance(String processInstanceId){
        processRuntimeService.suspendProcess(processInstanceId);
    }

}
