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
package org.activiti.cloud.services.events.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.activiti.engine.ManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageChannel;

@ExtendWith(MockitoExtension.class)
public class CloudProcessDeletedServiceTest {

    private CloudProcessDeletedService cloudProcessDeletedService;

    @Mock
    private RuntimeBundleProperties properties;

    @Mock
    private ProcessEngineChannels producer;

    @Mock
    private MessageChannel auditProducer;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Mock
    private ManagementService managementService;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @BeforeEach
    public void setUp() {
        RuntimeBundleInfoAppender runtimeBundleInfoAppender = new RuntimeBundleInfoAppender(properties);
        RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory = new RuntimeBundleMessageBuilderFactory(
            properties
        );
        cloudProcessDeletedService =
            new CloudProcessDeletedService(
                producer,
                runtimeBundleMessageBuilderFactory,
                runtimeBundleInfoAppender,
                managementService,
                processEngineEventsAggregator
            );
    }

    private void setProperties() {
        when(producer.auditProducer()).thenReturn(auditProducer);
        when(properties.getAppName()).thenReturn("an");
        when(properties.getServiceName()).thenReturn("sn");
        when(properties.getServiceFullName()).thenReturn("sfn");
        when(properties.getServiceType()).thenReturn("st");
        when(properties.getServiceVersion()).thenReturn("sv");
    }

    @Test
    public void should_sendDeleteEvent() {
        //given
        setProperties();

        //when
        cloudProcessDeletedService.sendDeleteEvent("1");

        //then
        verify(auditProducer)
            .send(
                argThat(arg ->
                    ((List<CloudRuntimeEvent>) arg.getPayload()).stream()
                        .filter(CloudProcessDeletedEventImpl.class::isInstance)
                        .map(CloudProcessDeletedEventImpl.class::cast)
                        .anyMatch(it -> it.getEntity().getId().equals("1"))
                )
            );
    }

    @Test
    public void should_deleteProcessInstance() {
        //given

        //when
        cloudProcessDeletedService.delete("1");

        ArgumentCaptor<DeleteCloudProcessInstanceCmd> deleteProcessPayloadArgument = ArgumentCaptor.forClass(
            DeleteCloudProcessInstanceCmd.class
        );

        //then
        verify(managementService).executeCommand(deleteProcessPayloadArgument.capture());

        assertThat(deleteProcessPayloadArgument.getValue()).isNotNull();
    }
}
