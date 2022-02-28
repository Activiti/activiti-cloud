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
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;

/**
 * This is present in case of a future scenario where we need to filter task or process instance variables more generally rather than per task or per proc.
 */
public class ProcessVariableLookupRestrictionService {

    private final ProcessVariableRestrictionService restrictionService;

    public ProcessVariableLookupRestrictionService(
        ProcessVariableRestrictionService restrictionService
    ) {
        this.restrictionService = restrictionService;
    }

    public Predicate restrictProcessInstanceVariableQuery(Predicate predicate) {
        return restrictionService.restrictProcessInstanceVariableQuery(
            predicate,
            SecurityPolicyAccess.READ
        );
    }
}
