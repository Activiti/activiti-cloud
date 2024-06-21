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
package org.activiti.cloud.services.query.events.config;

import jakarta.persistence.EntityManager;
import java.util.Set;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.events.handlers.ApplicationDeployedEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityCancelledEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityCompletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityStartedEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNSequenceFlowTakenEventHandler;
import org.activiti.cloud.services.query.events.handlers.EntityManagerFinder;
import org.activiti.cloud.services.query.events.handlers.IntegrationErrorReceivedEventHandler;
import org.activiti.cloud.services.query.events.handlers.IntegrationRequestedEventHandler;
import org.activiti.cloud.services.query.events.handlers.IntegrationResultReceivedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCancelledEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCandidateStarterGroupAddedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCandidateStarterGroupRemovedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCandidateStarterUserAddedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCandidateStarterUserRemovedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCompletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessDeletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessDeployedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessResumedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessStartedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessSuspendedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessUpdatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessVariableCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessVariableDeletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessVariableUpdateEventHandler;
import org.activiti.cloud.services.query.events.handlers.ProcessVariableUpdater;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandler;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.activiti.cloud.services.query.events.handlers.TaskActivatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskAssignedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCancelledEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCandidateGroupAddedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCandidateGroupRemovedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCandidateUserAddedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCandidateUserRemovedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCompletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskSuspendedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskUpdatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskVariableCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskVariableDeletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskVariableUpdatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.TaskVariableUpdater;
import org.activiti.cloud.services.query.events.handlers.VariableCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.VariableDeletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.VariableUpdatedEventHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class EventHandlersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QueryConsumerChannelHandler queryConsumerChannelHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer fetchingOptimizer,
        EntityManager entityManager
    ) {
        return new QueryConsumerChannelHandler(eventHandlerContext, fetchingOptimizer, entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryEventHandlerContextOptimizer queryEntityGraphFetchingOptimizer(EntityManager entityManager) {
        return new QueryEventHandlerContextOptimizer(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityManagerFinder entityManagerFinder(EntityManager entityManager) {
        return new EntityManagerFinder(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDeployedEventHandler processDeployedEventHandler(EntityManager entityManager) {
        return new ProcessDeployedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCancelledEventHandler processCancelledEventHandler(EntityManager entityManager) {
        return new ProcessCancelledEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCompletedEventHandler processCompletedEventHandler(
        EntityManager entityManager,
        TaskCancelledEventHandler taskCancelledEventHandler
    ) {
        return new ProcessCompletedEventHandler(entityManager, taskCancelledEventHandler);
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
    public ProcessDeletedEventHandler processDeletedEventHandler(EntityManager entityManager) {
        return new ProcessDeletedEventHandler(entityManager);
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
    public TaskCandidateGroupRemovedEventHandler taskCandidateGroupRemovedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new TaskCandidateGroupRemovedEventHandler(entityManager, entityManagerFinder);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateUserAddedEventHandler taskCandidateUserAddedEventHandler(EntityManager entityManager) {
        return new TaskCandidateUserAddedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCandidateUserRemovedEventHandler taskCandidateUserRemovedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new TaskCandidateUserRemovedEventHandler(entityManager, entityManagerFinder);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCompletedEventHandler taskCompletedEventHandler(EntityManager entityManager) {
        return new TaskCompletedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCreatedEventHandler taskCreatedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new TaskCreatedEventHandler(entityManager, entityManagerFinder);
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
    public VariableCreatedEventHandler variableCreatedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new VariableCreatedEventHandler(
            new TaskVariableCreatedEventHandler(entityManager, entityManagerFinder),
            new ProcessVariableCreatedEventHandler(entityManager, entityManagerFinder)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public VariableDeletedEventHandler variableDeletedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new VariableDeletedEventHandler(
            new ProcessVariableDeletedEventHandler(entityManager, entityManagerFinder),
            new TaskVariableDeletedEventHandler(entityManager, entityManagerFinder)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public VariableUpdatedEventHandler variableUpdatedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new VariableUpdatedEventHandler(
            new ProcessVariableUpdateEventHandler(new ProcessVariableUpdater(entityManager, entityManagerFinder)),
            new TaskVariableUpdatedEventHandler(new TaskVariableUpdater(entityManager, entityManagerFinder))
        );
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
    public BPMNSequenceFlowTakenEventHandler bpmnSequenceFlowTakenEventHandler(EntityManager entityManager) {
        return new BPMNSequenceFlowTakenEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultReceivedEventHandler integrationResultReceivedEventHandler(EntityManager entityManager) {
        return new IntegrationResultReceivedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationRequestedEventHandler integrationRequestedEventHandler(EntityManager entityManager) {
        return new IntegrationRequestedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorReceivedEventHandler integrationErrorReceivedEventHandler(EntityManager entityManager) {
        return new IntegrationErrorReceivedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryEventHandlerContext queryEventHandlerContext(Set<QueryEventHandler> handlers) {
        return new QueryEventHandlerContext(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationDeployedEventHandler applicationDeployedEventHandler(
        EntityManager entityManager,
        ApplicationRepository applicationRepository
    ) {
        return new ApplicationDeployedEventHandler(entityManager, applicationRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCandidateStarterUserAddedEventHandler processCandidateStarterUserAddedEventHandler(
        EntityManager entityManager
    ) {
        return new ProcessCandidateStarterUserAddedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCandidateStarterUserRemovedEventHandler processCandidateStarterUserRemovedEventHandler(
        EntityManager entityManager
    ) {
        return new ProcessCandidateStarterUserRemovedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCandidateStarterGroupAddedEventHandler processCandidateStarterGroupAddedEventHandler(
        EntityManager entityManager
    ) {
        return new ProcessCandidateStarterGroupAddedEventHandler(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessCandidateStarterGroupRemovedEventHandler processCandidateStarterGroupRemovedEventHandler(
        EntityManager entityManager
    ) {
        return new ProcessCandidateStarterGroupRemovedEventHandler(entityManager);
    }
}
