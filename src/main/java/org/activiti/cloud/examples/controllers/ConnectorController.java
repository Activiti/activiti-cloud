package org.activiti.cloud.examples.controllers;

import org.activiti.cloud.examples.connectors.ExampleConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectorController {

    @Autowired
    private ExampleConnector exampleConnector;

    @RequestMapping(method = RequestMethod.GET, path = "/")
    public String welcome() {
        return " { \"welcome\" : \"This is Example Cloud Connector\"," +
                "  \"var1\" : \""+exampleConnector.getVar1Copy()+"\" }";
    }

}
