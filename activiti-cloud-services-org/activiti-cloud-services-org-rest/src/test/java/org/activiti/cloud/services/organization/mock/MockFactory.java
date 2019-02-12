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

package org.activiti.cloud.services.organization.mock;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.activiti.cloud.organization.api.Extensions;
import org.activiti.cloud.organization.api.ProcessVariable;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.entity.ProjectEntity;

import static java.util.Collections.singletonMap;
import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.organization.api.VariableMappingType.INPUTS;
import static org.activiti.cloud.organization.api.VariableMappingType.OUTPUTS;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;

/**
 * Mocks factory
 */
public class MockFactory {

    public static ProjectEntity project(String name) {
        return new ProjectEntity(name);
    }

    public static ModelEntity processModel(String name) {
        return processModelWithExtensions(name,
                                          null);
    }

    public static ModelEntity processModel(ProjectEntity parentProject,
                                           String name) {
        ModelEntity model = processModel(name);
        model.setProject(parentProject);
        return model;
    }

    public static ModelEntity processModelWithExtensions(String name,
                                                         Extensions extensions) {
        return processModelWithExtensions(null,
                                          name,
                                          extensions);
    }

    public static ModelEntity processModelWithExtensions(ProjectEntity parentProject,
                                                         String name,
                                                         Extensions extensions) {
        ModelEntity model = new ModelEntity(name,
                                            PROCESS);
        model.setProject(parentProject);
        model.setExtensions(extensions);
        return model;
    }

    public static ModelEntity processModelWithContent(String name,
                                                      String content) {
        return processModelWithContent(null,
                                       name,
                                       content);
    }

    public static ModelEntity processModelWithContent(ProjectEntity project,
                                                      String name,
                                                      byte[] content) {
        return processModelWithContent(project,
                                       name,
                                       new String(content));
    }

    public static ModelEntity processModelWithContent(ProjectEntity project,
                                                      String name,
                                                      String content) {
        return processModelWithContent(project,
                                       name,
                                       null,
                                       content);
    }

    public static ModelEntity processModelWithContent(ProjectEntity project,
                                                      String name,
                                                      Extensions extensions,
                                                      byte[] content) {
        return processModelWithContent(project,
                                       name,
                                       extensions,
                                       new String(content));
    }

    public static ModelEntity processModelWithContent(ProjectEntity project,
                                                      String name,
                                                      Extensions extensions,
                                                      String content) {
        ModelEntity processModel = processModel(name);
        processModel.setProject(project);
        processModel.setExtensions(extensions);
        processModel.setContentType(CONTENT_TYPE_XML);
        processModel.setContent(content);
        return processModel;
    }

    public static Extensions extensions(String... processVariables) {
        Extensions extensions = new Extensions();
        extensions.setProcessVariables(
                Arrays.stream(processVariables)
                        .map(MockFactory::processVariable)
                        .collect(Collectors.toMap(ProcessVariable::getId,
                                                  Function.identity())));
        Map<String, String> autoMapping = Arrays.stream(processVariables)
                .collect(Collectors.toMap(Function.identity(),
                                          Function.identity()));
        extensions.setVariablesMappings(
                singletonMap("ServiceTask",
                             ImmutableMap.of(INPUTS,
                                             autoMapping,
                                             OUTPUTS,
                                             autoMapping))
        );
        return extensions;
    }

    public static ProcessVariable processVariable(String name) {
        ProcessVariable processVariable = new ProcessVariable();
        processVariable.setId(name);
        processVariable.setName(name);
        processVariable.setType("string");
        processVariable.setValue(name);
        return processVariable;
    }

    public static String id() {
        return UUID.randomUUID().toString();
    }
}
