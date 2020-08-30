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

import javax.validation.constraints.NotNull;

import org.activiti.cloud.services.query.model.QBPMNActivityEntity;

import com.querydsl.core.types.Predicate;

public class ServiceTasksFilter implements QueryDslPredicateFilter {

    public static final String SERVICE_TASK = "serviceTask";

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        return QBPMNActivityEntity.bPMNActivityEntity.activityType.eq(SERVICE_TASK)
                                                                  .and(currentPredicate);
    }
}
