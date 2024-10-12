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

package org.activiti.cloud.common.messaging.config;

import java.util.Optional;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class ActivitiMessagingDestinationTransformer implements Function<String, String> {

    private static final Logger log = LoggerFactory.getLogger(ActivitiMessagingDestinationTransformer.class);

    private final ActivitiCloudMessagingProperties messagingProperties;

    public ActivitiMessagingDestinationTransformer(ActivitiCloudMessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Override
    public String apply(String source) {
        ActivitiCloudMessagingProperties.DestinationProperties destinationProperties = messagingProperties
            .getDestinations()
            .get(source);
        String prefix = Optional
            .ofNullable(destinationProperties)
            .map(ActivitiCloudMessagingProperties.DestinationProperties::getPrefix)
            .orElseGet(this::getPrefix);

        String separator = Optional
            .ofNullable(destinationProperties)
            .map(ActivitiCloudMessagingProperties.DestinationProperties::getSeparator)
            .orElseGet(this::getSeparator);

        String scope = Optional
            .ofNullable(destinationProperties)
            .map(ActivitiCloudMessagingProperties.DestinationProperties::getScope)
            .orElse(null);

        String name = Optional
            .ofNullable(destinationProperties)
            .map(it -> it.getName())
            .filter(StringUtils::hasText)
            .orElse(source);

        log.debug(
            "Processing source destination '{}' with prefix '{}' and separator '{} to target name '{}' with scope '{}'",
            source,
            prefix,
            separator,
            name,
            scope
        );

        StringBuilder value = new StringBuilder();

        if (StringUtils.hasText(prefix)) {
            value.append(prefix).append(separator);
        }

        value.append(name);

        if (StringUtils.hasText(scope)) {
            value.append(separator).append(scope);
        }

        String target = value.toString();

        return messagingProperties.isDestinationTransformersEnabled()
            ? messagingProperties.transformDestination().apply(target)
            : target;
    }

    public String getPrefix() {
        return messagingProperties.getDestinationPrefix();
    }

    public String getSeparator() {
        return messagingProperties.getDestinationSeparator();
    }
}
