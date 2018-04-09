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

package org.activiti.cloud.services.core;

import java.util.Set;
import java.util.UUID;

import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Component;

@Component
public class SecurityPoliciesProcessDefinitionRestrictionApplier implements SecurityPoliciesRestrictionApplier<ProcessDefinitionQuery> {

    @Override
    public ProcessDefinitionQuery restrictToKeys(ProcessDefinitionQuery query,
                                                 Set<String> keys) {
        return query.processDefinitionKeys(keys);
    }

    @Override
    public ProcessDefinitionQuery denyAll(ProcessDefinitionQuery query) {
        //user should not see anything so give unsatisfiable condition
        return query.processDefinitionId("missing-" + UUID.randomUUID().toString());
    }
}
