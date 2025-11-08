/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.services.test.liquibase.CleanupLiquibaseAfterTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(
    classes = { QueryApplication.class },
    properties = "identity.test.token-interceptor.enabled=false",
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@CleanupLiquibaseAfterTest
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(QueryApplicationIT.BinderFactoryListenerConfiguration.class)
public class QueryApplicationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withReuse(true);

    static final Map<String, Queue> queues = new LinkedHashMap<>();
    static final Map<String, AnonymousQueue> anonQueues = new LinkedHashMap<>();
    static final Map<String, Exchange> exchanges = new LinkedHashMap<>();

    @TestConfiguration
    static class BinderFactoryListenerConfiguration {

        @Bean
        DeclarableCustomizer declarableCustomizer() {
            return declarable -> {
                if (declarable instanceof AnonymousQueue queue) {
                    anonQueues.computeIfAbsent(queue.getName(), key -> queue);
                } else if (declarable instanceof Queue queue) {
                    queues.computeIfAbsent(queue.getName(), key -> queue);
                } else if (declarable instanceof Exchange exchange) {
                    exchanges.computeIfAbsent(exchange.getName(), key -> exchange);
                }

                return declarable;
            };
        }
    }

    @AfterAll
    static void cleanUp() {
        queues.clear();
        exchanges.clear();
        anonQueues.clear();
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

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
        assertThat(queues).isNotEmpty().hasSize(2).containsOnlyKeys("engineEvents.query", "engineEvents.audit");
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(anonQueues)
            .isNotEmpty()
            .hasSize(1)
            .satisfies(map -> assertThat(map.keySet()).anyMatch(key -> key.startsWith("engineEvents.anonymous.")));
    }

    @Test
    void rabbitExchanges() {
        assertThat(exchanges).isNotEmpty().containsOnlyKeys("engineEvents");
    }
}
