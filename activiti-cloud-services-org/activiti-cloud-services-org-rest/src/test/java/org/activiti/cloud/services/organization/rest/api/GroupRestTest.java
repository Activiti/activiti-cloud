package org.activiti.cloud.services.organization.rest.api;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.jpa.GroupRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class GroupRestTest {

    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        List<Group> subgroups = new ArrayList<>();
        List<Project> projects = new ArrayList<>();
        List<Model> models = new ArrayList<>();

        Group group = new Group("group_id");
        Group demo = new Group("demo_id");

        Project projectA = new Project("projectA_id");
        Project projectB = new Project("projectB_id");

        Model processModel = new Model("processModel_id");
        models.add(processModel);
        projectA.setModels(models);

        projects.add(projectA);
        projects.add(projectB);
        demo.setProjects(projects);

        subgroups.add(demo);
        subgroups.add(new Group("anotherGroup_id"));
        group.setSubgroups(subgroups);

        final Group savedGroup = this.groupRepository.save(group);
        assertThat(savedGroup).isNotNull();
    }

    @Test
    public void testGroups() throws Exception {
        mockMvc.perform(get("/groups"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testSubgroups() throws Exception {
        mockMvc.perform(get("/groups/group_id/subgroups"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
