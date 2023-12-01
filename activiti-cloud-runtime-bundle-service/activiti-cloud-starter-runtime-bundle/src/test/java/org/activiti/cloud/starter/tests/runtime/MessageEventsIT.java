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
package org.activiti.cloud.starter.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.StartMessageDeploymentDefinition;
import org.activiti.api.process.model.StartMessageSubscription;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.core.commands.ReceiveMessageCmdExecutor;
import org.activiti.cloud.services.core.commands.StartMessageCmdExecutor;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.messages.events.MessageEventHeaders;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageReceivedEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageSentEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageWaitingEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.MessageSubscriptionCancelledEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.StartMessageDeployedEventMessageProducer;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.RuntimeService;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(
    classes = { RuntimeITConfiguration.class, MessageEventsIT.TestConfigurationContext.class },
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class MessageEventsIT {

    private static final String BUSINESS_KEY = "businessKey";

    private static final String INTERMEDIATE_CATCH_MESSAGE_PROCESS = "IntermediateCatchMessageProcess";

    private static final String INTERMEDIATE_THROW_MESSAGE_PROCESS = "IntermediateThrowMessageProcess";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

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
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private OutputDestination targetDestination;

    @TestConfiguration
    static class TestConfigurationContext {}

    @BeforeEach
    public void setUp() {
        targetDestination.clear();
    }

    @Test
    public void shouldProduceStartMessageDeployedEvents() {
        // given
        String expectedStartEventNames[] = { "BpmnMessage" };

        // when
        ArgumentCaptor<StartMessageDeployedEvent> argumentCaptor = ArgumentCaptor.forClass(
            StartMessageDeployedEvent.class
        );

        // then
        verify(startMessageDeployedEventMessageProducer, atLeast(expectedStartEventNames.length))
            .onEvent(argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues())
            .extracting(StartMessageDeployedEvent::getEntity)
            .extracting(StartMessageDeploymentDefinition::getMessageSubscription)
            .extracting(StartMessageSubscription::getEventName)
            .contains(expectedStartEventNames);
    }

    @Test
    public void testIntermediateThrowMessageEvent() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        //when
        ResponseEntity<CloudProcessInstance> throwProcessResponse = processInstanceRestTemplate.startProcess(
            throwProcessPayload
        );

        //then
        assertThat(throwProcessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(throwProcessResponse.getBody()).isNotNull();
        assertThat(
            runtimeService.createProcessInstanceQuery().processDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS).list()
        )
            .isEmpty();

        verify(bpmnMessageSentEventMessageProducer, times(1)).onEvent(any());
        verify(bpmnMessageWaitingEventMessageProducer, never()).onEvent(any());
        verify(bpmnMessageReceivedEventMessageProducer, never()).onEvent(any());

        verify(receiveMessageCmdExecutor, never()).execute(any());
        verify(startMessageСmdExecutor, never()).execute(any());

        assertOutputDestination();
    }

    @Test
    public void testIntermediateCatchMessageEvent() {
        //given
        StartProcessPayload catchProcessPayload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
            .withBusinessKey(BUSINESS_KEY)
            .build();
        // when
        ResponseEntity<CloudProcessInstance> catchProcessResponse = processInstanceRestTemplate.startProcess(
            catchProcessPayload
        );

        // then
        assertThat(catchProcessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                        .list()
                )
                    .hasSize(1);
            });

        verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(any());

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                        .list()
                )
                    .isEmpty();
            });

        verify(bpmnMessageReceivedEventMessageProducer, times(1)).onEvent(any());
        verify(bpmnMessageSentEventMessageProducer, never()).onEvent(any());

        verify(receiveMessageCmdExecutor, times(1)).execute(any());
        verify(startMessageСmdExecutor, never()).execute(any());

        assertOutputDestination();
    }

    @Test
    public void testStartMessageEvent() {
        // given
        StartMessagePayload receivePayload = MessagePayloadBuilder
            .start("BpmnMessage")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<StartMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();

        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService.createProcessInstanceQuery().processDefinitionKey("StartMessageProcess").list()
                )
                    .isEmpty();
            });

        verify(startMessageСmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testEndMessageEvent() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("EndMessageProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("EndMessageProcess").list())
                    .isEmpty();
            });

        verify(bpmnMessageSentEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testBoundaryTaskMessageEvent() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("BoundaryTaskMessageProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundaryTaskMessageProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundaryTaskMessageProcess")
                        .list()
                )
                    .isEmpty();
            });

        verify(receiveMessageCmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testEventGatewayMessageEvent() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("EventGatewayMessageEventProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("EventGatewayMessageEventProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("EventGatewayMessageEventProcess")
                        .list()
                )
                    .isEmpty();
            });

        verify(receiveMessageCmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testEventSubprocessStartMessageEvent() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("EventSubprocessStartMessageEventProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("EventSubprocessStartMessageEventProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("EventSubprocessStartMessageEventProcess")
                        .list()
                )
                    .isEmpty();
            });

        verify(receiveMessageCmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testEventSubprocessStartMessageEventNonInterrupting() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("EventSubprocessStartMessageEventNonInterruptingProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("EventSubprocessStartMessageEventNonInterruptingProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                verify(receiveMessageCmdExecutor).execute(any());
                verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());
            });

        assertThat(
            runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("EventSubprocessStartMessageEventNonInterruptingProcess")
                .list()
        )
            .hasSize(1);

        processInstanceRestTemplate.delete(response);

        assertOutputDestination();
    }

    @Test
    public void testBoundaryTaskMessageEventNonInterrupting() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("BoundaryTaskMessageEventNonInterruptingProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundaryTaskMessageEventNonInterruptingProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                verify(receiveMessageCmdExecutor).execute(any());
                verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());
            });

        assertThat(
            runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("BoundaryTaskMessageEventNonInterruptingProcess")
                .list()
        )
            .hasSize(1);

        processInstanceRestTemplate.delete(response);

        assertOutputDestination();
    }

    @Test
    public void testBoundarySubprocessMessageEvent() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("BoundarySubprocessMessageEventProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundarySubprocessMessageEventProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundarySubprocessMessageEventProcess")
                        .list()
                )
                    .hasSize(0);
            });

        verify(receiveMessageCmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void testBoundarySubprocessMessageEventNonInterrupting() {
        //given
        StartProcessPayload payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("BoundarySubprocessMessageEventNonInterruptingProcess")
            .withBusinessKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();
        // when
        ResponseEntity<CloudProcessInstance> response = processInstanceRestTemplate.startProcess(payload);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await()
            .untilAsserted(() -> {
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey("BoundarySubprocessMessageEventNonInterruptingProcess")
                        .list()
                )
                    .hasSize(1);

                verify(bpmnMessageWaitingEventMessageProducer).onEvent(any());
            });

        // given
        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive("BpmnMessage")
            .withCorrelationKey(BUSINESS_KEY)
            .withVariable("key", "value")
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                verify(receiveMessageCmdExecutor).execute(any());
                verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());
            });

        assertThat(
            runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("BoundarySubprocessMessageEventNonInterruptingProcess")
                .list()
        )
            .hasSize(1);

        processInstanceRestTemplate.delete(response);

        assertOutputDestination();
    }

    @Test
    public void shouldCancelWaitingMessageSubscription() {
        // given
        int processInstancesQuantity = 1;
        List<ResponseEntity<CloudProcessInstance>> processInstances = new ArrayList<>();

        // when
        IntStream
            .range(0, processInstancesQuantity)
            .mapToObj(i ->
                ProcessPayloadBuilder
                    .start()
                    .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                    .withBusinessKey(BUSINESS_KEY + i)
                    .build()
            )
            .map(processInstanceRestTemplate::startProcess)
            .forEach(processInstances::add);

        //then
        await()
            .atMost(Durations.FIVE_SECONDS)
            .untilAsserted(() ->
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                        .list()
                )
                    .hasSize(processInstancesQuantity)
            );

        verify(bpmnMessageWaitingEventMessageProducer, times(processInstancesQuantity)).onEvent(any());

        // when
        IntStream
            .range(0, processInstancesQuantity)
            .mapToObj(i -> processInstances.get(i))
            .forEach(processInstanceRestTemplate::delete);

        //then
        await()
            .atMost(Durations.FIVE_SECONDS)
            .untilAsserted(() ->
                assertThat(
                    runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                        .list()
                )
                    .isEmpty()
            );

        verify(messageSubscriptionCancelledEventMessageProducer, times(processInstancesQuantity)).onEvent(any());

        assertOutputDestination();
    }

    @Test
    public void shouldThrowCatchMessageWithCorrelationKeyAndMappedPayloads() {
        // given
        StartProcessPayload throwMsg = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("process-be954b8b-b412-4fcb-9fc5-bf1d096d249f")
            .build();

        // when
        ResponseEntity<CloudProcessInstance> throwMsgInstance = processInstanceRestTemplate.startProcess(throwMsg);

        // then
        assertThat(throwMsgInstance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(throwMsgInstance.getBody()).isNotNull();
        assertThat(
            runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("process-be954b8b-b412-4fcb-9fc5-bf1d096d249f")
                .list()
        )
            .isEmpty();

        ArgumentCaptor<BPMNMessageSentEvent> throwArgumentCaptor = ArgumentCaptor.forClass(BPMNMessageSentEvent.class);

        verify(bpmnMessageSentEventMessageProducer).onEvent(throwArgumentCaptor.capture());

        MessageEventPayload messageEventPayload = throwArgumentCaptor.getValue().getEntity().getMessagePayload();

        assertThat(messageEventPayload.getCorrelationKey()).isEqualTo("corr");
        assertThat(messageEventPayload.getVariables())
            .contains(entry("stringvar", "string"), entry("variablevar", "default"));

        // and given
        StartProcessPayload catchMsg = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey("process-bf064b4f-5cf7-440c-b6b1-e55ac532e56c")
            .build();
        ResponseEntity<CloudProcessInstance> catchMsgInstance = processInstanceRestTemplate.startProcess(catchMsg);

        ReceiveMessagePayload receivePayload = MessagePayloadBuilder
            .receive(messageEventPayload.getName())
            .withCorrelationKey(messageEventPayload.getCorrelationKey())
            .withVariables(messageEventPayload.getVariables())
            .build();

        Message<ReceiveMessagePayload> receiveMessage = MessageBuilder.withPayload(receivePayload).build();
        // when
        processEngineChannels.commandConsumer().send(receiveMessage);

        // then
        await()
            .untilAsserted(() -> {
                ResponseEntity<CollectionModel<CloudVariableInstance>> variables = processInstanceRestTemplate.getVariables(
                    catchMsgInstance
                );

                assertThat(variables.getBody().getContent())
                    .extracting(VariableInstance::getName, VariableInstance::getValue)
                    .contains(tuple("string", "string"), tuple("variable", "default"));
            });

        verify(receiveMessageCmdExecutor).execute(any());
        verify(bpmnMessageReceivedEventMessageProducer).onEvent(any());

        processInstanceRestTemplate.delete(catchMsgInstance);

        assertOutputDestination();
    }

    private void assertOutputDestination() {
        Message<?> message = targetDestination.receive(10000, "messageEvents_activiti-app");

        assertThat(message)
            .extracting(Message::getHeaders)
            .extracting(MessageEventHeaders.MESSAGE_EVENT_OUTPUT_DESTINATION)
            .isEqualTo(bindingServiceProperties.getBindingDestination("commandConsumer"));
    }
}
