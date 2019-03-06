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

package org.activiti.cloud.qa.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.form.FormData;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.organization.api.Extensions;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ProcessVariableMapping;
import org.activiti.cloud.organization.api.ServiceTaskActionType;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.EnableModelingContext;
import org.activiti.cloud.qa.service.ModelingModelsService;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.organization.api.ServiceTaskActionType.INPUTS;
import static org.activiti.cloud.organization.api.ServiceTaskActionType.OUTPUTS;
import static org.activiti.cloud.organization.api.VariableMappingType.VALUE;
import static org.activiti.cloud.organization.api.VariableMappingType.VARIABLE;
import static org.activiti.cloud.qa.model.modeling.ProcessExtensions.EXTENSIONS_TASK_NAME;
import static org.activiti.cloud.qa.model.modeling.ProcessExtensions.HOST_VALUE;
import static org.activiti.cloud.qa.model.modeling.ProcessExtensions.extensions;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling steps for models
 */
@EnableModelingContext
public class ModelingModelsSteps extends ModelingContextSteps<Model> {

    private static final String PROJECT_MODELS_REL = "models";

    @Autowired
    private ModelingModelsService modelingModelsService;
    @Autowired
    private ModelingTestsConfigurationProperties config;

    @Step
    public Resource<Model> create(String modelName,
                                  String modelType,
                                  List<String> processVariables) {
        Model model = mock(Model.class);
        doReturn(modelType.toUpperCase()).when(model).getType();
        doReturn(modelName).when(model).getName();
        if (processVariables != null) {
            doReturn(extensions(processVariables)).when(model).getExtensions();
        }
        return create(model);
    }

    @Step
    public void removeProcessVariableInCurrentModel(String processVariable) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        Model model = currentContext.getContent();

        Optional.ofNullable(model.getExtensions())
                .map(Extensions::getProcessVariables)
                .ifPresent(processVariables -> processVariables.remove(processVariable));

        Optional.ofNullable(model.getExtensions())
                .map(Extensions::getVariablesMappings)
                .map(mappings -> mappings.get(EXTENSIONS_TASK_NAME))
                .map(mappingsTypes -> mappingsTypes.get(INPUTS))
                .ifPresent(processVariableMappings -> processVariableMappings.remove(processVariable));

