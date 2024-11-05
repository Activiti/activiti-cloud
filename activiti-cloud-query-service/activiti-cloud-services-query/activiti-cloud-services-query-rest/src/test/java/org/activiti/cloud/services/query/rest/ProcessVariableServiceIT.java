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

import java.util.Set;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.util.QueryTestUtils;
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
    private QueryTestUtils queryTestUtils;

    @Test
    void should_fetchProcessVariables_forProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process1")
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process2")
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

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
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process1")
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process2")
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        Set<ProcessVariableKey> variableKeys = Set.of(
            new ProcessVariableKey("process1", "var1"),
            new ProcessVariableKey("process2", "var2")
        );

        TaskEntity task1 = processInstance1.getTasks().iterator().next();
        TaskEntity task2 = processInstance2.getTasks().iterator().next();

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
}
