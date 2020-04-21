package org.activiti.cloud.services.rest.api;

import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1")
public interface HomeController {

    @RequestMapping(method = RequestMethod.GET)
    EntityModel getHomeInfo();
}
