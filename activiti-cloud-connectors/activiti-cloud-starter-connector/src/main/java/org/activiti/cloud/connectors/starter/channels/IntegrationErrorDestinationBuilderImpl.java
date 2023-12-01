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
package org.activiti.cloud.connectors.starter.channels;

import java.util.Optional;
import java.util.function.Predicate;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.util.ObjectUtils;

public class IntegrationErrorDestinationBuilderImpl implements IntegrationErrorDestinationBuilder {

    private final ConnectorProperties connectorProperties;

    public IntegrationErrorDestinationBuilderImpl(ConnectorProperties connectorProperties) {
        this.connectorProperties = connectorProperties;
    }

    @Override
    public String buildDestination(IntegrationRequest event) {
        String errorDestinationOverride = connectorProperties.getErrorDestinationOverride();

        String destination = ObjectUtils.isEmpty(errorDestinationOverride)
            ? Optional
                .of(event)
                .map(IntegrationRequest::getErrorDestination)
                .filter(Predicate.not(ObjectUtils::isEmpty))
                .orElseGet(() -> this.getServiceDestination(event))
            : errorDestinationOverride;

        return destination;
    }

    private String getServiceDestination(IntegrationRequest event) {
        return new StringBuilder("integrationError")
            .append(connectorProperties.getMqDestinationSeparator())
            .append(event.getServiceFullName())
            .toString();
    }
}
