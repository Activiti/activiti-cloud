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
package org.activiti.cloud.starter.tests.services.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ActiveProfiles({ AuditProducerIT.AUDIT_PRODUCER_IT })
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.cloud.stream.default-binder=rabbit"
)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(
    classes = ServicesAuditITConfiguration.class,
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class MessageProducerCommandContextCloseListenerIT {

    @Autowired
    private RuntimeService runtimeService;

    @MockitoSpyBean
    private MessageProducerCommandContextCloseListener subject;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @DynamicPropertySource
    static void asyncProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activiti.asyncExecutorActivate", () -> true);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:msg-producer-test");
    }

    @BeforeEach
    void setUp() {
        streamHandler.clear();
    }

    @Test
    void contextLoads() {
        //
    }

    @Test
    void shouldNot_callCloseListener_when_exceptionOccursOnActivitiTransaction() {
        // given
        String processDefinitionKey = "rollbackProcess";

        // when
        Throwable thrown = catchThrowable(() -> {
            runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey).start();
        });

        // then
        ProcessInstance result = runtimeService
            .createProcessInstanceQuery()
            .processDefinitionKey(processDefinitionKey)
            .singleResult();
        assertThat(result).isNull();
        assertThat(thrown).isInstanceOf(ActivitiException.class);
        verify(subject, never()).closed(any(CommandContext.class));
    }

    /*
     * This test case works just when using RabbitMQ due to the usage of the 'transacted' property
     * of its binder. So, RabbitMQ container is required.
     */
    @Test
    void should_rollbackSentMessages_when_exceptionOccursAfterSent() throws InterruptedException {
        // given
        String processDefinitionKey = "SimpleProcess";

        doAnswer(
            new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    CommandContext commandContext = invocation.getArgument(0);

                    doCallRealMethod().when(subject).closed(any(CommandContext.class));

                    subject.closed(commandContext);

                    throw new MessageDeliveryException("Test exception");
                }
            }
        )
            .when(subject)
            .closed(any(CommandContext.class));

        // when
        Throwable thrown = catchThrowable(() -> {
            runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey).start();
        });

        // then
        ProcessInstance result = runtimeService
            .createProcessInstanceQuery()
            .processDefinitionKey(processDefinitionKey)
            .singleResult();
        assertThat(result).isNull();
        assertThat(thrown).isInstanceOf(MessageDeliveryException.class);

        // let's wait
        Thread.sleep(2000);
        assertThat(streamHandler.getAllReceivedEvents()).isEmpty();
    }
}
