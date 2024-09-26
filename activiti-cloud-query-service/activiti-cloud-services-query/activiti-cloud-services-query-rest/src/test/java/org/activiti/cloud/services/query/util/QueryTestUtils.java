package org.activiti.cloud.services.query.util;

import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    public static TaskSearchRequest buildTaskSearchRequestWithProcessVariableFilter(VariableFilter variableFilter) {
        return buildTaskSearchRequestWithProcessVariableFilters(Set.of(variableFilter));
    }

    @NotNull
    public static TaskSearchRequest buildTaskSearchRequestWithProcessVariableFilters(
        Set<VariableFilter> variableFilters
    ) {
        Set<ProcessVariableKey> processVariableKeys = variableFilters
            .stream()
            .map(variableFilter -> new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name()))
            .collect(Collectors.toSet());
        return new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            variableFilters,
            processVariableKeys
        );
    }

    @NotNull
    public static TaskSearchRequest buildTaskSearchRequestWithTaskVariableFilter(VariableFilter... variableFilter) {
        return new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of(variableFilter),
            null,
            null
        );
    }

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
