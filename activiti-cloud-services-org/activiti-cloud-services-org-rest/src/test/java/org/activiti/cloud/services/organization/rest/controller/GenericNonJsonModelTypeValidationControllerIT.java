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

package org.activiti.cloud.services.organization.rest.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContentValidator;
import org.activiti.cloud.organization.api.ModelExtensionsValidator;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.security.WithMockModelerUser;
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
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class GenericNonJsonModelTypeValidationControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @SpyBean(name = "genericNonJsonExtensionsValidator")
    ModelExtensionsValidator genericNonJsonExtensionsValidator;

    @SpyBean(name = "genericNonJsonContentValidator")
    ModelContentValidator genericNonJsonContentValidator;

    @Autowired
    ModelType genericNonJsonModelType;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private Model genericNonJsonModel;

    @Before
    public void setUp() {
        webAppContextSetup(context);
        genericNonJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                          genericNonJsonModelType.getName()));
    }

    private void validateInvalidContent() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericNonJsonContentValidator.createModelValidationError("Content invalid",
                                                                                         "The content is invalid!!")));

        doThrow(exception).when(genericNonJsonContentValidator).validateModelContent(Mockito.any(byte[].class),
                                                                                     Mockito.any(ValidationContext.class));
    }

    private void validateInvalidExtensions() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericNonJsonContentValidator.createModelValidationError("Extensions invalid",
                                                                                         "The extensions are invalid!!")));

        doThrow(exception).when(genericNonJsonExtensionsValidator).validateModelExtensions(Mockito.any(byte[].class),
                                                                                           Mockito.any(ValidationContext.class));
    }

    @Test
    public void testValidateModelContent() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        given().multiPart("file",
                          "simple-model.bin",
                          fileContent,
                          APPLICATION_OCTET_STREAM_VALUE)
                .post("/v1/models/{modelId}/validate",
                      genericNonJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_callGnericNonJsonContentValidatorAndNotCallGnericNonJsonExtensionsValidator_when_validatingModelContentTextContentType() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        given().multiPart("file",
                          "simple-model.bin",
                          fileContent,
                          "text/plain")
                .post("/v1/models/{modelId}/validate",
                      genericNonJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_callGnericNonJsonContentValidatorAndNotCallGnericNonJsonExtensionsValidator_when_validatingModelContentJsonContentType() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given().multiPart("file",
                          "simple-model.json",
                          fileContent,
                          "application/json")
                .post("/v1/models/{modelId}/validate",
                      genericNonJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> context.isEmpty()));
    }

    @Test
    public void sholud_throwExceptionAndCallGnericNonJsonContentValidatorAndNotCallGnericNonJsonExtensionsValidator_when_validatingInvalidModelContent() throws IOException {
        this.validateInvalidContent();

        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        assertThatResponse(given().multiPart("file",
                                             "invalid-simple-model.json",
                                             fileContent,
                                             APPLICATION_OCTET_STREAM_VALUE)
                .post("/v1/models/{modelId}/validate",
                      genericNonJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Content invalid");

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void sholud_notCallGnericNonJsonContentValidatorAndCallGnericNonJsonExtensionsValidator_when_validatingModelValidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        given().multiPart("file",
                          "simple-model-extensions.json",
                          fileContent,
                          "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericNonJsonModel.getId())
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> context.isEmpty()));
    }

    @Test
    public void sholud_throwSemanticValidationException_when_validatingModelInvalidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericNonJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("required key [id] not found");

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> context.isEmpty()));
    }
    
    @Test
    public void sholud_throwSyntacticValidationException_when_validatingInvalidJsonExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-json-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericNonJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSyntacticValidationException().hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> context.isEmpty()));
    }
    
    @Test
    public void sholud_throwSemanticValidationException_when_validatingModelInvalidTypeExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-type-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                .post("/v1/models/{modelId}/validate/extensions",
                      genericNonJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("expected type: String, found: Boolean");

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> context.isEmpty()));
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
                      genericNonJsonModel.getId())
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Extensions invalid");

        Mockito.verify(genericNonJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericNonJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> context.isEmpty()));
    }

}
