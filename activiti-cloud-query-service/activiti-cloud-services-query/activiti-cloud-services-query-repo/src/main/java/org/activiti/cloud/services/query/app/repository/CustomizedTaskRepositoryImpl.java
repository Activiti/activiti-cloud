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
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.hibernate.Filter;
import org.hibernate.Session;
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
    public Page<TaskEntity> findWithProcessVariables(
        List<String> variableKeys,
        Predicate taskPredicate,
        Predicate processVariablePredicate,
        Pageable pageable
    ) {
        Assert.notNull(variableKeys, "keys must not be null!");
        Assert.notNull(taskPredicate, "Predicate must not be null!");
        Assert.notNull(pageable, "Pageable must not be null!");

        EntityManager entityManager = getEntityManager();
        Querydsl querydsl = getQuerydsl();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;

        JPAQuery<String> taskIdsQuery = getTaskIdsQuery(taskPredicate, processVariablePredicate);

        long totalElements = taskIdsQuery.fetchCount();

        List<String> taskIds = querydsl.applyPagination(pageable, taskIdsQuery).fetch();

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("variablesFilter");
        filter.setParameterList("variableKeys", variableKeys);

        JPQLQuery<TaskEntity> tasksQuery = queryFactory
            .query()
            .select(taskEntity)
            .from(taskEntity)
            .where(taskEntity.id.in(taskIds))
            .leftJoin(taskEntity.processVariables, processVariableEntity)
            .fetchJoin()
            .leftJoin(taskEntity.taskCandidateGroups)
            .fetchJoin()
            .leftJoin(taskEntity.taskCandidateUsers)
            .fetchJoin();

        return PageableExecutionUtils.getPage(
            querydsl.applySorting(pageable.getSort(), tasksQuery).fetch(),
            pageable,
            () -> totalElements
        );
    }

    private JPAQuery<String> getTaskIdsQuery(Predicate taskPredicate, Predicate processVariablePredicate) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(getEntityManager());
        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        if (processVariablePredicate != null) {
            QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;
            JPAQuery<ProcessVariableEntity> subquery = queryFactory
                .query()
                .select(processVariableEntity)
                .from(processVariableEntity)
                .where(processVariablePredicate);
            return queryFactory
                .query()
                .select(taskEntity.id)
                .from(taskEntity)
                .join(taskEntity.processVariables, processVariableEntity)
                .where(processVariableEntity.in(subquery))
                .groupBy(taskEntity.id);
            //TODO having count > variaable filters size
        }
        return queryFactory.query().select(taskEntity.id).from(taskEntity).where(taskPredicate);
    }

    public Page<TaskEntity> searchByProcessVariableValue(
        Predicate predicate,
        List<String> processVariableKeys,
        Map<String, Object> processVariableFilters,
        Pageable pageable
    ) {
        Assert.notNull(processVariableKeys, "processVariableKeys must not be null!");
        Assert.notNull(predicate, "Predicate must not be null!");
        Assert.notNull(pageable, "Pageable must not be null!");

        EntityManager entityManager = getEntityManager();
        Querydsl querydsl = getQuerydsl();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;

        BooleanExpression condition = processVariableFilters
            .entrySet()
            .stream()
            .map(entry ->
                processVariableEntity.processDefinitionKey
                    .concat("/")
                    .concat(processVariableEntity.name)
                    .eq(entry.getKey())
                    .and(
                        Expressions
                            .stringTemplate("json_extract_path_text({0}, 'value')", processVariableEntity.value)
                            .eq(Expressions.constant(entry.getValue()))
                    )
            )
            .reduce(BooleanExpression::or)
            .orElse(null);

        JPAQuery<String> taskIdsQuery = queryFactory
            .query()
            .from(processVariableEntity)
            .select(taskEntity.id)
            .where(condition)
            .rightJoin(processVariableEntity.tasks, taskEntity);

        long totalElements = taskIdsQuery.fetchCount();

        List<String> taskIds = querydsl.applyPagination(pageable, taskIdsQuery).fetch();

        Set<String> extendedKeySet = Stream
            .of(processVariableKeys, processVariableFilters.keySet())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        BooleanExpression variableFetchFilter = processVariableEntity.processDefinitionKey
            .concat("/")
            .concat(processVariableEntity.name)
            .in(extendedKeySet);

        JPQLQuery<TaskEntity> tasksQuery = queryFactory
            .query()
            .select(taskEntity)
            .from(taskEntity)
            .leftJoin(taskEntity.processVariables, processVariableEntity)
            .fetchJoin()
            .leftJoin(taskEntity.taskCandidateGroups)
            .fetchJoin()
            .leftJoin(taskEntity.taskCandidateUsers)
            .fetchJoin()
            .where(taskEntity.id.in(taskIds))
            .where(processVariableEntity.isNull().or(variableFetchFilter));

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
