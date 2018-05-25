package org.activiti.cloud.services.rest.api.resources;

import org.activiti.runtime.api.model.VariableInstance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

public class VariableInstanceResource extends Resource<VariableInstance> {
    public VariableInstanceResource(VariableInstance content,
                                    Link... links) {
        super(content,
                links);
    }
}
