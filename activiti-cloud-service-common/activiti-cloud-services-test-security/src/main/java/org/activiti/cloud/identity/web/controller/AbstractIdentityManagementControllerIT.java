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
package org.activiti.cloud.identity.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.util.Set;
import org.activiti.cloud.services.common.security.test.support.WithActivitiMockUser;
import org.activiti.cloud.services.common.security.test.support.WithActivitiMockUser.ResourceRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

public abstract class AbstractIdentityManagementControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @WithActivitiMockUser(roles = { "role1" })
    public void should_notReturnApplicationAccessRoles_when_userHasNotResourceRoles() throws Exception {
        this.mockMvc.perform(get("/v1/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationAccess", hasSize(0)));
    }

    @Test
    @WithActivitiMockUser(resourcesRoles = { @ResourceRoles(resource = "app1", roles = { "role1" }) })
    public void should_notReturnGlobalAccessRoles_when_userHasNotRealmRoles() throws Exception {
        this.mockMvc.perform(get("/v1/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.globalAccess").exists())
            .andExpect(jsonPath("$.globalAccess.roles", hasSize(0)));
    }

    @Test
    public void should_returnGroups_when_searchByName() throws Exception {
        this.mockMvc.perform(get("/v1/groups?search=group"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.name)].name", containsInAnyOrder("testgroup", "salesgroup")));
    }

    @Test
    public void should_returnUsers_when_searchByUsername() throws Exception {
        this.mockMvc.perform(get("/v1/users?search=hr"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.username)].username", containsInAnyOrder("hradmin", "hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByGroup() throws Exception {
        this.mockMvc.perform(get("/v1/users?group=hr"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[?(@.username)].username", containsInAnyOrder("hradmin", "hruser", "johnsnow")));
    }

    @Test
    public void should_returnUsers_when_searchByEmail() throws Exception {
        this.mockMvc.perform(get("/v1/users?search=hr@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByLastName() throws Exception {
        this.mockMvc.perform(get("/v1/users?search=snow"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("johnsnow")));
    }

    @Test
    public void should_returnUsers_when_searchByFirstName() throws Exception {
        this.mockMvc.perform(get("/v1/users?search=john"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("johnsnow")));
    }

    @Test
    public void should_returnOnlyUsers_when_searchByUsernameAndRoleUser() throws Exception {
        mockMvc
            .perform(get("/v1/users?search=johnsnow&role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("johnsnow")));
    }

    @Test
    public void should_returnUsers_when_searchByApplication() throws Exception {
        mockMvc
            .perform(get("/v1/users?application=activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].username", is("hruser")))
            .andExpect(jsonPath("$[1].username", is("testactivitiadmin")))
            .andExpect(jsonPath("$[2].username", is("testmanager")))
            .andExpect(jsonPath("$[3].username", is("testuser")));
    }

    @Test
    public void should_notReturnUsers_when_searchByInvalidApplication() throws Exception {
        mockMvc
            .perform(get("/v1/users?application=activitis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void should_returnUsers_when_searchByUsernameAndApplication() throws Exception {
        mockMvc
            .perform(get("/v1/users?search=hruser&application=activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByGroupAndApplication() throws Exception {
        mockMvc
            .perform(get("/v1/users?group=hr&application=activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("hruser")));
    }

    @Test
    public void should_returnUsers_when_searchByRoleAndApplication() throws Exception {
        mockMvc
            .perform(get("/v1/users?role=ACTIVITI_ADMIN&application=activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("testactivitiadmin")));
    }

    @Test
    public void should_returnOnlyAdmins_when_searchByUsernameAndRoleAdmin() throws Exception {
        mockMvc
            .perform(get("/v1/users?search=hr&role=ACTIVITI_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].username", is("hradmin")));
    }

    @Test
    public void should_returnOnlyAdmins_when_searchByRoleAdmin() throws Exception {
        mockMvc
            .perform(get("/v1/users?role=ACTIVITI_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].username", is("admin")))
            .andExpect(jsonPath("$[1].username", is("hradmin")))
            .andExpect(jsonPath("$[2].username", is("testactivitiadmin")))
            .andExpect(jsonPath("$[3].username", is("testadmin")));
    }

    @Test
    public void should_returnGroups_when_searchByNameAndRole() throws Exception {
        mockMvc
            .perform(get("/v1/groups?search=group&role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("salesgroup")));
    }

    @Test
    public void should_returnGroups_when_searchByRole() throws Exception {
        mockMvc
            .perform(get("/v1/groups?role=ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("hr")))
            .andExpect(jsonPath("$[1].name", is("salesgroup")));
    }

    @Test
    public void should_returnGroups_when_searchByApplication() throws Exception {
        mockMvc
            .perform(get("/v1/groups?application=activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("salesgroup")));
    }

    @Test
    public void should_notReturnGroups_when_searchByInvalidApplication() throws Exception {
        mockMvc
            .perform(get("/v1/groups?application=fake"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithActivitiMockUser(
        roles = { "role1" },
        resourcesRoles = {
            @ResourceRoles(resource = "app1", roles = { "role1", "role2" }),
            @ResourceRoles(resource = "app2", roles = "role1"),
        }
    )
    public void should_returnAccessRoles() throws Exception {
        mockMvc
            .perform(get("/v1/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.globalAccess").exists())
            .andExpect(jsonPath("$.globalAccess.roles", hasSize(1)))
            .andExpect(jsonPath("$.globalAccess.roles[0]", is("role1")))
            .andExpect(jsonPath("$.applicationAccess", hasSize(2)))
            .andExpect(jsonPath("$.applicationAccess[0].name", is("app2")))
            .andExpect(jsonPath("$.applicationAccess[0].roles", contains("role1")))
            .andExpect(jsonPath("$.applicationAccess[1].name", is("app1")))
            .andExpect(jsonPath("$.applicationAccess[1].roles", contains("role1", "role2")));
    }

    @Test
    public void should_returnStatusOk_when_addingAppPermissions() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "test-client")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"ACTIVITI_ADMIN\",\n" +
                        "    \"users\":[\"hradmin\"],\n" +
                        "    \"groups\":[]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isOk());
    }

    @Test
    public void should_returnNotFound_when_addingAppPermissionsToInvalidApplication() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "fakeApp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"ACTIVITI_ADMIN\",\n" +
                        "    \"users\":[\"hradmin\"],\n" +
                        "    \"groups\":[]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Invalid Security data: application {fakeApp} is invalid or doesn't exist"));
    }

    @Test
    public void should_returnBadRequest_when_addingAppPermissionsWithInvalidRole() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "test-client")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"fakeRole\",\n" +
                        "    \"users\":[\"testUser\"],\n" +
                        "    \"groups\":[]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Invalid Security data: role {fakeRole} is invalid or doesn't exist"));
    }

    @Test
    public void should_returnBadRequest_when_addingAppPermissionsWithInvalidUserRole() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "test-client")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"ACTIVITI_ADMIN\",\n" +
                        "    \"users\":[\"hruser\"],\n" +
                        "    \"groups\":[]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                status().reason("Invalid Security data: role {ACTIVITI_ADMIN} can't be assigned to user {hruser}")
            );
    }

    @Test
    public void should_returnBadRequest_when_addingAppPermissionsWithInvalidGroup() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "test-client")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"ACTIVITI_ADMIN\",\n" +
                        "    \"users\":[\"testadmin\"],\n" +
                        "    \"groups\":[\"fakeGroup\"]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Invalid Security data: group {fakeGroup} is invalid or doesn't exist"));
    }

    @Test
    public void should_returnBadRequest_when_addingAppPermissionsWithInvalidGroupRole() throws Exception {
        mockMvc
            .perform(
                post("/v1/permissions/{application}", "test-client")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "[\n" +
                        "    {\"role\":\"ACTIVITI_ADMIN\",\n" +
                        "    \"users\":[\"testadmin\"],\n" +
                        "    \"groups\":[\"hr\"]}\n" +
                        "  ]"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Invalid Security data: role {ACTIVITI_ADMIN} can't be assigned to group {hr}"));
    }

    @Test
    public void should_returnApplicationPermissions_when_filteringByRole() throws Exception {
        this.mockMvc.perform(get("/v1/permissions/{application}?role={role}", "activiti", "ACTIVITI_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].role", is("ACTIVITI_USER")))
            .andExpect(jsonPath("$[0].users[?(@.username)].username", containsInAnyOrder("hruser", "testuser")))
            .andExpect(jsonPath("$[0].groups[?(@.name)].name", containsInAnyOrder("salesgroup")));
    }

    @Test
    public void should_returnApplicationPermissions() throws Exception {
        this.mockMvc.perform(get("/v1/permissions/{application}", "activiti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(6)))
            .andExpect(
                jsonPath(
                    "$[?(@.role)].role",
                    containsInAnyOrder(
                        "ACTIVITI_USER",
                        "ACTIVITI_ADMIN",
                        "APPLICATION_MANAGER",
                        "uma_authorization",
                        "offline_access",
                        "DYNAMIC_ROLE"
                    )
                )
            );
    }

    @Test
    public void should_notReturnApplicationPermissions_when_roleIsInvalid() throws Exception {
        this.mockMvc.perform(get("/v1/permissions/{application}?role={role}", "activiti", "role"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void should_returnGroups_when_searchByNameWithCache() throws Exception {
        mockMvc.perform(get("/v1/groups?search=search")).andExpect(status().isOk());

        mockMvc.perform(get("/v1/groups?search=search&role=role")).andExpect(status().isOk());

        mockMvc.perform(get("/v1/groups?role=role")).andExpect(status().isOk());

        Cache cache = cacheManager.getCache("groupSearch");
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", null, null))).isNotNull();
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", Set.of("role"), null))).isNotNull();
        assertThat(cache.get(SimpleKeyGenerator.generateKey(null, Set.of("role"), null))).isNotNull();
    }

    @Test
    public void should_returnUsers_when_searchByLastNameCache() throws Exception {
        mockMvc.perform(get("/v1/users?search=search")).andExpect(status().isOk());

        mockMvc.perform(get("/v1/users?search=search&group=group")).andExpect(status().isOk());

        mockMvc.perform(get("/v1/users?search=search&role=role")).andExpect(status().isOk());

        mockMvc.perform(get("/v1/users?search=search&role=role&group=group")).andExpect(status().isOk());

        Cache cache = cacheManager.getCache("userSearch");
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", null, null, null))).isNotNull();
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", Set.of("role"), null, null))).isNotNull();
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", Set.of("role"), Set.of("group"), null)))
            .isNotNull();
        assertThat(cache.get(SimpleKeyGenerator.generateKey("search", null, Set.of("group"), null))).isNotNull();
    }
}
