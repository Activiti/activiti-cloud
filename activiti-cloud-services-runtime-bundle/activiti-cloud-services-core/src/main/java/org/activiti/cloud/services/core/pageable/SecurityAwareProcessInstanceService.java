/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.core.pageable;

import java.util.List;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.ProcessInstanceMeta;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.model.payloads.DeleteProcessPayload;
import org.activiti.runtime.api.model.payloads.GetProcessInstancesPayload;
import org.activiti.runtime.api.model.payloads.GetVariablesPayload;
import org.activiti.runtime.api.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.payloads.SetProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.activiti.runtime.api.model.payloads.StartProcessPayload;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SecurityAwareProcessInstanceService {

    private final ProcessRuntime processRuntime;

    private final SecurityPoliciesApplicationService securityService;

    private final SpringPageConverter springPageConverter;

    private final SpringSecurityAuthenticationWrapper authenticationWrapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAwareProcessInstanceService.class);

    public SecurityAwareProcessInstanceService(ProcessRuntime processRuntime,
                                               SecurityPoliciesApplicationService securityPolicyApplicationService,
                                               SpringPageConverter springPageConverter,
                                               SpringSecurityAuthenticationWrapper authenticationWrapper) {
        this.processRuntime = processRuntime;
        this.securityService = securityPolicyApplicationService;
        this.springPageConverter = springPageConverter;
        this.authenticationWrapper = authenticationWrapper;
    }

    public Page<ProcessInstance> getAuthorizedProcessInstances(Pageable pageable) {

        GetProcessInstancesPayload getProcessInstancesPayload = securityService.restrictProcessInstQuery(SecurityPolicy.READ);

        return springPageConverter.toSpringPage(pageable,
                                                processRuntime.processInstances(springPageConverter.toAPIPageable(pageable),
                                                                                getProcessInstancesPayload));
    }

    public Page<ProcessInstance> getAllProcessInstances(Pageable pageable) {

        return springPageConverter.toSpringPage(pageable,
                                                processRuntime.processInstances(springPageConverter.toAPIPageable(pageable)));
    }

    public ProcessInstanceMeta processInstanceMeta(String processInstanceId) {
        return processRuntime.processInstanceMeta(processInstanceId);
    }

    public ProcessInstance startProcess(StartProcessPayload startProcessPayload) {

        ProcessDefinition processDefinition = null;
        if (startProcessPayload.getProcessDefinitionId() != null) {
            processDefinition = processRuntime.processDefinition(startProcessPayload.getProcessDefinitionId());
        }
        if (processDefinition == null && startProcessPayload.getProcessDefinitionKey() != null) {
            processDefinition = processRuntime.processDefinition(startProcessPayload.getProcessDefinitionKey());
        }
        if (processDefinition == null) {
            throw new IllegalStateException("At least Process Definition Id or Key needs to be provided to start a process");
        }
        if (!securityService.canWrite(processDefinition.getKey())) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinition.getKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinition.getKey());
        }
        return processRuntime.start(startProcessPayload);
    }

    public ProcessInstance getAuthorizedProcessInstanceById(String processInstanceId) {
        ProcessInstance processInstance = processRuntime.processInstance(processInstanceId);
        if (processInstance == null || !securityService.canRead(processInstance.getProcessDefinitionKey())) {
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + processInstanceId + "'");
        }
        return processInstance;
    }

    public void signal(SignalPayload signalPayload) {
        //TODO: plan is to restrict access to events using a new security policy on events
        // - that's another piece of work though so for now no security here

        processRuntime.signal(signalPayload);
    }

    private ProcessInstance verifyCanWriteToProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = getAuthorizedProcessInstanceById(processInstanceId);

        String processDefinitionKey = processInstance.getProcessDefinitionKey();
        if (!securityService.canWrite(processDefinitionKey)) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinitionKey);
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinitionKey);
        }

        return processInstance;
    }

    public ProcessInstance suspend(SuspendProcessPayload suspendProcessPayload) {
        ProcessInstance processInstance = verifyCanWriteToProcessInstance(suspendProcessPayload.getProcessInstanceId());
        return processRuntime.suspend(suspendProcessPayload);
    }

    public ProcessInstance activate(ResumeProcessPayload resumeProcessPayload) {
        ProcessInstance processInstance = verifyCanWriteToProcessInstance(resumeProcessPayload.getProcessInstanceId());
        return processRuntime.resume(resumeProcessPayload);
    }

    public void setProcessVariables(SetProcessVariablesPayload setVariablesPayload) {
        ProcessInstance processInstance = getAuthorizedProcessInstanceById(setVariablesPayload.getProcessInstanceId());
        verifyCanWriteToProcessInstance(processInstance.getId());
        processRuntime.setVariables(setVariablesPayload);
    }

    public ProcessInstance deleteProcessInstance(DeleteProcessPayload deleteProcessPayload) {
        ProcessInstance processInstance = verifyCanWriteToProcessInstance(deleteProcessPayload.getProcessInstanceId());
        return processRuntime.delete(deleteProcessPayload);
    }

    public void removeProcessVariables(RemoveProcessVariablesPayload removeVariablesPayload) {
        ProcessInstance processInstance = verifyCanWriteToProcessInstance(removeVariablesPayload.getProcessInstanceId());
        processRuntime.removeVariables(removeVariablesPayload);
    }

    public List<VariableInstance> getVariableInstances(GetVariablesPayload getVariablesPayload) {
        ProcessInstance processInstance = getAuthorizedProcessInstanceById(getVariablesPayload.getProcessInstanceId());
        return processRuntime.variables(getVariablesPayload);
    }
}
