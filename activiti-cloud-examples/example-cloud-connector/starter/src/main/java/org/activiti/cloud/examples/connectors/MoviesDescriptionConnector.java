/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.examples.connectors;

import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@ConnectorBinding(
    input = MoviesDescriptionConnectorChannels.MOVIES_DESCRIPTION_CONSUMER,
    condition = "",
    outputHeader = ""
)
@Component(MoviesDescriptionConnectorChannels.MOVIES_DESCRIPTION_CONSUMER + "Connector")
public class MoviesDescriptionConnector implements ConsumerConnector<IntegrationRequest> {

    private Logger logger = LoggerFactory.getLogger(MoviesDescriptionConnector.class);

    private IntegrationResultSender integrationResultSender;
    private ConnectorProperties connectorProperties;

    public MoviesDescriptionConnector(
        IntegrationResultSender integrationResultSender,
        ConnectorProperties connectorProperties
    ) {
        this.integrationResultSender = integrationResultSender;
        this.connectorProperties = connectorProperties;
    }

    @Override
    public void accept(IntegrationRequest integrationRequest) {
        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();
        Map<String, Object> inBoundVariables = integrationContext.getInBoundVariables();
        logger.info(">>inbound: " + inBoundVariables);
        integrationContext.addOutBoundVariable(
            "movieDescription",
            "The Lord of the Rings is an epic high fantasy novel written by English author and scholar J. R. R. Tolkien"
        );

        integrationResultSender.send(
            IntegrationResultBuilder.resultFor(integrationRequest, connectorProperties).buildMessage()
        );
    }
}
