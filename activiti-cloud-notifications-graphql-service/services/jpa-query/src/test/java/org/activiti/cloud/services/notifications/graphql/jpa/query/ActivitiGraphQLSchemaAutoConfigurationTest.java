/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.notifications.graphql.jpa.query;

import static graphql.validation.ValidationErrorType.FieldUndefined;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.util.Lists.list;
import static org.mockito.Mockito.mock;

import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import com.introproventures.graphql.jpa.query.schema.JavaScalars;
import com.introproventures.graphql.jpa.query.schema.RestrictedKeysProvider;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaSchemaBuilder;
import graphql.ExecutionResult;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.time.Instant;
import java.util.Date;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.data.jpa.repositories.bootstrap-mode=default")
@TestPropertySource("classpath:application-test.properties")
class ActivitiGraphQLSchemaAutoConfigurationTest {

    @Autowired(required = false)
    private GraphQLSchema schema;

    @Autowired
    private GraphQLJpaSchemaBuilder graphQLJpaSchemaBuilder;

    @Autowired
    private RestrictedKeysProvider restrictedKeysProvider;

    @Autowired
    private GraphQLExecutor executor;

    @Autowired
    private ActivitiGraphQlJPASchemaProperties activitiGraphQlJPASchemaProperties;

    @Autowired
    private ActivitiGraphQlFieldVisibilityProvider activitiGraphQlFieldVisibilityProvider;

    @SpringBootApplication
    static class TestApplication {

        @Bean
        JsonMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        RestrictedKeysProvider restrictedKeysProviderMock() {
            return mock(RestrictedKeysProvider.class);
        }
    }

    @Test
    void contextLoads() {
        assertThat(schema)
            .isNotNull()
            .extracting(GraphQLSchema::getQueryType)
            .extracting(GraphQLObjectType::getFields)
            .asInstanceOf(InstanceOfAssertFactories.list(GraphQLFieldDefinition.class))
            .extracting(GraphQLFieldDefinition::getName)
            .containsOnly(
                "TaskVariable",
                "ProcessVariable",
                "Application",
                "ProcessDefinition",
                "ProcessInstance",
                "Task",
                "TaskVariables",
                "ProcessVariables",
                "Applications",
                "ProcessDefinitions",
                "ProcessInstances",
                "Tasks",
                "ProcessModel",
                "ProcessModels",
                "ServiceTask",
                "ServiceTasks"
            );
    }

    @Test
    void activitiGraphQlJPASchemaProperties() {
        assertThat(activitiGraphQlJPASchemaProperties.getEntities()).containsOnly(
            ProcessInstanceEntity.class,
            TaskEntity.class,
            ProcessDefinitionEntity.class,
            ProcessVariableEntity.class,
            TaskVariableEntity.class,
            ServiceTaskEntity.class,
            ProcessModelEntity.class,
            ApplicationEntity.class
        );
        assertThat(activitiGraphQlJPASchemaProperties.getRestrictedKeysProvider().isEnabled()).isTrue();
        assertThat(activitiGraphQlJPASchemaProperties.getRestrictedKeysProvider().getRolePrefix()).isEqualTo("ROLE_");
        assertThat(activitiGraphQlJPASchemaProperties.getRestrictedKeysProvider().getUnrestrictedRoles()).containsOnly(
            "ACTIVITI_ADMIN",
            "APPLICATION_MANAGER"
        );
        assertThat(activitiGraphQlJPASchemaProperties.getAggregate().isEnabled()).isTrue();
        assertThat(activitiGraphQlJPASchemaProperties.getFieldsVisibility().isEnabled()).isTrue();
        assertThat(activitiGraphQlJPASchemaProperties.getFieldsVisibility().getPatterns().toString()).isEqualTo(
            "{" +
                "ACTIVITI_MODELER=[JPA.(ProcessModel|ProcessModels)], " +
                "APPLICATION_MANAGER=[.*], " +
                "ACTIVITI_USER=[JPA.(Task|Tasks|ProcessInstance|ProcessInstances|ProcessDefinition|ProcessDefinitions|ProcessVariable|ProcessVariables|TaskVariable|TaskVariables)], " +
                "ACTIVITI_ADMIN=[.*]" +
                "}"
        );
    }

