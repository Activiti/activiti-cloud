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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TaskEntityVariableEntityUpdatedEventHandlerTest {

    @InjectMocks
    private TaskVariableUpdatedEventHandler handler;

    @Mock
    private TaskVariableUpdater variableUpdater;


    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateVariableValue() {
        //given
        String taskId = "10";
        TaskVariableEntity updatedVariableEntity = new TaskVariableEntity();
        updatedVariableEntity.setName("var");
        updatedVariableEntity.setType("string");
        updatedVariableEntity.setValue("content");
        updatedVariableEntity.setTaskId(taskId);

        //when
        handler.handle(updatedVariableEntity);

        //then
        verify(variableUpdater).update(eq(updatedVariableEntity), any(Predicate.class), anyString());
    }

}