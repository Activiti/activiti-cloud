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
package org.activiti.cloud.starter.tests.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.CloudIntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationResultEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(value = "classpath:application-test.properties")
@ContextConfiguration(
    classes = { RuntimeITConfiguration.class },
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@TestExecutionListeners(
    listeners = { RabbitMQQueuesCleanupTestExecutionListener.class },
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DirtiesContext
class ByteArrayIntegrationResultIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoSpyBean
    private ServiceTaskIntegrationResultEventHandler resultEventHandler;

    @Test
    void should_DeserializeJsonBytesViaRabbitMQ_whenContentTypeIsOctetStream() {
        IntegrationResultImpl integrationResult = buildTestIntegrationResult();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(integrationResult);

        String exchange = bindingServiceProperties.getBindingDestination("integrationResultsConsumer");

        MessageProperties props = new MessageProperties();
        props.setContentType("application/octet-stream");
        rabbitTemplate.send(exchange, "#", new Message(jsonBytes, props));

        verify(resultEventHandler, timeout(10_000)).receive(any(IntegrationResult.class));
    }

    private IntegrationResultImpl buildTestIntegrationResult() {
        CloudIntegrationContextImpl context = new CloudIntegrationContextImpl();
        context.setId("octet-stream-test-integration-context");
        context.setProcessInstanceId("octet-stream-test-process-instance");
        context.setProcessDefinitionId("octet-stream-test-process-def");
        context.setExecutionId("octet-stream-test-execution");
        context.setClientId("octet-stream-test-service-task");
        context.setOutBoundVariables(Map.of("result", "fromIDPConnector"));

        IntegrationRequestImpl request = new IntegrationRequestImpl(context);
        return new IntegrationResultImpl(request, context);
    }
}
