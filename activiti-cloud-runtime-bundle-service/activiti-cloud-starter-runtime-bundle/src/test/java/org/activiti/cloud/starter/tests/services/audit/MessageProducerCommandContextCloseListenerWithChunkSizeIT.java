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
package org.activiti.cloud.starter.tests.services.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.events.services.IncidentService;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ActiveProfiles({ AuditProducerIT.AUDIT_PRODUCER_IT })
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.stream.default-binder=rabbit",
        "activiti.cloud.runtime-bundle.events-properties.chunk-size-in-bytes-close-listener=5",
    }
)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(
    classes = ServicesAuditITConfiguration.class,
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestExecutionListeners(
    value = RabbitMQQueuesCleanupTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
@ExtendWith(MockitoExtension.class)
class MessageProducerCommandContextCloseListenerWithChunkSizeIT {

    @Autowired
    private RuntimeService runtimeService;

    @MockitoSpyBean
    private MessageProducerCommandContextCloseListener subject;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @MockitoSpyBean
    private IncidentService incidentService;

    @Captor
    private ArgumentCaptor<ExecutionContext> executionContextCaptor;

    @DynamicPropertySource
    public static void asyncProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activiti.asyncExecutorActivate", () -> true);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:msg-producer-test");
    }

    @BeforeEach
    void setUp() {
        this.streamHandler.clear();
    }

    @Test
    void should_createIncident_when_messageExceedChunkSizeLimit() throws InterruptedException {
        var processDefinitionKey = "SimpleProcess";

        assertThrows(IllegalArgumentException.class, () ->
            this.runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey).start()
        );

        verify(this.incidentService).createAndSendIncidentEvent(
            this.executionContextCaptor.capture(),
            any(Exception.class)
        );

        var executionContextCaptorValue = this.executionContextCaptor.getValue();
        assertThat(executionContextCaptorValue.getProcessInstance().getProcessInstanceId()).isNotEmpty();
        assertThat(executionContextCaptorValue.getProcessInstance().getProcessDefinitionKey()).isEqualTo(
            processDefinitionKey
        );
        assertThat(executionContextCaptorValue.getProcessInstance().getProcessDefinitionName()).isEqualTo(
            processDefinitionKey
        );
        assertThat(executionContextCaptorValue.getExecution().getProcessInstanceId()).isNotEmpty();

        var result = this.runtimeService.createProcessInstanceQuery()
            .processDefinitionKey(processDefinitionKey)
            .singleResult();
        assertThat(result).isNull();

        Thread.sleep(2000);
        assertThat(this.streamHandler.getAllReceivedEvents()).isEmpty();
    }
}
