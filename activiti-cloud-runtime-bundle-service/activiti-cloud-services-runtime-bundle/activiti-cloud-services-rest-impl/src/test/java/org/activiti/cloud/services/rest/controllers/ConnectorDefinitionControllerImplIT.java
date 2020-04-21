/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.rest.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.rest.assemblers.ConnectorDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.core.common.model.connector.ConnectorDefinition;
import org.activiti.core.common.spring.connector.autoconfigure.ConnectorAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.runtime.api.conf.ConnectorsAutoConfiguration;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(ConnectorDefinitionControllerImpl.class)
@Import({ConnectorsAutoConfiguration.class,
        ConnectorAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        CloudEventsAutoConfiguration.class,
        RuntimeBundleProperties.class,
        ProcessExtensionsAutoConfiguration.class})
public class ConnectorDefinitionControllerImplIT {

    private MockMvc mockMvc;

    @SpyBean
    private CollectionModelAssembler representationModelAssembler;

    @MockBean
    private ProcessExtensionService processExtensionService;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private RepositoryService repositoryService;

    @BeforeEach
    public void setup() {
        assertThat(processExtensionService).isNotNull();

        List<ConnectorDefinition> connectorDefinitions = new ArrayList<>();
        ConnectorDefinition connectorDefinition1 = new ConnectorDefinition();
        connectorDefinition1.setId("id1");
        ConnectorDefinition connectorDefinition2 = new ConnectorDefinition();
        connectorDefinition2.setId("id2");
        connectorDefinitions.add(connectorDefinition1);
        connectorDefinitions.add(connectorDefinition2);

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new ConnectorDefinitionControllerImpl(connectorDefinitions, new ConnectorDefinitionRepresentationModelAssembler(), representationModelAssembler))
                .setControllerAdvice(new RuntimeBundleExceptionHandler())
                .build();
    }

    @Test
    public void getAllConnectorDefinitions() throws Exception {

        this.mockMvc.perform(get("/v1/connector-definitions/").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content[0].links[0].rel", is("self")))
                .andExpect(jsonPath("content[0].id", is("id1")))
                .andExpect(jsonPath("content[1].id", is("id2")));
    }

    @Test
    public void getOneSpecificConnectorDefinition() throws Exception {

        this.mockMvc.perform(get("/v1/connector-definitions/id1").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("links[0].rel", is("self")))
                .andExpect(jsonPath("links[0].href", containsString("v1/connector-definitions/id1")))
                .andExpect(jsonPath("id", is("id1")));
    }

    @Test
    public void getConnectorDefinitionNotFound() throws Exception {

        this.mockMvc.perform(get("/v1/connector-definitions/idNotFound").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

}
