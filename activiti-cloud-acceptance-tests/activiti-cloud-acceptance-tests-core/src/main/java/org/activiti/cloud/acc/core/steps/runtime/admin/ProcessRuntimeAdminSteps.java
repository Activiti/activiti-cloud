package org.activiti.cloud.acc.core.steps.runtime.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.ProcessRuntimeAdminService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import java.io.IOException;

@EnableRuntimeFeignContext
public class ProcessRuntimeAdminSteps {

    @Autowired
    private ProcessRuntimeAdminService processRuntimeAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeAdminService.isServiceUp()).isTrue();
    }

    @Step
    public PagedResources<CloudProcessInstance> getProcessInstances(){
        return processRuntimeAdminService.getProcessInstances();
    }
    
    @Step
    public void deleteProcessInstance(String id) {
        processRuntimeAdminService.deleteProcess(id);
    }
    
    @Step
    public CloudProcessInstance message(StartMessagePayload payload) throws IOException {
        return processRuntimeAdminService.message(payload);
    }
    
    @Step
    public void message(ReceiveMessagePayload payload) throws IOException {
        processRuntimeAdminService.message(payload);
    }

}
