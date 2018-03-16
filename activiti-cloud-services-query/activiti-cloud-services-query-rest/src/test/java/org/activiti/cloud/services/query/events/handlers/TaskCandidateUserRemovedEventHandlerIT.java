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

import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.events.TaskCandidateUserRemovedEvent;
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
 * TaskCandidateUserRemovedEventHandler JPA Repository Integration Tests
 * 
 */
@RunWith(SpringRunner.class)
@DataJpaTest(showSql = true)
@Sql(value = "classpath:/jpa-test.sql")
public class TaskCandidateUserRemovedEventHandlerIT {

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateUserRemovedEventHandler handler;

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = TaskCandidateUserRepository.class)
    @EntityScan(basePackageClasses = TaskCandidateUser.class)
    @Import(TaskCandidateUserRemovedEventHandler.class)
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
    public void handleShouldStoreRemoveTaskCandidateUser() throws Exception {

        //given
        TaskCandidateUser eventTaskCandidateUser = new TaskCandidateUser("task_id","user_id");
        taskCandidateUserRepository.save(eventTaskCandidateUser);

        TaskCandidateUserRemovedEvent taskCandidateUserRemovedEvent = new TaskCandidateUserRemovedEvent(System.currentTimeMillis(),
                                                                                        "taskCandidateUserRemoved",
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        "runtime-bundle-a",
                                                                                        eventTaskCandidateUser);

        //when
        handler.handle(taskCandidateUserRemovedEvent);

        //then
        Iterable<TaskCandidateUser> iterable = taskCandidateUserRepository.findAll();

        assertThat(iterable.iterator().hasNext()).isFalse();

    }

}
