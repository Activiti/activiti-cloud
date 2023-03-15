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
package org.activiti.services.connectors.message;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.cloud.services.events.message.RuntimeBundleInfoMessageBuilderAppender;
import org.springframework.util.Assert;

public class IntegrationContextMessageBuilderFactory implements MessageBuilderChainFactory<IntegrationContext> {

    private final RuntimeBundleProperties properties;

    public IntegrationContextMessageBuilderFactory(RuntimeBundleProperties properties) {
        Assert.notNull(properties, "properties must not be null");

        this.properties = properties;
    }

    @Override
    public MessageBuilderAppenderChain create(IntegrationContext integrationContext) {
        Assert.notNull(integrationContext, "integrationContext must not be null");

        return new MessageBuilderAppenderChain()
            .routingKeyResolver(new IntegrationContextRoutingKeyResolver())
            .chain(new RuntimeBundleInfoMessageBuilderAppender(properties))
            .chain(new IntegrationContextMessageBuilderAppender(integrationContext));
    }
}
