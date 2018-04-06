package org.activiti.cloud.services.rest.api.resources;

import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

public class ProcessVariableResource extends Resource<ProcessInstanceVariable> {
    public ProcessVariableResource(ProcessInstanceVariable content,
                                   Link... links) {
        super(content,
                links);
    }
}
