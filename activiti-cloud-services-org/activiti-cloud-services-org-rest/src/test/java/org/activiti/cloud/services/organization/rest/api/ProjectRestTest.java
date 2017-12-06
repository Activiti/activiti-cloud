package org.activiti.cloud.services.organization.rest.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.activiti.cloud.organization.core.model.Project;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProjectRestTest extends AbstractRestTest {

    @Test
    public void getProjects() throws Exception {
        mockMvc.perform(get("/projects/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)));
    }

    @Test
    public void createProject() throws Exception {

        mockMvc.perform(post("/projects").content(toJson(new Project("project_id"))))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }
}