    @Test
    @WithMockUser(roles = "ACTIVITI_USER")
    public void testGraphqlFieldVisibilityForActivitiUser() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ProcessModel")),
                tuple(FieldUndefined, list("ProcessModels")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    @WithMockUser(roles = "ACTIVITI_MODELER")
    public void testGraphqlFieldVisibilityForModelerUser() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Task")),
                tuple(FieldUndefined, list("Tasks")),
                tuple(FieldUndefined, list("ProcessInstance")),
                tuple(FieldUndefined, list("ProcessInstances")),
                tuple(FieldUndefined, list("ProcessDefinitions")),
                tuple(FieldUndefined, list("ProcessDefinition")),
                tuple(FieldUndefined, list("ProcessVariable")),
                tuple(FieldUndefined, list("ProcessVariables")),
                tuple(FieldUndefined, list("TaskVariable")),
                tuple(FieldUndefined, list("TaskVariables")),
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    @WithMockUser(roles = { "ACTIVITI_MODELER", "ACTIVITI_USER" })
    public void testGraphqlFieldVisibilityForCompositeRolesUser() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    @WithAnonymousUser
    public void testGraphqlFieldVisibilityAnonymous() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Task")),
                tuple(FieldUndefined, list("Tasks")),
                tuple(FieldUndefined, list("ProcessInstance")),
                tuple(FieldUndefined, list("ProcessInstances")),
                tuple(FieldUndefined, list("ProcessDefinitions")),
                tuple(FieldUndefined, list("ProcessDefinition")),
                tuple(FieldUndefined, list("ProcessVariable")),
                tuple(FieldUndefined, list("ProcessVariables")),
                tuple(FieldUndefined, list("TaskVariable")),
                tuple(FieldUndefined, list("TaskVariables")),
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ProcessModel")),
                tuple(FieldUndefined, list("ProcessModels")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    public void testGraphqlFieldVisibilityUnauthenticated() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        var result = catchThrowable(() -> executor.execute(query));

        // then
        assertThat(result).isNotNull().isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser
    public void testGraphqlFieldVisibilityEmptyRoles() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Task")),
                tuple(FieldUndefined, list("Tasks")),
                tuple(FieldUndefined, list("ProcessInstance")),
                tuple(FieldUndefined, list("ProcessInstances")),
                tuple(FieldUndefined, list("ProcessDefinitions")),
                tuple(FieldUndefined, list("ProcessDefinition")),
                tuple(FieldUndefined, list("ProcessVariable")),
                tuple(FieldUndefined, list("ProcessVariables")),
                tuple(FieldUndefined, list("TaskVariable")),
                tuple(FieldUndefined, list("TaskVariables")),
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ProcessModel")),
                tuple(FieldUndefined, list("ProcessModels")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    @WithMockUser(roles = "FOO")
    public void testGraphqlFieldVisibilityNoMatchingRoles() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors())
            .isNotEmpty()
            .extracting("validationErrorType", "queryPath")
            .containsOnly(
                tuple(FieldUndefined, list("Task")),
                tuple(FieldUndefined, list("Tasks")),
                tuple(FieldUndefined, list("ProcessInstance")),
                tuple(FieldUndefined, list("ProcessInstances")),
                tuple(FieldUndefined, list("ProcessDefinitions")),
                tuple(FieldUndefined, list("ProcessDefinition")),
                tuple(FieldUndefined, list("ProcessVariable")),
                tuple(FieldUndefined, list("ProcessVariables")),
                tuple(FieldUndefined, list("TaskVariable")),
                tuple(FieldUndefined, list("TaskVariables")),
                tuple(FieldUndefined, list("Application")),
                tuple(FieldUndefined, list("Applications")),
                tuple(FieldUndefined, list("ProcessModel")),
                tuple(FieldUndefined, list("ProcessModels")),
                tuple(FieldUndefined, list("ServiceTask")),
                tuple(FieldUndefined, list("ServiceTasks"))
            );
    }

    @Test
    @WithMockUser(roles = { "ACTIVITI_ADMIN", "ACTIVITI_USER" })
    public void testGraphqlFieldVisibilityAdmin() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @WithMockUser(roles = { "APPLICATION_MANAGER", "ACTIVITI_USER" })
    public void testGraphqlFieldVisibilityManager() {
        //given
        String query = """
            {
                Task(id: "1") { id }
                Tasks { select { id } }
                ProcessInstance(id: "1") { id }
                ProcessInstances { select { id } }
                ProcessDefinition(id: "1") { id }
                ProcessDefinitions { select { id } }
                ProcessVariable(id: 1) { id }
                ProcessVariables { select { id } }
                TaskVariable(id: 1) { id }
                TaskVariables { select { id } }
                Application(id: "foo") { name }
                Applications { select { name } }
                ProcessModel(id: "1") { id }
                ProcessModels { select { id } }
                ServiceTask(id: "1") { id }
                ServiceTasks { select { id } }
            }
            """;

        //when
        ExecutionResult result = executor.execute(query);

        // then
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void correctlyDerivesSchemaFromGivenEntities() {
        //when

        // then
        assertThat(schema).describedAs("Ensure the result is returned").isNotNull();

        //then
        assertThat(schema.getQueryType().getFieldDefinition("Task").getArgument("id"))
            .describedAs("Ensure that identity can be queried on")
            .isNotNull();

        //then
        assertThat(schema.getQueryType().getFieldDefinition("Task").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(1);

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessInstance").getArgument("id").getType()).isEqualTo(
            Scalars.GraphQLString
        );

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessInstance").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(1);

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessVariable").getArgument("id").getType()).isEqualTo(
            ExtendedScalars.GraphQLLong
        );

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessVariable").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(1);

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessDefinition").getArgument("id"))
            .describedAs("Ensure that identity can be queried on")
            .isNotNull();

        //then
        assertThat(schema.getQueryType().getFieldDefinition("ProcessDefinition").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(1);
    }

    @Test
    void correctlyDerivesPageableSchemaFromGivenEntities() {
        //when

        // then
        assertThat(schema).describedAs("Ensure the result is returned").isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("ProcessInstances").getArgument("where"))
            .describedAs("Ensure that collections can be queried")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("ProcessInstances").getArgument("page"))
            .describedAs("Ensure that collections can be paged")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("Tasks").getArgument("page"))
            .describedAs("Ensure that collections can be queried on by page")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("Tasks").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(2);

