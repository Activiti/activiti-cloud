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
package org.activiti.cloud.starter.tests.swagger;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QuerySwaggerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_swaggerDefinitionHavePathsAndDefinitionsAndInfo() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/v3/api-docs?group=Query").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.servers").isNotEmpty())
            .andExpect(jsonPath("$.servers[0].url").value(equalTo("/")))
            .andExpect(jsonPath("$.paths").isNotEmpty())
            .andExpect(jsonPath("$.components.schemas").isNotEmpty())
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("ListResponseContentOf"))))
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("EntriesResponseContentOf"))))
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("EntryResponseContentOf"))))
            .andExpect(jsonPath("$.info.title").value("Query Service ReST API"))
            .andExpect(content().string(
                        both(notNullValue(String.class))
                                .and(not(containsString("ListResponseContent«")))
                                .and(not(containsString("EntriesResponseContent«")))
                                .and(not(containsString("EntryResponseContent«")))
                                .and(not(containsString("PagedResources«")))
                                .and(not(containsString("PagedResources«")))
                                .and(not(containsString("Resources«Resource«")))
                                .and(not(containsString("Resource«")))
            ))
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
            .inPath("$.paths./v1/tasks.get.parameters[*].['name', 'required']")
            .isArray()
            .contains(
                "{name: \"variables.name\", required: false}",
                "{name: \"variables.value\", required: false}",
                "{name: \"variables.type\", required: false}");
    }

}
