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
package org.activiti.cloud.services.events.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.activiti.api.process.model.BPMNMessage;
import org.activiti.api.process.model.MessageSubscription;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.model.events.MessageSubscriptionCancelledEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.runtime.event.impl.BPMNMessageReceivedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNMessageSentEventImpl;
import org.activiti.api.runtime.event.impl.MessageSubscriptionCancelledEventImpl;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.MessageSubscriptionImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ToCloudProcessRuntimeMessageEventsConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Test
    public void shouldConvertBPMNMessageSentEventToCloudBPMNMessageSentEvent() {
        //given
        BPMNMessage entity = bpmnMessageEntity("entityId");

        BPMNMessageSentEvent runtimeEvent = new BPMNMessageSentEventImpl(entity);

        //when
        CloudBPMNMessageSentEvent cloudEvent = converter.from(runtimeEvent);

        //then
        CloudBPMNMessageEventAssert.assertThat(cloudEvent).hasEntity(entity);

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    public void shouldConvertBPMNMessageWaitingEventToCloudBPMNMessageWaitingEvent() {
        //given
        BPMNMessage entity = bpmnMessageEntity("entityId");

        BPMNMessageSentEvent runtimeEvent = new BPMNMessageSentEventImpl(entity);

        //when
        CloudBPMNMessageSentEvent cloudEvent = converter.from(runtimeEvent);

        //then
        CloudBPMNMessageEventAssert.assertThat(cloudEvent).hasEntity(entity);

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    public void shouldConvertBPMNMessageReceivedEventToCloudBPMNMessageReceivedEvent() {
        //given
        BPMNMessage entity = bpmnMessageEntity("entityId");

        BPMNMessageReceivedEvent runtimeEvent = new BPMNMessageReceivedEventImpl(entity);

        //when
        CloudBPMNMessageReceivedEvent cloudEvent = converter.from(runtimeEvent);

        //then
        CloudBPMNMessageEventAssert.assertThat(cloudEvent).hasEntity(entity);

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    public void shouldConvertMessageSubscriptionCancelledEventToCloudMessageSubscriptionCancelledEvent() {
        //given
        MessageSubscription entity = MessageSubscriptionImpl
            .builder()
            .withId("entityId")
            .withEventName("messageName")
            .withConfiguration("correlationKey")
            .withProcessDefinitionId("procDefId")
            .withProcessInstanceId("procInstId")
            .withBusinessKey("businessKey")
            .build();

        MessageSubscriptionCancelledEvent runtimeEvent = new MessageSubscriptionCancelledEventImpl(entity);

        //when
        CloudMessageSubscriptionCancelledEvent cloudEvent = converter.from(runtimeEvent);

        //then
        Assertions
            .assertThat(cloudEvent.getEntity())
            .isNotNull()
            .isEqualTo(entity)
            .extracting(
                MessageSubscription::getEventName,
                MessageSubscription::getConfiguration,
                MessageSubscription::getProcessDefinitionId,
                MessageSubscription::getProcessInstanceId,
                MessageSubscription::getBusinessKey
            )
            .contains(
                entity.getEventName(),
                entity.getConfiguration(),
                entity.getProcessDefinitionId(),
                entity.getProcessInstanceId(),
                entity.getBusinessKey()
            );

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    private BPMNMessage bpmnMessageEntity(String entityId) {
        BPMNMessageImpl entity = new BPMNMessageImpl("entityId");
        entity.setProcessInstanceId("procInstId");
        entity.setProcessDefinitionId("procDefId");

        MessageEventPayload payload = MessagePayloadBuilder
            .event("message")
            .withBusinessKey("businessId")
            .withCorrelationKey("correlationId")
            .withVariable("name", "value")
            .build();
        entity.setMessagePayload(payload);

        return entity;
    }

    static class CloudBPMNMessageEventAssert
        extends AbstractAssert<CloudBPMNMessageEventAssert, CloudBPMNMessageEvent> {

        public CloudBPMNMessageEventAssert(CloudBPMNMessageEvent actual, Class<?> selfType) {
            super(actual, selfType);
        }

        public static CloudBPMNMessageEventAssert assertThat(CloudBPMNMessageEvent actual) {
            return new CloudBPMNMessageEventAssert(actual, CloudBPMNMessageEventAssert.class);
        }

        public CloudBPMNMessageEventAssert hasEntity(BPMNMessage entity) {
            isNotNull();

            Assertions.assertThat(actual.getEntity()).isEqualTo(entity);
            Assertions
                .assertThat(actual.getEntity().getProcessDefinitionId())
                .isEqualTo(entity.getProcessDefinitionId());
            Assertions.assertThat(actual.getEntity().getProcessInstanceId()).isEqualTo(entity.getProcessInstanceId());

            return this;
        }
    }
}
