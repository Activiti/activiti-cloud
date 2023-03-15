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
package org.activiti.cloud.services.events.message;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.util.Assert;

public class CloudRuntimeEventMessageBuilderFactory implements MessageBuilderChainFactory<CloudRuntimeEvent<?, ?>> {

    private final RuntimeBundleProperties properties;

    public CloudRuntimeEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        Assert.notNull(properties, "properties must not be null");

        this.properties = properties;
    }

    @Override
    public MessageBuilderAppenderChain create(CloudRuntimeEvent<?, ?> event) {
        Assert.notNull(event, "event must not be null");

        return new MessageBuilderAppenderChain()
            .routingKeyResolver(new AuditProducerRoutingKeyResolver())
            .chain(new RuntimeBundleInfoMessageBuilderAppender(properties))
            .chain(new CloudRuntimeEventMessageBuilderAppender(event));
    }
}
