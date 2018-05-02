package org.activiti.cloud.qa.steps;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.ApplicationDeploymentDescriptor;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.AppsService;
import org.activiti.cloud.qa.service.AuditService;
import org.activiti.cloud.qa.service.QueryService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuntimeFeignContext
public class AppsServiceSteps {

    @Autowired
    private AppsService appsService;

    @Autowired
    private RuntimeBundleService runtimeBundleService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private AuditService auditService;

    @Step
    public void checkAnAppIsRunning() {
        assertThat(runtimeBundleService.isServiceUp()).isTrue();
        assertThat(queryService.isServiceUp()).isTrue();
        assertThat(auditService.isServiceUp()).isTrue();
    }

    @Step
    public void checkAppsServiceHealth() {
        assertThat(appsService.isServiceUp()).isTrue();
    }

    @Step
    public void checkAppsServiceShowsApp(String appName){
        ApplicationDeploymentDescriptor descriptor = appsService.getAppDeployments(appName);
        assertThat(descriptor.getApplicationName()).isEqualToIgnoringCase(appName);
        assertThat(descriptor.getServiceDeploymentDescriptors()).isNotEmpty();
    }

}
