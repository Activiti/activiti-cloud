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

package org.activiti.cloud.starter.audit.tests.it;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.converters.ActivityCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ActivityCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ActivityStartedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessDeployedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessResumedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessStartedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessSuspendedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.SequenceFlowTakenEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.SignalReceivedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskAssignedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCandidateGroupAddedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCandidateGroupRemovedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCandidateUserAddedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCandidateUserRemovedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskSuspendedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableDeletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableUpdatedEventConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource("classpath:application.properties")
public class APIEventToEntityConvertersIT {

    @Autowired
    private APIEventToEntityConverters eventConverters;

    @Test
    public void shouldHaveConvertersForAllCoveredEvents() {
        
        EventToEntityConverter converter;
        
        converter = eventConverters.getConverterByEventTypeName(BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name());
        assertThat(converter).isNotNull().isInstanceOf(ActivityCancelledEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name());
        assertThat(converter).isNotNull().isInstanceOf(ActivityCompletedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name());
        assertThat(converter).isNotNull().isInstanceOf(ActivityStartedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessCancelledEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessCompletedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessCreatedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessDeployedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessResumedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessStartedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessSuspendedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name());
        assertThat(converter).isNotNull().isInstanceOf(ProcessUpdatedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name());
        assertThat(converter).isNotNull().isInstanceOf(SignalReceivedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name());
        assertThat(converter).isNotNull().isInstanceOf(SequenceFlowTakenEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskAssignedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCancelledEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCompletedEventConverter.class);

        converter = eventConverters.getConverterByEventTypeName(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCandidateGroupAddedEventConverter.class);

        converter = eventConverters.getConverterByEventTypeName(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCandidateGroupRemovedEventConverter.class);

        converter = eventConverters.getConverterByEventTypeName(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCandidateUserAddedEventConverter.class);

        converter = eventConverters.getConverterByEventTypeName(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCandidateUserRemovedEventConverter.class);

        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskCreatedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskSuspendedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name());
        assertThat(converter).isNotNull().isInstanceOf(TaskUpdatedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
        assertThat(converter).isNotNull().isInstanceOf(VariableCreatedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(VariableEvent.VariableEvents.VARIABLE_DELETED.name());
        assertThat(converter).isNotNull().isInstanceOf(VariableDeletedEventConverter.class);
        
        converter = eventConverters.getConverterByEventTypeName(VariableEvent.VariableEvents.VARIABLE_UPDATED.name());
        assertThat(converter).isNotNull().isInstanceOf(VariableUpdatedEventConverter.class);
        
    }
 
    
}
