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

import java.util.HashMap;
import java.util.List;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.cmd.RemoveProcessVariables;
import org.activiti.runtime.api.cmd.ResumeProcess;
import org.activiti.runtime.api.cmd.SendSignal;
import org.activiti.runtime.api.cmd.SetProcessVariables;
import org.activiti.runtime.api.cmd.StartProcess;
import org.activiti.runtime.api.cmd.SuspendProcess;
import org.activiti.runtime.api.model.FluentProcessDefinition;
import org.activiti.runtime.api.model.FluentProcessInstance;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.query.ProcessInstanceFilter;
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

        ProcessInstanceFilter filter = securityService.restrictProcessInstQuery(SecurityPolicy.READ);

        return springPageConverter.toSpringPage(pageable,
                                                processRuntime.processInstances(springPageConverter.toAPIPageable(pageable),
                                                                                filter));
    }

    public Page<ProcessInstance> getAllProcessInstances(Pageable pageable) {

        return springPageConverter.toSpringPage(pageable,
                                                processRuntime.processInstances(springPageConverter.toAPIPageable(pageable)));
    }

    public ProcessInstance startProcess(StartProcess cmd) {

        FluentProcessDefinition processDefinition;
        if (cmd.getProcessDefinitionKey() != null) {
            processDefinition = processRuntime.processDefinitionByKey(cmd.getProcessDefinitionKey());
        } else {
            processDefinition = processRuntime.processDefinitionById(cmd.getProcessDefinitionId());
        }

        if (!securityService.canWrite(processDefinition.getKey())) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinition.getKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinition.getKey());
        }

        return processDefinition.startProcessWith()
                .variables(cmd.getVariables())
                .businessKey(cmd.getBusinessKey())
                .doIt();
    }

    public FluentProcessInstance getAuthorizedProcessInstanceById(String processInstanceId) {
        FluentProcessInstance processInstance = processRuntime.processInstance(processInstanceId);
        if (processInstance == null || !securityService.canRead(processInstance.getProcessDefinitionKey())) {
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + processInstanceId + "'");
        }
        return processInstance;
    }

    public void signal(SendSignal signaCmd) {
        //TODO: plan is to restrict access to events using a new security policy on events
        // - that's another piece of work though so for now no security here

        processRuntime.sendSignalWith()
                .name(signaCmd.getName())
                .variables(signaCmd.getInputVariables())
                .doIt();
    }

    private FluentProcessInstance verifyCanWriteToProcessInstance(String processInstanceId) {
        FluentProcessInstance processInstance = getAuthorizedProcessInstanceById(processInstanceId);

        String processDefinitionKey = processInstance.getProcessDefinitionKey();
        if (!securityService.canWrite(processDefinitionKey)) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinitionKey);
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinitionKey);
        }

        return processInstance;
    }

    public void suspend(SuspendProcess suspendProcessInstanceCmd) {
        FluentProcessInstance processInstance = verifyCanWriteToProcessInstance(suspendProcessInstanceCmd.getProcessInstanceId());
        processInstance.suspend();
    }

    public void activate(ResumeProcess resumeProcess) {
        FluentProcessInstance processInstance = verifyCanWriteToProcessInstance(resumeProcess.getProcessInstanceId());
        processInstance.resume();
    }

    public void setProcessVariables(SetProcessVariables setProcessVariablesCmd) {
        FluentProcessInstance processInstance = getAuthorizedProcessInstanceById(setProcessVariablesCmd.getProcessInstanceId());
        verifyCanWriteToProcessInstance(processInstance.getId());
        processInstance.variables(new HashMap<>(setProcessVariablesCmd.getVariables()));
    }

    public void deleteProcessInstance(String processInstanceId) {
        FluentProcessInstance processInstance = verifyCanWriteToProcessInstance(processInstanceId);
        processInstance.delete("Cancelled by " + authenticationWrapper.getAuthenticatedUserId());
    }

    public void removeProcessVariables(RemoveProcessVariables removeProcessVariablesCmd) {
        FluentProcessInstance processInstance = verifyCanWriteToProcessInstance(removeProcessVariablesCmd.getProcessInstanceId());
        processInstance.removeVariables(removeProcessVariablesCmd.getVariableNames());
    }

    public List<VariableInstance> getVariableInstances(String processInstanceId) {
        FluentProcessInstance processInstance = getAuthorizedProcessInstanceById(processInstanceId);
        return processRuntime.processInstance(processInstance.getId()).variables();
    }

    public List<VariableInstance> getLocalVariableInstances(String processInstanceId) {
        FluentProcessInstance processInstance = getAuthorizedProcessInstanceById(processInstanceId);
        return processRuntime.processInstance(processInstance.getId()).localVariables();
    }

}
