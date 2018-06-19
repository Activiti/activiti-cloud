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

public class TaskVariableDeletedEventHandlerTest {

//    @InjectMocks
//    private TaskVariableDeletedEventHandler handler;
//
//    @Mock
//    private VariableRepository variableRepository;
//
//    @Mock
//    private TaskRepository taskRepository;
//
//    @Mock
//    private EntityFinder entityFinder;
//
//    @Before
//    public void setUp() throws Exception {
//        initMocks(this);
//    }
//
//    @Test
//    public void handleShouldRemoveVariableFromProcessAndSoftDeleteIt() throws Exception {
//        //given
//        VariableDeletedEvent event = new VariableDeletedEvent();
//        event.setTaskId("10");
//        event.setVariableName("var");
//
//        Variable variable = new Variable();
//        given(entityFinder.findOne(eq(variableRepository), any(Predicate.class), anyString())).willReturn(variable);
//
//
//        //when
//        handler.handle(event);
//
//        //then
//        verify(variableRepository).save(variable);
//        assertThat(variable.getMarkedAsDeleted()).isTrue();
//    }
//
}