/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.querydsl.core.types.dsl.StringPath;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;

public class ProcessInstanceRestrictionService {

    private SecurityPoliciesManager securityPoliciesManager;

    private ProcessInstanceFilter processInstanceFilter;

    private ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder;

    private SecurityManager securityManager;

    public ProcessInstanceRestrictionService(
        SecurityPoliciesManager securityPoliciesManager,
        ProcessInstanceFilter processInstanceFilter,
        ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder,
        SecurityManager securityManager
    ) {
        this.securityPoliciesManager = securityPoliciesManager;
        this.processInstanceFilter = processInstanceFilter;
        this.restrictionBuilder = restrictionBuilder;
        this.securityManager = securityManager;
    }

    public Predicate restrictProcessInstanceQuery(Predicate predicate, SecurityPolicyAccess securityPolicyAccess) {
        Predicate initiatorPredicate = applyInvolvedRestriction(predicate);

        if (!securityPoliciesManager.arePoliciesDefined()) {
            return initiatorPredicate;
        }

        return restrictionBuilder.applyProcessDefinitionKeyFilter(
            initiatorPredicate,
            securityPolicyAccess,
            processInstanceFilter
        );
    }

    private Predicate applyInvolvedRestriction(Predicate predicate) {
        String userId = securityManager.getAuthenticatedUserId();

        if (userId == null) {
            return predicate;
        }

        StringPath initiatorPath = QProcessInstanceEntity.processInstanceEntity.initiator;
        BooleanExpression assigneeExpression = QProcessInstanceEntity.processInstanceEntity.tasks
            .any()
            .assignee.eq(userId);
        BooleanExpression candidateExpression = QProcessInstanceEntity.processInstanceEntity.tasks
            .any()
            .taskCandidateUsers.any()
            .userId.eq(userId);
        return initiatorPath.eq(userId).or(assigneeExpression).or(candidateExpression).and(predicate);
    }
}
