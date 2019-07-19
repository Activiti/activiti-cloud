/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.tests.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.activiti.spring.ProcessDeployedEventProducer;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SwaggerITSupport {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ProcessDeployedEventProducer producer;

    private Gson formatter = new GsonBuilder().setPrettyPrinting().create();

    private JsonParser parser = new JsonParser();

    @Before
    public void setUp() {
        assertThat(producer).isNotNull();
    }

    /**
     * This is more than a simple test. It's actually generating the swagger.json definition of the service
     */
    @Test
    public void generateSwagger() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs").accept(MediaType.APPLICATION_JSON))
                .andDo((result) -> {
                    JsonElement je = parser.parse(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger.json"),
                                                formatter.toJson(je),
                                                StandardCharsets.UTF_8);
                    JsonNode jsonNodeTree = new ObjectMapper().readTree(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger.yaml"),
                                                new YAMLMapper().writeValueAsString(jsonNodeTree),
                                                StandardCharsets.UTF_8);
                });
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs?group=hal").accept(MediaType.APPLICATION_JSON))
                .andDo((result) -> {
                    JsonElement je = parser.parse(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger-hal.json"),
                                                formatter.toJson(je),
                                                StandardCharsets.UTF_8);
                    // TODO: 09/04/2019 the yaml generated out this json file will be produced once we make sure clients can be generated with swagger-hal.json
                });
    }

}
