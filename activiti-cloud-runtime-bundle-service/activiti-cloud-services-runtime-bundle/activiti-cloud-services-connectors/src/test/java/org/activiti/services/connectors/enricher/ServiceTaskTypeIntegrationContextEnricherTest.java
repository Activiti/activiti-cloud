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
package org.activiti.services.connectors.enricher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceTaskTypeIntegrationContextEnricherTest {

    private static final String PROCESS_DEFINITION_ID = "processDefinitionId";
    private static final String CLIENT_ID = "clientId";

    @Mock
    private ProcessExtensionService processExtensionService;

    private ServiceTaskTypeIntegrationContextEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new ServiceTaskTypeIntegrationContextEnricher(processExtensionService);
    }

    @Test
    void should_setServiceTaskType_when_definedInExtensions() {
        Extension extension = new Extension();
        extension.setServiceTaskTypes(Map.of(CLIENT_ID, "Rest Connector: GET"));
        given(processExtensionService.getExtensionsForId(PROCESS_DEFINITION_ID)).willReturn(extension);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);
        integrationContext.setClientId(CLIENT_ID);

        enricher.enrich(integrationContext);

        assertThat(integrationContext.getServiceTaskType()).isEqualTo("Rest Connector: GET");
    }

    @Test
    void should_notSetServiceTaskType_when_extensionsAreNull() {
        given(processExtensionService.getExtensionsForId(PROCESS_DEFINITION_ID)).willReturn(null);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);
        integrationContext.setClientId(CLIENT_ID);

        enricher.enrich(integrationContext);

        assertThat(integrationContext.getServiceTaskType()).isNull();
    }

    @Test
    void should_notSetServiceTaskType_when_typeNotDefinedForElement() {
        given(processExtensionService.getExtensionsForId(PROCESS_DEFINITION_ID)).willReturn(new Extension());

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);
        integrationContext.setClientId(CLIENT_ID);

        enricher.enrich(integrationContext);

        assertThat(integrationContext.getServiceTaskType()).isNull();
    }

    @Test
    void should_doNothing_when_integrationContextIsNotMutable() {
        IntegrationContext integrationContext = mock(IntegrationContext.class);

        enricher.enrich(integrationContext);

        verifyNoInteractions(processExtensionService);
    }
}
