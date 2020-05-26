/*
 *   Copyright 2017-2020 Alfresco Software, Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.UUID;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class TaskRepositoryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskVariableRepository taskVariableRepository;

    @AfterEach
    void tearDown() {
        taskVariableRepository.deleteAll();
        taskRepository.deleteAll();
    }


    @Test
    public void shouldFilterByVariableNameAndValue() {

        TaskEntity task1 = createTask("t1");
        TaskEntity task2 = createTask("t2");

        TaskVariableEntity variable = createVariable(task1, "outcome", "approved");
        createVariable(task2, "outcome", "rejected");
        createVariable(task2, "anotherVariable", "approved");



        Page<TaskEntity> approvedTasks = taskRepository
            .findByVariablesNameAndVariablesInternalValue("outcome", new VariableValue<>("approved"),
                PageRequest.of(0, 10));
        Page<TaskEntity> rejectedTasks = taskRepository
            .findByVariablesNameAndVariablesInternalValue("outcome", new VariableValue<>("rejected"),
                PageRequest.of(0, 10));
        Page<TaskEntity> nonMatchingTasks = taskRepository
            .findByVariablesNameAndVariablesInternalValue("outcome", new VariableValue<>("notMatching"),
                PageRequest.of(0, 10));

        assertThat(approvedTasks).extracting(TaskEntity::getName).containsExactly("t1");
        assertThat(rejectedTasks).extracting(TaskEntity::getName).containsExactly("t2");
        assertThat(nonMatchingTasks).extracting(TaskEntity::getName).isEmpty();


    }

    private <T> TaskVariableEntity createVariable(TaskEntity task, String variableName, T variableValue) {
        TaskVariableEntity variableEntity = new TaskVariableEntity();
        variableEntity.setTask(task);
        variableEntity.setTaskId(task.getId());
        variableEntity.setName(variableName);
        variableEntity.setValue(variableValue);
        taskVariableRepository.save(variableEntity);
        task.setVariables(Collections.singleton(variableEntity));
        taskRepository.save(task);
        return variableEntity;
    }

    private TaskEntity createTask(String name) {
        TaskEntity task1 = new TaskEntity();
        task1.setId(UUID.randomUUID().toString());
        task1.setName(name);
        task1.setStatus(TaskStatus.CREATED);
        taskRepository.save(task1);
        return task1;
    }
}
