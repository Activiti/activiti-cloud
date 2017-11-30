package org.activiti.cloud.connectors.starter.channels;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.stereotype.Component;

@Component
@EnableBinding({CloudConnectorChannels.class, ProcessRuntimeChannels.class})
public class CloudConnectorIntegrationHandler {

}
