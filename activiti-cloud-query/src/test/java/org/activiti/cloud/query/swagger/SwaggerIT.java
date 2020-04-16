/*
 * Copyright 2005-2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.activiti.cloud.query.swagger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
public class SwaggerIT {

    @Autowired
    private WebApplicationContext context;

    @Test
    public void defaultSpecificationFileShouldBeAlfrescoFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        both(notNullValue(String.class))
                                .and(containsString("ListResponseContentOfCloudProcessDefinition"))
                                .and(containsString("EntriesResponseContentOfCloudProcessDefinition"))
                                .and(containsString("EntryResponseContentOfCloudProcessDefinition"))
                                .and(not(containsString("PagedResources«")))
                                .and(not(containsString("PagedResources«")))
                                .and(not(containsString("Resources«Resource«")))
                                .and(not(containsString("Resource«")))
                                           ));
    }

}
