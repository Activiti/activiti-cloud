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
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

public class CustomizedTaskRepositoryImpl extends QuerydslRepositorySupport implements CustomizedTaskRepository {

    public CustomizedTaskRepositoryImpl() {
        super(TaskEntity.class);
    }

    @Override
    public Page<TaskEntity> findByVariableNameAndValue(
        String name,
        VariableValue<?> value,
        Predicate predicate,
        Pageable pageable
    ) {
        Assert.notNull(name, "name must not be null!");
        Assert.notNull(value, "value must not be null!");
        Assert.notNull(predicate, "Predicate must not be null!");
        Assert.notNull(pageable, "Pageable must not be null!");

        EntityManager entityManager = getEntityManager();
        Querydsl querydsl = getQuerydsl();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QTaskVariableEntity variableEntity = QTaskVariableEntity.taskVariableEntity;

        Predicate condition = variableEntity.name
            .eq(name)
            .and(Expressions.booleanTemplate("{0} = {1}", variableEntity.value, value));

        JPAQuery<String> taskIdsQuery = queryFactory
            .query()
            .select(taskEntity.id)
            .from(taskEntity)
            .innerJoin(taskEntity.variables, variableEntity)
            .on(condition)
            .where(predicate);

        long totalElements = taskIdsQuery.fetchCount();

        List<String> taskIds = querydsl.applyPagination(pageable, taskIdsQuery).fetch();

        JPQLQuery<TaskEntity> tasksQuery = queryFactory
            .query()
            .select(taskEntity)
            .from(taskEntity)
            .innerJoin(taskEntity.variables, variableEntity)
            .on(condition)
            .where(taskEntity.id.in(taskIds));

        return PageableExecutionUtils.getPage(
            querydsl.applySorting(pageable.getSort(), tasksQuery).fetch(),
            pageable,
            () -> totalElements
        );
    }

    @Override
    public Iterable<TaskEntity> findInProcessInstanceScope(Predicate predicate) {
        QTaskEntity taskEntity = QTaskEntity.taskEntity;

        JPQLQuery<TaskEntity> from = buildLeftJoin(taskEntity, predicate);
        JPQLQuery<TaskEntity> tasks = from.select(taskEntity);

        return tasks.fetch();
    }

    @Override
    public Page<TaskEntity> findInProcessInstanceScope(Predicate predicate, Pageable pageable) {
        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        final Querydsl querydsl = getQuerydsl();

        JPQLQuery<String> taskIdsQuery = buildLeftJoin(taskEntity).select(taskEntity.id).where(predicate);
        final long totalElements = taskIdsQuery.fetchCount();
        List<String> taskIds = querydsl.applyPagination(pageable, taskIdsQuery).fetch();
        JPQLQuery<TaskEntity> tasks = buildLeftJoin(taskEntity).select(taskEntity).where(taskEntity.id.in(taskIds));
        return PageableExecutionUtils.getPage(
            querydsl.applySorting(pageable.getSort(), tasks).fetch(),
            pageable,
            () -> totalElements
        );
    }

    @Override
    public boolean existsInProcessInstanceScope(Predicate predicate) {
        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        JPQLQuery<TaskEntity> from = buildLeftJoin(taskEntity, predicate);
        JPQLQuery<?> countQuery = from.select(taskEntity.count());
        return countQuery.fetchCount() > 0;
    }

    private JPQLQuery<TaskEntity> buildLeftJoin(QTaskEntity taskEntity, Predicate predicate) {
        return buildLeftJoin(taskEntity).where(predicate);
    }

    private JPQLQuery<TaskEntity> buildLeftJoin(QTaskEntity taskEntity) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;
        Predicate condition = processInstanceEntity.id.eq(taskEntity.processInstanceId);

        return from(taskEntity).leftJoin(processInstanceEntity).on(condition);
    }
}
