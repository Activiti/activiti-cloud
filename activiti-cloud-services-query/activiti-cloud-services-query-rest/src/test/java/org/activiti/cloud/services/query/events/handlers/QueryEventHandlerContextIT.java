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

import java.util.Map;

import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.TaskCandidateGroupEvent;
import org.activiti.runtime.api.event.TaskCandidateUserEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.VariableEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextIT.MOCK_DEPENDENCIES_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = QueryEventHandlerContextIT.QueryEventHandlerContextConfig.class)
@ActiveProfiles(MOCK_DEPENDENCIES_PROFILE)
//@DataJpaTest // Needed to auto configure JPA Entity Manager
public class QueryEventHandlerContextIT {

    public static final String MOCK_DEPENDENCIES_PROFILE = "mockDependencies";

    @Autowired
    private QueryEventHandlerContext context;

    @Configuration
    @Profile(MOCK_DEPENDENCIES_PROFILE)
    @ComponentScan(basePackages = {"org.activiti.cloud.services.query.events.handlers",  "org.activiti.cloud.services.query.app.repository"})
    public static class QueryEventHandlerContextConfig {

        // the purpose of this test is to verify that handlers for every supported event is inject
        // so we can mock transitive component dependencies
        @Primary
        @Bean
        public ProcessInstanceRepository getProcessInstanceRepository() {
            return mock(ProcessInstanceRepository.class);
        }

        @Primary
        @Bean
        public TaskRepository getTaskRepository() {
            return mock(TaskRepository.class);
        }

        @Primary
        @Bean
        public VariableRepository getVariableRepository() {
            return mock(VariableRepository.class);
        }

        @Primary
        @Bean
        public TaskCandidateGroupRepository taskCandidateGroupRepository() {
            return mock(TaskCandidateGroupRepository.class);
        }


        @Primary
        @Bean
        public TaskCandidateUserRepository taskCandidateUserRepository() {
            return mock(TaskCandidateUserRepository.class);
        }

        @Primary
        @Bean
        public EntityManager entityManager(){
            return mock(EntityManager.class);
        }

    }

    @Test
    public void shouldHaveHandlersForAllSupportedEvents() {
        //when
        Map<String, QueryEventHandler> handlers = context.getHandlers();

        //then
        assertThat(handlers).containsOnlyKeys(
                ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name(),
                ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_CREATED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name(),
                TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name(),
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name(),
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name(),
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name(),
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name(),
                VariableEvent.VariableEvents.VARIABLE_CREATED.name(),
                VariableEvent.VariableEvents.VARIABLE_UPDATED.name(),
                VariableEvent.VariableEvents.VARIABLE_DELETED.name()
        );
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name())).isInstanceOf(ProcessCreatedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name())).isInstanceOf(ProcessStartedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name())).isInstanceOf(ProcessCompletedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name())).isInstanceOf(ProcessSuspendedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name())).isInstanceOf(ProcessResumedEventHandler.class);
        assertThat(handlers.get(ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name())).isInstanceOf(ProcessCancelledEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name())).isInstanceOf(TaskCreatedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name())).isInstanceOf(TaskAssignedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name())).isInstanceOf(TaskSuspendedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name())).isInstanceOf(TaskActivatedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name())).isInstanceOf(TaskCompletedEventHandler.class);
        assertThat(handlers.get(TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name())).isInstanceOf(TaskCancelledEventHandler.class);
        assertThat(handlers.get(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name())).isInstanceOf(TaskCandidateUserAddedEventHandler.class);
        assertThat(handlers.get(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name())).isInstanceOf(TaskCandidateUserRemovedEventHandler.class);
        assertThat(handlers.get(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name())).isInstanceOf(TaskCandidateGroupAddedEventHandler.class);
        assertThat(handlers.get(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name())).isInstanceOf(TaskCandidateGroupRemovedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_CREATED.name())).isInstanceOf(VariableCreatedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_UPDATED.name())).isInstanceOf(VariableUpdatedEventHandler.class);
        assertThat(handlers.get(VariableEvent.VariableEvents.VARIABLE_DELETED.name())).isInstanceOf(VariableDeletedEventHandler.class);
    }
}