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

package org.activiti.cloud.services.audit.jpa.converters.json;

import java.io.IOException;
import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.process.model.BPMNSignal;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.services.audit.api.AuditException;

public class JpaJsonConverter<T> implements AttributeConverter<T, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        {
            SimpleModule module = new SimpleModule("mapCommonModelInterfaces",
                                                   Version.unknownVersion());
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

            resolver.addMapping(ProcessDefinition.class,
                                ProcessDefinitionImpl.class);

            resolver.addMapping(VariableInstance.class,
                                VariableInstanceImpl.class);
            resolver.addMapping(ProcessInstance.class,
                                ProcessInstanceImpl.class);

            resolver.addMapping(Task.class,
                                TaskImpl.class);
            resolver.addMapping(BPMNActivity.class,
                                BPMNActivityImpl.class);
            resolver.addMapping(BPMNSequenceFlow.class,
                                BPMNSequenceFlowImpl.class);
            resolver.addMapping(BPMNSignal.class,
            					BPMNSignalImpl.class);
            
            module.setAbstractTypes(resolver);

            objectMapper.registerModule(module);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }

    private Class<T> entityClass;

    public JpaJsonConverter(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public String convertToDatabaseColumn(T entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new AuditException("Unable to serialize object.",
                                     e);
        }
    }

    @Override
    public T convertToEntityAttribute(String entityTextRepresentation) {
        try {
            return objectMapper.readValue(entityTextRepresentation,
                                          entityClass);
        } catch (IOException e) {
            throw new AuditException("Unable to deserialize object.",
                                        e);
        }
    }
}