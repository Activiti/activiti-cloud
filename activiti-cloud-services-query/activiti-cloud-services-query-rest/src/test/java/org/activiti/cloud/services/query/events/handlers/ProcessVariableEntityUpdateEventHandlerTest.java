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

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessVariableEntityUpdateEventHandlerTest {

    @InjectMocks
    private ProcessVariableUpdateEventHandler handler;

    @Mock
    private VariableUpdater variableUpdater;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateVariable() {
        //given
        VariableEntity variableEntity = new VariableEntity();
        variableEntity.setName("var");
        variableEntity.setValue("v1");
        variableEntity.setProcessInstanceId("10");

        //when
        handler.handle(variableEntity);

        //then
        verify(variableUpdater).update(eq(variableEntity), any(Predicate.class), anyString());
    }
}