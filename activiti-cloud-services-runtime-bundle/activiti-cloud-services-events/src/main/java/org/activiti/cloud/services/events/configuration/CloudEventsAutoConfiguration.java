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
import org.activiti.cloud.services.events.listeners.CloudProcessResumedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessStartedProducer;
import org.activiti.cloud.services.events.listeners.CloudProcessSuspendedProducer;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudEventsAutoConfiguration {

    @Bean
    public RuntimeBundleInfoAppender runtimeBundleInfoAppender(RuntimeBundleProperties properties) {
        return new RuntimeBundleInfoAppender(properties);
    }

    @Bean
    public ToCloudProcessRuntimeEventConverter toCloudProcessRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudProcessRuntimeEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    public ToCloudTaskRuntimeEventConverter toCloudTaskRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudTaskRuntimeEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    public MessageProducerCommandContextCloseListener apiMessageProducerCommandContextCloseListener(ProcessEngineChannels processEngineChannels) {
        return new MessageProducerCommandContextCloseListener(processEngineChannels);
    }

    @Bean
    public ProcessEngineEventsAggregator apiProcessEngineEventsAggregator(MessageProducerCommandContextCloseListener closeListener) {
        return new ProcessEngineEventsAggregator(closeListener);
    }

    @Bean
    public CloudTaskCreatedProducer cloudTaskCreatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                             ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCreatedProducer(taskRuntimeEventConverter,
                                            eventsAggregator);
    }

    @Bean
    public CloudTaskUpdatedProducer cloudTaskUpdatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                             ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskUpdatedProducer(taskRuntimeEventConverter,
                                            eventsAggregator);
    }

    @Bean
    public CloudTaskCancelledProducer cloudTaskCancelledProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCancelledProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    public CloudTaskAssignedProducer cloudTaskAssignedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                               ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskAssignedProducer(taskRuntimeEventConverter,
                                             eventsAggregator);
    }

    @Bean
    public CloudTaskSuspendedProducer cloudTaskSuspendedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskSuspendedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    public CloudTaskActivatedProducer cloutTaskActivatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskActivatedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    public CloudTaskCompletedProducer cloudTaskCompletedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCompletedProducer(taskRuntimeEventConverter,
                                              eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateUserAddedProducer cloudTaskCandidateUserAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                                   ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateUserAddedProducer(taskRuntimeEventConverter,
                                                       eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateUserRemovedProducer taskCandidateUserRemovedProducer(ToCloudTaskRuntimeEventConverter converter,
                                                                                  ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateUserRemovedProducer(converter,
                                                         eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateGroupAddedProducer cloudTaskCandidateGroupAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateGroupAddedProducer(taskRuntimeEventConverter,
                                                        eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateGroupRemovedProducer cloudTaskCandidateGroupRemovedProducer(ToCloudTaskRuntimeEventConverter converter,
                                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateGroupRemovedProducer(converter,
                                                          eventsAggregator);
    }

    @Bean
    public CloudProcessCreatedProducer processCreatedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                              ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCreatedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    public CloudProcessStartedProducer processStartedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                              ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessStartedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    public CloudProcessSuspendedProducer cloudProcessSuspendedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessSuspendedProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    public CloudProcessResumedProducer cloudProcessResumedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                   ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessResumedProducer(eventConverter,
                                               eventsAggregator);
    }

    @Bean
    public CloudProcessCompletedProducer cloudProcessCompletedProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCompletedProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    public CloudProcessCancelledProducer cloudProcessCancelledProducer(ToCloudProcessRuntimeEventConverter eventConverter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudProcessCancelledProducer(eventConverter,
                                                 eventsAggregator);
    }

    @Bean
    public ToCloudVariableEventConverter cloudVariableEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudVariableEventConverter(runtimeBundleInfoAppender);
    }

    @Bean
    public CloudVariableCreatedProducer cloudVariableCreatedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableCreatedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudVariableUpdatedProducer cloudVariableUpdatedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableUpdatedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudVariableDeletedProducer cloudVariableDeletedProducer(ToCloudVariableEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudVariableDeletedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudActivityStartedProducer cloudActivityStartedProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityStartedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudActivityCompletedProducer cloudActivityCompletedProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                       ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityCompletedProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudActivityCancelledProducer cloudActivityCancelledProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudActivityCancelledProducer(converter,
                                                eventsAggregator);
    }

    @Bean
    public CloudSequenceFlowTakenProducer cloudSequenceFlowTakenProducer(ToCloudProcessRuntimeEventConverter converter,
                                                                         ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudSequenceFlowTakenProducer(converter, eventsAggregator);
    }

}
