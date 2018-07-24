package org.activiti.cloud.examples.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectorController {

    @RequestMapping(method = RequestMethod.GET, path = "/")
    public String welcome() {
        return " { \"welcome\" : \"This is a Cloud Connector\" }";
    }

}
