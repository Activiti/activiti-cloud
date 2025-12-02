/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import org.activiti.api.process.model.builders.GetProcessDefinitionsPayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseProcessDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseProcessDefinitionService.class);

    private final List<ProcessDefinitionDecorator> processDefinitionDecorators;

    public BaseProcessDefinitionService(List<ProcessDefinitionDecorator> processDefinitionDecorators) {
        this.processDefinitionDecorators = processDefinitionDecorators;
    }

    public Page<ProcessDefinition> getProcessDefinitions(Pageable pageable, List<String> include) {
        return getProcessDefinitions(pageable, include, null);
    }

    public abstract Page<ProcessDefinition> getProcessDefinitions(
        Pageable pageable,
        List<String> include,
        String excludedCategory
    );

    protected ExtendedCloudProcessDefinition decorateAll(ProcessDefinition processDefinition, List<String> include) {
        ExtendedCloudProcessDefinition decoratedProcessDefinition = new CloudProcessDefinitionImpl(processDefinition);
        for (String param : include) {
            decoratedProcessDefinition = decorate(decoratedProcessDefinition, param);
        }
        return decoratedProcessDefinition;
    }

    protected ExtendedCloudProcessDefinition decorate(
        ExtendedCloudProcessDefinition processDefinition,
        String includeParam
    ) {
        return processDefinitionDecorators
            .stream()
            .filter(decorator -> decorator.applies(includeParam))
            .findFirst()
            .map(decorator -> decorator.decorate(processDefinition))
            .orElse(processDefinition);
    }

    protected GetProcessDefinitionsPayload buildGetProcessDefinitionsPayload(String excludedConstant) {
        var processDefinitionsPayloadBuilder = getGetProcessDefinitionsPayloadBuilder(excludedConstant);
        return processDefinitionsPayloadBuilder.build();
    }

    private GetProcessDefinitionsPayloadBuilder getGetProcessDefinitionsPayloadBuilder(String excludedConstant) {
        var processDefinitionsPayloadBuilder = ProcessPayloadBuilder.processDefinitions();
        if (validateInput(excludedConstant)) {
            LOGGER.debug("Excluding process definitions with constant: {}", excludedConstant);

            processDefinitionsPayloadBuilder.withProcessCategoryToExclude(excludedConstant);
        }
        return processDefinitionsPayloadBuilder;
    }

    protected GetProcessDefinitionsPayload buildGetProcessDefinitionsPayloadWithLatestVersion(boolean latestVersion) {
        return ProcessPayloadBuilder.processDefinitions().withLatestVersionOnly(latestVersion).build();
    }

    protected boolean validateInput(String excludedConstant) {
        return StringUtils.isNotEmpty(excludedConstant) && excludedConstant.matches("[a-zA-Z0-9_\\-#.]+");
    }
}
