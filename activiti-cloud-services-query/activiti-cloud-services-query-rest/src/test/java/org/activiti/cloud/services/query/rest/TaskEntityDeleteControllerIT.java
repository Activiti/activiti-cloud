package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.conf.QueryRestAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.resourcesResponseFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties="activiti.rest.enable-deletion=true")
@RunWith(SpringRunner.class)
@WebMvcTest(TaskDeleteController.class)
@Import({
        QueryRestAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.query.rest.assembler", "org.activiti.cloud.alfresco"})
public class TaskEntityDeleteControllerIT {

    private static final String TASK_ADMIN_ALFRESCO_IDENTIFIER = "task-admin-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @Before
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("admin");
        assertThat(entityFinder).isNotNull();
        assertThat(securityPoliciesManager).isNotNull();
        assertThat(processDefinitionRepository).isNotNull();
        assertThat(securityPoliciesProperties).isNotNull();
        assertThat(taskLookupRestrictionService).isNotNull();
    }

    @Test
    public void deleteTasksShouldReturnAllTasksAndDeleteThem() throws Exception{

        //given
        List<TaskEntity> taskEntities = Collections.singletonList(buildDefaultTask());
        given(taskRepository.findAll(ArgumentMatchers.<Predicate>any()))
                .willReturn(taskEntities);

        //when
        mockMvc.perform(delete("/admin/v1/tasks")
                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(TASK_ADMIN_ALFRESCO_IDENTIFIER + "/list",
                        resourcesResponseFields()
                ));

        verify(taskRepository).deleteAll(taskEntities);
    }

    private TaskEntity buildDefaultTask() {
        return new TaskEntity(UUID.randomUUID().toString(),
                "john",
                "Review",
                "Review the report",
                new Date(),
                new Date(),
                20,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "My app",
                "My app",
                "1",
                null,
                null,
                Task.TaskStatus.ASSIGNED,
                new Date(),
                new Date(),
                "peter",
                null,
                "aFormKey",
                10,
                "businessKey"
        );
    }
}
