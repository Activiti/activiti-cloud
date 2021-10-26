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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.Ordered;

import java.util.AbstractMap;
import java.util.Map;

public class ConnectorDestinationsBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorImplementationsProvider.class);

    private final ConnectorImplementationsProvider destinationsProvider;
    private final ConnectorDestinationMappingStrategy destinationMappingStrategy;

    public ConnectorDestinationsBeanPostProcessor(ConnectorImplementationsProvider destinationsProvider,
                                                  ConnectorDestinationMappingStrategy destinationMappingStrategy) {
        this.destinationsProvider = destinationsProvider;
        this.destinationMappingStrategy = destinationMappingStrategy;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (BindingServiceProperties.class.isInstance(bean)) {
            BindingServiceProperties bindingServiceProperties = BindingServiceProperties.class.cast(bean);

            destinationsProvider.getImplementations()
                                .stream()
                                .map(this::getDestination)
                                .map(entry -> applyDestination(bindingServiceProperties, entry))
                                .forEach(this::log);
        }

        return bean;
    }

    protected Map.Entry<String, String> getDestination(String implementation) {
        String destination = destinationMappingStrategy.apply(implementation);

        return new AbstractMap.SimpleEntry<>(implementation,
                                             destination);
    }

    protected Map.Entry<String, BindingProperties> applyDestination(BindingServiceProperties bindingServiceProperties,
                                                                    Map.Entry<String, String> entry) {
        BindingProperties bindingProperties = bindingServiceProperties.getBindingProperties(entry.getKey());

        bindingProperties.setDestination(entry.getValue());

        return new AbstractMap.SimpleEntry<String, BindingProperties>(entry.getKey(),
                                                                      bindingProperties);
    }


    protected void log(Map.Entry<String, BindingProperties> entry) {
        logger.info("Configured Connector '{}' implementation to '{}' destination",
                    entry.getKey(),
                    entry.getValue()
                         .getDestination());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
