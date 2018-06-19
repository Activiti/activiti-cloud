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

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for {@link TaskCancelledEventHandler}
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Sql(value = "classpath:/jpa-test.sql")
public class TaskCancelledEventHandlerIT {

//    @Autowired
//    private TaskRepository taskRepository;
//
//    @Autowired
//    private TaskCancelledEventHandler handler;
//
//    @SpringBootConfiguration
//    @EnableJpaRepositories(basePackageClasses = TaskRepository.class)
//    @EntityScan(basePackageClasses = Task.class)
//    @Import(TaskCancelledEventHandler.class)
//    static class Configuation {
//
//    }
//
//    @Test
//    public void contextLoads() {
//        // Should pass
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        taskRepository.deleteAll();
//    }
//
//    /**
//     * Test that consuming a task cancelled event
//     * will update the status and the last modified for the existing task.
//     */
//    @Test
//    public void testUpdateExistingTaskWhenTaskCancelledEventIsConsumend() {
//        //GIVEN
//        String taskId = "8";
//        assertThat(taskRepository.findById(taskId)).hasValueSatisfying(task -> {
//            assertThat(task.getStatus()).isEqualTo("ASSIGNED");
//        });
//
//        //WHEN
//        Long eventTime = System.currentTimeMillis();
//        TaskCancelledEvent event = new TaskCancelledEvent();
//        event.setTimestamp(eventTime);
//        event.setTask(aTask().withId(taskId).build());
//        handler.handle(event);
//
//        //THEN
//        assertThat(taskRepository.findById(taskId)).hasValueSatisfying(task -> {
//            assertThat(task.getStatus()).isEqualTo("CANCELLED");
//            assertThat(task.getLastModified()).isEqualTo(new Date(eventTime));
//        });
//    }

}
