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

package org.activiti.cloud.services.modeling.rest.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.Collections;

/**
 * Integration tests for models rest api dealing with JSON models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class GenericJsonModelTypeValidationControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @SpyBean(name = "genericJsonExtensionsValidator")
    ModelExtensionsValidator genericJsonExtensionsValidator;

    @SpyBean(name = "genericJsonContentValidator")
    ModelContentValidator genericJsonContentValidator;

    @Autowired
    JsonModelType genericJsonModelType;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private Model genericJsonModel;

    @Before
    public void setUp() {
        webAppContextSetup(context);
        genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                       genericJsonModelType.getName()));
    }

    private void validateInvalidContent() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericJsonContentValidator.createModelValidationError("Content invalid",
                                                                                      "The content is invalid!!")));

        doThrow(exception).when(genericJsonContentValidator).validateModelContent(Mockito.any(byte[].class),
                                                                                  Mockito.any(ValidationContext.class));
    }

    private void validateInvalidExtensions() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericJsonContentValidator.createModelValidationError("Extensions invalid",
                                                                                      "The extensions are invalid!!")));

        doThrow(exception).when(genericJsonExtensionsValidator).validateModelExtensions(Mockito.any(byte[].class),
                                                                                        Mockito.any(ValidationContext.class));
    }

    @Test
    public void sholud_callGenericJsonContentValidatorAndNotCallGenericJsonExtensionsValidator_when_validatingModelContentJsonContentType() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given().multiPart("file",
                          "simple-model.json",
                          fileContent,
                          "application/json")
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_callGenericJsonContentValidatorAndNotCallGenericJsonExtensionsValidator_when_validatingModelContentTextContentType() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given().multiPart("file",
                          "simple-model.json",
                          fileContent,
                          "text/plain")
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_throwExceptionAndCallGenericJsonContentValidatorAndNotCallGenericJsonExtensionsValidator_when_validatingInvalidModelContent() throws IOException {
        this.validateInvalidContent();

        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        assertThatResponse(given().multiPart("file",
                                             "invalid-simple-model.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Content invalid");

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_notCallGenericJsonContentValidatorAndCallGenericJsonExtensionsValidator_when_validatingModelValidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        given().multiPart("file",
                          "simple-model-extensions.json",
                          fileContent,
                          "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_throwSemanticValidationException_when_validatingModelInvalidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("required key [id] not found");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_throwSyntacticValidationException_when_validatingInvalidJsonExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-json-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSyntacticValidationException()
                        .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_throwSemanticValidationException_when_validatingModelInvalidTypeExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-type-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("expected type: String, found: Boolean");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_throwException_when_validatingModelInvalidSemanticExtensions() throws IOException {
        this.validateInvalidExtensions();

        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Extensions invalid");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }
}
