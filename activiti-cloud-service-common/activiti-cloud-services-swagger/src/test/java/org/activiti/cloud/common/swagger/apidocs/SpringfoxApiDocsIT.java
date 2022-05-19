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
package org.activiti.cloud.common.swagger.apidocs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(BuildProperties.class)
@SpringBootTest(classes = {TestSwaggerSpringfoxConfig.class, BuildPropertiesConfig.class})
@TestPropertySource("classpath:application-springfox.properties")
public class SpringfoxApiDocsIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("classpath:org/activiti/cloud/common/swagger/apidocs/springfox-api-docs.json")
    private Resource springfoxApiDocsFile;

    @Test
    public void should_generateSpringfoxApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs?group=testing").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(new String(springfoxApiDocsFile.getInputStream().readAllBytes())))
            .andReturn();
    }

    @Test
    public void shouldNot_generateSpringdocApiDocs() throws Exception {
        mockMvc.perform(get("/springdoc/v3/api-docs").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();
    }
}
