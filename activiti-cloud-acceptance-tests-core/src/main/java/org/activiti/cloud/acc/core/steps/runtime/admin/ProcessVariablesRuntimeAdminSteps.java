package org.activiti.cloud.acc.core.steps.runtime.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.ProcessVariablesRuntimeAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@EnableRuntimeFeignContext
public class ProcessVariablesRuntimeAdminSteps {

    @Autowired
    private ProcessVariablesRuntimeAdminService processVariablesRuntimeAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(processVariablesRuntimeAdminService.isServiceUp()).isTrue();
    }
    
    @Step
    ResponseEntity<List<String>> updateVariables(String id,
                                                 SetProcessVariablesPayload setProcessVariablesPayload) {
        return processVariablesRuntimeAdminService.updateVariables(id, setProcessVariablesPayload);
    }
    
    @Step
    ResponseEntity<Void> removeVariables(String id,
                                         RemoveProcessVariablesPayload removeProcessVariablesPayload) {
        return processVariablesRuntimeAdminService.removeVariables(id, removeProcessVariablesPayload);
        
    }

}
