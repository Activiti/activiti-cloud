package org.activiti.cloud.services.query.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;

public class ProcessInstanceBuilder {

    private final ProcessInstanceEntity process;
    private final Set<TaskBuilder> taskBuffer = new HashSet<>();

    private final VariableRepository variableRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    public ProcessInstanceBuilder(
        VariableRepository variableRepository,
        ProcessInstanceRepository processInstanceRepository
    ) {
        this.variableRepository = variableRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.process = new ProcessInstanceEntity();
        this.process.setId(UUID.randomUUID().toString());
        this.process.setName(UUID.randomUUID().toString());
        this.withProcessDefinitionKey(UUID.randomUUID().toString());
    }

    public ProcessInstanceBuilder withProcessDefinitionKey(String processDefinitionKey) {
        process.setProcessDefinitionKey(processDefinitionKey);
        return this;
    }

    public ProcessInstanceBuilder withVariables(QueryTestUtils.VariableInput... variables) {
        process.setVariables(
            Arrays
                .stream(variables)
                .map(variable -> {
                    ProcessVariableEntity processVariable = new ProcessVariableEntity();
                    processVariable.setProcessInstanceId(process.getId());
                    processVariable.setProcessDefinitionKey(process.getProcessDefinitionKey());
                    processVariable.setName(variable.name());
                    processVariable.setType(variable.type().name().toLowerCase());
                    processVariable.setValue(variable.value());
                    return processVariable;
                })
                .collect(Collectors.toSet())
        );
        return this;
    }

    public ProcessInstanceBuilder withTasks(TaskBuilder... tasks) {
        taskBuffer.addAll(Arrays.asList(tasks));
        return this;
    }

    public ProcessInstanceEntity buildAndSave() {
        variableRepository.saveAll(process.getVariables());
        process.setTasks(
            taskBuffer
                .stream()
                .map(builder -> builder.withParentProcess(process))
                .map(TaskBuilder::buildAndSave)
                .collect(Collectors.toSet())
        );
        return processInstanceRepository.save(process);
    }
}
