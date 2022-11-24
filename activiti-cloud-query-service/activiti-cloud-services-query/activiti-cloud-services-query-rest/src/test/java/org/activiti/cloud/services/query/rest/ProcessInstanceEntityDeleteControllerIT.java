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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManagerFactory;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "activiti.rest.enable-deletion=true")
@TestPropertySource("classpath:application-test.properties")
@WebMvcTest(ProcessInstanceDeleteController.class)
@Import({
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser("admin")
public class ProcessInstanceEntityDeleteControllerIT {

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

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private VariableRepository variableRepository;

    @MockBean
    private  ServiceTaskRepository serviceTaskRepository;

    @MockBean
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @MockBean
    private BPMNActivityRepository bpmnActivityRepository;

    @MockBean
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("admin");
        assertThat(entityFinder).isNotNull();
        assertThat(securityPoliciesManager).isNotNull();
        assertThat(processDefinitionRepository).isNotNull();
        assertThat(securityPoliciesProperties).isNotNull();
        assertThat(taskLookupRestrictionService).isNotNull();
        assertThat(taskRepository).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    public void deleteProcessInstancesShouldReturnAllProcessInstancesAndDeleteThem() throws Exception{

        //given
        List<ProcessInstanceEntity> processInstanceEntities = Collections.singletonList(buildDefaultProcessInstance());
        given(processInstanceRepository.findAll(any(Predicate.class)))
                .willReturn(processInstanceEntities);

        //when
        mockMvc.perform(delete("/admin/v1/process-instances")
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk());

        verify(processInstanceRepository).deleteAll(processInstanceEntities);

    }

    @Test
    public void deleteProcessInstancesShouldDeleteProcessInstancesAndRelatedEntities() throws Exception{
        //given
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        List<ProcessInstanceEntity> processInstanceEntities = Collections.singletonList(processInstanceEntity);
        given(processInstanceRepository.findAll(any(Predicate.class)))
            .willReturn(processInstanceEntities);

        //when
        mockMvc.perform(delete("/admin/v1/process-instances")
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isOk());

        verify(taskRepository).deleteAll(processInstanceEntity.getTasks());
        verify(variableRepository).deleteAll(processInstanceEntity.getVariables());
        verify(bpmnActivityRepository).deleteAll(processInstanceEntity.getActivities());
        verify(serviceTaskRepository).deleteAll(processInstanceEntity.getServiceTasks());
        verify(bpmnSequenceFlowRepository).deleteAll(processInstanceEntity.getSequenceFlows());

        verify(processInstanceRepository).deleteAll(processInstanceEntities);
    }

    private ProcessInstanceEntity buildDefaultProcessInstance() {
        ProcessInstanceEntity processInstance = new ProcessInstanceEntity("My-app", "My-app", "1", null, null,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                ProcessInstance.ProcessInstanceStatus.RUNNING,
                new Date());
        processInstance.setTasks(Set.of(buildDefaultTask()));
        processInstance.setVariables(Set.of(buildDefaultProcessVariableEntity()));
        processInstance.setActivities(Set.of(buildDefaultBPMNActivityEntity()));
        processInstance.setServiceTasks(Arrays.asList(buildDefaulterviceTaskEntity()));
        processInstance.setSequenceFlows(Arrays.asList(buildDefaultBPMNSequenceFlowEntity()));
        return processInstance;
    }

    private TaskEntity buildDefaultTask() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("My-task");
        taskEntity.setName("My-task");
        return taskEntity;
    }

    private ProcessVariableEntity buildDefaultProcessVariableEntity() {
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName("My-process-variable");
        return processVariableEntity;
    }

    private BPMNActivityEntity buildDefaultBPMNActivityEntity() {
        BPMNActivityEntity bpmnActivityEntity = new BPMNActivityEntity("My-bpmn-activiti", "My-bpmn-activiti", "1",
            "My-app", "2");
        return bpmnActivityEntity;
    }

    private ServiceTaskEntity buildDefaulterviceTaskEntity() {

        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity("My-service-task", "My-service-task", "1",
            "My-app", "2");
        serviceTaskEntity.setActivityType("serviceTask");
        return serviceTaskEntity;
    }

    private BPMNSequenceFlowEntity buildDefaultBPMNSequenceFlowEntity() {
        BPMNSequenceFlowEntity bpmnSequenceFlowEntity = new BPMNSequenceFlowEntity("My-sequence-flow", "My-sequence-flow",
            "1", "My-app", "2");
        return bpmnSequenceFlowEntity;
    }
}
