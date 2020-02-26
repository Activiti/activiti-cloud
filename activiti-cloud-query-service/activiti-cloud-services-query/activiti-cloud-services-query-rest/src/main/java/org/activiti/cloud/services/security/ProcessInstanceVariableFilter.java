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
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;

public class ProcessInstanceVariableFilter implements ProcessDefinitionKeyBasedFilter {

    @Override
    public ProcessDefinitionRestrictionProperties getRestrictionProperties() {
        QProcessInstanceEntity processInstance = QProcessVariableEntity.processVariableEntity.processInstance;
        return new ProcessDefinitionRestrictionProperties(processInstance.serviceName,
                                                          processInstance.serviceFullName,
                                                          processInstance.processDefinitionKey);
    }

    @Override
    public Predicate buildImpossiblePredicate() {
        QProcessInstanceEntity qProcessInstanceEntity = QProcessVariableEntity.processVariableEntity.processInstance;
        return qProcessInstanceEntity.id.eq("1").and(qProcessInstanceEntity.id.eq("2"));
    }
}
