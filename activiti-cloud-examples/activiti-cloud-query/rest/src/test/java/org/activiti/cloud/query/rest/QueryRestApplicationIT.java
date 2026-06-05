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
package org.activiti.cloud.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.introproventures.graphql.jpa.query.schema.RestrictedKeysProvider;
import com.introproventures.graphql.jpa.query.web.GraphQLController;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.notifications.graphql.web.api.GraphQLQueryResult;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.activiti.cloud.starters.test.binder.BinderFactoryListenerTestContext;
import org.activiti.cloud.starters.test.binder.EnableBinderFactoryListenerTestContext;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@AutoConfigureTestRestTemplate
@SpringBootTest(
    classes = { QueryRestApplication.class },
    properties = { "identity.test.token-interceptor.enabled=false", "spring.sql.init.mode=always" },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@EnableCleanupLiquibaseAfterTest
@EnableBinderFactoryListenerTestContext
@ResourceLocks(value = { @ResourceLock("postgres"), @ResourceLock("rabbitmq") })
public class QueryRestApplicationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    protected BinderFactoryListenerTestContext binderFactoryListenerTestContext;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private RestrictedKeysProvider restrictedKeysProvider;

    @Autowired
    private GraphQLSchema graphQLSchema;

    @Autowired
    protected Environment environment;

    @Autowired
    protected ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    protected BindingServiceProperties bindingServiceProperties;

    @Test
    public void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    public void defaultSpecificationFileShouldBeAlfrescoFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs/Query").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        both(notNullValue(String.class))
                            .and(containsString("ListResponseContentCloudProcessDefinition"))
                            .and(containsString("EntriesResponseContentCloudProcessDefinition"))
                            .and(containsString("EntryResponseContentCloudProcessDefinition"))
                            .and(not(containsString("PagedModel")))
                            .and(not(containsString("ResourcesOfResource")))
                            .and(not(containsString("Resource")))
                    )
            );
    }

    @Test
    void shouldUseAlfrescoDbpRestFormat_whenGetProcessInstancesWithAcceptApplicationJson() {
        var responseEntity = testRestTemplate.exchange(
            "/v1/process-instances",
            HttpMethod.GET,
            entityWithAcceptJsonContentHeaders(entityWithAuthorizationHeader("testuser", "password")),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(responseEntity.getBody())
            .isNotEmpty()
            .containsKey("list")
            .extracting("list")
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
            .containsKeys("entries", "pagination");
    }

    @Test
    void graphQLSchema() {
        assertThat(graphQLSchema)
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
                "ServiceTasks",
                "hello"
            );
    }

    @Test
    public void testGraphqlModelerUserShouldNotSeeTasks() {
        GraphQLController.GraphQLQueryRequest query = new GraphQLController.GraphQLQueryRequest(
            "{Tasks{select{name assignee priority}}}"
        );

        ResponseEntity<GraphQLQueryResult> entity = testRestTemplate.postForEntity(
            "/graphql",
            entityWithAcceptJsonContentHeaders(query, entityWithAuthorizationHeader("johnsnow", "password")),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();
        assertThat("{Tasks={select=[]}}").isEqualTo(result.getData().toString());
    }

    @Test
    public void testGraphqlAdminUserShouldSeeAllTasks() {
        GraphQLController.GraphQLQueryRequest query = new GraphQLController.GraphQLQueryRequest(
            "{Tasks{select{name assignee priority}}}"
        );

        ResponseEntity<GraphQLQueryResult> entity = testRestTemplate.postForEntity(
            "/graphql",
            entityWithAcceptJsonContentHeaders(query, entityWithAuthorizationHeader("testadmin", "password")),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();
        assertThat(result.getData().toString())
            .isEqualTo(
                "{Tasks={select=[" +
                "{name=task1, assignee=testuser, priority=5}, " +
                "{name=task2, assignee=hruser, priority=10}, " +
                "{name=task3, assignee=hruser, priority=5}, " +
                "{name=task4, assignee=hruser, priority=10}, " +
                "{name=task5, assignee=hruser, priority=10}, " +
                "{name=task6, assignee=hruser, priority=10}" +
                "]}}"
            );
    }

    @Test
    public void testGraphqlUserShouldSeeInvolvedTasks() {
        GraphQLController.GraphQLQueryRequest query = new GraphQLController.GraphQLQueryRequest(
            "{Tasks{select{name assignee priority}}}"
        );

        ResponseEntity<GraphQLQueryResult> entity = testRestTemplate.postForEntity(
            "/graphql",
            entityWithAcceptJsonContentHeaders(query, entityWithAuthorizationHeader("testuser", "password")),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();
        assertThat(result.getData().toString())
            .isEqualTo("{Tasks={select=[{name=task1, assignee=testuser, priority=5}]}}");
    }

    @Test
    void rabbitBinderCompression() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.binder.compression-level", Integer.class))
            .isEqualTo(9);
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.compress", Boolean.class))
            .isTrue();
    }

    @Test
    void messagingPropertiesRabbitMqCompression() {
        assertThat(messagingProperties.getRabbitmq().getCompressionLevel()).isEqualTo(9);
        assertThat(messagingProperties.getRabbitmq().isCompress()).isTrue();
    }

    @Test
    void messagingRabbitMqPrefixProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isNullOrEmpty();
    }

    @Test
    void rabbitBinderDefaultPrefix() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class))
            .isNullOrEmpty();
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class))
            .isNullOrEmpty();
    }

    @Test
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues()).isEmpty();
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getAnonymousQueues())
            .isNotEmpty()
            .satisfies(map ->
                assertThat(map.keySet()).isNotEmpty().allMatch(key -> key.startsWith("queryEvents.anonymous."))
            );
    }

    @Test
    void rabbitExchanges() {
        assertThat(binderFactoryListenerTestContext.getExchanges())
            .isNotEmpty()
            .containsOnlyKeys("engineEvents", "queryEvents");
    }

    private HttpEntity entityWithAuthorizationHeader(String user, String password) {
        HttpEntity authEntity = identityTokenProducer.entityWithAuthorizationHeader(user, password);
        return new HttpEntity(authEntity.getHeaders());
    }

    private HttpEntity entityWithAcceptJsonContentHeaders(HttpEntity authEntity) {
        var headers = new HttpHeaders();
        headers.set("Authorization", authEntity.getHeaders().getFirst("Authorization"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity(headers);
    }

    private HttpEntity entityWithAcceptJsonContentHeaders(Object body, HttpEntity authEntity) {
        var headers = new HttpHeaders();
        headers.set("Authorization", authEntity.getHeaders().getFirst("Authorization"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity(body, headers);
    }
}
