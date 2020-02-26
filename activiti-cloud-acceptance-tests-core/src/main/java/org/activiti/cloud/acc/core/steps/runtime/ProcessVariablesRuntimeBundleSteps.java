package org.activiti.cloud.acc.core.steps.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessVariablesRuntimeService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;

@EnableRuntimeFeignContext
public class ProcessVariablesRuntimeBundleSteps {

    @Autowired
    private ProcessVariablesRuntimeService processVariablesRuntimeService;

    @Step
    public void checkServicesHealth() {
        assertThat(processVariablesRuntimeService.isServiceUp()).isTrue();
    }
    
    @Step
    public Resources<CloudVariableInstance> getVariables(String id) {
        return processVariablesRuntimeService.getVariables(id);
    }
    
    @Step
    public ResponseEntity<Void> setVariables(String id,
                                      SetProcessVariablesPayload setProcessVariablesPayload) {
        return processVariablesRuntimeService.setVariables(id, setProcessVariablesPayload);
    }
    
}
