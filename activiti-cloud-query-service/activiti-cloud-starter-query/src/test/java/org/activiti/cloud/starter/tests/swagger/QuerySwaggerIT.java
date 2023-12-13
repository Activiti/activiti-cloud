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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QuerySwaggerIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("classpath:swagger-expected.json")
    private Resource swaggerExpectedResource;

    @Test
    public void should_swaggerDefinitionHavePathsAndDefinitionsAndInfo() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/v3/api-docs/Query").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.servers").isNotEmpty())
            .andExpect(jsonPath("$.servers[0].url").value(equalTo("/")))
            .andExpect(jsonPath("$.paths").isNotEmpty())
            .andExpect(jsonPath("$.components.schemas").isNotEmpty())
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("ListResponseContent"))))
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("EntriesResponseContent"))))
            .andExpect(jsonPath("$.components.schemas").value(hasKey(startsWith("EntryResponseContent"))))
            .andExpect(jsonPath("$.info.title").value("OpenAPI definition"))
            .andExpect(
                content()
                    .string(
                        both(notNullValue(String.class))
                            .and(not(containsString("ListResponseContent«")))
                            .and(not(containsString("EntriesResponseContent«")))
                            .and(not(containsString("EntryResponseContent«")))
                            .and(not(containsString("PagedResources«")))
                            .and(not(containsString("PagedResources«")))
                            .and(not(containsString("Resources«Resource«")))
                            .and(not(containsString("Resource«")))
                    )
            )
            .andExpect(jsonPath("$.paths[*].[*].summary").value(not(hasItem(matchesRegex("\\w*(_[0-9])+$")))))
            .andExpect(jsonPath("$.paths[*].[*].operationId").value(not(hasItem(matchesRegex("\\w*(_[0-9])+$")))))
            .andReturn();

        String generatedAPIDoc = result.getResponse().getContentAsString();
        assertThatJson(generatedAPIDoc)
            .inPath("$.paths./v1/tasks.get.parameters[*].['name', 'required']")
            .isArray()
            .contains("{name: \"variables.name\", required: false}", "{name: \"variables.value\", required: false}");

        assertThatJson(generatedAPIDoc).isEqualTo(swaggerExpectedResource.getContentAsString(Charset.defaultCharset()));
    }
}
