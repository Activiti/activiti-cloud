package org.activiti.cloud.acc.core.services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;

public interface ProcessVariablesRuntimeService extends BaseService {
    
    @RequestLine("GET /v1/process-instances/{id}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    Resources<CloudVariableInstance> getVariables(@Param("id") String id);
    
    @RequestLine("POST /v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<Void> setVariables(@Param("id") String id,
                                      SetProcessVariablesPayload setProcessVariablesPayload); 

}
