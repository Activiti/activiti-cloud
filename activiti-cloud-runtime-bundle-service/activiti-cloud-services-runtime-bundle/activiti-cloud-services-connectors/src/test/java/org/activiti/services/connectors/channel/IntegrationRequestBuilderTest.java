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
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.services.connectors.recovery.OrphanedIntegrationRecoveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@ExtendWith(MockitoExtension.class)
class IntegrationRequestBuilderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-23T10:00:00.000Z");

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private FunctionBindingConfiguration.BindingResolver bindingResolver;

    @Mock
    private IntegrationContext integrationContext;

    private OrphanedIntegrationRecoveryProperties orphanedIntegrationRecoveryProperties;

    private IntegrationRequestBuilder builder;

    @BeforeEach
    void setUp() {
        orphanedIntegrationRecoveryProperties = new OrphanedIntegrationRecoveryProperties();
        builder = new IntegrationRequestBuilder(
            runtimeBundleInfoAppender,
            bindingResolver,
            orphanedIntegrationRecoveryProperties,
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        );
    }

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

    @Test
    void should_setRequestDateFromClock() {
        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getRequestDate()).isEqualTo(Date.from(FIXED_INSTANT));
    }

    @Test
    void should_setTtlSecondsFromConfiguredThreshold() {
        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getTtlSeconds()).isEqualTo(orphanedIntegrationRecoveryProperties.getThresholdSeconds());
    }

    @Test
    void should_setTtlSeconds_when_thresholdPropertyIsChanged() {
        orphanedIntegrationRecoveryProperties.setThresholdSeconds(42);

        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getTtlSeconds()).isEqualTo(42);
    }

    @Test
    void should_leaveTtlSecondsNull_when_recoveryCronIsDisabled() {
        orphanedIntegrationRecoveryProperties.setCron(ScheduledTaskRegistrar.CRON_DISABLED);

        IntegrationRequestImpl request = builder.build(integrationContext);

        assertThat(request.getTtlSeconds()).isNull();
    }

    @Test
    void should_useDefaultPropertiesAndSystemClock_when_usingTwoArgConstructor() {
        IntegrationRequestBuilder twoArgBuilder = new IntegrationRequestBuilder(
            runtimeBundleInfoAppender,
            bindingResolver
        );

        IntegrationRequestImpl request = twoArgBuilder.build(integrationContext);

        assertThat(request.getRequestDate().toInstant()).isCloseTo(Instant.now(), within(Duration.ofMinutes(1)));
        assertThat(request.getTtlSeconds()).isEqualTo(
            new OrphanedIntegrationRecoveryProperties().getThresholdSeconds()
        );
    }
}
