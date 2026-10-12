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
package org.activiti.cloud.api.process.model.impl.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Date;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationRequestImplTest {

    @Mock
    private IntegrationContext integrationContext;

    @Test
    void should_always_haveAppVersionSet() {
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        assertThat(integrationRequest.getAppVersion()).isEqualTo("1");
    }

    @Test
    void should_setAndGetIncidentDestination() {
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setIncidentDestination("connectorIncident_myApp");
        assertThat(integrationRequest.getIncidentDestination()).isEqualTo("connectorIncident_myApp");
    }

    @Test
    void should_returnNullIncidentDestination_whenNotSet() {
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        assertThat(integrationRequest.getIncidentDestination()).isNull();
    }

    @Test
    void should_setAndGetRequestDate() {
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        Date requestDate = new Date();

        integrationRequest.setRequestDate(requestDate);

        assertThat(integrationRequest.getRequestDate()).isEqualTo(requestDate);
    }

    @Test
    void should_setAndGetTtlSeconds() {
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);

        integrationRequest.setTtlSeconds(10800);

        assertThat(integrationRequest.getTtlSeconds()).isEqualTo(10800);
    }

    @Test
    void should_returnNullRequestDateAndTtl_whenNotSet() {
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);

        assertThat(integrationRequest.getRequestDate()).isNull();
        assertThat(integrationRequest.getTtlSeconds()).isNull();
    }

    @Test
    void copyWithoutContext_should_copyAllRoutingMetadata() {
        // given
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl source = new IntegrationRequestImpl(integrationContext);
        source.setAppName("myApp");
        source.setServiceName("myService");
        source.setServiceFullName("myServiceFullName");
        source.setServiceType("myServiceType");
        source.setServiceVersion("1.0");
        source.setResultDestination("resultDest");
        source.setErrorDestination("errorDest");
        source.setIncidentDestination("incidentDest");
        Date requestDate = new Date();
        source.setRequestDate(requestDate);
        source.setTtlSeconds(30);

        // when
        IntegrationRequestImpl copy = IntegrationRequestImpl.copyWithoutContext(source);

        // then
        assertThat(copy.getAppName()).isEqualTo("myApp");
        assertThat(copy.getAppVersion()).isEqualTo("1");
        assertThat(copy.getServiceName()).isEqualTo("myService");
        assertThat(copy.getServiceFullName()).isEqualTo("myServiceFullName");
        assertThat(copy.getServiceType()).isEqualTo("myServiceType");
        assertThat(copy.getServiceVersion()).isEqualTo("1.0");
        assertThat(copy.getResultDestination()).isEqualTo("resultDest");
        assertThat(copy.getErrorDestination()).isEqualTo("errorDest");
        assertThat(copy.getIncidentDestination()).isEqualTo("incidentDest");
        assertThat(copy.getRequestDate()).isEqualTo(requestDate);
        assertThat(copy.getTtlSeconds()).isEqualTo(30);
    }

    @Test
    void copyWithoutContext_should_notCopyIntegrationContext() {
        // given
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl source = new IntegrationRequestImpl(integrationContext);
        assertThat(source.getIntegrationContext()).isNotNull();

        // when
        IntegrationRequestImpl copy = IntegrationRequestImpl.copyWithoutContext(source);

        // then
        assertThat(copy.getIntegrationContext()).isNull();
    }

    @Test
    void copyWithoutContext_should_handleNullFields() {
        // given
        IntegrationRequestImpl source = new IntegrationRequestImpl();

        // when
        IntegrationRequestImpl copy = IntegrationRequestImpl.copyWithoutContext(source);

        // then
        assertThat(copy.getAppName()).isNull();
        assertThat(copy.getServiceFullName()).isNull();
        assertThat(copy.getResultDestination()).isNull();
        assertThat(copy.getErrorDestination()).isNull();
        assertThat(copy.getIncidentDestination()).isNull();
        assertThat(copy.getRequestDate()).isNull();
        assertThat(copy.getTtlSeconds()).isNull();
        assertThat(copy.getIntegrationContext()).isNull();
    }
}
