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
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;

public class ToCloudProcessDefinitionConverter {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudProcessDefinitionConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public ExtendedCloudProcessDefinition from(ProcessDefinition processDefinition) {
        CloudProcessDefinitionImpl cloudProcessDefinition = new CloudProcessDefinitionImpl(processDefinition);
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessDefinition);
        if (processDefinition instanceof ExtendedCloudProcessDefinition extendedCloudProcessDefinition) {
            cloudProcessDefinition.setVariableDefinitions(extendedCloudProcessDefinition.getVariableDefinitions());
            cloudProcessDefinition.setConstantValues(extendedCloudProcessDefinition.getConstantValues());
        }
        return cloudProcessDefinition;
    }
}
