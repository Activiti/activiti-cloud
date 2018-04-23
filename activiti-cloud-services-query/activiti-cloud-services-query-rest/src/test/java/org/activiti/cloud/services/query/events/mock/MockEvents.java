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

package org.activiti.cloud.services.query.events.mock;

import org.activiti.cloud.services.query.events.AbstractProcessEngineEvent;
import org.activiti.cloud.services.query.events.TaskCancelledEvent;
import org.activiti.cloud.services.query.model.Task;

import static org.activiti.cloud.services.query.events.handlers.TaskBuilder.aTask;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock events builder
 */
public class MockEvents<T extends AbstractProcessEngineEvent> {

    private T mockEvent;

    public static MockEvents<TaskCancelledEvent> taskCancelledEvent(String taskId) {
        MockEvents<TaskCancelledEvent> builder = new MockEvents(TaskCancelledEvent.class);
        TaskCancelledEvent event = builder.mockEvent;
        Task task = aTask().withId(taskId).build();
        when(event.getTask()).thenReturn(task);
        return builder;
    }

    private MockEvents(Class<T> eventClass) {
        mockEvent = mock(eventClass);
    }

    public MockEvents withTimestamp(Long timestamp) {
        when(mockEvent.getTimestamp()).thenReturn(timestamp);
        return this;
    }
    public T get() {
        return mockEvent;
    }

}
