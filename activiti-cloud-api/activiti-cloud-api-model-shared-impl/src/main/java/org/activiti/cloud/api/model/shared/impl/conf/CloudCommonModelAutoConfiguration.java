/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.api.model.shared.impl.conf;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.CloudVariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleAbstractTypeResolver;
import tools.jackson.databind.module.SimpleModule;

@AutoConfiguration
public class CloudCommonModelAutoConfiguration {

    //this bean will be automatically injected inside boot's ObjectMapper
    @Bean
    public JacksonModule customizeCloudCommonModelObjectMapper() {
        SimpleModule module = new SimpleModule("mapMixCloudRuntimeEvents", Version.unknownVersion());

        module.registerSubtypes(
            new NamedType(CloudVariableCreatedEventImpl.class, VariableEvent.VariableEvents.VARIABLE_CREATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudVariableUpdatedEventImpl.class, VariableEvent.VariableEvents.VARIABLE_UPDATED.name())
        );
        module.registerSubtypes(
            new NamedType(CloudVariableDeletedEventImpl.class, VariableEvent.VariableEvents.VARIABLE_DELETED.name())
        );

        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver() {};

        resolver.addMapping(CloudVariableInstance.class, CloudVariableInstanceImpl.class);

        module.setAbstractTypes(resolver);

        module.setMixInAnnotation(CloudRuntimeEvent.class, CloudRuntimeMixIn.class);
        return module;
    }
}
