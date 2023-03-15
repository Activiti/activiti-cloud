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

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@AutoConfigureBefore(BinderFactoryAutoConfiguration.class)
@ConditionalOnClass(BindingServiceProperties.class)
public class ActivitiMessagingDestinationsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ActivitiMessagingDestinationTransformer activitiMessagingDestinationTransformer(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new ActivitiMessagingDestinationTransformer(messagingProperties);
    }

    @Bean
    public ActivitiMessagingDestinationsBeanPostProcessor activitiMessagingDestinationsBeanPostProcessor(
        ActivitiMessagingDestinationTransformer destinationTransformer
    ) {
        return new ActivitiMessagingDestinationsBeanPostProcessor(destinationTransformer);
    }

    @Bean
    public InputConverterFunction toLowerCase() {
        return String::toLowerCase;
    }

    @Bean
    public InputConverterFunction escapeIllegalChars(@Lazy ActivitiCloudMessagingProperties messagingProperties) {
        return value ->
            value.replaceAll(
                messagingProperties.getDestinationIllegalCharsRegex(),
                messagingProperties.getDestinationIllegalCharsReplacement()
            );
    }
}
