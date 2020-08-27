package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.rest.api.configuration.ClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "runtime",
    url = "${runtime.url}",
    path = "${runtime.path}",
    configuration = {ClientConfiguration.class},
    decode404 = true)
public interface TaskApiClient extends TaskController{

}
