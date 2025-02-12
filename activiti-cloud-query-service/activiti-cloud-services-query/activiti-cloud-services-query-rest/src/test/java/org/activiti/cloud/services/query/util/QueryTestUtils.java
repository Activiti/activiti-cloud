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
package org.activiti.cloud.services.query.util;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryTestUtils {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private ServiceTaskRepository serviceTaskRepository;

    public void cleanUp() {
        taskVariableRepository.deleteAll();
        taskRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        serviceTaskRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    public ProcessInstanceBuilder buildProcessInstance() {
        return new ProcessInstanceBuilder(variableRepository, processInstanceRepository);
    }

    public TaskBuilder buildTask() {
        return new TaskBuilder(
            taskRepository,
            taskVariableRepository,
            taskCandidateUserRepository,
            taskCandidateGroupRepository
        );
    }

    public ServiceTaskBuilder buildServiceTask() {
        return new ServiceTaskBuilder(serviceTaskRepository);
    }

    public record VariableInput(String name, VariableType type, Object value) {
        public String getValue() {
            return value.toString();
        }
    }
}
