package org.activiti.cloud.services.query.rest;


import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.resourcesResponseFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
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

@TestPropertySource(properties="activiti.rest.enable-deletion=true")
@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceDeleteController.class)
@Import({
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessInstanceEntityDeleteControllerIT {

    private static final String PROCESS_INSTANCE_ALFRESCO_IDENTIFIER = "process-instance-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessInstanceRepository processInstanceRepository;

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
    public void deleteProcessInstancesShouldReturnAllProcessInstancesAndDeleteThem() throws Exception{

        //given
        List<ProcessInstanceEntity> processInstanceEntities = Collections.singletonList(buildDefaultProcessInstance());
        given(processInstanceRepository.findAll(ArgumentMatchers.<Predicate>any()))
                .willReturn(processInstanceEntities);

        //when
        mockMvc.perform(delete("/admin/v1/process-instances")
                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_INSTANCE_ALFRESCO_IDENTIFIER + "/list",
                        resourcesResponseFields()

                ));

        verify(processInstanceRepository).deleteAll(processInstanceEntities);

    }

    private ProcessInstanceEntity buildDefaultProcessInstance() {
        return new ProcessInstanceEntity("My-app", "My-app", "1", null, null,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                ProcessInstance.ProcessInstanceStatus.RUNNING,
                new Date());
    }
}
