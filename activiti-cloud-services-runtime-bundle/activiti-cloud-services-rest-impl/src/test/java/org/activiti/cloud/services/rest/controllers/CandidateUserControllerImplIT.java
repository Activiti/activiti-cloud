package org.activiti.cloud.services.rest.controllers;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.resourcesResponseFields;
import static org.activiti.alfresco.rest.docs.HALDocumentation.unpagedCandidateUsers;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = CandidateUserControllerImpl.class, secure = true)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
public class CandidateUserControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "candidate-user";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "candidate-user-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private TaskRuntime taskRuntime;

    @SpyBean
    private SpringPageConverter springPageConverter;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        assertThat(springPageConverter).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getUserCandidatesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {

        List<String> stringList = Arrays.asList("hruser",
                                                "testuser");
        when(taskRuntime.userCandidates("1")).thenReturn(stringList);

        MvcResult result = this.mockMvc.perform(get("/v1/tasks/{taskId}/candidate-users",
                                                    1).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                resourcesResponseFields()))
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("list.entries[0].entry.user")
                .isEqualTo("hruser");
        assertThatJson(result.getResponse().getContentAsString())
                .node("list.entries[1].entry.user")
                .isEqualTo("testuser");
    }

    @Test
    public void getUserCandidatesShouldHaveProperHALFormat() throws Exception {

        List<String> stringList = Arrays.asList("hruser",
                                                "testuser");
        when(taskRuntime.userCandidates("1")).thenReturn(stringList);

        MvcResult result = this.mockMvc.perform(get("/v1/tasks/{taskId}/candidate-users",
                                                    1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                unpagedCandidateUsers()))
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("_embedded.candidateUsers[0].user")
                .isEqualTo("hruser");
        assertThatJson(result.getResponse().getContentAsString())
                .node("_embedded.candidateUsers[1].user")
                .isEqualTo("testuser");
    }

}
