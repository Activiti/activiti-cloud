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

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.events.handlers.*;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(QueryConsumerChannelsConfiguration.class)
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
    public BPMNSequenceFlowTakenEventHandler bpmnSequenceFlowTakenEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder
    ) {
        return new BPMNSequenceFlowTakenEventHandler(entityManager, entityManagerFinder);
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

    @FunctionBinding(input = QueryConsumerChannels.QUERY_CONSUMER)
    @Bean
    public Consumer<List<CloudRuntimeEvent<?, ?>>> queryConsumerFunction(
        QueryConsumerChannelHandler queryConsumerChannelHandler
    ) {
        return queryConsumerChannelHandler::receive;
    }
}
