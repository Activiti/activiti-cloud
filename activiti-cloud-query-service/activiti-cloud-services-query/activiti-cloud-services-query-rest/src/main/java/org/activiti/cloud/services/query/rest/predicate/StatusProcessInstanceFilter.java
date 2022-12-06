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
package org.activiti.cloud.services.query.rest.predicate;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import javax.validation.constraints.NotNull;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.springframework.util.StringUtils;

public class StatusProcessInstanceFilter implements QueryDslPredicateFilter {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StatusProcessInstanceFilter(String status){
        this.status = status;
    }

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        Predicate extendedPredicate = currentPredicate;
        if (StringUtils.hasText(status)) {
            BooleanExpression processInstanceIdNull = QProcessInstanceEntity.processInstanceEntity.status.eq(ProcessInstanceStatus.valueOf(status));
            extendedPredicate = processInstanceIdNull.and(currentPredicate);
        }
        return extendedPredicate;
    }
}
