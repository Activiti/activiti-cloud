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
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateGroupAddedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCandidateUserAddedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCompletedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskAssignedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCancelledProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskCreatedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskSuspendedProducer;
import org.activiti.cloud.services.events.listeners.CloudTaskActivatedProducer;
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
                                                             ProcessEngineEventsAggregator eventsAggregator){
        return new CloudTaskCreatedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskCancelledProducer cloudTaskCancelledProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                          ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCancelledProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskAssignedProducer cloudTaskAssignedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                        ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskAssignedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskSuspendedProducer cloudTaskSuspendedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                  ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskSuspendedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskActivatedProducer cloutTaskActivatedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                 ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskActivatedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskCompletedProducer cloudTaskCompletedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCompletedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateUserAddedProducer cloudTaskCandidateUserAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                            ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateUserAddedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

    @Bean
    public CloudTaskCandidateGroupAddedProducer cloudTaskCandidateGroupAddedProducer(ToCloudTaskRuntimeEventConverter taskRuntimeEventConverter,
                                                                                     ProcessEngineEventsAggregator eventsAggregator) {
        return new CloudTaskCandidateGroupAddedProducer(taskRuntimeEventConverter, eventsAggregator);
    }

}
