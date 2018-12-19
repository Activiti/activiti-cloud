/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.security;

import com.querydsl.core.types.Predicate;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;

public class ProcessDefinitionRestrictionService {

    private SecurityPoliciesManager securityPoliciesManager;

    private ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder;

    private ProcessDefinitionFilter processDefinitionFilter;

    public ProcessDefinitionRestrictionService(SecurityPoliciesManager securityPoliciesManager,
                                               ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder,
                                               ProcessDefinitionFilter processDefinitionFilter) {
        this.securityPoliciesManager = securityPoliciesManager;
        this.restrictionBuilder = restrictionBuilder;
        this.processDefinitionFilter = processDefinitionFilter;
    }

    public Predicate restrictProcessDefinitionQuery(Predicate predicate,
                                                    SecurityPolicyAccess securityPolicyAccess) {
        if (!securityPoliciesManager.arePoliciesDefined()) {
            return predicate;
        }
        return restrictionBuilder.applyProcessDefinitionKeyFilter(predicate,
                                                                  securityPolicyAccess, processDefinitionFilter);
    }
}
