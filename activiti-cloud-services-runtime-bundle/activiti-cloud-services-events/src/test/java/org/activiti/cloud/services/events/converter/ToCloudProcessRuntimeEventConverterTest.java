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

import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ToCloudProcessRuntimeEventConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessStartedEventToExternalEvent() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");

        ProcessStartedEventImpl event = new ProcessStartedEventImpl(processInstance);
        event.setNestedProcessDefinitionId("myParentProcessDef");
        event.setNestedProcessInstanceId("2");

        //when
        CloudProcessStartedEvent processStarted = converter.from(event);

        //then
        assertThat(processStarted).isInstanceOf(CloudProcessStartedEvent.class);

        assertThat(processStarted.getEntity().getId()).isEqualTo("10");
        assertThat(processStarted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStarted.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStarted.getNestedProcessInstanceId()).isEqualTo("2");

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
}