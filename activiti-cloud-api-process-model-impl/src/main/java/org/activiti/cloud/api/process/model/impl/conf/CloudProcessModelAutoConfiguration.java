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

package org.activiti.cloud.api.process.model.impl.conf;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudProcessModelAutoConfiguration {

    //this bean will be automatically injected inside boot's ObjectMapper
    @Bean
    public Module customizeCloudProcessModelObjectMapper() {
        SimpleModule module = new SimpleModule("mapProcessRuntimeEvents",
                                               Version.unknownVersion());
        module.registerSubtypes(new NamedType(CloudBPMNActivityStartedEventImpl.class,
                                              BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name()));
        module.registerSubtypes(new NamedType(CloudBPMNActivityCompletedEventImpl.class,
                                              BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()));
        module.registerSubtypes(new NamedType(CloudBPMNActivityCancelledEventImpl.class,
                                              BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name()));
        module.registerSubtypes(new NamedType(CloudProcessDeployedEventImpl.class,
                                              ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name()));
        module.registerSubtypes(new NamedType(CloudProcessStartedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name()));
        module.registerSubtypes(new NamedType(CloudProcessCreatedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name()));
        module.registerSubtypes(new NamedType(CloudProcessUpdatedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name()));
        module.registerSubtypes(new NamedType(CloudProcessCompletedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name()));
        module.registerSubtypes(new NamedType(CloudProcessSuspendedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name()));
        module.registerSubtypes(new NamedType(CloudProcessResumedEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name()));
        module.registerSubtypes(new NamedType(CloudProcessCancelledEventImpl.class,
                                              ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name()));
        module.registerSubtypes(new NamedType(CloudSequenceFlowTakenImpl.class,
                                              SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name()));

        module.registerSubtypes(new NamedType(CloudIntegrationRequestedImpl.class,
                                              IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED.name()));
        module.registerSubtypes(new NamedType(CloudIntegrationResultReceivedImpl.class,
                                              IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED.name()));

        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver() {
            //this is a workaround for https://github.com/FasterXML/jackson-databind/issues/2019
            //once version 2.9.6 is related we can remove this @override method
            @Override
            public JavaType resolveAbstractType(DeserializationConfig config,
                                                BeanDescription typeDesc) {
                return findTypeMapping(config,
                                       typeDesc.getType());
            }
        };

        resolver.addMapping(IntegrationRequest.class, IntegrationRequestImpl.class);
        resolver.addMapping(IntegrationResult.class, IntegrationResultImpl.class);

        resolver.addMapping(CloudProcessDefinition.class,
                            CloudProcessDefinitionImpl.class);
        resolver.addMapping(CloudProcessInstance.class,
                            CloudProcessInstanceImpl.class);

        module.setAbstractTypes(resolver);

        return module;
    }

}
