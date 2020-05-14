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

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

public class IntegrationResultChannelResolverImpl implements IntegrationResultChannelResolver {

    private final BinderAwareChannelResolver resolver;

    private final IntegrationResultDestinationBuilder integrationResultDestinationBuilder;

    public IntegrationResultChannelResolverImpl(BinderAwareChannelResolver resolver,
                                                IntegrationResultDestinationBuilder integrationResultDestinationBuilder) {
        this.resolver = resolver;
        this.integrationResultDestinationBuilder = integrationResultDestinationBuilder;
    }

    @Override
    public MessageChannel resolveDestination(IntegrationRequest event) {
        String destination = integrationResultDestinationBuilder.buildDestination(event);

        return resolver.resolveDestination(destination);
    }




}
