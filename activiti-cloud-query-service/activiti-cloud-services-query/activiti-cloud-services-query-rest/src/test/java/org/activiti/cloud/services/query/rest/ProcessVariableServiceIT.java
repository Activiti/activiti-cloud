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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
class ProcessVariableServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ProcessVariableService processVariableService;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Test
    void should_fetchProcessVariables_forProcessInstances() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            "process1",
            Map.of("var1", "value1", "var2", "value2")
        );
        ProcessInstanceEntity processInstance2 = createProcessInstance(
            "process2",
            Map.of("var1", "value1", "var2", "value2")
        );

        Set<ProcessVariableKey> variableKeys = Set.of(
            new ProcessVariableKey("process1", "var1"),
            new ProcessVariableKey("process2", "var2")
        );

        processVariableService.fetchProcessVariablesForProcessInstances(
            Set.of(processInstance1, processInstance2),
            variableKeys
        );

        assertThat(processInstance1.getVariables())
            .hasSize(1)
            .first()
            .satisfies(processVariableEntity -> {
                assertThat(processVariableEntity.getName()).isEqualTo("var1");
                assertThat((String) processVariableEntity.getValue()).isEqualTo("value1");
            });

        assertThat(processInstance2.getVariables())
            .hasSize(1)
            .first()
            .satisfies(processVariableEntity -> {
                assertThat(processVariableEntity.getName()).isEqualTo("var2");
                assertThat((String) processVariableEntity.getValue()).isEqualTo("value2");
            });
    }

    @Test
    void should_fetchProcessVariables_forTasks() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            "process1",
            Map.of("var1", "value1", "var2", "value2")
        );
        ProcessInstanceEntity processInstance2 = createProcessInstance(
            "process2",
            Map.of("var1", "value1", "var2", "value2")
        );

        Set<ProcessVariableKey> variableKeys = Set.of(
            new ProcessVariableKey("process1", "var1"),
            new ProcessVariableKey("process2", "var2")
        );

        TaskEntity task1 = new TaskEntity();
        task1.setId(UUID.randomUUID().toString());
        task1.setProcessInstanceId(processInstance1.getId());
        task1.setProcessVariables(processInstance1.getVariables());
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId(UUID.randomUUID().toString());
        task2.setProcessInstanceId(processInstance2.getId());
        task2.setProcessVariables(processInstance2.getVariables());
        taskRepository.save(task2);

        processVariableService.fetchProcessVariablesForTasks(Set.of(task1, task2), variableKeys);

        assertThat(task1.getProcessVariables())
            .hasSize(1)
            .first()
            .satisfies(processVariableEntity -> {
                assertThat(processVariableEntity.getName()).isEqualTo("var1");
                assertThat((String) processVariableEntity.getValue()).isEqualTo("value1");
            });

        assertThat(task2.getProcessVariables())
            .hasSize(1)
            .first()
            .satisfies(processVariableEntity -> {
                assertThat(processVariableEntity.getName()).isEqualTo("var2");
                assertThat((String) processVariableEntity.getValue()).isEqualTo("value2");
            });
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefKey, Map<String, Object> variables) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName(UUID.randomUUID().toString());
        processInstanceEntity.setProcessDefinitionKey(processDefKey);
        processInstanceEntity.setAppVersion(UUID.randomUUID().toString());
        processInstanceRepository.save(processInstanceEntity);

        Set<ProcessVariableEntity> processVariables = variables
            .entrySet()
            .stream()
            .map(entry -> {
                ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
                processVariableEntity.setName(entry.getKey());
                processVariableEntity.setValue(entry.getValue());
                processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
                processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
                variableRepository.save(processVariableEntity);
                return processVariableEntity;
            })
            .collect(Collectors.toSet());

        processInstanceEntity.setVariables(processVariables);
        return processInstanceRepository.save(processInstanceEntity);
    }
}
