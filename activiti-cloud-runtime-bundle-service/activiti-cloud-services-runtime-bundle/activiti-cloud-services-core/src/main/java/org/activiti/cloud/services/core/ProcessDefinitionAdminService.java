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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;
import org.activiti.runtime.api.query.impl.PageImpl;

public class ProcessDefinitionAdminService extends BaseProcessDefinitionService {

    private final ProcessAdminRuntime processAdminRuntime;

    public ProcessDefinitionAdminService(
        ProcessAdminRuntime processAdminRuntime,
        List<ProcessDefinitionDecorator> processDefinitionDecorators
    ) {
        super(processDefinitionDecorators);
        this.processAdminRuntime = processAdminRuntime;
    }
    public Page<ProcessDefinition> getProcessDefinitions(Pageable pageable, List<String> include, boolean versions) {
        Page<ProcessDefinition> processDefinitions;
        if (!versions) {
            processDefinitions =
                processAdminRuntime.processDefinitionsLatestVersions(pageable);
        } else {
            processDefinitions =
                processAdminRuntime.processDefinitions(pageable);
        }
        processDefinitions.getContent().replaceAll(processDefinition -> super.decorateAll(processDefinition, include));
        return processDefinitions;
    }

    @Override
    public Page<ProcessDefinition> getProcessDefinitions(Pageable pageable, List<String> include) {
        return null;
    }
}
