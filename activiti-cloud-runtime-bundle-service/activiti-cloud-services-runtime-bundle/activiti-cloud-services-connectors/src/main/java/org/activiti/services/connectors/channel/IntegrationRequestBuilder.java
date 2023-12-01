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

package org.activiti.services.connectors.channel;

import java.io.Serializable;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.springframework.cloud.stream.config.BindingServiceProperties;

public class IntegrationRequestBuilder implements Serializable {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final BindingServiceProperties bindingServiceProperties;

    public IntegrationRequestBuilder(
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        BindingServiceProperties bindingServiceProperties
    ) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.bindingServiceProperties = bindingServiceProperties;
    }

    public IntegrationRequestImpl build(IntegrationContext integrationContext) {
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);

        String resultDestination = bindingServiceProperties.getBindingDestination(
            ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER
        );
        String errorDestination = bindingServiceProperties.getBindingDestination(
            ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER
        );

        integrationRequest.setErrorDestination(errorDestination);
        integrationRequest.setResultDestination(resultDestination);

        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationRequest);
        return integrationRequest;
    }
}
