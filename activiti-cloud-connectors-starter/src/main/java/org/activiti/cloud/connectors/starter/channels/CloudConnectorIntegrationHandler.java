package org.activiti.cloud.connectors.starter.channels;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(CloudConnectorChannels.class)
public class CloudConnectorIntegrationHandler {


    @Autowired
    public CloudConnectorIntegrationHandler() {

    }


}
