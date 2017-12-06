package org.activiti.cloud.services.organization.rest.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.jpa.GroupRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public abstract class AbstractRestTest {

    public static final String GROUP_ID = "group_id";
    public static final String SUBGROUP_1_ID = "demo_group_id";
    public static final String SUBGROUP_2_ID = "anotherGroup_id";
    public static final String PROJECT_A_ID = "projectA_id";
    public static final String PROJECT_B_ID = "projectB_id";
    public static final String PROCESS_MODEL_ID = "processModel_id";
    public static final String PROCESS_MODEL_REF_ID = "processModel_refId";

    MockMvc mockMvc;
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper mapper;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                      this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        this.mapper = new ObjectMapper();

        List<Group> subgroups = new ArrayList<>();
        List<Project> projects = new ArrayList<>();
        List<Model> models = new ArrayList<>();

        Group group = new Group(GROUP_ID);
        Group demo = new Group(SUBGROUP_1_ID);

        Project projectA = new Project(PROJECT_A_ID);
        Project projectB = new Project(PROJECT_B_ID);

        Model processModel = new Model(PROCESS_MODEL_ID,
                                       Model.ModelType.PROCESS_MODEL,
                                       PROCESS_MODEL_REF_ID);
        models.add(processModel);
        projectA.setModels(models);

        projects.add(projectA);
        projects.add(projectB);

        subgroups.add(demo);
        subgroups.add(new Group(SUBGROUP_2_ID));
        group.setSubgroups(subgroups);
        group.setProjects(projects);

        final Group savedGroup = this.groupRepository.save(group);
        assertThat(savedGroup).isNotNull();
    }

    @After
    public void tearDown() {
        this.groupRepository.deleteAllInBatch();
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    protected String toJson(Object o) throws JsonProcessingException {
        return mapper.writeValueAsString(o);
    }
}
