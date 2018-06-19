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

public class TaskCandidateUserAddedEventHandlerTest {

//    @InjectMocks
//    private TaskCandidateUserAddedEventHandler handler;
//
//    @Mock
//    private TaskCandidateUserRepository taskCandidateRepository;
//
//    @Before
//    public void setUp() throws Exception {
//        initMocks(this);
//    }
//
//    @Test
//    public void handleShouldStoreNewTaskInstance() throws Exception {
//        //given
//        TaskCandidateUser eventTaskCandidate = mock(TaskCandidateUser.class);
//        TaskCandidateUserAddedEvent taskCreated = new TaskCandidateUserAddedEvent(System.currentTimeMillis(),
//                                                            "taskCandidateUserAdded",
//                                                            "10",
//                                                            "100",
//                                                            "200",
//                                                "runtime-bundle-a",
//                                                "runtime-bundle-a",
//                                                "runtime-bundle",
//                                                "1",
//                                                null,
//                                                null,
//                                                            eventTaskCandidate);
//
//
//        //when
//        handler.handle(taskCreated);
//
//        //then
//        verify(taskCandidateRepository).save(eventTaskCandidate);
//    }
//
//    @Test
//    public void getHandledEventClassShouldReturnTaskCreatedEventClass() throws Exception {
//        //when
//        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();
//
//        //then
//        assertThat(handledEventClass).isEqualTo(TaskCandidateUserAddedEvent.class);
//    }
}