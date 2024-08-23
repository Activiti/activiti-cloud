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
package org.activiti.cloud.api.process.model.impl.conf;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.activiti.api.process.model.Deployment;
import org.activiti.api.process.model.events.ApplicationEvent;
import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.api.process.model.events.MessageDefinitionEvent;
import org.activiti.api.process.model.events.MessageSubscriptionEvent;
import org.activiti.api.process.model.events.ProcessCandidateStarterGroupEvent;
import org.activiti.api.process.model.events.ProcessCandidateStarterUserEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.api.process.model.CloudApplication;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.activiti.cloud.api.process.model.CloudStartMessageDeploymentDefinition;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.CloudApplicationImpl;
import org.activiti.cloud.api.process.model.impl.CloudBPMNActivityImpl;
import org.activiti.cloud.api.process.model.impl.CloudIntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.CloudServiceTaskImpl;
import org.activiti.cloud.api.process.model.impl.CloudStartMessageDeploymentDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageSentEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageWaitingEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerExecutedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFailedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerRetriesDecrementedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudStartMessageDeployedEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.function.json.JacksonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration
public class CloudProcessModelAutoConfiguration {

    //this bean will be automatically injected inside boot's ObjectMapper
    @Bean
    public Module customizeCloudProcessModelObjectMapper() {
        SimpleModule module = new SimpleModule("mapProcessRuntimeEvents", Version.unknownVersion());
        module.registerSubtypes(
            new NamedType(
                CloudBPMNActivityStartedEventImpl.class,
                BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudBPMNActivityCompletedEventImpl.class,
                BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudBPMNActivityCancelledEventImpl.class,
                BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNSignalReceivedEventImpl.class, BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name())
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessDeployedEventImpl.class,
                ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudStartMessageDeployedEventImpl.class,
                MessageDefinitionEvent.MessageDefinitionEvents.START_MESSAGE_DEPLOYED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudProcessStartedEventImpl.class, ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudProcessCreatedEventImpl.class, ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudProcessUpdatedEventImpl.class, ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudProcessDeletedEventImpl.class, ProcessRuntimeEvent.ProcessEvents.PROCESS_DELETED.name())
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessCompletedEventImpl.class,
                ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessSuspendedEventImpl.class,
                ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudProcessResumedEventImpl.class, ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name())
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessCancelledEventImpl.class,
                ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudSequenceFlowTakenEventImpl.class,
                SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name()
            )
        );

        module.registerSubtypes(
            new NamedType(
                CloudIntegrationRequestedEventImpl.class,
                IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudIntegrationResultReceivedEventImpl.class,
                IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudIntegrationErrorReceivedEventImpl.class,
                IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED.name()
            )
        );

        module.registerSubtypes(
            new NamedType(CloudBPMNTimerFiredEventImpl.class, BPMNTimerEvent.TimerEvents.TIMER_FIRED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNTimerScheduledEventImpl.class, BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNTimerExecutedEventImpl.class, BPMNTimerEvent.TimerEvents.TIMER_EXECUTED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNTimerFailedEventImpl.class, BPMNTimerEvent.TimerEvents.TIMER_FAILED.name())
        );
        module.registerSubtypes(
            new NamedType(
                CloudBPMNTimerRetriesDecrementedEventImpl.class,
                BPMNTimerEvent.TimerEvents.TIMER_RETRIES_DECREMENTED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNTimerCancelledEventImpl.class, BPMNTimerEvent.TimerEvents.TIMER_CANCELLED.name())
        );

        module.registerSubtypes(
            new NamedType(
                CloudBPMNMessageReceivedEventImpl.class,
                BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNMessageSentEventImpl.class, BPMNMessageEvent.MessageEvents.MESSAGE_SENT.name())
        );
        module.registerSubtypes(
            new NamedType(CloudBPMNMessageWaitingEventImpl.class, BPMNMessageEvent.MessageEvents.MESSAGE_WAITING.name())
        );

        module.registerSubtypes(
            new NamedType(
                CloudBPMNErrorReceivedEventImpl.class,
                BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED.name()
            )
        );

        module.registerSubtypes(
            new NamedType(
                CloudMessageSubscriptionCancelledEventImpl.class,
                MessageSubscriptionEvent.MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudApplicationDeployedEventImpl.class,
                ApplicationEvent.ApplicationEvents.APPLICATION_DEPLOYED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(CloudApplicationDeployedEventImpl.class, ApplicationEvents.APPLICATION_ROLLBACK.name())
        );

        module.registerSubtypes(
            new NamedType(
                CloudProcessCandidateStarterUserAddedEventImpl.class,
                ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_ADDED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessCandidateStarterUserRemovedEventImpl.class,
                ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_REMOVED.name()
            )
        );

        module.registerSubtypes(
            new NamedType(
                CloudProcessCandidateStarterGroupAddedEventImpl.class,
                ProcessCandidateStarterGroupEvent.ProcessCandidateStarterGroupEvents.PROCESS_CANDIDATE_STARTER_GROUP_ADDED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudProcessCandidateStarterGroupRemovedEventImpl.class,
                ProcessCandidateStarterGroupEvent.ProcessCandidateStarterGroupEvents.PROCESS_CANDIDATE_STARTER_GROUP_REMOVED.name()
            )
        );

        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver() {
            //this is a workaround for https://github.com/FasterXML/jackson-databind/issues/2019
            //once version 2.9.6 is related we can remove this @override method
            @Override
            public JavaType resolveAbstractType(DeserializationConfig config, BeanDescription typeDesc) {
                return findTypeMapping(config, typeDesc.getType());
            }
        };

        resolver.addMapping(IntegrationRequest.class, IntegrationRequestImpl.class);
        resolver.addMapping(IntegrationResult.class, IntegrationResultImpl.class);
        resolver.addMapping(IntegrationError.class, IntegrationErrorImpl.class);

        resolver.addMapping(CloudProcessDefinition.class, CloudProcessDefinitionImpl.class);
        resolver.addMapping(
            CloudStartMessageDeploymentDefinition.class,
            CloudStartMessageDeploymentDefinitionImpl.class
        );
        resolver.addMapping(CloudProcessInstance.class, CloudProcessInstanceImpl.class);
        resolver.addMapping(CloudBPMNActivity.class, CloudBPMNActivityImpl.class);
        resolver.addMapping(CloudIntegrationContext.class, CloudIntegrationContextImpl.class);
        resolver.addMapping(CloudServiceTask.class, CloudServiceTaskImpl.class);
        resolver.addMapping(Deployment.class, DeploymentImpl.class);
        resolver.addMapping(CloudApplication.class, CloudApplicationImpl.class);

        module.setAbstractTypes(resolver);

        return module;
    }

    @Bean
    @Primary
    public JacksonMapper jacksonMapper(@Autowired(required = false) final ObjectMapper objectMapper) {
        //temporary workaround for https://github.com/spring-cloud/spring-cloud-function/issues/1159
        if (objectMapper == null) {
            return new JacksonMapper(new ObjectMapper());
        }
        return new JacksonMapper(objectMapper);
    }
}
