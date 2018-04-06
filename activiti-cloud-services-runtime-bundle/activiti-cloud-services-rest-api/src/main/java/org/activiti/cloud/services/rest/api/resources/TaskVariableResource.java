package org.activiti.cloud.services.rest.api.resources;

import org.activiti.cloud.services.api.model.TaskVariable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

public class TaskVariableResource extends Resource<TaskVariable> {
    public TaskVariableResource(TaskVariable content,
                                Link... links) {
        super(content,
                links);
    }
}
