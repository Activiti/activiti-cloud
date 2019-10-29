/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.DateFormatterProvider;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.activiti.spring.process.model.VariableDefinition;
import org.activiti.spring.process.variable.VariableValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessVariablesPayloadValidator.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
public class ProcessVariablesPayloadValidatorIT {

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @Autowired
    private VariableValidationService variableValidationService;

    @MockBean
    private RepositoryService repositoryService;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @MockBean
    private ProcessExtensionService processExtensionService;

    private ProcessVariablesPayloadValidator processVariablesValidator;

    @Before
    public void setUp() {
        ProcessExtensionModel processExtensionModel;

        VariableDefinition variableDefinitionName = new VariableDefinition();
        variableDefinitionName.setName("name");
        variableDefinitionName.setType("string");

        VariableDefinition variableDefinitionAge = new VariableDefinition();
        variableDefinitionAge.setName("age");
        variableDefinitionAge.setType("integer");

        VariableDefinition variableDefinitionSubscribe = new VariableDefinition();
        variableDefinitionSubscribe.setName("subscribe");
        variableDefinitionSubscribe.setType("boolean");

        VariableDefinition variableDefinitionDate = new VariableDefinition();
        variableDefinitionDate.setName("mydate");
        variableDefinitionDate.setType("date");

        Map<String, VariableDefinition> properties = new HashMap<>();
        properties.put("name", variableDefinitionName);
        properties.put("age", variableDefinitionAge);
        properties.put("subscribe", variableDefinitionSubscribe);
        properties.put("mydate", variableDefinitionDate);

        Extension extension = new Extension();
        extension.setProperties(properties);

        processExtensionModel = new ProcessExtensionModel();
        processExtensionModel.setId("1");
        processExtensionModel.setExtensions(extension);

        processVariablesValidator = new ProcessVariablesPayloadValidator(dateFormatterProvider,
                processExtensionService,
                variableValidationService);

        given(processExtensionService.getExtensionsForId(any()))
                .willReturn(processExtensionModel);
    }

    @Test
    public void shouldReturnErrorListWhenSetVariablesWithWrongType() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", "24");
        variables.put("subscribe", "false");

        //WHEN
        Throwable throwable = catchThrowable(() -> processVariablesValidator.checkPayloadVariables(
                ProcessPayloadBuilder
                        .setVariables()
                        .withVariables(variables)
                        .build(),
                "10"));

        //THEN
        assertThat(throwable).isInstanceOf(IllegalStateException.class);

        assertThat(throwable.getMessage())
                .contains("Boolean",
                        "Integer");
    }

    @Test
    public void shouldReturnErrorListWhenSetVariablesWithNameWrongType() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("gender", "female");
        variables.put("age", "24");
        variables.put("subs", true);
        variables.put("subscribe", true);
        variables.put("mydate", "2019-08-26T10:20:30.000Z");

        String expectedTypeErrorMessage = "class java.lang.String is not assignable from class java.lang.Integer";

        //WHEN
        //WHEN
        Throwable throwable = catchThrowable(() -> processVariablesValidator.checkPayloadVariables(
                ProcessPayloadBuilder
                        .setVariables()
                        .withVariables(variables)
                        .build(),
                "10"));

        //THEN
        assertThat(throwable).isInstanceOf(IllegalStateException.class);

        assertThat(throwable.getMessage())
                .contains(expectedTypeErrorMessage);

    }

    @Test
    public void shouldReturnErrorWhenSetVariablesWithWrongDateFormat() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("mydate", "2019-08-26TT10:20:30.000Z");

        //THEN
        Throwable throwable = catchThrowable(() -> processVariablesValidator.checkPayloadVariables(
                ProcessPayloadBuilder
                        .setVariables()
                        .withVariables(variables)
                        .build(),
                "10"));

        //THEN
        assertThat(throwable).isInstanceOf(IllegalStateException.class);

    }
}
