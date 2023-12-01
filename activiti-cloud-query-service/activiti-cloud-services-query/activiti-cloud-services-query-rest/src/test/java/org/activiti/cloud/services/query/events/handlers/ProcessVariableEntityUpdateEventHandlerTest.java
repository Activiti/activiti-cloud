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
package org.activiti.cloud.services.query.events.handlers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessVariableEntityUpdateEventHandlerTest {

    @InjectMocks
    private ProcessVariableUpdateEventHandler handler;

    @Mock
    private ProcessVariableUpdater variableUpdater;

    @Test
    public void handleShouldUpdateVariable() {
        //given
        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");
        variableEntity.setValue("v1");
        variableEntity.setProcessInstanceId("10");

        //when
        handler.handle(variableEntity);

        //then
        verify(variableUpdater).update(eq(variableEntity), anyString());
    }
}
