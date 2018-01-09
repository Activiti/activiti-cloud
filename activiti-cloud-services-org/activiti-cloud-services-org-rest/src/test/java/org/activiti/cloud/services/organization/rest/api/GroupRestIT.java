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

package org.activiti.cloud.services.organization.rest.api;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.jpa.GroupRepository;
import org.activiti.cloud.services.organization.jpa.ProjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class GroupRestIT {

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void tearDown() {
        groupRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
    }

    @Test
    public void getGroups() throws Exception {

        mockMvc.perform(get("{version}/groups",
                            RepositoryRestConfig.API_VERSION))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$._embedded.groups",
                                    hasSize(0)));
    }

    @Test
    public void getSubgroups() throws Exception {
        //given
        final String parentGroupId = "parent_group_id";
        Group parentGroup = new Group(parentGroupId);
        final String subgroupAId = "subgroup_A_id";
        final String subgroupBId = "subgroup_B_id";
        parentGroup.setSubgroups(asList(
                new Group(subgroupAId),
                new Group(subgroupBId)
        ));

        final Group savedGroup = this.groupRepository.save(parentGroup);
        assertThat(savedGroup).isNotNull();
        //when
        mockMvc.perform(get("{version}/groups/{groupId}/subgroups",
                            RepositoryRestConfig.API_VERSION,
                            parentGroupId))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void createGroup() throws Exception {
        //given
        final String newGroupId = "new_group_id";
        final Group newGroup = new Group(newGroupId);
        final String newGroupName = "New Group";
        newGroup.setName(newGroupName);

        //when
        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(newGroup))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //then
        mockMvc.perform(get("{version}/groups/{groupId}",
                            RepositoryRestConfig.API_VERSION,
                            newGroupId))
                .andDo(print())
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is(newGroupName)));
    }

    @Test
    public void createGroupWithSubgroupsCallingRestApi() throws Exception {
        //given

        final String newParentGroupName = "New Parent Group";
        final String newParentGroupId = "new_parent_group_id";
        final Group newParentGroup = new Group(newParentGroupId,
                                               newParentGroupName);
        //create the parent group
        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(newParentGroup)).contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //create subgroup A
        final String newSubgroupAName = "New Subgroup A";
        final String newSubgroupAId = "new_subgroup_A_id";
        Group newSubgroupA = new Group(newSubgroupAId,
                                       newSubgroupAName);
        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(newSubgroupA))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //create subgroup B
        String newSubgroupBId = "new_subgroup_B_id";
        String newSubgroupBName = "New Subgroup B";
        Group newSubgroupB = new Group(newSubgroupBId,
                                       newSubgroupBName);
        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(newSubgroupB))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //when

        String uriList = "http://localhost" + RepositoryRestConfig.API_VERSION + "/groups/" + newSubgroupAId + "\n"
                + "http://localhost/" + RepositoryRestConfig.API_VERSION + "groups/" + newSubgroupBId;
        // add relation between parent group and subgroup
        mockMvc.perform(put("{version}/groups/{groupId}/subgroups",
                            RepositoryRestConfig.API_VERSION,
                            newParentGroupId)
                                .contentType("text/uri-list")
                                .content(uriList))
                .andDo(print())
                .andExpect(status().isNoContent());

        //then
        mockMvc.perform(get("{version}/groups/{groupId}/subgroups",
                            RepositoryRestConfig.API_VERSION,
                            newParentGroupId))
                .andDo(print())
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.groups",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.groups[0].name",
                                    is(newSubgroupAName)))
                .andExpect(jsonPath("$._embedded.groups[1].name",
                                    is(newSubgroupBName)));
    }

    @Test
    public void createGroupWithProjects() throws Exception {
        //given

        final String newGroupWithProjectsId = "new_group_with_projects_id";
        final String groupWithProjectsName = "Group with projects";
        Group groupWithProjects = new Group(newGroupWithProjectsId,
                                            groupWithProjectsName);

        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(groupWithProjects))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        // create project A
        final String projectAId = "project_A_id";
        final String projectAName = "Project A";
        Project projectA = new Project(projectAId,
                                       projectAName);
        mockMvc.perform(post("{version}/projects",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(projectA))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //when
        mockMvc.perform(put("{version}/groups/{groupId}/projects",
                            RepositoryRestConfig.API_VERSION,
                            newGroupWithProjectsId)
                                .contentType("text/uri-list")
                                .content("http://localhost/projects/" + projectAId))
                .andDo(print())
                .andExpect(status().isNoContent());
        //then
        mockMvc.perform(get("{version}/groups/{groupId}/projects",
                            RepositoryRestConfig.API_VERSION,
                            newGroupWithProjectsId))
                .andDo(print())
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(1)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is(projectAName)));
        //given

        // create project B
        final String projectBId = "project_B_id";
        final String projectBName = "Project B";
        Project projectB = new Project(projectBId,
                                       projectBName);
        mockMvc.perform(post("{version}/projects",
                             RepositoryRestConfig.API_VERSION)
                                .content(mapper.writeValueAsString(projectB))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isCreated());

        //when
        mockMvc.perform(patch("{version}/groups/{groupId}/projects",
                              RepositoryRestConfig.API_VERSION,
                              newGroupWithProjectsId)
                                .contentType("text/uri-list")
                                .content("http://localhost/projects/" + projectBId))
                .andDo(print())
                .andExpect(status().isNoContent());
        //then
        mockMvc.perform(get("{version}/groups/{groupId}/projects",
                            RepositoryRestConfig.API_VERSION,
                            newGroupWithProjectsId))
                .andDo(print())
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is(projectAName)))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                                    is(projectBName)));
    }

    @Test
    @Ignore
    public void createGroupWithSubgroupsDirectly() throws Exception {
        Group parentGroup = new Group("parent_group_id",
                                      "Parent Group");

        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(parentGroup)))
                .andDo(print())
                .andExpect(status().isCreated());

        Group subgroup = new Group("subgroup_A_id",
                                   "Subgroup A",
                                   parentGroup);

        mockMvc.perform(post("{version}/groups",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(subgroup)))
                .andDo(print())
                .andExpect(status().isCreated());

        final List<Group> all = groupRepository.findAll();
        assertThat(all.size()).isEqualTo(2);
    }
}
