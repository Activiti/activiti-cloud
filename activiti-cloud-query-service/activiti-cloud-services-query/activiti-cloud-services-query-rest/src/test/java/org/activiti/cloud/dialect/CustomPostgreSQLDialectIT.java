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
package org.activiti.cloud.dialect;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import java.util.Arrays;
import java.util.List;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@Testcontainers
class CustomPostgreSQLDialectIT {

    @Autowired
    VariableRepository variableRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_findEntity_usingJsonValueEquals_whenVariableIsStringType() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("value1", "value2");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("value1")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueEquals_whenVariableIsBooleanType() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(true, false);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(true)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueLikeCaseSensitive() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("value1", "value2", "Value3");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_LIKE_CASE_SENSITIVE,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("value")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactlyInAnyOrder(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueLikeCaseInsensitive() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("value1", "Value2", "other");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_LIKE_CASE_INSENSITIVE,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("value")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactlyInAnyOrder(variables.get(0), variables.get(1), variables.get(2));
    }

    @Test
    void should_findEntity_usingJsonValueNumericEquals() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1, "1.0", 2);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(1)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueNumericGreaterThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1, "1.0", 0.1);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_GREATER_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(0.5)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueNumericGreaterThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1, "1.0", 0.1);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_GREATER_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(1)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1), variables.get(2));
    }

    @Test
    void should_findEntity_usingJsonValueNumericLessThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1, "1.0", 2);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_LESS_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(1.5)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueNumericLessThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1, "1.0", 2);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_LESS_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal(1)
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1), variables.get(2));
    }

    @Test
    void should_acceptStringArgument_whenTypeIsNumeric() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(1.2, 3);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("1.2")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueDateEquals() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("2021-01-01", "2021-01-03");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATE_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2021-01-01")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueDateGreaterThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("2021-01-01", "2021-01-02", "2021-01-03");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATE_GREATER_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2021-01-01")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(1), variables.get(2));
    }

    @Test
    void should_findEntity_usingJsonValueDateGreaterThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("2021-01-01", "2021-01-02", "2021-01-03");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATE_GREATER_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2021-01-02")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(1), variables.get(2));
    }

    @Test
    void should_findEntity_usingJsonValueDateLessThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("2021-01-01", "2021-01-02", "2021-01-03");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATE_LESS_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2021-01-03")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueDateLessThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues("2021-01-01", "2021-01-02", "2021-01-03");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATE_LESS_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2021-01-02")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueDatetimeEquals() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(
            "2024-08-02T00:11:00.000+00:00",
            "2024-08-02T00:11:35.000+00:00"
        );
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATETIME_EQUALS,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2024-08-02T00:11:00.000+00:00")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueDatetimeGreaterThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(
            "2024-08-02T00:11:00.000+00:00",
            "2024-08-02T00:11:35.000+00:00"
        );
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATETIME_GREATER_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2024-08-02T00:11:00.000+00:00")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(1));
    }

    @Test
    void should_findEntity_usingJsonValueDatetimeGreaterThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(
            "2024-08-02T00:11:00.000+00:00",
            "2024-08-02T00:11:35.000+00:00",
            "2024-08-02T00:11:46.000+00:00"
        );
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATETIME_GREATER_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2024-08-02T00:11:35.000+00:00")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(1), variables.get(2));
    }

    @Test
    void should_findEntity_usingJsonValueDatetimeLessThan() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(
            "2024-08-02T00:11:00.000+00:00",
            "2024-08-02T00:11:35.000+00:00"
        );
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATETIME_LESS_THAN,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2024-08-02T00:11:35.000+00:00")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0));
    }

    @Test
    void should_findEntity_usingJsonValueDatetimeLessThanEqual() {
        List<ProcessVariableEntity> variables = createVariablesWithValues(
            "2024-08-02T00:11:00.000+00:00",
            "2024-08-02T00:11:35.000+00:00",
            "2024-08-02T00:11:46.000+00:00"
        );
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Expression<Boolean> condition = criteriaBuilder.function(
            CustomPostgreSQLDialect.JSON_VALUE_DATETIME_LESS_THAN_EQUAL,
            Boolean.class,
            query.from(ProcessVariableEntity.class).get(ProcessVariableEntity_.value),
            criteriaBuilder.literal("2024-08-02T00:11:35.000+00:00")
        );
        query.where(condition);
        List<Object> resultList = entityManager.createQuery(query).getResultList();
        assertThat(resultList).containsExactly(variables.get(0), variables.get(1));
    }

    private List<ProcessVariableEntity> createVariablesWithValues(Object... values) {
        {
            return Arrays
                .stream(values)
                .map(value -> {
                    ProcessVariableEntity var = new ProcessVariableEntity();
                    var.setValue(value);
                    variableRepository.save(var);
                    return var;
                })
                .toList();
        }
    }
}
