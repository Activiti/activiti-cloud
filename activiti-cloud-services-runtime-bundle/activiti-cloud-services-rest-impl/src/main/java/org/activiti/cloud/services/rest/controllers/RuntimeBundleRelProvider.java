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

package org.activiti.cloud.services.rest.controllers;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.api.model.shared.impl.CloudVariableInstanceImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.RelProvider;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RuntimeBundleRelProvider implements RelProvider {

    private Map<Class<?>, ResourceRelationDescriptor> resourceRelationDescriptors;

    public RuntimeBundleRelProvider() {
        resourceRelationDescriptors = new HashMap<>();
        resourceRelationDescriptors.put(CloudProcessDefinitionImpl.class,
                                        new ResourceRelationDescriptor("processDefinition",
                                                                       "processDefinitions"));
        resourceRelationDescriptors.put(CloudProcessInstanceImpl.class,
                                        new ResourceRelationDescriptor("processInstance",
                                                                       "processInstances"));
        resourceRelationDescriptors.put(CloudTaskImpl.class,
                                        new ResourceRelationDescriptor("task",
                                                                       "tasks"));
        
        resourceRelationDescriptors.put(CloudVariableInstanceImpl.class,
                                        new ResourceRelationDescriptor("variable",
                                                                       "variables"));
    }

    @Override
    public String getItemResourceRelFor(Class<?> aClass) {
        return resourceRelationDescriptors.get(aClass).getItemResourceRel();
    }

    @Override
    public String getCollectionResourceRelFor(Class<?> aClass) {
        return resourceRelationDescriptors.get(aClass).getCollectionResourceRel();
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return resourceRelationDescriptors.keySet().contains(aClass);
    }

    class ResourceRelationDescriptor {

        private String itemResourceRel;

        private String collectionResourceRel;

        public ResourceRelationDescriptor(String itemResourceRel,
                                          String collectionResourceRel) {
            this.itemResourceRel = itemResourceRel;
            this.collectionResourceRel = collectionResourceRel;
        }

        public String getItemResourceRel() {
            return itemResourceRel;
        }

        public String getCollectionResourceRel() {
            return collectionResourceRel;
        }
    }
}
