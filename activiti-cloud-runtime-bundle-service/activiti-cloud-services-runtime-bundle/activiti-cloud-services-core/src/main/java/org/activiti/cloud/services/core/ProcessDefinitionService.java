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
package org.activiti.cloud.services.core;

import java.util.List;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;

public class ProcessDefinitionService extends BaseProcessDefinitionService {

    private final ProcessRuntime processRuntime;

    public ProcessDefinitionService(
        ProcessRuntime processRuntime,
        List<ProcessDefinitionDecorator> processDefinitionDecorators
    ) {
        super(processDefinitionDecorators);
        this.processRuntime = processRuntime;
    }

    public Page<ProcessDefinition> getProcessDefinitions(Pageable pageable, List<String> include) {
        Page<ProcessDefinition> processDefinitions = processRuntime.processDefinitions(pageable);
        processDefinitions.getContent().replaceAll(processDefinition -> super.decorateAll(processDefinition, include));
        return processDefinitions;
    }
}
