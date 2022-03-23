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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@ContextConfiguration(initializers = {KeycloakContainerApplicationInitializer.class})
@WithMockModelerUser
class IdentityManagementControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private IdentityManagementController identityManagementController;

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnUsers_when_searchByUsername() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=hr"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(2)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("hradmin")))
            .andExpect(jsonPath("$.list.entries[1].entry.username", is("hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByEmail() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=hr@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByLastName() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=snow"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("johnsnow")));
    }

    @Test
    public void should_returnUsers_when_searchByFirstName() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=john"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("johnsnow")));
    }

    @Test
    public void should_returnOnlyUsers_when_searchByUsernameAndRoleUser() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=johnsnow&role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("johnsnow")));
    }

    @Test
    public void should_returnOnlyAdmins_when_searchByUsernameAndRoleAdmin() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?search=hr&role=ACTIVITI_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("hradmin")));
    }

    @Test
    public void should_returnOnlyAdmins_when_searchByRoleAdmin() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?role=ACTIVITI_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(4)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("admin")))
            .andExpect(jsonPath("$.list.entries[1].entry.username", is("hradmin")))
            .andExpect(jsonPath("$.list.entries[2].entry.username", is("testactivitiadmin")))
            .andExpect(jsonPath("$.list.entries[3].entry.username", is("testadmin")));
    }

    @Test
    public void shouldReturnMultiplePages_when_searchUsersUsingPagination() throws Exception {
        mockMvc
            .perform(get("/v1/identity/users?skipCount=0&maxItems=8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(8)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("admin")))
            .andExpect(jsonPath("$.list.entries[1].entry.username", is("hradmin")))
            .andExpect(jsonPath("$.list.entries[2].entry.username", is("hruser")))
            .andExpect(jsonPath("$.list.entries[3].entry.username", is("johnsnow")))
            .andExpect(jsonPath("$.list.entries[4].entry.username", is("modeler")))
            .andExpect(jsonPath("$.list.entries[5].entry.username", is("testactivitiadmin")))
            .andExpect(jsonPath("$.list.entries[6].entry.username", is("testadmin")))
            .andExpect(jsonPath("$.list.entries[7].entry.username", is("testdevops")));

        mockMvc
            .perform(get("/v1/identity/users?skipCount=8&maxItems=8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.username", is("testuser")));

        mockMvc
            .perform(get("/v1/identity/users?skipCount=16&maxItems=8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(0)));
    }

    @Test
    public void should_returnGroups_when_searchByName() throws Exception {
        mockMvc
            .perform(get("/v1/identity/groups?search=group"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(2)))
            .andExpect(jsonPath("$.list.entries[0].entry.name", is("salesgroup")))
            .andExpect(jsonPath("$.list.entries[1].entry.name", is("testgroup")));
    }

    @Test
    public void should_returnGroups_when_searchByNameAndRole() throws Exception {
        mockMvc
            .perform(get("/v1/identity/groups?search=group&role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.name", is("salesgroup")));
    }

    @Test
    public void should_returnGroups_when_searchByRole() throws Exception {
        mockMvc
            .perform(get("/v1/identity/groups?role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.name", is("salesgroup")));
    }

    @Test
    public void shouldReturnMultiplePages_when_searchGroupsUsingPagination() throws Exception {
        mockMvc
            .perform(get("/v1/identity/groups?skipCount=0&maxItems=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(2)))
            .andExpect(jsonPath("$.list.entries[0].entry.name", is("hr")))
            .andExpect(jsonPath("$.list.entries[1].entry.name", is("salesgroup")));

        mockMvc
            .perform(get("/v1/identity/groups?skipCount=2&maxItems=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(1)))
            .andExpect(jsonPath("$.list.entries[0].entry.name", is("testgroup")));

        mockMvc
            .perform(get("/v1/identity/groups?skipCount=4&maxItems=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries", hasSize(0)));
    }

}
