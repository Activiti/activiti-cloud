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

package org.activiti.cloud.services.events.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.BPMNError;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.runtime.event.impl.BPMNErrorReceivedEventImpl;
import org.activiti.api.runtime.model.impl.BPMNErrorImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ToCloudProcessRuntimeErrorEventsConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertBPMNErrorReceivedEventToCloudBPMNErrorReceivedEvent() {
        BPMNError entity = bpmnErrorEntity("entityId");

        BPMNErrorReceivedEvent runtimeEvent = new BPMNErrorReceivedEventImpl(entity);

        CloudBPMNErrorReceivedEvent cloudEvent = converter.from(runtimeEvent);

        assertThat(cloudEvent.getEntity()).isEqualTo(entity);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }

    private BPMNError bpmnErrorEntity(String entityId) {
        BPMNErrorImpl entity = new BPMNErrorImpl(entityId);
        entity.setProcessInstanceId("procInstId");
        entity.setProcessDefinitionId("procDefId");
        entity.setErrorCode("errorCode");
        entity.setErrorId("errorId");
        return entity;
    }
}
