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
package org.activiti.cloud.services.query.rest.specification;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@Testcontainers
class SpecificationSupportIT {

    @Autowired
    VariableRepository variableRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
            Arguments.of(VariableType.STRING, FilterOperator.EQUALS, "value1", List.of("value1", "value2"), 1),
            Arguments.of(VariableType.STRING, FilterOperator.LIKE, "value", List.of("value1", "Value2", "other"), 2),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.EQUALS, "true", List.of(true, false), 1),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.EQUALS, "1.2", List.of(1.2, 2), 1),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.GREATER_THAN, "1.2", List.of(1.3, 1), 1),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.LESS_THAN, "1.2", List.of(1.1, 2), 1),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.GREATER_THAN_OR_EQUAL, "1.2", List.of(1.2, 1.3, 1), 2),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.LESS_THAN_OR_EQUAL, "1.2", List.of(1.2, 1.1, 2), 2),
            Arguments.of(VariableType.INTEGER, FilterOperator.EQUALS, "1", List.of(1, 2), 1),
            Arguments.of(VariableType.INTEGER, FilterOperator.GREATER_THAN, "1", List.of(2, 1), 1),
            Arguments.of(VariableType.INTEGER, FilterOperator.LESS_THAN, "1", List.of(0, 1), 1),
            Arguments.of(VariableType.INTEGER, FilterOperator.GREATER_THAN_OR_EQUAL, "1", List.of(1, 2, 0), 2),
            Arguments.of(VariableType.INTEGER, FilterOperator.LESS_THAN_OR_EQUAL, "1", List.of(1, 0, 2), 2),
            Arguments.of(
                VariableType.DATE,
                FilterOperator.EQUALS,
                "2021-01-01",
                List.of("2021-01-01", "2021-01-02"),
                1
            ),
            Arguments.of(
                VariableType.DATE,
                FilterOperator.GREATER_THAN,
                "2021-01-01",
                List.of("2021-01-02", "2021-01-01"),
                1
            ),
            Arguments.of(
                VariableType.DATE,
                FilterOperator.LESS_THAN,
                "2021-01-02",
                List.of("2021-01-01", "2021-01-02"),
                1
            ),
            Arguments.of(
                VariableType.DATE,
                FilterOperator.GREATER_THAN_OR_EQUAL,
                "2021-01-02",
                List.of("2021-01-03", "2021-01-02", "2021-01-01"),
                2
            ),
            Arguments.of(
                VariableType.DATE,
                FilterOperator.LESS_THAN_OR_EQUAL,
                "2021-01-02",
                List.of("2021-01-01", "2021-01-02", "2021-01-03"),
                2
            ),
            Arguments.of(
                VariableType.DATETIME,
                FilterOperator.EQUALS,
                "2024-08-02T00:11:00.000+00:00",
                List.of("2024-08-02T00:11:00.000+00:00", "2024-08-02T00:11:22.000+00:00"),
                1
            ),
            Arguments.of(
                VariableType.DATETIME,
                FilterOperator.GREATER_THAN,
                "2024-08-02T00:11:00.000+00:00",
                List.of("2024-08-02T00:11:22.000+00:00", "2024-08-02T00:11:00.000+00:00"),
                1
            ),
            Arguments.of(
                VariableType.DATETIME,
                FilterOperator.LESS_THAN,
                "2024-08-02T00:11:22.000+00:00",
                List.of("2024-08-02T00:11:00.000+00:00", "2024-08-02T00:11:22.000+00:00"),
                1
            ),
            Arguments.of(
                VariableType.DATETIME,
                FilterOperator.GREATER_THAN_OR_EQUAL,
                "2024-08-02T00:11:22.000+00:00",
                List.of(
                    "2024-08-02T00:11:22.000+00:00",
                    "2024-08-02T00:11:44.000+00:00",
                    "2024-08-02T00:11:00.000+00:00"
                ),
                2
            ),
            Arguments.of(
                VariableType.DATETIME,
                FilterOperator.LESS_THAN_OR_EQUAL,
                "2024-08-02T00:11:22.000+00:00",
                List.of(
                    "2024-08-02T00:11:00.000+00:00",
                    "2024-08-02T00:11:22.000+00:00",
                    "2024-08-02T00:11:44.000+00:00"
                ),
                2
            )
        );
    }

    private static Stream<Arguments> provideArgumentsThatShouldThrow() {
        return Stream.of(
            Arguments.of(VariableType.STRING, FilterOperator.GREATER_THAN),
            Arguments.of(VariableType.STRING, FilterOperator.LESS_THAN),
            Arguments.of(VariableType.STRING, FilterOperator.GREATER_THAN_OR_EQUAL),
            Arguments.of(VariableType.STRING, FilterOperator.LESS_THAN_OR_EQUAL),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.LIKE),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.GREATER_THAN),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.LESS_THAN),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.GREATER_THAN_OR_EQUAL),
            Arguments.of(VariableType.BOOLEAN, FilterOperator.LESS_THAN_OR_EQUAL),
            Arguments.of(VariableType.BIGDECIMAL, FilterOperator.LIKE),
            Arguments.of(VariableType.INTEGER, FilterOperator.LIKE)
        );
    }

    @AfterEach
    void tearDown() {
        variableRepository.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void should_findEntitiesByVariableValueUsingSpecification(
        VariableType variableType,
        FilterOperator operator,
        String filterValue,
        List<Object> values,
        Integer expectedSublistToIndex
    ) {
        VariableFilter filter = new VariableFilter(null, "name", variableType, filterValue, operator);
        Specification<ProcessVariableEntity> specification = getSpecification(filter);
        List<ProcessVariableEntity> variables = createVariablesWithValues(values);
        List<ProcessVariableEntity> retrieved = variableRepository.findAll(specification);

        assertThat(retrieved).containsExactlyInAnyOrderElementsOf(variables.subList(0, expectedSublistToIndex));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsThatShouldThrow")
    void should_throw_InvalidDataAccessApiUsageException(VariableType variableType, FilterOperator operator) {
        VariableFilter filter = new VariableFilter(null, "name", variableType, "", operator);
        Specification<ProcessVariableEntity> specification = getSpecification(filter);

        assertThatThrownBy(() -> variableRepository.findAll(specification))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessageContaining(variableType.name())
            .hasMessageContaining(operator.name());
    }

    private Specification<ProcessVariableEntity> getSpecification(VariableFilter filter) {
        return new SpecificationSupport<>() {
            @Override
            public Predicate toPredicate(
                Root<ProcessVariableEntity> root,
                CriteriaQuery<?> query,
                CriteriaBuilder criteriaBuilder
            ) {
                return getVariableValueCondition(root.get(ProcessVariableEntity_.value), filter, criteriaBuilder);
            }
        };
    }

    private List<ProcessVariableEntity> createVariablesWithValues(List<Object> values) {
        {
            return values
                .stream()
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
