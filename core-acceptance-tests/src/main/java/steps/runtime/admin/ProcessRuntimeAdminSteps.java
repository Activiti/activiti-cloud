package steps.runtime.admin;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import services.runtime.admin.ProcessRuntimeAdminService;

public class ProcessRuntimeAdminSteps {

    @Autowired
    private ProcessRuntimeAdminService processRuntimeAdminService;

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstancesAdmin(){
        return processRuntimeAdminService.getAllProcessInstancesAdmin();
    }
}
