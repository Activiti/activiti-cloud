package org.activiti.cloud.qa.service;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.qa.model.ApplicationDeploymentDescriptor;

public interface AppsService extends BaseService {

    @RequestLine("GET /v1/deployments/{appName}")
    @Headers("Content-Type: application/json")
    ApplicationDeploymentDescriptor getAppDeployments(@Param("appName") String appName);
}