        assertThat(schema.getQueryType().getFieldDefinition("ProcessVariables").getArgument("page"))
            .describedAs("Ensure that collections can be queried on by page")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("ProcessVariables").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(2);

        assertThat(schema.getQueryType().getFieldDefinition("TaskVariables").getArgument("page"))
            .describedAs("Ensure that collections can be queried on by page")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("TaskVariables").getArguments())
            .describedAs("Ensure query has correct number of arguments")
            .hasSize(2);

        assertThat(schema.getQueryType().getFieldDefinition("ProcessDefinitions").getArgument("where"))
            .describedAs("Ensure that collections can be queried")
            .isNotNull();

        assertThat(schema.getQueryType().getFieldDefinition("ProcessDefinitions").getArgument("page"))
            .describedAs("Ensure that collections can be paged")
            .isNotNull();
    }

    @Test
    void correctlyCoercesDateToISO8601FormatWithTimeAndZoneOffset() {
        // given
        Coercing<?, ?> subject = JavaScalars.of(Date.class).getCoercing();

        // when
        Object result = subject.serialize(Date.from(Instant.EPOCH));

        // then
        assertThat(result).asString().isEqualTo("1970-01-01T00:00:00Z");

        // when
        result = subject.serialize(Date.from(Instant.parse("1970-01-01T00:00:00.000Z")));

        // then
        assertThat(result).asString().isEqualTo("1970-01-01T00:00:00Z");

        // when
        result = subject.serialize(Date.from(Instant.parse("1970-01-01T00:00:00.001Z")));

        // then
        assertThat(result).asString().isEqualTo("1970-01-01T00:00:00.001Z");
    }

    @Test
    void restrictedKeysProvider() {
        assertThat(graphQLJpaSchemaBuilder.getRestrictedKeysProvider()).isEqualTo(restrictedKeysProvider);
    }
}
