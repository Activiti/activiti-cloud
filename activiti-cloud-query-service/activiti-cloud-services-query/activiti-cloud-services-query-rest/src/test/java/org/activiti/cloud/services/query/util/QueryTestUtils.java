package org.activiti.cloud.services.query.util;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
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

    public void cleanUp() {
        taskVariableRepository.deleteAll();
        taskRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
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

    public record VariableInput(String name, VariableType type, Object value) {
        public String getValue() {
            return value.toString();
        }
    }
}
