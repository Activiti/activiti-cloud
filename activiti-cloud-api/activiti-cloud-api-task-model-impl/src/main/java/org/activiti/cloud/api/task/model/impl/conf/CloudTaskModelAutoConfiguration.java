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
package org.activiti.cloud.api.task.model.impl.conf;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.activiti.cloud.api.task.model.impl.QueryCloudTaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskActivatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskSuspendedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudTaskModelAutoConfiguration {

    //this bean will be automatically injected inside boot's ObjectMapper
    @Bean
    public Module customizeCloudTaskModelObjectMapper() {
        SimpleModule module = new SimpleModule("mapCloudTaskModelInterfaces", Version.unknownVersion());

        module.registerSubtypes(
            new NamedType(CloudTaskCreatedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_CREATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskUpdatedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskAssignedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskCompletedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskSuspendedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskActivatedEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudTaskCancelledEventImpl.class, TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name())
        );

        module.registerSubtypes(
            new NamedType(
                CloudTaskCandidateUserAddedEventImpl.class,
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudTaskCandidateUserRemovedEventImpl.class,
                TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name()
            )
        );

        module.registerSubtypes(
            new NamedType(
                CloudTaskCandidateGroupAddedEventImpl.class,
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name()
            )
        );
        module.registerSubtypes(
            new NamedType(
                CloudTaskCandidateGroupRemovedEventImpl.class,
                TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name()
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

        resolver.addMapping(CloudTask.class, CloudTaskImpl.class);
        resolver.addMapping(QueryCloudTask.class, QueryCloudTaskImpl.class);

        module.setAbstractTypes(resolver);

        return module;
    }
}
