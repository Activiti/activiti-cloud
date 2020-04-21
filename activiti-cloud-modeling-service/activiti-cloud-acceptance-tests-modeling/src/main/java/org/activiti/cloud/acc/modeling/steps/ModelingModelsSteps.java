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

package org.activiti.cloud.acc.modeling.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.form.FormData;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.modeling.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.acc.modeling.modeling.EnableModelingContext;
import org.activiti.cloud.acc.modeling.service.ModelingModelsService;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.api.process.ProcessVariable;
import org.activiti.cloud.modeling.api.process.ProcessVariableMapping;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;

import static org.activiti.cloud.acc.modeling.modeling.ProcessExtensions.EXTENSIONS_TASK_NAME;
import static org.activiti.cloud.acc.modeling.modeling.ProcessExtensions.HOST_VALUE;
import static org.activiti.cloud.acc.modeling.modeling.ProcessExtensions.extensions;
import static org.activiti.cloud.modeling.api.process.ServiceTaskActionType.INPUTS;
import static org.activiti.cloud.modeling.api.process.ServiceTaskActionType.OUTPUTS;
import static org.activiti.cloud.modeling.api.process.VariableMappingType.VALUE;
import static org.activiti.cloud.modeling.api.process.VariableMappingType.VARIABLE;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

/**
 * Modeling steps for models
 */
@EnableModelingContext
public class ModelingModelsSteps extends ModelingContextSteps<Model> {

    private static final String PROJECT_MODELS_REL = "models";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelingModelsService modelingModelsService;
    @Autowired
    private ModelingTestsConfigurationProperties config;

    @Step
    public EntityModel<Model> create(String modelName,
                                  String modelType,
                                  List<String> processVariables) {
        Model model = mock(Model.class);
        doReturn(modelType.toUpperCase()).when(model).getType();
        doReturn(modelName).when(model).getName();
        if (processVariables != null) {
            Map<String, Extensions> processExtension = Collections.singletonMap(modelName, extensions(processVariables));
            doReturn(processExtension).when(model).getExtensions();
        }
        return create(model);
    }

    @Step
    public void removeProcessVariableInCurrentModel(String processVariable) {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        Model model = currentContext.getContent();

        Optional.ofNullable(this.getExtensionFromMap(model))
                .map(Extensions::getProcessVariables)
                .ifPresent(processVariables -> processVariables.remove(processVariable));

        Optional.ofNullable(this.getExtensionFromMap(model))
                .map(Extensions::getVariablesMappings)
                .map(mappings -> mappings.get(EXTENSIONS_TASK_NAME))
                .map(mappingsTypes -> mappingsTypes.get(INPUTS))
                .ifPresent(processVariableMappings -> processVariableMappings.remove(processVariable));

        Optional.ofNullable(this.getExtensionFromMap(model))
                .map(Extensions::getVariablesMappings)
                .map(mappings -> mappings.get(EXTENSIONS_TASK_NAME))
                .map(mappingsTypes -> mappingsTypes.get(OUTPUTS))
                .ifPresent(processVariableMappings -> processVariableMappings.remove(processVariable));
    }

    @Step
    public void addProcessVariableInCurrentModel(List<String> processVariable) {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        Model model = currentContext.getContent();

        addProcessVariableToModelModel(model,
                                       processVariable);
    }

    @Step
    public void addProcessVariableToModelModel(Model model,
                                               List<String> processVariable) {
        Set<String> processVariables = Optional.ofNullable(this.getExtensionFromMap(model))
                .map(Extensions::getProcessVariables)
                .map(Map::keySet)
                .map(HashSet::new)
                .orElseGet(HashSet::new);
        processVariables.addAll(processVariable);
        Map<String, Extensions> processsExtensionMap = new HashMap();
        processsExtensionMap.put(model.getName(), extensions(processVariables));
        model.setExtensions(processsExtensionMap);
    }

    @Step
    public void saveCurrentModel(boolean updateContent) {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        if (updateContent) {
            String updateMsg = "updated content";
            currentContext.getContent().setContent(updateMsg.getBytes());
        }

        saveModel(currentContext);
        updateCurrentModelingObject();
    }

    @Step
    public void saveModel(EntityModel<Model> model) {
        modelingModelsService.updateByUri(modelingUri(model.getLink(SELF).get().getHref()),
                                          model.getContent());
    }

