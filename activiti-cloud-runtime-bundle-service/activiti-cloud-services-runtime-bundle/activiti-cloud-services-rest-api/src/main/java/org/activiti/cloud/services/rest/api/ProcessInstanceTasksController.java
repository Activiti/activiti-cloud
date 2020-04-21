package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/process-instances/{processInstanceId}", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceTasksController {

    @RequestMapping(value = "/tasks", method = RequestMethod.GET)
    PagedModel<EntityModel<CloudTask>> getTasks(@PathVariable String processInstanceId,
                                                 Pageable pageable);
}
