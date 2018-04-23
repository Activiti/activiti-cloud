/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.events.handlers;

import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.events.TaskCandidateGroupAddedEvent;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskCandidateGroupAddedEventHandler JPA Repository Integration Tests
 * 
 */
@RunWith(SpringRunner.class)
@DataJpaTest(showSql = true)
@Sql(value = "classpath:/jpa-test.sql")
public class TaskCandidateGroupAddedEventHandlerIT {

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateGroupAddedEventHandler handler;

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = TaskCandidateGroupRepository.class)
    @EntityScan(basePackageClasses = TaskCandidateGroup.class)
    @Import(TaskCandidateGroupAddedEventHandler.class)
    static class Configuation {
    }

    @Test
    public void contextLoads() {
        // Should pass
    }


    @After
    public void tearDown() throws Exception {
        taskCandidateGroupRepository.deleteAll();
    }

    @Test
    public void handleShouldStoreNewTaskCandidateGroup() throws Exception {

        //given
        TaskCandidateGroup eventTaskCandidateGroup = new TaskCandidateGroup("task_id","group_id");
        TaskCandidateGroupAddedEvent taskCandidateGroupAddedEvent = new TaskCandidateGroupAddedEvent(System.currentTimeMillis(),
                                                                                        "taskCandidateGroupAdded",
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                "runtime-bundle-a",
                                                                                "runtime-bundle-a",
                                                                                "runtime-bundle",
                                                                                "1",
                                                                                null,
                                                                                null,
                                                                                        eventTaskCandidateGroup);

        //when
        handler.handle(taskCandidateGroupAddedEvent);

        //then
        Iterable<TaskCandidateGroup> iterable = taskCandidateGroupRepository.findAll();

        assertThat(iterable.iterator().hasNext()).isTrue();
        TaskCandidateGroup returnedTaskCandidateGroup = iterable.iterator().next();
        assertThat(returnedTaskCandidateGroup.getGroupId()).isEqualToIgnoringCase("group_id");
        assertThat(returnedTaskCandidateGroup.getTaskId()).isEqualToIgnoringCase("task_id");
    }

}
