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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationRequestBuilderTest {

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private FunctionBindingConfiguration.BindingResolver bindingResolver;

    @Mock
    private IntegrationContext integrationContext;

    @InjectMocks
    private IntegrationRequestBuilder builder;

    @Test
    void should_populateAllDestinations() {
        when(bindingResolver.getBindingDestination("integrationErrorsConsumer")).thenReturn("integrationError_my-rb");
        when(bindingResolver.getBindingDestination("integrationResultsConsumer")).thenReturn("integrationResult_my-rb");
        when(bindingResolver.getBindingDestination("connectorIncidentConsumer")).thenReturn("connectorIncident_my-rb");

        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getErrorDestination()).isEqualTo("integrationError_my-rb");
        assertThat(request.getResultDestination()).isEqualTo("integrationResult_my-rb");
        assertThat(request.getIncidentDestination()).isEqualTo("connectorIncident_my-rb");
    }

    @Test
    void should_appendRuntimeBundleInfo() {
        IntegrationRequestImpl request = builder.build(integrationContext);

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(request);
    }

    @Test
    void should_setIntegrationContext() {
        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getIntegrationContext()).isSameAs(integrationContext);
    }

    @Test
    void should_handleNullDestinations_whenBindingResolverReturnsNull() {
        when(bindingResolver.getBindingDestination("integrationErrorsConsumer")).thenReturn(null);
        when(bindingResolver.getBindingDestination("integrationResultsConsumer")).thenReturn(null);
        when(bindingResolver.getBindingDestination("connectorIncidentConsumer")).thenReturn(null);

        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getErrorDestination()).isNull();
        assertThat(request.getResultDestination()).isNull();
        assertThat(request.getIncidentDestination()).isNull();
    }
}
