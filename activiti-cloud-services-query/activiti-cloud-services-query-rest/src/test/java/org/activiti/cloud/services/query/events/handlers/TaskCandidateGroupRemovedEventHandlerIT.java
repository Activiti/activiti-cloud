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
 * TaskCandidateGroupRemovedEventHandler JPA Repository Integration Tests
 * 
 */
@RunWith(SpringRunner.class)
@DataJpaTest(showSql = true)
@Sql(value = "classpath:/jpa-test.sql")
public class TaskCandidateGroupRemovedEventHandlerIT {

//    @Autowired
//    private TaskCandidateGroupRepository taskCandidateGroupRepository;
//
//    @Autowired
//    private TaskCandidateGroupRemovedEventHandler handler;
//
//    @SpringBootConfiguration
//    @EnableJpaRepositories(basePackageClasses = TaskCandidateGroupRepository.class)
//    @EntityScan(basePackageClasses = TaskCandidateGroup.class)
//    @Import(TaskCandidateGroupRemovedEventHandler.class)
//    static class Configuation {
//    }
//
//    @Test
//    public void contextLoads() {
//        // Should pass
//    }
//
//
//    @After
//    public void tearDown() throws Exception {
//        taskCandidateGroupRepository.deleteAll();
//    }
//
//    @Test
//    public void handleShouldStoreNewTaskCandidateGroup() throws Exception {
//
//        //given
//        TaskCandidateGroup eventTaskCandidateGroup = new TaskCandidateGroup("task_id","group_id");
//        taskCandidateGroupRepository.save(eventTaskCandidateGroup);
//
//        TaskCandidateGroupRemovedEvent taskCandidateGroupRemovedEvent = new TaskCandidateGroupRemovedEvent(System.currentTimeMillis(),
//                                                                                        "taskCandidateGroupRemoved",
//                                                                                        null,
//                                                                                        null,
//                                                                                        null,
//                                                                            "runtime-bundle-a",
//                                                                            "runtime-bundle-a",
//                                                                            "runtime-bundle",
//                                                                            "1",
//                                                                            null,
//                                                                            null,
//                                                                                        eventTaskCandidateGroup);
//
//        //when
//        handler.handle(taskCandidateGroupRemovedEvent);
//
//        //then
//        Iterable<TaskCandidateGroup> iterable = taskCandidateGroupRepository.findAll();
//
//        assertThat(iterable.iterator().hasNext()).isFalse();
//    }

}
