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

import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;

public class CustomizedTaskRepositoryImpl extends QuerydslRepositorySupport implements CustomizedTaskRepository {

    public CustomizedTaskRepositoryImpl() {
        super(TaskEntity.class);
    }

    @Override
    public Page<TaskEntity> findByVariableNameAndValue(String name,
                                                       VariableValue<?> value,
                                                       Predicate predicate,
                                                       Pageable pageable) {
        Assert.notNull(name, "name must not be null!");
        Assert.notNull(value, "value must not be null!");
        Assert.notNull(predicate, "Predicate must not be null!");
        Assert.notNull(pageable, "Pageable must not be null!");

        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QTaskVariableEntity variableEntity = QTaskVariableEntity.taskVariableEntity;

        Predicate condition = variableEntity.name.eq(name)
                .and(variableEntity.value.eq(value));

        JPQLQuery<TaskEntity> from = from(taskEntity).innerJoin(taskEntity.variables, variableEntity).on(condition)
                                                     .where(predicate);

        final JPQLQuery<?> countQuery = from.select(taskEntity.count());

        JPQLQuery<TaskEntity> tasks = from.select(taskEntity);

        return PageableExecutionUtils.getPage(tasks.fetch(), pageable, countQuery::fetchCount);
   }

}