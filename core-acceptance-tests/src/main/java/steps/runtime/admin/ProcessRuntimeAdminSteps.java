package steps.runtime.admin;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import rest.feign.EnableRuntimeFeignContext;
import services.runtime.admin.ProcessRuntimeAdminService;

import static org.assertj.core.api.Assertions.assertThat;

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
}
