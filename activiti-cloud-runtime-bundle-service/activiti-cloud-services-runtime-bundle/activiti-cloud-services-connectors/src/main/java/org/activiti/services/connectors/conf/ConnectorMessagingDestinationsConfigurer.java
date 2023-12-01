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

package org.activiti.services.connectors.conf;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.Ordered;

public class ConnectorMessagingDestinationsConfigurer implements InitializingBean, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorImplementationsProvider.class);

    private final ConnectorImplementationsProvider destinationsProvider;
    private final ConnectorDestinationMappingStrategy destinationMappingStrategy;
    private final BindingServiceProperties bindingServiceProperties;

    public ConnectorMessagingDestinationsConfigurer(
        ConnectorImplementationsProvider destinationsProvider,
        ConnectorDestinationMappingStrategy destinationMappingStrategy,
        BindingServiceProperties bindingServiceProperties
    ) {
        this.destinationsProvider = destinationsProvider;
        this.destinationMappingStrategy = destinationMappingStrategy;
        this.bindingServiceProperties = bindingServiceProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        destinationsProvider
            .getImplementations()
            .stream()
            .map(this::resolveBindingDestination)
            .map(this::applyBindingDestination)
            .forEach(this::log);
    }

    protected Map.Entry<String, String> resolveBindingDestination(String implementation) {
        String destination = destinationMappingStrategy.apply(implementation);

        return Map.entry(implementation, destination);
    }

    protected Map.Entry<String, BindingProperties> applyBindingDestination(Map.Entry<String, String> entry) {
        BindingProperties bindingProperties = bindingServiceProperties.getBindingProperties(entry.getKey());

        bindingProperties.setDestination(entry.getValue());

        return Map.entry(entry.getKey(), bindingProperties);
    }

    protected void log(Map.Entry<String, BindingProperties> entry) {
        logger.warn(
            "Configured Connector '{}' implementation to '{}' destination",
            entry.getKey(),
            entry.getValue().getDestination()
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
