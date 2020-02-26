/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.messages.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.activiti.api.model.shared.Payload;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.StartMessageDeploymentDefinition;
import org.activiti.api.process.model.StartMessageSubscription;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.model.events.BPMNMessageWaitingEvent;
import org.activiti.api.process.model.events.MessageSubscriptionCancelledEvent;
import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.api.process.model.payloads.DeleteProcessPayload;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.results.ProcessInstanceResult;
import org.activiti.cloud.services.core.commands.CommandEndpoint;
import org.activiti.cloud.services.core.commands.ReceiveMessageCmdExecutor;
import org.activiti.cloud.services.core.commands.StartMessageCmdExecutor;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageReceivedEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageSentEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageWaitingEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.MessageSubscriptionCancelledEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.StartMessageDeployedEventMessageProducer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.RuntimeService;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {
                    "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres",
                    "spring.datasource.username=postgres",
                    "spring.datasource.password=",
                    "spring.datasource.platform=postgresql",
                    "spring.application.name=messages-app",
                    "spring.jmx.enabled=false",
                    "spring.rabbitmq.host=localhost"        
                })
public class MessageEventsIT {

    private static final String BOUNDARY_SUBPROCESS_THROW_CATCH_MESSAGE_IT_PROCESS1 = "BoundarySubprocessThrowCatchMessageIT_Process1";

    private static final String EVENT_SUBPROCESS_NON_INTERRUPTING_THROW_CATCH_MESSAGE_IT_PROCESS1 = "EventSubprocessNonInterruptingThrowCatchMessageIT_Process1";

    private static final String EVENT_SUBPROCESS_THROW_CATCH_MESSAGE_IT_PROCESS1 = "EventSubprocessThrowCatchMessageIT_Process1";

    private static final String BOUNDARY_THROW_CATCH_MESSAGE_IT_PROCESS1 = "BoundaryThrowCatchMessageIT_Process1";

    private static final String THROW_CATCH_MESSAGE_IT_PROCESS1 = "ThrowCatchMessageIT_Process1";

    private static final String CORRELATION_ID = "correlationId";

    private static final String CORRELATION_KEY = "correlationKey";

    private static final String BUSINESS_KEY = "businessKey";

    private static final String INTERMEDIATE_CATCH_MESSAGE_PROCESS = "IntermediateCatchMessageProcess";

    private static final String INTERMEDIATE_THROW_MESSAGE_PROCESS = "IntermediateThrowMessageProcess";
    
    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class Application {
        
    }

    @Autowired
    private RuntimeService runtimeService;
    
    @SpyBean
    private BpmnMessageReceivedEventMessageProducer bpmnMessageReceivedEventMessageProducer;

    @SpyBean
    private BpmnMessageSentEventMessageProducer bpmnMessageSentEventMessageProducer;
    
    @SpyBean
    private BpmnMessageWaitingEventMessageProducer bpmnMessageWaitingEventMessageProducer;
    
    @SpyBean
    private StartMessageCmdExecutor startMessageСmdExecutor; 

    @SpyBean
    private ReceiveMessageCmdExecutor receiveMessageCmdExecutor;
    
    @SpyBean
    private MessageSubscriptionCancelledEventMessageProducer messageSubscriptionCancelledEventMessageProducer; 
    
    @SpyBean(reset = MockReset.NONE)
    private StartMessageDeployedEventMessageProducer startMessageDeployedEventMessageProducer;

    @Autowired
    private CommandEndpoint<Payload> commandEndpoint;
    
    @Autowired
    private MessageGroupStore messageGroupStore;
    
    @Test
    public void shouldProduceStartMessageDeployedEvents() {
        // given
        String expectedStartEventNames[] = {
                "EventSubprocessThrowEndMessage",
                "EventSubprocessStartProcess3",
                "BoundaryThrowEndMessage",
                "BoundaryThrowIntermediateMessage",
                "EventSubprocessNonInterruptingThrowEndMessage",
                "EventSubprocessStartProcessNonInterrupting3",
                "ThrowEndMessage",
                "ThrowIntermediateMessage",
                "BoundarySubprocessThrowEndMessage",
                "SartBoundarySubprocessThrowIntermediateMessage"
        };
        
        // when
        ArgumentCaptor<StartMessageDeployedEvent> argumentCaptor = ArgumentCaptor.forClass(StartMessageDeployedEvent.class);
        
        // then
        verify(startMessageDeployedEventMessageProducer, atLeast(expectedStartEventNames.length)).onEvent(argumentCaptor.capture());
        
        assertThat(argumentCaptor.getAllValues()).extracting(StartMessageDeployedEvent::getEntity)
                                                 .extracting(StartMessageDeploymentDefinition::getMessageSubscription)
                                                 .extracting(StartMessageSubscription::getEventName)
                                                 .contains(expectedStartEventNames);
        
        Stream.of(expectedStartEventNames)
              .forEach(messageName -> {
                  String groupId = "messages-app:" + messageName;
                  assertThat(messageGroupStore.getMessagesForGroup(groupId)).hasSize(1);
              });
    }
    
