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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryEventHandlerContextOptimizerTest {

    @InjectMocks
    private QueryEventHandlerContextOptimizer subject;

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {}

    @Test
    void optimizeTaskVariableEvents() {
        //given
        CloudVariableCreatedEventImpl cloudVariableCreatedEvent = new CloudVariableCreatedEventImpl();
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl();
        CloudVariableDeletedEventImpl cloudVariableDeletedEvent = new CloudVariableDeletedEventImpl();
        CloudVariableUpdatedEventImpl cloudVariableUpdatedEvent = new CloudVariableUpdatedEventImpl();

        List<CloudRuntimeEvent<?, ?>> events = Arrays.asList(
            cloudVariableCreatedEvent,
            cloudTaskCreatedEvent,
            cloudVariableDeletedEvent,
            cloudVariableUpdatedEvent
        );

        //when
        List<CloudRuntimeEvent<?, ?>> result = subject.optimize(events);

        //then
        assertThat(result)
            .containsExactly(
                cloudTaskCreatedEvent,
                cloudVariableCreatedEvent,
                cloudVariableUpdatedEvent,
                cloudVariableDeletedEvent
            );
    }

    @Test
    void optimizeTaskCandidateGroupsEvents() {
        //given
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl();
        CloudTaskAssignedEventImpl cloudTaskAssignedEvent = new CloudTaskAssignedEventImpl();
        CloudTaskCompletedEventImpl cloudTaskCompletedEvent = new CloudTaskCompletedEventImpl();
        CloudTaskUpdatedEventImpl cloudTaskUpdatedEvent = new CloudTaskUpdatedEventImpl();
        CloudTaskCancelledEventImpl cloudTaskCancelledEvent = new CloudTaskCancelledEventImpl();
        CloudTaskCandidateUserAddedEventImpl cloudTaskCandidateUserAddedEvent = new CloudTaskCandidateUserAddedEventImpl();
        CloudTaskCandidateGroupAddedEventImpl cloudTaskCandidateGroupAddedEvent = new CloudTaskCandidateGroupAddedEventImpl();
        CloudTaskCandidateUserRemovedEventImpl cloudTaskCandidateUserRemovedEvent = new CloudTaskCandidateUserRemovedEventImpl();
        CloudTaskCandidateGroupRemovedEventImpl cloudTaskCandidateGroupRemovedEvent = new CloudTaskCandidateGroupRemovedEventImpl();

        List<CloudRuntimeEvent<?, ?>> events = Arrays.asList(
            cloudTaskCandidateUserAddedEvent,
            cloudTaskCandidateGroupAddedEvent,
            cloudTaskCreatedEvent,
            cloudTaskAssignedEvent,
            cloudTaskUpdatedEvent,
            cloudTaskCandidateUserRemovedEvent,
            cloudTaskCandidateGroupRemovedEvent,
            cloudTaskCompletedEvent,
            cloudTaskCancelledEvent
        );

        //when
        List<CloudRuntimeEvent<?, ?>> result = subject.optimize(events);

        //then
        assertThat(result)
            .containsExactly(
                cloudTaskCreatedEvent,
                cloudTaskCandidateUserAddedEvent,
                cloudTaskCandidateGroupAddedEvent,
                cloudTaskAssignedEvent,
                cloudTaskUpdatedEvent,
                cloudTaskCompletedEvent,
                cloudTaskCancelledEvent,
                cloudTaskCandidateUserRemovedEvent,
                cloudTaskCandidateGroupRemovedEvent
            );
    }
}
