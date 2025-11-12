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
package org.activiti.cloud.services.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.VariableDefinition;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.VariableDefinitionImpl;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionServiceTest {

    private static final String NO_USER_STARTABLE_PROCESSES = "noUserStartableProcesses";
    private static final String VARIABLES = "variables";
    private final ProcessRuntime processRuntime = Mockito.mock(ProcessRuntime.class);

    private final ProcessDefinitionDecorator processDefinitionDecorator = Mockito.mock(
        ProcessDefinitionDecorator.class
    );

    private final ProcessDefinitionService processDefinitionService = new ProcessDefinitionService(
        processRuntime,
        List.of(processDefinitionDecorator)
    );

    @Test
    void should_getProcessDefinitionsWithVariables_whenIncludeVariablesParameterPresent() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("id");
        ArrayList<ProcessDefinition> processDefinitions = new ArrayList<>();
        processDefinitions.add(processDefinition);
        when(
            processRuntime.processDefinitions(
                any(Pageable.class),
                any(GetProcessDefinitionsPayload.class),
                eq(List.of(VARIABLES))
            )
        )
            .thenReturn(new PageImpl<>(processDefinitions, 1));

        VariableDefinitionImpl variableDefinition = new VariableDefinitionImpl();
        when(processDefinitionDecorator.applies(VARIABLES)).thenReturn(true);
        when(
            processDefinitionDecorator.decorate(argThat(argument -> argument.getId().equals(processDefinition.getId())))
        )
            .thenAnswer(call -> {
                CloudProcessDefinitionImpl cloudProcessDefinition = new CloudProcessDefinitionImpl(processDefinition);
                cloudProcessDefinition.setVariableDefinitions(List.of(variableDefinition));
                return cloudProcessDefinition;
            });

        List<ProcessDefinition> result = processDefinitionService
            .getProcessDefinitions(Pageable.of(0, 50), List.of(VARIABLES))
            .getContent();

        assertThat(result).hasSize(1);
        List<VariableDefinition> variableDefinitions =
            ((ExtendedCloudProcessDefinition) result.getFirst()).getVariableDefinitions();
        assertThat(variableDefinitions).hasSize(1);
        assertThat(variableDefinitions.getFirst()).isEqualTo(variableDefinition);
        verify(processDefinitionDecorator)
            .decorate(argThat(argument -> argument.getId().equals(processDefinition.getId())));
    }

    @ParameterizedTest
    @MethodSource("emptyIncludeVariables")
    void should_getProcessDefinitionsWithVariables_whenIncludeVariablesParameterNotPresent(List<String> include) {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("id");
        ArrayList<ProcessDefinition> processDefinitions = new ArrayList<>();
        processDefinitions.add(processDefinition);
        when(processRuntime.processDefinitions(any(Pageable.class), any(GetProcessDefinitionsPayload.class), any()))
            .thenReturn(new PageImpl<>(processDefinitions, 1));

        lenient().when(processDefinitionDecorator.applies(VARIABLES)).thenReturn(true);

        List<ProcessDefinition> result = processDefinitionService
            .getProcessDefinitions(Pageable.of(0, 50), include)
            .getContent();

        assertThat(result).hasSize(1);
        verify(processDefinitionDecorator, never()).decorate(any());
    }

    @Test
    void should_getProcessDefinitionsWithVariablesAndNoUserStartableProcesses_whenIncludeVariablesAndNoUserStartableProcessesParametersPresent() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("id");
        ArrayList<ProcessDefinition> processDefinitions = new ArrayList<>();
        processDefinitions.add(processDefinition);
        when(
            processRuntime.processDefinitions(
                any(Pageable.class),
                any(GetProcessDefinitionsPayload.class),
                eq(List.of(VARIABLES, NO_USER_STARTABLE_PROCESSES))
            )
        )
            .thenReturn(new PageImpl<>(processDefinitions, 1));

        VariableDefinitionImpl variableDefinition = new VariableDefinitionImpl();
        when(processDefinitionDecorator.applies(VARIABLES)).thenReturn(true);
        when(
            processDefinitionDecorator.decorate(argThat(argument -> argument.getId().equals(processDefinition.getId())))
        )
            .thenAnswer(call -> {
                CloudProcessDefinitionImpl cloudProcessDefinition = new CloudProcessDefinitionImpl(processDefinition);
                cloudProcessDefinition.setVariableDefinitions(List.of(variableDefinition));
                return cloudProcessDefinition;
            });

        var pageable = Pageable.of(0, 50);

        List<ProcessDefinition> result = processDefinitionService
            .getProcessDefinitions(pageable, List.of(VARIABLES, NO_USER_STARTABLE_PROCESSES))
            .getContent();

        assertThat(result).hasSize(1);
        List<VariableDefinition> variableDefinitions =
            ((ExtendedCloudProcessDefinition) result.getFirst()).getVariableDefinitions();
        assertThat(variableDefinitions).containsExactly(variableDefinition);
        verify(processDefinitionDecorator)
            .decorate(argThat(argument -> argument.getId().equals(processDefinition.getId())));

        ArgumentCaptor<List<String>> includeParameter = ArgumentCaptor.forClass(List.class);
        verify(processRuntime)
            .processDefinitions(eq(pageable), any(GetProcessDefinitionsPayload.class), includeParameter.capture());
        assertThat(includeParameter.getValue()).containsExactly(VARIABLES, NO_USER_STARTABLE_PROCESSES);
    }

    @Test
    void should_getProcessDefinitionsWithNoUserStartableProcesses_whenIncludeNoUserStartableProcessesParameterPresent() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("id");
        ArrayList<ProcessDefinition> processDefinitions = new ArrayList<>();
        processDefinitions.add(processDefinition);
        when(
            processRuntime.processDefinitions(
                any(Pageable.class),
                any(GetProcessDefinitionsPayload.class),
                eq(List.of(NO_USER_STARTABLE_PROCESSES))
            )
        )
            .thenReturn(new PageImpl<>(processDefinitions, 1));

        when(processDefinitionDecorator.applies(VARIABLES)).thenReturn(false);

        var pageable = Pageable.of(0, 50);

        List<ProcessDefinition> result = processDefinitionService
            .getProcessDefinitions(pageable, List.of(NO_USER_STARTABLE_PROCESSES))
            .getContent();

        assertThat(result).hasSize(1);
        List<VariableDefinition> variableDefinitions =
            ((ExtendedCloudProcessDefinition) result.getFirst()).getVariableDefinitions();
        assertThat(variableDefinitions).isEmpty();

        verify(processDefinitionDecorator, never()).decorate(any());

        ArgumentCaptor<List<String>> includeParameter = ArgumentCaptor.forClass(List.class);
        verify(processRuntime)
            .processDefinitions(eq(pageable), any(GetProcessDefinitionsPayload.class), includeParameter.capture());
        assertThat(includeParameter.getValue()).containsExactly(NO_USER_STARTABLE_PROCESSES);
    }

    private static Stream<Arguments> emptyIncludeVariables() {
        return Stream.of(Arguments.of(List.of()), Arguments.of(List.of("")), Arguments.of(List.of("other")));
    }
}
