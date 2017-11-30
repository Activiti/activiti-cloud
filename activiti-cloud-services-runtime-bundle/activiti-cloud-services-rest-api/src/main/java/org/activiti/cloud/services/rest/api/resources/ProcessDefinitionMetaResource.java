package org.activiti.cloud.services.rest.api.resources;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

public class ProcessDefinitionMetaResource extends Resource<ProcessDefinitionMeta> {

    public ProcessDefinitionMetaResource(ProcessDefinitionMeta content, Link... links) {
        super(content, links);
    }

}
