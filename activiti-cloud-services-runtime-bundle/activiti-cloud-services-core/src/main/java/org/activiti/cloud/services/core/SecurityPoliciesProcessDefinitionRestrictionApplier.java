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

import org.activiti.runtime.api.query.ProcessDefinitionFilter;
import org.springframework.stereotype.Component;

@Component
public class SecurityPoliciesProcessDefinitionRestrictionApplier implements SecurityPoliciesRestrictionApplier<ProcessDefinitionFilter> {

    @Override
    public ProcessDefinitionFilter restrictToKeys(Set<String> keys) {
        return ProcessDefinitionFilter.filterOnKeys(keys);
    }

    @Override
    public ProcessDefinitionFilter denyAll() {
        //user should not see anything so give unsatisfiable condition
        return ProcessDefinitionFilter.filterOnKey("missing-" + UUID.randomUUID().toString());
    }

    @Override
    public ProcessDefinitionFilter allowAll() {
        return ProcessDefinitionFilter.unfiltered();
    }
}
