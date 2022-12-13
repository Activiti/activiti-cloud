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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"spring.activiti.asyncExecutorActivate=true"})
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = ServicesAuditITConfiguration.class,
                      initializers = {RabbitMQContainerApplicationInitializer.class,
                                      KeycloakContainerApplicationInitializer.class}
)
@DirtiesContext
public class MessageProducerCommandContextCloseListenerIT {

    @Autowired
    private RuntimeService runtimeService;

    @SpyBean
    private MessageProducerCommandContextCloseListener subject;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @BeforeEach
    public void setUp() {
        streamHandler.clear();
    }

    @Test
    public void contextLoads() {
        //
    }

    @Test
    public void shouldNot_callCloseListener_when_exceptionOccursOnActivitiTransaction() {
        // given
        String processDefinitionKey = "rollbackProcess";

        // when
        Throwable thrown = catchThrowable(() -> {
            runtimeService.createProcessInstanceBuilder()
                          .processDefinitionKey(processDefinitionKey)
                          .start();
        });

        // then
        ProcessInstance result = runtimeService.createProcessInstanceQuery()
                                               .processDefinitionKey(processDefinitionKey)
                                               .singleResult();
        assertThat(result).isNull();
        assertThat(thrown).isInstanceOf(ActivitiException.class);
        verify(subject, never()).closed(any(CommandContext.class));
    }

    // @Disabled //TODO fix & enable before merge
    @Test
    public void should_rollbackSentMessages_when_exceptionOccursAfterSent() throws InterruptedException {
        // given
        String processDefinitionKey = "SimpleProcess";
        Thread.sleep(2000);

        System.out.println("should_rollbackSentMessages_when_exceptionOccursAfterSent - START - " + streamHandler.getAllReceivedEvents().size());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                System.out.println("should_rollbackSentMessages_when_exceptionOccursAfterSent - SYNC - " + TransactionSynchronizationManager.isSynchronizationActive());

                CommandContext commandContext = invocation.getArgument(0);

                doCallRealMethod().when(subject)
                                  .closed(any(CommandContext.class));

                subject.closed(commandContext);

                throw new MessageDeliveryException("Test exception");
            }
        }).when(subject)
          .closed(any(CommandContext.class));

        // when
        Throwable thrown = catchThrowable(() -> {
            runtimeService.createProcessInstanceBuilder()
                          .processDefinitionKey(processDefinitionKey)
                          .start();
        });

        System.out.println("should_rollbackSentMessages_when_exceptionOccursAfterSent - MIDDLE - " + streamHandler.getAllReceivedEvents().size());

        // then
        ProcessInstance result = runtimeService.createProcessInstanceQuery()
                                               .processDefinitionKey(processDefinitionKey)
                                               .singleResult();
        assertThat(result).isNull();
        assertThat(thrown).isInstanceOf(MessageDeliveryException.class);

        System.out.println("should_rollbackSentMessages_when_exceptionOccursAfterSent - PRE-WAIT - " + streamHandler.getAllReceivedEvents().size());

        // let's wait
        Thread.sleep(2000);
        System.out.println("should_rollbackSentMessages_when_exceptionOccursAfterSent - END - " + streamHandler.getAllReceivedEvents().size());
        assertThat(streamHandler.getAllReceivedEvents()).isEmpty();
    }
}
