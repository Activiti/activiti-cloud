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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.runtime.event.impl.VariableCreatedEventImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.spring.process.CachingProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.VariableDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToCloudVariableEventConverterTest {

    @InjectMocks
    private ToCloudVariableEventConverter toCloudVariableEventConverter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    @Mock
    private CachingProcessExtensionService processExtensionService;

    @Test
    void should_convertToCloudVariableCreatedEventWithVariableDefinitionId() {
        VariableInstance variableInstance = new VariableInstanceImpl<>("variableName", "string", "example", "processInstanceId", null);
        VariableCreatedEventImpl event = new VariableCreatedEventImpl(variableInstance, "processDefinitionId");

        Extension extension = new Extension();
        HashMap<String, VariableDefinition> properties = new HashMap<>();
        VariableDefinition variableDefinition = new VariableDefinition();
        variableDefinition.setName("variableName");
        variableDefinition.setId("variableDefinitionId");
        properties.put("variableDefinitionId", variableDefinition);
        extension.setProperties(properties);

        when(processExtensionService.getExtensionsForId("processDefinitionId")).thenReturn(extension);

        CloudVariableCreatedEvent cloudVariableCreatedEvent = toCloudVariableEventConverter.from(event);

        assertThat(cloudVariableCreatedEvent.getVariableDefinitionId()).isEqualTo("variableDefinitionId");
        VariableInstance entity = cloudVariableCreatedEvent.getEntity();
        assertThat(entity.getName()).isEqualTo("variableName");
        assertThat(entity.getType()).isEqualTo("string");
        assertThat((String) entity.getValue()).isEqualTo("example");
        assertThat(entity.getProcessInstanceId()).isEqualTo("processInstanceId");
    }
}
