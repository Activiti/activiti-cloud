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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.helper.HelperConfiguration;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.runtime.IntegrationResultSender;
import org.activiti.cloud.starter.tests.runtime.ServiceTaskConsumerHandler;
import org.activiti.engine.RuntimeService;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationResultEventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@Import(
    {
        HelperConfiguration.class,
        ServiceTaskConsumerHandler.class,
        IntegrationResultSender.class,
        TestChannelBinderConfiguration.class,
    }
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
public class GatewayConcurrencyIT {

    private static final String PROCESS_ID = "gateway_concurrency";

    private static final String SIGNAL_NAME = "concurrentSignal";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private ServiceTaskIntegrationResultEventHandler serviceTaskIntegrationResultEventHandler;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ObjectMapper objectMapper;

    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        identityTokenProducer.withTestUser("testuser");
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    public void cleanUp() {
        executorService.shutdown();
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

        List<Callable<Void>> tasks = new ArrayList<>();

        tasks.add(() -> {
            serviceTaskIntegrationResultEventHandler.receive(integrationResult);
            return null;
        });

        tasks.add(() -> {
            runtimeService.signalEventReceived(SIGNAL_NAME);
            return null;
        });

        executorService.invokeAll(tasks);

        await()
            .atMost(Duration.ofMinutes(10))
            .untilAsserted(() -> {
                ResponseEntity<CloudProcessInstance> completedProcessInstance = processInstanceRestTemplate.getProcessInstance(
                    processInstanceId
                );

                assertThat(completedProcessInstance.getBody().getStatus()).isEqualTo(ProcessInstanceStatus.COMPLETED);
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
