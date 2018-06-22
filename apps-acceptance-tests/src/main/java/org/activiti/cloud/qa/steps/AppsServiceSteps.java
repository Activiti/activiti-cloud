package org.activiti.cloud.qa.steps;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.ApplicationDeploymentDescriptor;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.AppsService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuntimeFeignContext
public class AppsServiceSteps {

    @Autowired
    private AppsService appsService;

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
