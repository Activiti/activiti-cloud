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

import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.payloads.GetProcessDefinitionsPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SecurityAwareRepositoryService {

    private final SecurityPoliciesApplicationService securityService;

    private final ProcessRuntime processRuntime;

    private final SpringPageConverter pageConverter;

    @Autowired
    public SecurityAwareRepositoryService(SecurityPoliciesApplicationService securityPolicyApplicationService,
                                          ProcessRuntime processRuntime,
                                          SpringPageConverter pageConverter) {
        this.securityService = securityPolicyApplicationService;
        this.processRuntime = processRuntime;
        this.pageConverter = pageConverter;
    }

    public Page<ProcessDefinition> getAuthorizedProcessDefinitions(Pageable pageable) {
        GetProcessDefinitionsPayload getProcessDefinitionsPayload = securityService.restrictProcessDefQuery(SecurityPolicy.READ);

        return pageConverter.toSpringPage(pageable,
                                          processRuntime.processDefinitions(pageConverter.toAPIPageable(pageable),
                                                                            getProcessDefinitionsPayload));
    }

    public Page<ProcessDefinition> getAllProcessDefinitions(Pageable pageable) {
        return pageConverter.toSpringPage(pageable,
                                          processRuntime.processDefinitions(pageConverter.toAPIPageable(pageable)));
    }

    public ProcessDefinition getAuthorizedProcessDefinition(String processDefinitionId) {
        ProcessDefinition processDefinition = processRuntime.processDefinition(processDefinitionId);
        if (!securityService.canRead(processDefinition.getKey())) {
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + processDefinitionId + "'");
        }
        return processDefinition;
    }
}
