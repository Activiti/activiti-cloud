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

import java.util.Collections;

import org.activiti.runtime.api.event.CloudTaskCompletedEvent;
import org.activiti.runtime.api.event.CloudTaskCreatedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCreatedEventImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueryEventHandlerContextTest {

    private QueryEventHandlerContext context;

    @Mock
    private QueryEventHandler handler;

    @Before
    public void setUp() {
        initMocks(this);
        doReturn(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name()).when(handler).getHandledEvent();
        context = new QueryEventHandlerContext(Collections.singleton(handler));
    }

    @Test
    public void handleShouldSelectHandlerBasedOnEventType() {
        //given
        CloudTaskCreatedEvent event = new CloudTaskCreatedEventImpl();

        //when
        context.handle(event);

        //then
        verify(handler).handle(event);
    }

    @Test
    public void handleShouldDoNothingWhenNoHandlerIsFoundForTheGivenEvent() {
        //given
        CloudTaskCompletedEvent event = new CloudTaskCompletedEventImpl();

        //when
        context.handle(event);

        //then
        verify(handler, never()).handle(any());
    }
}