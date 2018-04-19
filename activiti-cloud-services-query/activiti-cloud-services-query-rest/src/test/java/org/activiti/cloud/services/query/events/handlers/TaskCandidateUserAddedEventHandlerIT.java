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

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.events.TaskCandidateUserAddedEvent;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
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
 * TaskCandidateUserAddedEventHandler JPA Repository Integration Tests
 * 
 */
@RunWith(SpringRunner.class)
@DataJpaTest(showSql = true)
@Sql(value = "classpath:/jpa-test.sql")
public class TaskCandidateUserAddedEventHandlerIT {

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateUserAddedEventHandler handler;

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = TaskCandidateUserRepository.class)
    @EntityScan(basePackageClasses = TaskCandidateUser.class)
    @Import(TaskCandidateUserAddedEventHandler.class)
    static class Configuation {
    }


    @After
    public void tearDown() throws Exception {
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    public void contextLoads() {
        // Should pass
    }

    @Test
    public void handleShouldStoreNewTaskCandidateUser() throws Exception {

        //given
        TaskCandidateUser eventTaskCandidateUser = new TaskCandidateUser("task_id","user_id");
        TaskCandidateUserAddedEvent taskCandidateUserAddedEvent = new TaskCandidateUserAddedEvent(System.currentTimeMillis(),
                                                                                        "taskCandidateUserAdded",
                                                                                        null,
                                                                                        null,
                                                                                        null,
                new Service("runtime-bundle-a","runtime-bundle-a",null,null),
                new Application(),
                                                                                        eventTaskCandidateUser);

        //when
        handler.handle(taskCandidateUserAddedEvent);

        //then
        Iterable<TaskCandidateUser> iterable = taskCandidateUserRepository.findAll();

        assertThat(iterable.iterator().hasNext()).isTrue();
        TaskCandidateUser returnedTaskCandidateUser = iterable.iterator().next();
        assertThat(returnedTaskCandidateUser.getUserId()).isEqualToIgnoringCase("user_id");
        assertThat(returnedTaskCandidateUser.getTaskId()).isEqualToIgnoringCase("task_id");
    }

}
