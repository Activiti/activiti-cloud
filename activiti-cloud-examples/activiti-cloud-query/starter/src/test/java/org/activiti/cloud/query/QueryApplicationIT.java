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
package org.activiti.cloud.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = { QueryApplication.class })
@WebAppConfiguration
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
public class QueryApplicationIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void contextLoads() throws Exception {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    public void defaultSpecificationFileShouldBeAlfrescoFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs/Query").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        both(notNullValue(String.class))
                            .and(containsString("ListResponseContentCloudProcessDefinition"))
                            .and(containsString("EntriesResponseContentCloudProcessDefinition"))
                            .and(containsString("EntryResponseContentCloudProcessDefinition"))
                            .and(not(containsString("PagedModel")))
                            .and(not(containsString("ResourcesOfResource")))
                            .and(not(containsString("Resource")))
                    )
            );
    }
}
