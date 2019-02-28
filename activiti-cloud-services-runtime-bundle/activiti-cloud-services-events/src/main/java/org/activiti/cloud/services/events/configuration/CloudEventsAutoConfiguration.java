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

package org.activiti.cloud.services.events.configuration;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.activiti.cloud.services.events.converter.ToCloudTaskRuntimeEventConverter;
import org.activiti.cloud.services.events.converter.ToCloudVariableEventConverter;
import org.activiti.cloud.services.events.listeners.CloudActivityCancelledProducer;
import org.activiti.cloud.services.events.listeners.CloudActivityCompletedProducer;
import org.activiti.cloud.services.events.listeners.CloudActivityStartedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessCancelledProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessCompletedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessCreatedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessResumedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessStartedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessSuspendedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessUpdatedProducer;
import org.activiti.cloud.services.events.listeners.CloudSequenceFlowTakenProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskActivatedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskAssignedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCancelledProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateGroupAddedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateGroupRemovedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateUserAddedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateUserRemovedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCompletedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCreatedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskSuspendedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskUpdatedProducer;
import org.activiti.cloud.services.events.listeners.CloudVariableCreatedProducer;
import org.activiti.cloud.services.events.listeners.CloudVariableDeletedProducer;
import org.activiti.cloud.services.events.listeners.CloudVariableUpdatedProducer;
import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.cloud.services.events.message.CloudRuntimeEventMessageBuilderFactory;
import org.activiti.cloud.services.events.message.ExecutionContextMessageBuilderFactory;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources(value={
    @PropertySource(value="classpath:/META-INF/activiti-audit-producer.properties"), // default
    @PropertySource(value="classpath:/activiti-audit-producer.properties", ignoreResourceNotFound = true) // optional override
})
public class CloudEventsAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RuntimeBundleInfoAppender runtimeBundleInfoAppender(RuntimeBundleProperties properties) {
        return new RuntimeBundleInfoAppender(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudRuntimeEventMessageBuilderFactory cloudRuntimeEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new CloudRuntimeEventMessageBuilderFactory(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ExecutionContextMessageBuilderFactory executionContextMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new ExecutionContextMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToCloudProcessRuntimeEventConverter toCloudProcessRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudProcessRuntimeEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToCloudTaskRuntimeEventConverter toCloudTaskRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudTaskRuntimeEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageProducerCommandContextCloseListener apiMessageProducerCommandContextCloseListener(ProcessEngineChannels processEngineChannels,
                                                                                                    ExecutionContextMessageBuilderFactory executionContextMessageBuilderFactory,
                                                                                                    RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new MessageProducerCommandContextCloseListener(processEngineChannels,
                                                              executionContextMessageBuilderFactory,
                                                              runtimeBundleInfoAppender);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessEngineEventsAggregator apiProcessEngineEventsAggregator(MessageProducerCommandContextCloseListener closeListener) {
        return new ProcessEngineEventsAggregator(closeListener);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCreatedProducer cloudTaskCreatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                             ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCreatedProducer(taskRuntimeEventConverter,
                                            eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskUpdatedProducer cloudTaskUpdatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                             ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskUpdatedProducer(taskRuntimeEventConverter,
                                            eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCancelledProducer cloudTaskCancelledProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCancelledProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskAssignedProducer cloudTaskAssignedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                               ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskAssignedProducer(taskRuntimeEventConverter,
                                             eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskSuspendedProducer cloudTaskSuspendedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskSuspendedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskActivatedProducer cloutTaskActivatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskActivatedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCompletedProducer cloudTaskCompletedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCompletedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCandidateUserAddedProducer cloudTaskCandidateUserAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                                   ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateUserAddedProducer(taskRuntimeEventConverter,
                                                       eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCandidateUserRemovedProducer taskCandidateUserRemovedProducer(ToCloudTaskRuntimeEventConverter converter,
                                                                                  ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateUserRemovedProducer(converter,
                                                         eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCandidateGroupAddedProducer cloudTaskCandidateGroupAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateGroupAddedProducer(taskRuntimeEventConverter,
                                                        eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudTaskCandidateGroupRemovedProducer cloudTaskCandidateGroupRemovedProducer(ToCloudTaskRuntimeEventConverter converter,
                                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateGroupRemovedProducer(converter,
                                                          eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessCreatedProducer processCreatedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                              ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCreatedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessStartedProducer processStartedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                              ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessStartedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessSuspendedProducer cloudProcessSuspendedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessSuspendedProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessResumedProducer cloudProcessResumedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                   ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessResumedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessCompletedProducer cloudProcessCompletedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCompletedProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudProcessCancelledProducer cloudProcessCancelledProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCancelledProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    public CloudProcessUpdatedProducer cloudProcessUpdatedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                   ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessUpdatedProducer(eventConverter,
                                               eventsAggregator);
    } 

    @Bean
    @ConditionalOnMissingBean
    public ToCloudVariableEventConverter cloudVariableEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudVariableEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudVariableCreatedProducer cloudVariableCreatedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableCreatedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudVariableUpdatedProducer cloudVariableUpdatedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableUpdatedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudVariableDeletedProducer cloudVariableDeletedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableDeletedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudActivityStartedProducer cloudActivityStartedProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityStartedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudActivityCompletedProducer cloudActivityCompletedProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityCompletedProducer(converter,
                                                  eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudActivityCancelledProducer cloudActivityCancelledProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityCancelledProducer(converter,
                                                  eventsAggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudSequenceFlowTakenProducer cloudSequenceFlowTakenProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudSequenceFlowTakenProducer(converter,
                                                  eventsAggregator);
    }

    @Bean
    public CloudProcessDeployedProducer cloudProcessDeployedProducer(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                                                     ProcessEngineChannels processEngineChannels,
                                                                     RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory) {
        return new CloudProcessDeployedProducer(runtimeBundleInfoAppender,
                                                processEngineChannels,
                                                runtimeBundleMessageBuilderFactory);
    }

    @Bean
    public RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new RuntimeBundleMessageBuilderFactory(properties);
    }

}
