package org.activiti.cloud.acc.core.services.runtime.admin;

import java.util.List;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.springframework.http.ResponseEntity;

public interface ProcessVariablesRuntimeAdminService extends BaseService {

    @RequestLine("PUT /admin/v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<List<String>> updateVariables(@Param("id") String id,
                                                 SetProcessVariablesPayload setProcessVariablesPayload);
    
    @RequestLine("DELETE /admin/v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<Void> removeVariables(@Param("id") String id,
                                         RemoveProcessVariablesPayload removeProcessVariablesPayload);

}
