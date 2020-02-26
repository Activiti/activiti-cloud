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
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;

public class ProcessVariableRestrictionService {

    private SecurityPoliciesManager securityPoliciesManager;

    private ProcessInstanceVariableFilter processInstanceVariableFilter;

    private ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder;

    public ProcessVariableRestrictionService(SecurityPoliciesManager securityPoliciesManager,
                                             ProcessInstanceVariableFilter processInstanceVariableFilter,
                                             ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder) {
        this.securityPoliciesManager = securityPoliciesManager;
        this.processInstanceVariableFilter = processInstanceVariableFilter;
        this.restrictionBuilder = restrictionBuilder;
    }

    public Predicate restrictProcessInstanceVariableQuery(Predicate predicate,
                                                          SecurityPolicyAccess securityPolicyAccess) {
        if (!securityPoliciesManager.arePoliciesDefined()) {
            return predicate;
        }

        QProcessInstanceEntity processInstance = QProcessVariableEntity.processVariableEntity.processInstance;

        BooleanExpression varIsProcInstVar = processInstance.isNotNull();

        Predicate extendedPredicate = varIsProcInstVar;
        if (predicate != null) {
            extendedPredicate = varIsProcInstVar.and(predicate);
        }

        return restrictionBuilder.applyProcessDefinitionKeyFilter(extendedPredicate,
                                                 securityPolicyAccess,
                                                                  processInstanceVariableFilter);
    }

}
