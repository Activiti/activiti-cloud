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
package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@Import({ TestChannelBinderConfiguration.class })
@DirtiesContext
@ContextConfiguration(
    classes = ServicesAuditITConfiguration.class,
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Isolated
public class TestGatewayConcurrencyIT {

    private static final String PROCESS_ID = "gateway_concurrency";

    private static final String SIGNAL_NAME = "concurrentSignal";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    private ExecutorService executorService;

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:concurrency-test");
    }

    @BeforeEach
    public void setUp() {
        identityTokenProducer.withTestUser("testuser");
        executorService = Executors.newFixedThreadPool(2);
        streamHandler.clear();
    }

    @AfterEach
    public void cleanUp() {
        executorService.shutdown();
        streamHandler.clear();
    }

    @Test
    public void shouldExecuteWithoutConcurrencyException() throws IOException, InterruptedException {
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            PROCESS_ID,
            Map.of("signal", SIGNAL_NAME),
            null
        );
        final String processInstanceId = processInstance.getBody().getId();

        IntegrationRequest integrationRequest = getIntegrationRequest();

        final IntegrationResult integrationResult = createIntegrationResult(integrationRequest);

        Set<Callable<Void>> tasks = new LinkedHashSet<>();

        tasks.add(() -> {
            Message message = MessageBuilder
                .withPayload(new SignalPayload(SIGNAL_NAME, Collections.emptyMap()))
                .build();
            String destination = bindingServiceProperties.getBindingDestination("signalConsumer");

            inputDestination.send(message, destination);
            return null;
        });

        tasks.add(() -> {
            Message message = MessageBuilder.withPayload(integrationResult).build();
            String destination = bindingServiceProperties.getBindingDestination("integrationResultsConsumer");

            inputDestination.send(message, destination);
            return null;
        });
        executorService.invokeAll(tasks);

        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

                Assertions
                    .assertThat(receivedEvents)
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getEntityId
                    )
                    .contains(
                        tuple(PROCESS_CREATED, processInstanceId, processInstanceId),
                        tuple(PROCESS_STARTED, processInstanceId, processInstanceId),
                        tuple(PROCESS_COMPLETED, processInstanceId, processInstanceId)
                    );
            });
    }

    private IntegrationRequest getIntegrationRequest() throws IOException {
        Message<byte[]> message = outputDestination.receive(10000, "generate-signal-connector.GENERATE");
        assertThat(message).isNotNull();
        return objectMapper.readValue(message.getPayload(), IntegrationRequestImpl.class);
    }

    private IntegrationResult createIntegrationResult(IntegrationRequest integrationRequest) {
        return new IntegrationResultImpl(integrationRequest, integrationRequest.getIntegrationContext());
    }
}
