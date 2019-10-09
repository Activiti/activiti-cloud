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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryEventHandlerContextIT {

    @Autowired
    private QueryEventHandlerContext context;

    @Test
    public void shouldHaveHandlersForAllSupportedEvents() {
        //when
        Map<String, QueryEventHandler> handlers = context.getHandlers();

        //then
        assertThat(handlers).containsOnlyKeys(
                ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_CREATED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name(),
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name(),
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name(),
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name(),
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name(),
                VariableEvent.VariableEvents.VARIABLE_CREATED.name(),
                VariableEvent.VariableEvents.VARIABLE_UPDATED.name(),
                VariableEvent.VariableEvents.VARIABLE_DELETED.name(),
                BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name(),
                BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name(),
                BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name(),
                SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name()
        );
        assertThat(handlers.get(ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name())).isInstanceOf(ProcessDeployedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name())).isInstanceOf(ProcessCreatedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name())).isInstanceOf(ProcessStartedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name())).isInstanceOf(ProcessCompletedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name())).isInstanceOf(ProcessSuspendedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name())).isInstanceOf(ProcessResumedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name())).isInstanceOf(ProcessCancelledEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name())).isInstanceOf(ProcessUpdatedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name())).isInstanceOf(TaskCreatedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name())).isInstanceOf(TaskAssignedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name())).isInstanceOf(TaskSuspendedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name())).isInstanceOf(TaskActivatedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name())).isInstanceOf(TaskCompletedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name())).isInstanceOf(TaskCancelledEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name())).isInstanceOf(TaskUpdatedEventHandler.class);
        assertThat(handlers.get(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name())).isInstanceOf(TaskCandidateUserAddedEventHandler.class);
        assertThat(handlers.get(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name())).isInstanceOf(TaskCandidateGroupAddedEventHandler.class);
        assertThat(handlers.get(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name())).isInstanceOf(TaskCandidateUserRemovedEventHandler.class);
        assertThat(handlers.get(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name())).isInstanceOf(TaskCandidateGroupRemovedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_CREATED.name())).isInstanceOf(VariableCreatedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_UPDATED.name())).isInstanceOf(VariableUpdatedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_DELETED.name())).isInstanceOf(VariableDeletedEventHandler.class);
        assertThat(handlers.get(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name())).isInstanceOf(BPMNActivityStartedEventHandler.class);
        assertThat(handlers.get(BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name())).isInstanceOf(BPMNActivityCompletedEventHandler.class);
        assertThat(handlers.get(BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name())).isInstanceOf(BPMNActivityCancelledEventHandler.class);
        assertThat(handlers.get(SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name())).isInstanceOf(BPMNSequenceFlowTakenEventHandler.class);
    }
}