package org.activiti.cloud.starter.rb.configuration;

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

import org.activiti.cloud.common.messaging.config.ActivitiMessagingDestinationTransformer;
import org.activiti.services.connectors.conf.ConnectorDestinationMappingStrategy;

import java.util.Optional;

public class ActivitiConnectorDestinationMappingStrategy implements ConnectorDestinationMappingStrategy {
    private static final String REPLACEMENT = "-";
    private static final String ILLEGAL_CHARACTERS = "[\\t\\s*#:]";

    private final ActivitiMessagingDestinationTransformer destinationTransformer;

    public ActivitiConnectorDestinationMappingStrategy(ActivitiMessagingDestinationTransformer destinationTransformer) {
        this.destinationTransformer = destinationTransformer;
    }

    @Override
    public String apply(String implementation) {
        return Optional.ofNullable(destinationTransformer.apply(implementation))
                       .map(this::escapeIllegalCharacters)
                       .map(String::toLowerCase)
                       .orElse(implementation);
    }

    protected String escapeIllegalCharacters(String value) {
        return value.replaceAll(ILLEGAL_CHARACTERS, REPLACEMENT);
    }

}
