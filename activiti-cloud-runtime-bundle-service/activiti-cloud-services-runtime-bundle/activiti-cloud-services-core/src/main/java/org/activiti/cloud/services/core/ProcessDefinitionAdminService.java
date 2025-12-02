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
import java.util.Map;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;
import org.activiti.runtime.api.query.impl.PageImpl;

public class ProcessDefinitionAdminService extends BaseProcessDefinitionService {

    private final ProcessAdminRuntime processAdminRuntime;

    private final ProcessDefinitionValuesService processDefinitionValuesService;

    public ProcessDefinitionAdminService(
        ProcessAdminRuntime processAdminRuntime,
        List<ProcessDefinitionDecorator> processDefinitionDecorators,
        ProcessDefinitionValuesService processDefinitionValuesService
    ) {
        super(processDefinitionDecorators);
        this.processAdminRuntime = processAdminRuntime;
        this.processDefinitionValuesService = processDefinitionValuesService;
    }

    public Page<ProcessDefinition> getProcessDefinitions(
        Pageable pageable,
        List<String> include,
        String excludedConstant,
        boolean latestVersion
    ) {
        GetProcessDefinitionsPayload processDefinitionsPayload = buildGetProcessDefinitionsPayloadWithLatestVersion(
            latestVersion
        );
        Page<ProcessDefinition> processDefinitions = processAdminRuntime.processDefinitions(
            pageable,
            processDefinitionsPayload
        );

        List<ProcessDefinition> decorated = processDefinitions
            .getContent()
            .stream()
            .map(def -> super.decorateAll(def, include))
            .collect(Collectors.toList());

        if (validateInput(excludedConstant)) {
            decorated =
                decorated
                    .stream()
                    .filter(def ->
                        shouldIncludeDefinition((ExtendedCloudProcessDefinition) def, excludedConstant, include)
                    )
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(decorated, processDefinitions.getTotalItems());
    }

    private boolean shouldIncludeDefinition(
        ExtendedCloudProcessDefinition definition,
        String excludedConstant,
        List<String> include
    ) {
        Map<String, Object> constants = null;
        boolean constantsRequested = include != null && include.stream().anyMatch("constant-values"::equalsIgnoreCase);

        if (constantsRequested) {
            constants = definition.getConstantValues();
        } else if (processDefinitionValuesService != null) {
            constants = processDefinitionValuesService.getProcessModelConstantValuesForStartEvent(definition.getId());
        }

        return constants == null || !constants.containsKey(excludedConstant);
    }

    @Override
    public Page<ProcessDefinition> getProcessDefinitions(
        Pageable pageable,
        List<String> include,
        String excludedConstant
    ) {
        return getProcessDefinitions(pageable, include, excludedConstant, false);
    }
}
