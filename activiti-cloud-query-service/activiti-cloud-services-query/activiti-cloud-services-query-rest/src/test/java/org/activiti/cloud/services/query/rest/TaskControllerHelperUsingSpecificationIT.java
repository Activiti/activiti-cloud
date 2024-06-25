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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepositorySpecification;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableNameValuePair;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskSearchCriteria;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
@Testcontainers
public class TaskControllerHelperUsingSpecificationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepositorySpecification taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    public void should_returnTasks_withProcessVariablesByKeys() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        //        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);

        Set<ProcessVariableEntity> variables = new HashSet<>();

        ProcessVariableEntity processVariableEntity = getProcessVariableEntity(
            "test",
            "string",
            "{\"value\":\"test\"}",
            processInstanceEntity
        );

        ProcessVariableEntity processVariableIntEntity = getProcessVariableEntity(
            "inttest",
            "integer",
            "{\"value\":\"2\"}",
            processInstanceEntity
        );

        variables.add(processVariableEntity);
        variables.add(processVariableIntEntity);

        variableRepository.saveAll(variables);
        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);

        List<TaskEntity> taskEntities = new ArrayList<>();

        TaskEntity taskEntity = new TaskEntity();
        String taskId = "id";
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(LocalDateTime.fromDateFields(new Date()).toDate());
        taskEntity.setProcessVariables(variables);
        taskEntity.setProcessInstance(processInstanceEntity);
        taskEntity.setProcessInstanceId(processInstanceEntity.getId());
        taskEntities.add(taskEntity);
        taskRepository.save(taskEntity);

        List<ProcessVariableNameValuePair> processVariableNameValuePairs = new ArrayList<>();

        ProcessVariableNameValuePair pair = new ProcessVariableNameValuePair();
        pair.setName("test");
        pair.setValue("test");

        ProcessVariableNameValuePair pairInt = new ProcessVariableNameValuePair();
        pairInt.setName("inttest");
        pairInt.setValue("2");

        processVariableNameValuePairs.add(pair);
        processVariableNameValuePairs.add(pairInt);

        TaskSearchCriteria taskSearchCriteria = new TaskSearchCriteria(processVariableNameValuePairs);

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findTaskByProcessVariables(
            taskSearchCriteria,
            pageable
        );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(QueryCloudTask::getId)
            .containsExactly(
                taskEntities.reversed().stream().limit(pageSize).map(TaskEntity::getId).toArray(String[]::new)
            );
    }

    private static @NotNull ProcessVariableEntity getProcessVariableEntity(
        String name,
        String type,
        String value,
        ProcessInstanceEntity processInstanceEntity
    ) {
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName(name);
        processVariableEntity.setType(type);
        processVariableEntity.setJsonValue(value);
        processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
        processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
        processVariableEntity.setProcessInstance(processInstanceEntity);
        return processVariableEntity;
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("processInstanceId");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }
}
