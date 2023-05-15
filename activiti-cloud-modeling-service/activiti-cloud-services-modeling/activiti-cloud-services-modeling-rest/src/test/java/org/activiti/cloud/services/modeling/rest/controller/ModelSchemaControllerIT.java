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
package org.activiti.cloud.services.modeling.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.activiti.cloud.services.modeling.service.SchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for models schema rest api
 */
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@WithMockModelerUser
@AutoConfigureMockMvc
public class ModelSchemaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaService schemaService;

    @Test
    public void should_return200andAConnectorJsonSchema_when_connectorSchemaIsRequested() throws Exception {
        mockMvc
            .perform(get("/v1/schemas/CONNECTOR"))
            .andExpect(status().isOk())
            .andExpect(content().json(schemaService.getJsonSchemaFromType("CONNECTOR").get().toString()));
    }

    @Test
    public void should_return200andAExtensionProcessJsonSchema_when_extensionProcessSchemaIsRequested()
        throws Exception {
        mockMvc
            .perform(get("/v1/schemas/PROCESS-EXTENSION"))
            .andExpect(status().isOk())
            .andExpect(content().json(schemaService.getJsonSchemaFromType("PROCESS-EXTENSION").get().toString()));
    }

    @Test
    public void should_return404_when_isRequestedAnonDefinedSchema() throws Exception {
        mockMvc.perform(get("/v1/schemas/test")).andExpect(status().isNotFound());
    }
}
