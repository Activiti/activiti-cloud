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

public class TaskSuspendedEventHandlerTest {

//    @InjectMocks
//    private TaskSuspendedEventHandler handler;
//
//    @Mock
//    private TaskRepository taskRepository;
//
//    @Rule
//    public ExpectedException expectedException = ExpectedException.none();
//
//    @Before
//    public void setUp() throws Exception {
//        initMocks(this);
//    }
//
//    @Test
//    public void handleShouldUpdateTaskStatusToSuspended() throws Exception {
//        //given
//        String taskId = "30";
//        Task task = aTask()
//                .withId(taskId)
//                .build();
//
//        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
//
//        //when
//        handler.handle(new TaskSuspendedEvent(System.currentTimeMillis(),
//                             "taskSuspended",
//                             "10",
//                             "100",
//                        "runtime-bundle-a",
//                        "runtime-bundle-a",
//                        "runtime-bundle",
//                        "1",
//                        null,
//                        null,
//                             "200",
//                             task));
//
//        //then
//        verify(taskRepository).save(task);
//        verify(task).setStatus("SUSPENDED");
//        verify(task).setLastModified(any(Date.class));
//    }
//
//    @Test
//    public void handleShouldThrowExceptionWhenNoTaskIsFoundForTheGivenId() throws Exception {
//        //given
//        String taskId = "30";
//        Task task = aTask().withId(taskId).build();
//
//        given(taskRepository.findById(taskId)).willReturn(Optional.empty());
//
//        //then
//        expectedException.expect(ActivitiException.class);
//        expectedException.expectMessage("Unable to find task with id: " + taskId);
//
//        //when
//        handler.handle(new TaskSuspendedEvent(System.currentTimeMillis(),
//                                             "taskSuspended",
//                                             "10",
//                                             "100",
//                                    "runtime-bundle-a",
//                                    "runtime-bundle-a",
//                                    "runtime-bundle",
//                                    "1",
//                                    null,
//                                    null,
//                                             "200",
//                                             task));
//    }
//
//    @Test
//    public void getHandledEventClassShouldReturnTaskAssignedEventClass() throws Exception {
//        //when
//        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();
//
//        //then
//        assertThat(handledEventClass).isEqualTo(TaskSuspendedEvent.class);
//    }
}