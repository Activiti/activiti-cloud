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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.querydsl.core.types.Predicate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public class TaskControllerHelperIT {

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private org.activiti.cloud.services.query.app.repository.VariableRepository variableRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Test
    public void shouldtest() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        int numberOfVariables = 16;

        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < numberOfVariables; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue("id");
            processVariableEntity.setProcessInstanceId("15");
            processVariableEntity.setProcessDefinitionKey("defKey1");
            processVariableEntity.setProcessInstance(processInstanceRepository.findById("15").orElseThrow());
            variables.add(processVariableEntity);
        }
        variableRepository.saveAll(variables);

        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);

        int numberOfTasks = 2000;
        int batchSize = 1000;

        List<TaskEntity> taskEntities = new ArrayList<>();

        //Create candidate users

        for (int i = 0; i < numberOfTasks; i++) {
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setId("id" + i);
            TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity("id" + i, "group" + i);
            taskEntity.setTaskCandidateGroups(Set.of(groupCand));
            TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity("id" + i, "user" + i);
            taskEntity.setTaskCandidateUsers(Set.of(usrCand));
            taskEntity.setProcessVariables(variables);
            taskEntity.setProcessInstance(processInstanceEntity);
            taskEntity.setProcessInstanceId("15");
            taskEntities.add(taskEntity);

            taskCandidateGroupRepository.save(groupCand);
            taskCandidateUserRepository.save(usrCand);

            if (i > 0 && i % batchSize == 0) {
                taskRepository.saveAll(taskEntities);
                taskEntities.clear();
            }
        }

        if (!taskEntities.isEmpty()) {
            taskRepository.saveAll(taskEntities);
        }

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);
        int pageSize = 200;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());
        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = IntStream
            .range(0, numberOfVariables)
            .mapToObj(i -> "defKey1/name" + i)
            .toList();

        enableSqlLogging();

        PagedModel<EntityModel<QueryCloudTask>> allWithProcessVariables = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );
        List<EntityModel<QueryCloudTask>> entityModels = allWithProcessVariables.getContent().stream().toList();
        assertThat(entityModels).isNotEmpty();
        assertThat(entityModels).hasSize(pageSize);
        EntityModel<QueryCloudTask> queryCloudTaskEntityModel = entityModels.get(0);
        assertThat(queryCloudTaskEntityModel.getContent().getProcessVariables()).hasSize(numberOfVariables);
    }

    private static void enableSqlLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("org.hibernate.SQL");
        logger.setLevel(Level.DEBUG);
    }
}
