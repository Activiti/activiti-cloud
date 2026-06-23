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
package org.activiti.cloud.services.core.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.services.core.ProcessDefinitionValuesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProcessDefinitionConstantValuesDecoratorTest {

    @Mock
    private ProcessDefinitionValuesService processDefinitionValuesService;

    @InjectMocks
    private ProcessDefinitionConstantValuesDecorator decorator;

    @Test
    void should_return_constant_values_when_decorate() {
        var processDefinition = new CloudProcessDefinitionImpl();
        processDefinition.setId("processId");
        Map<String, Object> constantValues = Map.of("constantKey", "constantValue");

        when(processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId")).thenReturn(
            constantValues
        );

        var result = decorator.decorate(processDefinition);

        assertThat(result.getConstantValues()).hasSize(1).containsEntry("constantKey", "constantValue");
    }

    @Test
    void should_return_empty_constant_values_when_no_constants_in_decorate() {
        var processDefinition = new CloudProcessDefinitionImpl();
        processDefinition.setId("processId");

        when(processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId")).thenReturn(
            Map.of()
        );

        var result = decorator.decorate(processDefinition);

        assertThat(result.getConstantValues()).isEmpty();
    }

    @Test
    void should_handle_constant_values() {
        assertThat(decorator.getHandledValue()).isEqualTo("constant-values");
    }
}
