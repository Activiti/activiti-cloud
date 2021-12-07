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
package org.activiti.cloud.conf;

import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.activiti.cloud.services.query.app.repository.*;
import org.activiti.cloud.services.query.events.handlers.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import java.util.Set;

@Configuration
@EnableBinding(QueryConsumerChannels.class)
public class EventHandlersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QueryConsumerChannelHandler queryConsumerChannelHandler(QueryEventHandlerContext eventHandlerContext) {
        return new QueryConsumerChannelHandler(eventHandlerContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDeployedEventHandler processDeployedEventHandler(ProcessDefinitionRepository processDefinitionRepository,
                                                                   ProcessModelRepository processModelRepository) {
        return new ProcessDeployedEventHandler(processDefinitionRepository,
                                               processModelRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCancelledEventHandler processCancelledEventHandler(EntityManager entityManager) {
        return new ProcessCancelledEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCompletedEventHandler processCompletedEventHandler(EntityManager entityManager) {
        return new ProcessCompletedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCreatedEventHandler processCreatedEventHandler(EntityManager entityManager) {
        return new ProcessCreatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessResumedEventHandler processResumedEventHandler(EntityManager entityManager) {
        return new ProcessResumedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessStartedEventHandler processStartedEventHandler(EntityManager entityManager) {
        return new ProcessStartedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessSuspendedEventHandler processSuspendedEventHandler(EntityManager entityManager) {
        return new ProcessSuspendedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessUpdatedEventHandler processUpdatedEventHandler(EntityManager entityManager) {
        return new ProcessUpdatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskActivatedEventHandler taskActivatedEventHandler(EntityManager entityManager) {
        return new TaskActivatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskAssignedEventHandler taskAssignedEventHandler(EntityManager entityManager) {
        return new TaskAssignedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCancelledEventHandler taskCancelledEventHandler(EntityManager entityManager) {
        return new TaskCancelledEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateGroupAddedEventHandler taskCandidateGroupAddedEventHandler(EntityManager entityManager) {
        return new TaskCandidateGroupAddedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateGroupRemovedEventHandler taskCandidateGroupRemovedEventHandler(TaskRepository taskRepository,
                                                                                       TaskCandidateGroupRepository taskCandidateGroupRepository) {
        return new TaskCandidateGroupRemovedEventHandler(taskRepository, taskCandidateGroupRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateUserAddedEventHandler taskCandidateUserAddedEventHandler(TaskCandidateUserRepository taskCandidateUserRepository) {
        return new TaskCandidateUserAddedEventHandler(taskCandidateUserRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateUserRemovedEventHandler taskCandidateUserRemovedEventHandler(TaskRepository taskRepository,
                                                                                     TaskCandidateUserRepository taskCandidateUserRepository) {
        return new TaskCandidateUserRemovedEventHandler(taskRepository, taskCandidateUserRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCompletedEventHandler taskCompletedEventHandler(EntityManager entityManager) {
        return new TaskCompletedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCreatedEventHandler taskCreatedEventHandler(EntityManager entityManager) {
        return new TaskCreatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskSuspendedEventHandler taskSuspendedEventHandler(EntityManager entityManager) {
        return new TaskSuspendedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskUpdatedEventHandler taskUpdatedEventHandler(EntityManager entityManager) {
        return new TaskUpdatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public VariableCreatedEventHandler variableCreatedEventHandler(EntityManager entityManager) {
        return new VariableCreatedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public VariableDeletedEventHandler variableDeletedEventHandler(TaskRepository taskRepository,
                                                                   ProcessInstanceRepository processInstanceRepository,
                                                                   VariableRepository variableRepository,
                                                                   EntityFinder entityFinder,
                                                                   TaskVariableRepository taskVariableRepository) {
        return new VariableDeletedEventHandler(new ProcessVariableDeletedEventHandler(processInstanceRepository,
                                                                                      variableRepository,
                                                                                      entityFinder),
                                               new TaskVariableDeletedEventHandler(taskRepository,
                                                                                   taskVariableRepository,
                                                                                   entityFinder));
    }

    @Bean
    @ConditionalOnMissingBean
    public VariableUpdatedEventHandler variableUpdatedEventHandler(EntityFinder entityFinder,
                                                                   VariableRepository variableRepository,
                                                                   TaskVariableRepository taskVariableRepository) {
        return new VariableUpdatedEventHandler(new ProcessVariableUpdateEventHandler(new ProcessVariableUpdater(entityFinder,
                                                                                                                variableRepository)),
                                               new TaskVariableUpdatedEventHandler(new TaskVariableUpdater(entityFinder,
                                                                                                           taskVariableRepository)));
    }

    @Bean
    @ConditionalOnMissingBean
    public BPMNActivityStartedEventHandler bpmnActivityStartedEventHandler(EntityManager entityManager) {
        return new BPMNActivityStartedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public BPMNActivityCompletedEventHandler bpmnActivityCompletedEventHandler(EntityManager entityManager) {
        return new BPMNActivityCompletedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public BPMNActivityCancelledEventHandler bpmnActivityCancelledEventHandler(EntityManager entityManager) {
        return new BPMNActivityCancelledEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public BPMNSequenceFlowTakenEventHandler bpmnSequenceFlowTakenEventHandler(BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
                                                                               EntityManager entityManager) {
        return new BPMNSequenceFlowTakenEventHandler(bpmnSequenceFlowRepository,
                                                     entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultReceivedEventHandler integrationResultReceivedEventHandler(IntegrationContextRepository integrationContextRepository,
                                                                                       ServiceTaskRepository serviceTaskRepository,
                                                                                       EntityManager entityManager) {
        return new IntegrationResultReceivedEventHandler(integrationContextRepository,
                                                         serviceTaskRepository,
                                                         entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationRequestedEventHandler integrationRequestedEventHandler(IntegrationContextRepository integrationContextRepository,
                                                                             ServiceTaskRepository serviceTaskRepository,
                                                                             EntityManager entityManager) {
        return new IntegrationRequestedEventHandler(integrationContextRepository,
                                                    serviceTaskRepository,
                                                    entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorReceivedEventHandler integrationErrorReceivedEventHandler(IntegrationContextRepository integrationContextRepository,
                                                                                     ServiceTaskRepository serviceTaskRepository,
                                                                                     EntityManager entityManager) {
        return new IntegrationErrorReceivedEventHandler(integrationContextRepository,
                                                        serviceTaskRepository,
                                                        entityManager);
    }


    @Bean
    @ConditionalOnMissingBean
    public QueryEventHandlerContext queryEventHandlerContext(Set<QueryEventHandler> handlers) {
        return new QueryEventHandlerContext(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationDeployedEventHandler applicationDeployedEventHandler(EntityManager entityManager,
                                                                           ApplicationRepository applicationRepository) {
        return new ApplicationDeployedEventHandler(entityManager,
                                                   applicationRepository);
    }
}