    @Test
    public void shouldThrowCatchBpmnMessage() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .build();

        StartProcessPayload catchProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .build();
        //when
        commandEndpoint.execute(throwProcessPayload);
        commandEndpoint.execute(catchProcessPayload);

        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(1)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(1)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
    
            verify(receiveMessageCmdExecutor, times(1)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, never()).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }
    
    @Test
    public void shouldCompleteComplexBpmnMessageEventProcessWithIntermediateCatchEvent() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .withVariable(CORRELATION_KEY, CORRELATION_ID)
                                                                       .build();
        //when
        commandEndpoint.execute(throwProcessPayload);

        //then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(3)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(startMessageСmdExecutor, times(2)).execute(ArgumentMatchers.<StartMessagePayload>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(receiveMessageCmdExecutor, times(1)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(3)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
        });
        
    }
    
    @Test
    public void shouldCompleteComplexBpmnMessageEventProcessWithBoundaryCatchEvent() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(BOUNDARY_THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .build();
        //when
        commandEndpoint.execute(throwProcessPayload);

        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(3)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(3)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(1)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }
    

    @Test
    public void shouldCompleteComplexBpmnMessageEventMultipleProcessesWithIntermediateCatchEvent() {
        // given
        int processInstances = 10;
        
        //when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }
    
    @Test
    public void shouldCompleteComplexBpmnMessageEventMultipleProcessesWithBoundaryTaskMessageCatchEvent() {
        // given
        int processInstances = 10;
        
        //when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(BOUNDARY_THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }

    @Test
    public void shouldCompleteComplexBpmnMessageEventMultipleProcessesWithBoundarySubprocessMessageCatchEvent() {
        // given
        int processInstances = 10;
        
        //when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(BOUNDARY_SUBPROCESS_THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(3 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
        
    }
    
    @Test
    public void shouldCompleteComplexBpmnMessageEventMultipleProcessesWithStartEventSubprocessEvent() {
        // given
        int processInstances = 10;
        
        //when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(EVENT_SUBPROCESS_THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(4 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(2 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(4 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
        
    }
    
    @Test
    public void shouldCompleteComplexBpmnMessageEventMultipleProcessesWithStartEventSubprocessNonInterruptingEvent() {
        // given
        int processInstances = 10;
        
        //when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(EVENT_SUBPROCESS_NON_INTERRUPTING_THROW_CATCH_MESSAGE_IT_PROCESS1)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(4 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(2 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(4 * processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());

            verify(receiveMessageCmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, times(2 * processInstances)).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }
    
    @Test
    public void shouldThrowCatchBpmnMessages() {
        // given
        int processInstances = 10;
        
        // when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);

        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);
        
        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());
            
            verify(receiveMessageCmdExecutor, times(processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, never()).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }

    @Test
    public void shouldCatchThrowBpmnMessages() {
        // given
        int processInstances = 10;

        // when
        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);

        IntStream.rangeClosed(1, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .forEach(commandEndpoint::execute);

        // then
        Awaitility.await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageSentEvent>any());
            verify(bpmnMessageWaitingEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent>any());
            verify(bpmnMessageReceivedEventMessageProducer, times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageReceivedEvent>any());

            verify(receiveMessageCmdExecutor, times(processInstances)).execute(ArgumentMatchers.<ReceiveMessagePayload>any());
            verify(startMessageСmdExecutor, never()).execute(ArgumentMatchers.<StartMessagePayload>any());
        });
    }

    @Test
    public void shouldCancelWaitingMessageSubscription() {
        // given
        int processInstances = 10;
        List<ProcessInstance> instances = new ArrayList<>();

        // when
        IntStream.range(0, processInstances)
                 .mapToObj(i -> ProcessPayloadBuilder.start()
                                                     .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                                     .withBusinessKey(BUSINESS_KEY + i)
                                                     .build())
                 .<ProcessInstanceResult> map(commandEndpoint::execute)
                 .map(ProcessInstanceResult::getEntity)
                 .forEach(instances::add);

        // then
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                 .list()).hasSize(processInstances);

        verify(bpmnMessageWaitingEventMessageProducer,
               times(processInstances)).onEvent(ArgumentMatchers.<BPMNMessageWaitingEvent> any());

        // when
        IntStream.range(0, processInstances)
                 .mapToObj(i -> instances.get(i))
                 .map(it -> new DeleteProcessPayload(it.getId(), "cancelled"))
                 .forEach(commandEndpoint::execute);

        // then
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                 .list()).isEmpty();

        verify(messageSubscriptionCancelledEventMessageProducer,
               times(processInstances)).onEvent(ArgumentMatchers.<MessageSubscriptionCancelledEvent> any());

        IntStream.range(0, processInstances)
                 .mapToObj(i -> BUSINESS_KEY + i)
                 .map("messages-app:BpmnMessage:"::concat)
                 .forEach(groupId -> {
                     assertThat(messageGroupStore.getMessagesForGroup(groupId)).isEmpty();
                 });

    }
}