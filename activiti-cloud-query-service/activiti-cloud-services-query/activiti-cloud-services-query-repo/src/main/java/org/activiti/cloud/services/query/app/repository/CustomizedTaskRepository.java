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
package org.activiti.cloud.services.query.app.repository;

import com.querydsl.core.types.Predicate;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomizedTaskRepository {
    Page<TaskEntity> findByVariableNameAndValue(
        String name,
        VariableValue<?> value,
        Predicate predicate,
        Pageable pageable
    );

    Page<TaskEntity> findWithProcessVariables(
        List<String> variableKeys,
        List<ProcessVariableValueFilter> processVariableValueFilters,
        Predicate taskPredicate,
        Pageable pageable
    );

    Iterable<TaskEntity> findInProcessInstanceScope(Predicate predicate);

    Page<TaskEntity> findInProcessInstanceScope(Predicate predicate, Pageable pageable);

    boolean existsInProcessInstanceScope(Predicate predicate);
}
