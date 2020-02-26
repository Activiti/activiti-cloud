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

import java.util.Map;
import java.util.Set;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;

public class ProcessDefinitionKeyBasedRestrictionBuilder {

    private SecurityPoliciesManager securityPoliciesManager;

    private SecurityPoliciesProperties securityPoliciesProperties;

    public ProcessDefinitionKeyBasedRestrictionBuilder(SecurityPoliciesManager securityPoliciesManager,
                                                       SecurityPoliciesProperties securityPoliciesProperties) {
        this.securityPoliciesManager = securityPoliciesManager;
        this.securityPoliciesProperties = securityPoliciesProperties;
    }

    private BooleanExpression equalsIgnoringCaseAndHyphen(StringPath propertyPath,
                                                          String value) {
        //expect to remove hyphens when passing in environment variables
        return Expressions.stringTemplate("replace({0},'-','')",
                                          propertyPath).equalsIgnoreCase(value.replace("-",
                                                                                       ""));
    }

    private BooleanExpression buildServiceNameRestriction(String serviceName,
                                                          StringPath serviceNamePath,
                                                          StringPath serviceFullNamePath) {
        BooleanExpression appNamePredicate = equalsIgnoringCaseAndHyphen(serviceNamePath,
                                                                         serviceName);
        appNamePredicate = appNamePredicate.or(equalsIgnoringCaseAndHyphen(serviceFullNamePath,
                                                                           serviceName));
        return appNamePredicate;
    }


    public Predicate applyProcessDefinitionKeyFilter(Predicate currentPredicate,
                                                     SecurityPolicyAccess securityPolicyAccess,
                                                     ProcessDefinitionKeyBasedFilter filterMetaData) {
        ProcessDefinitionRestrictionProperties restrictionProperties = filterMetaData.getRestrictionProperties();

        Map<String, Set<String>> restrictions = securityPoliciesManager.getAllowedKeys(securityPolicyAccess);

        BooleanExpression securityExpression = null;
        for (String appName : restrictions.keySet()) {
            Set<String> defKeys = restrictions.get(appName);
            securityExpression = addProcessDefRestrictionToExpression(restrictionProperties,
                                                                      securityExpression,
                                                                      appName,
                                                                      defKeys);
        }

        //policies are defined but none are applicable
        if (securityExpression == null && securityPoliciesManager.arePoliciesDefined()) {
            //user should not see anything so give unsatisfiable condition
            return filterMetaData.buildImpossiblePredicate();
        }

        return securityExpression != null ? securityExpression.and(currentPredicate) : currentPredicate;
    }


    private BooleanExpression addProcessDefRestrictionToExpression(ProcessDefinitionRestrictionProperties restrictionProperties,
                                                                   BooleanExpression securityExpression,
                                                                   String appName,
                                                                   Set<String> defKeys) {

        BooleanExpression appNamePredicate = buildServiceNameRestriction(appName,
                                                                                                       restrictionProperties.getServiceNamePath(),
                                                                                                       restrictionProperties.getServiceFullNamePath());

        BooleanExpression nextExpression = appNamePredicate;
        //will filter by app name and will also filter by definition keys if no wildcard
        if (!defKeys.contains(securityPoliciesProperties.getWildcard())) {
            nextExpression = restrictionProperties.getProcessDefinitionKeyPath().in(defKeys).and(appNamePredicate);
        }

        if (securityExpression == null) {
            securityExpression = nextExpression;
        } else {
            securityExpression = securityExpression.or(nextExpression);
        }
        return securityExpression;
    }


}
