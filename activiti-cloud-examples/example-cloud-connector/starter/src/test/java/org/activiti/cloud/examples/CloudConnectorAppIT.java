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
package org.activiti.cloud.examples;

import static org.activiti.cloud.examples.connectors.ExampleConnector.EXAMPLE_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.HeadersConnector.HEADERS_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.MoviesDescriptionConnector.MOVIES_DESCRIPTION_CONSUMER;
import static org.activiti.cloud.examples.connectors.MultiInstanceConnector.MULTI_INSTANCE_CONSUMER;
import static org.activiti.cloud.examples.connectors.TestBpmnErrorConnector.TEST_BPMN_ERROR_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.TestErrorConnector.TEST_ERROR_CONNECTOR_CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.examples.connectors.CustomPojo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = { CloudConnectorApp.class })
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test.properties")
@Import(CloudConnectorAppIT.BinderFactoryListenerConfiguration.class)
@ResourceLock("rabbitmq")
public class CloudConnectorAppIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    private static final String CONNECTOR_SUFFIX = "Connector";

    static final Map<String, Queue> queues = new LinkedHashMap<>();
    static final Map<String, Queue> anonQueues = new LinkedHashMap<>();
    static final Map<String, Exchange> exchanges = new LinkedHashMap<>();

    @TestConfiguration
    static class BinderFactoryListenerConfiguration {

        @Bean
        DeclarableCustomizer declarableCustomizer() {
            return declarable -> {
                if (declarable instanceof AnonymousQueue anonymousQueue) {
                    anonQueues.computeIfAbsent(anonymousQueue.getName(), key -> anonymousQueue);
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
    private ApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private FunctionCatalog functionCatalog;

    @Autowired
    protected Environment environment;

    @Autowired
    protected ActivitiCloudMessagingProperties messagingProperties;

    @Test
    void contextLoads() {
        //then
        assertThat(context).isNotNull();
        assertThat(appName).isNotEmpty();

        assertThat(functionCatalog).isNotNull();
    }

    @Test
    void rabbitQueues() {
        assertThat(queues).isNotEmpty().containsOnlyKeys("processing-connector");
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(anonQueues).isEmpty();
    }

    @Test
    void rabbitExchanges() {
        assertThat(exchanges)
            .isNotEmpty()
            .containsOnlyKeys(
                "restconnector.POST",
                "restConnector.GET",
                "test-bpmn-error-connector.throwError",
                "test-error-connector.throwError",
                "miCloudConnector",
                "headers.GET",
                "Movies.getMovieDesc",
                "ExampleConnector"
            );
    }

    @Test
    public void functionCatalogContainsFunctionDefinitions() {
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(EXAMPLE_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(HEADERS_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(HEADERS_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(MOVIES_DESCRIPTION_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(MULTI_INSTANCE_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(TEST_BPMN_ERROR_CONNECTOR_CONSUMER)))
            .isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(TEST_ERROR_CONNECTOR_CONSUMER)))
            .isNotNull();
    }

    @Test
    public void shouldConvertExpectedJsonToPojo() throws IOException {
        String json = "{ \"test-json-variable-element1\":\"test-json-variable-value1\"}";
        Object jsonValue = objectMapper.readValue(json, Object.class);
        CustomPojo customPojo = objectMapper.convertValue(jsonValue, CustomPojo.class);
        assertThat(customPojo).isNotNull();
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

    private static String getRegisteredConnectorName(String functionName) {
        return functionName + CONNECTOR_SUFFIX + REGISTRATION_NAME_SUFFIX;
    }
}
