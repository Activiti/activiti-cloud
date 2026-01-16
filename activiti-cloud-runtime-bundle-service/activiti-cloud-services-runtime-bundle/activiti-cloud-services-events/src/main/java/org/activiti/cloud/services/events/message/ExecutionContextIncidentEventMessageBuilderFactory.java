/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.impl.context.ExecutionContext;
import org.springframework.util.Assert;

public class ExecutionContextIncidentEventMessageBuilderFactory
    implements MessageBuilderChainFactory<ExecutionContext> {

    private final RuntimeBundleProperties properties;

    public ExecutionContextIncidentEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        Assert.notNull(properties, "properties must not be null");

        this.properties = properties;
    }

    @Override
    public MessageBuilderAppenderChain create(ExecutionContext executionContext) {
        return new MessageBuilderAppenderChain()
            .routingKeyResolver(new AuditProducerIncidentsRoutingKeyResolver())
            .chain(new RuntimeBundleInfoMessageBuilderAppender(properties))
            .chain(new ExecutionContextMessageBuilderAppender(executionContext));
    }
}
