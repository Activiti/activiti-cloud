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
package org.activiti.services.connectors.channel;

import static org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER;
import static org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER;

import java.io.Serializable;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;

public class IntegrationRequestBuilder implements Serializable {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final FunctionBindingConfiguration.BindingResolver bindingResolver;

    public IntegrationRequestBuilder(
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        FunctionBindingConfiguration.BindingResolver bindingResolver
    ) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.bindingResolver = bindingResolver;
    }

    public IntegrationRequestImpl build(IntegrationContext integrationContext) {
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);

        integrationRequest.setErrorDestination(bindingResolver.getBindingDestination(INTEGRATION_ERRORS_CONSUMER));
        integrationRequest.setResultDestination(bindingResolver.getBindingDestination(INTEGRATION_RESULTS_CONSUMER));

        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationRequest);
        return integrationRequest;
    }
}
