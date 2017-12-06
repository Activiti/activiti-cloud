package org.activiti.cloud.services.organization.rest.api;

import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Project;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GroupRestTest extends AbstractRestTest {

    @Test
    public void getGroups() throws Exception {
        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$._embedded.groups",
                                    hasSize(3)));
    }

    @Test
    public void getSubgroupsForGroup() throws Exception {
        mockMvc.perform(get("/groups/{groupId}/subgroups",
                            "parent_group_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.groups",
                                    hasSize(2)));
    }

    @Test
    public void getProjectsForGroup() throws Exception {
        mockMvc.perform(get("/groups/{groupId}/projects",
                            "parent_group_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)));
    }

    @Test
    @Ignore
    public void createProjectWithSubprojects() throws Exception {

        // create a group
        mockMvc.perform(post("/groups")
                                                            .content(toJson(new Group("group_with_project_id"))))
                .andDo(print())
                .andExpect(status().isCreated());

        // create a project
        mockMvc.perform(post("/projects").content(toJson(new Project("project_id"))))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/projects/project_id"))
                .andExpect(status().isOk())
                .andDo(print());

        // associate the project to group
        mockMvc.perform(put("/projects/project_id/group")
                                .contentType("text/uri-list")
                                .content("http://localhost/groups/group_with_project_id"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$._embedded.groups",
                                    hasSize(3)));


    }
}