        Optional.ofNullable(model.getExtensions())
                .map(Extensions::getVariablesMappings)
                .map(mappings -> mappings.get(EXTENSIONS_TASK_NAME))
                .map(mappingsTypes -> mappingsTypes.get(OUTPUTS))
                .ifPresent(processVariableMappings -> processVariableMappings.remove(processVariable));
    }

    @Step
    public void addProcessVariableInCurrentModel(List<String> processVariable) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        Model model = currentContext.getContent();

        addProcessVariableToModelModel(model,
                                       processVariable);
    }

    @Step
    public void addProcessVariableToModelModel(Model model,
                                               List<String> processVariable) {
        Set<String> processVariables = Optional.ofNullable(model.getExtensions())
                .map(Extensions::getProcessVariables)
                .map(Map::keySet)
                .map(HashSet::new)
                .orElseGet(HashSet::new);
        processVariables.addAll(processVariable);

        model.setExtensions(extensions(processVariables));
    }

    @Step
    public void saveCurrentModel(boolean updateContent) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        if (updateContent) {
            currentContext.getContent().setContent("updated content");
        }

        saveModel(currentContext);
        updateCurrentModelingObject();
    }

    @Step
    public void saveModel(Resource<Model> model) {
        modelingModelsService.updateByUri(modelingUri(model.getLink(REL_SELF).getHref()),
                                          model.getContent());
    }

    private List<Response> validateCurrentModel() throws IOException {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        final Model model = currentContext.getContent();
        List<Response> responses = new ArrayList<>();
        responses.add(validateModel(currentContext,
                                    getFormData(model)));
        if (Optional.ofNullable(model.getExtensions()).isPresent()) {
            responses.add(validateModel(currentContext,
                                        getFormData(model,
                                                    true)));
        }
        return responses;
    }

    private FormData getFormData(Model model,
                                 boolean isExtensionType) throws IOException {
        final String fileExtension = isExtensionType ? CONTENT_TYPE_JSON : getModelType(model.getType()).getContentFileExtension();
        final String fileName = isExtensionType ? toJsonFilename(model.getName() + getModelType(model.getType()).getMetadataFileSuffix()) : setExtension(model.getName(),
                                                                                                                                                         fileExtension);
        return new FormData(fileExtension,
                            fileName,
                            resourceAsByteArray(model.getType().toLowerCase() + "/" + fileName));
    }

    private FormData getFormData(Model model) throws IOException {
        return getFormData(model,
                           false);
    }

    @Step
    public void checkCurrentModelValidation() throws IOException {
        assertThat(validateCurrentModel())
                .extracting(Response::status)
                .containsOnly(HttpStatus.SC_NO_CONTENT);
    }

    @Step
    public void checkCurrentModelValidationFailureForExtensions(String errorMessage) throws IOException {
        assertThat(validateCurrentModel()).extracting(Response::status)
                .contains(HttpStatus.SC_BAD_REQUEST);
        validateCurrentModel().stream()
                .filter(response -> response.status() == HttpStatus.SC_BAD_REQUEST)
                .map(this::convertResponseBodyAsJsonNode)
                .map(node -> node.get("message").asText())
                .forEach(message -> assertThat(message).contains(errorMessage));
    }

    private JsonNode convertResponseBodyAsJsonNode(Response response) {
        try (InputStream in = response.body().asInputStream()) {
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse response body as json",
                                       e);
        }
    }

    @Step
    public Response validateModel(Resource<Model> model,
                                  FormData file) {
        Link validateModelLink = model.getLink("self");
        assertThat(validateModelLink).isNotNull();
        return modelingModelsService.validateModelByUri(modelingUri(validateModelLink.getHref() + "/validate"),
                                                        file);
    }

    @Step
    public void checkCurrentModelVersion(String expectedModelVersion) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        Model model = currentContext.getContent();
        assertThat(model.getVersion()).isEqualTo(expectedModelVersion);
    }

    @Step
    public void checkCurrentModelContainsVariables(String... processVariables) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        Model model = currentContext.getContent();
        assertThat(model.getExtensions()).isNotNull();
        assertThat(model.getExtensions().getProcessVariables()).containsKeys(processVariables);
        assertThat(model.getExtensions().getVariablesMappings()).containsKeys(EXTENSIONS_TASK_NAME);
        assertThat(model.getExtensions().getVariablesMappings().get(EXTENSIONS_TASK_NAME)).containsKeys(INPUTS,
                                                                                                        OUTPUTS);
        assertProcessVariableMappings(model,
                                      INPUTS,
                                      processVariables);

        assertProcessVariableMappings(model,
                                      OUTPUTS,
                                      processVariables);
    }

    private void assertProcessVariableMappings(Model model,
                                               ServiceTaskActionType serviceTaskActionType,
                                               String... processVariables) {
        Map<String, ProcessVariableMapping> outputsMappings = model
                .getExtensions()
                .getVariablesMappings()
                .get(EXTENSIONS_TASK_NAME)
                .get(serviceTaskActionType);

        assertThat(outputsMappings).containsKeys(processVariables);
        Arrays.stream(processVariables).forEach(
                processVariable -> assertThat(Optional.ofNullable(outputsMappings.get(processVariable)))
                        .hasValueSatisfying(processVariableMapping -> {
                            assertThat(processVariableMapping.getType())
                                    .isEqualTo(serviceTaskActionType == INPUTS ? VALUE : VARIABLE);
                            assertThat(processVariableMapping.getValue())
                                    .isEqualTo(serviceTaskActionType == INPUTS ? processVariable : HOST_VALUE);
                        })
        );
    }

    @Override
    protected Optional<String> getRel() {
        return Optional.of(PROJECT_MODELS_REL);
    }

    @Override
    public ModelingModelsService service() {
        return modelingModelsService;
    }
}