    private List<Response> validateCurrentModel() throws IOException {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);
        final Model model = currentContext.getContent();
        model.setId(getModelId(currentContext));
        List<Response> responses = new ArrayList<>();
        responses.add(validateModel(currentContext,
                                    getFormData(model)));
        if (Optional.ofNullable(model.getExtensions()).isPresent()) {
            responses.add(validateExtensions(currentContext,
                                             getFormData(model,
                                                         true)));
        }
        return responses;
    }

    private String getModelId(EntityModel<Model> model) {
        String href = model.getLink(SELF).get().getHref();
        return href.substring(href.lastIndexOf('/') + 1);
    }
    private FormData getFormData(Model model,
                                 boolean isExtensionType) throws IOException {
        final String fileExtension = isExtensionType ? CONTENT_TYPE_JSON : getModelType(model.getType()).getContentFileExtension();
        final String fileName = isExtensionType ?
                toJsonFilename(model.getName() + getModelType(model.getType()).getExtensionsFileSuffix()) :
                setExtension(model.getName(),
                             fileExtension);
        final String resourcePath = model.getType().toLowerCase() + "/" + fileName;
        return new FormData(fileExtension,
                            fileName,
                            isExtensionType ?
                                    resourceAsModelExtensionsFile(model,
                                                                  resourcePath) :
                                    resourceAsByteArray(resourcePath));
    }

    private byte[] resourceAsModelExtensionsFile(Model model,
                                                 String resourcePath) throws IOException {
        return new String(resourceAsByteArray(resourcePath))
                .replaceFirst("\"id\": \".*\"",
                              "\"id\": \"process-" + model.getId() + "\"")
                .getBytes();
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
        assertThat(validateCurrentModel()
                           .stream()
                           .filter(response -> response.status() == HttpStatus.SC_BAD_REQUEST)
                           .map(this::convertResponseBodyAsJsonNode)
                           .map(node -> node.get("message"))
                           .map(JsonNode::asText)
                           .findFirst())
                .hasValueSatisfying(message -> assertThat(message).contains(errorMessage));
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
    public Response validateModel(EntityModel<Model> model,
                                  FormData file) {
        Link validateModelLink = model.getLink(SELF).get();
        assertThat(validateModelLink).isNotNull();
        return modelingModelsService.validateModelByUri(modelingUri(validateModelLink.getHref() + "/validate"),
                                                        file);
    }

    @Step
    public Response validateExtensions(EntityModel<Model> model,
                                       FormData file) {
        Link validateModelLink = model.getLink(SELF).get();
        assertThat(validateModelLink).isNotNull();
        return modelingModelsService.validateModelByUri(modelingUri(validateModelLink.getHref() + "/validate/extensions"),
                                                        file);
    }

    @Step
    public void checkCurrentModelVersion(String expectedModelVersion) {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        Model model = currentContext.getContent();
        assertThat(model.getVersion()).isEqualTo(expectedModelVersion);
    }

    @Step
    public void checkCurrentModelContainsVariables(String... processVariables) {
        EntityModel<Model> currentContext = checkAndGetCurrentContext(Model.class);
        Model model = currentContext.getContent();
        assertThat(model.getExtensions()).isNotNull();
        assertThat(this.getExtensionFromMap(model).getProcessVariables()).containsKeys(processVariables);
        Arrays.stream(processVariables).forEach(processVariableId -> {
            ProcessVariable processVariable = this.getExtensionFromMap(model).getProcessVariables().get(processVariableId);
            assertThat(processVariable.getId()).isEqualTo(processVariableId);
            assertThat(processVariable.getName()).isEqualTo(processVariableId);
            assertThat(processVariable.isRequired()).isEqualTo(false);
            assertThat(processVariable.getType()).isEqualTo("boolean");
            assertThat(processVariable.getValue()).isEqualTo(true);
        });

        assertThat(this.getExtensionFromMap(model).getVariablesMappings()).containsKeys(EXTENSIONS_TASK_NAME);
        assertThat(this.getExtensionFromMap(model).getVariablesMappings().get(EXTENSIONS_TASK_NAME)).containsKeys(INPUTS,
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
        Map<String, ProcessVariableMapping> outputsMappings = this.getExtensionFromMap(model)
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

    private Extensions getExtensionFromMap(Model model) {
        Map<String, Extensions> extensionProcessMap = this.retrieveExtensionForModel(model);
        return extensionProcessMap.get(model.getName());
    }

    private Map<String, Extensions> retrieveExtensionForModel(Model model) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(model.getExtensions()),
                objectMapper.getTypeFactory().constructMapType(Map.class,String.class, Extensions.class));
        } catch (IOException e) {
            return null;
        }
    }
}
