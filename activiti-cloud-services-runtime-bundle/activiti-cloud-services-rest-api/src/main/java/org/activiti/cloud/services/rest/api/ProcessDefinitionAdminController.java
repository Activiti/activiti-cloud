package org.activiti.cloud.services.rest.api;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/admin/v1/process-definitions",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessDefinitionAdminController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources getAllProcessDefinitions(Pageable pageable);

}
