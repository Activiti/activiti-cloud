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
package org.activiti.cloud.services.modeling.rest.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Collections;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with JSON models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@SpringBootTest(classes = ModelingRestApplication.class)
@Transactional
@WebAppConfiguration
@WithMockModelerUser
public class GenericNonJsonModelTypeValidationControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ModelRepository modelRepository;

    @SpyBean(name = "genericNonJsonExtensionsValidator")
    private ModelExtensionsValidator genericNonJsonExtensionsValidator;

    @SpyBean(name = "genericNonJsonContentValidator")
    private ModelContentValidator genericNonJsonContentValidator;

    @Autowired
    private ModelType genericNonJsonModelType;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private Model genericNonJsonModel;

    @BeforeEach
    public void setUp() {
        webAppContextSetup(context);
        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));
    }

    private void validateInvalidContent() {
        SemanticModelValidationException exception = new SemanticModelValidationException(
            Collections.singletonList(new ModelValidationError("Content invalid", "The content is" + " invalid" + "!!"))
        );

        doThrow(exception)
            .when(genericNonJsonContentValidator)
            .validateModelContent(any(), any(byte[].class), any(ValidationContext.class), anyBoolean());
    }

    private void validateInvalidExtensions() {
        SemanticModelValidationException exception = new SemanticModelValidationException(
            Collections.singletonList(
                new ModelValidationError("Extensions " + "invalid", "The extensions" + " are " + "invalid" + "!!")
            )
        );

        doThrow(exception)
            .when(genericNonJsonExtensionsValidator)
            .validateModelExtensions(any(byte[].class), any(ValidationContext.class));
    }

    @Test
    public void testValidateModelContent() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        given()
            .multiPart("file", "simple-model.bin", fileContent, APPLICATION_OCTET_STREAM_VALUE)
            .post("/v1/models/{modelId}/validate", genericNonJsonModel.getId())
            .then()
            .expect(status().isNoContent())
            .body(is(emptyString()));

        verify(genericNonJsonExtensionsValidator, times(0)).validateModelExtensions(any(), any());

        verify(genericNonJsonContentValidator, times(1))
            .validateModelContent(
                any(),
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> !context.isEmpty()),
                eq(false)
            );
    }

    @Test
    public void should_callGenericNonJsonContentValidatorAndNotCallGenericNonJsonExtensionsValidator_when_validatingModelContentTextContentType()
        throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        given()
            .multiPart("file", "simple-model.bin", fileContent, "text/plain")
            .post("/v1/models/{modelId}/validate", genericNonJsonModel.getId())
            .then()
            .expect(status().isNoContent())
            .body(is(emptyString()));

        verify(genericNonJsonExtensionsValidator, times(0)).validateModelExtensions(any(), any());

        verify(genericNonJsonContentValidator, times(1))
            .validateModelContent(
                any(),
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> !context.isEmpty()),
                eq(false)
            );
    }

    @Test
    public void should_callGenericNonJsonContentValidatorAndNotCallGnericNonJsonExtensionsValidator_when_validatingModelContentJsonContentType()
        throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given()
            .multiPart("file", "simple-model.json", fileContent, "application/json")
            .post("/v1/models/{modelId}/validate", genericNonJsonModel.getId())
            .then()
            .expect(status().isNoContent())
            .body(is(emptyString()));

        verify(genericNonJsonExtensionsValidator, times(0)).validateModelExtensions(any(), any());

        verify(genericNonJsonContentValidator, times(1))
            .validateModelContent(
                any(),
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty()),
                eq(false)
            );
    }

    @Test
    public void should_throwExceptionAndCallGenericNonJsonContentValidatorAndNotCallGenericNonJsonExtensionsValidator_when_validatingInvalidModelContent()
        throws IOException {
        this.validateInvalidContent();

        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        assertThatResponse(
            given()
                .multiPart("file", "invalid-simple-model.json", fileContent, APPLICATION_OCTET_STREAM_VALUE)
                .post("/v1/models/{modelId}/validate", genericNonJsonModel.getId())
                .then()
                .expect(status().isBadRequest())
        )
            .isSemanticValidationException()
            .hasValidationErrors("Content invalid");

        verify(genericNonJsonExtensionsValidator, times(0)).validateModelExtensions(any(), any());

        verify(genericNonJsonContentValidator, times(1))
            .validateModelContent(
                any(),
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> !context.isEmpty()),
                eq(false)
            );
    }

    @Test
    public void should_notCallGenericNonJsonContentValidatorAndCallGenericNonJsonExtensionsValidator_when_validatingModelValidExtensions()
        throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        given()
            .multiPart("file", "simple-model-extensions.json", fileContent, "application/json")
            .post("/v1/models/{modelId}/validate/extensions", genericNonJsonModel.getId())
            .then()
            .expect(status().isNoContent())
            .body(is(emptyString()));

        verify(genericNonJsonContentValidator, times(0)).validateModelContent(any(), any());

        verify(genericNonJsonExtensionsValidator, times(1))
            .validateModelExtensions(
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty())
            );
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingModelInvalidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-extensions.json");

        assertThatResponse(
            given()
                .multiPart("file", "simple-model-extensions.json", fileContent, "application/json")
                .post("/v1/models/{modelId}/validate/extensions", genericNonJsonModel.getId())
                .then()
                .expect(status().isBadRequest())
        )
            .isSemanticValidationException()
            .hasValidationErrors("required key [id] not found");

        verify(genericNonJsonContentValidator, times(0)).validateModelContent(any(), any());

        verify(genericNonJsonExtensionsValidator, times(1))
            .validateModelExtensions(
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty())
            );
    }

    @Test
    public void should_throwSyntacticValidationException_when_validatingInvalidJsonExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-json-extensions.json");

        assertThatResponse(
            given()
                .multiPart("file", "simple-model-extensions.json", fileContent, "application/json")
                .post("/v1/models/{modelId}/validate/extensions", genericNonJsonModel.getId())
                .then()
                .expect(status().isBadRequest())
        )
            .isSyntacticValidationException()
            .hasValidationErrors(
                "org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]"
            );

        verify(genericNonJsonContentValidator, times(0)).validateModelContent(any(), any());

        verify(genericNonJsonExtensionsValidator, times(1))
            .validateModelExtensions(
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty())
            );
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingModelInvalidTypeExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-type-extensions.json");

        assertThatResponse(
            given()
                .multiPart("file", "simple-model-extensions.json", fileContent, "application/json")
                .post("/v1/models/{modelId}/validate/extensions", genericNonJsonModel.getId())
                .then()
                .expect(status().isBadRequest())
        )
            .isSemanticValidationException()
            .hasValidationErrors("expected type: String, found: Boolean");

        verify(genericNonJsonContentValidator, times(0)).validateModelContent(any(), any());

        verify(genericNonJsonExtensionsValidator, times(1))
            .validateModelExtensions(
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty())
            );
    }

    @Test
    public void should_throwException_when_validatingModelInvalidSemanticExtensions() throws IOException {
        this.validateInvalidExtensions();

        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        assertThatResponse(
            given()
                .multiPart("file", "simple-model-extensions.json", fileContent, "application/json")
                .post("/v1/models/{modelId}/validate/extensions", genericNonJsonModel.getId())
                .then()
                .expect(status().isBadRequest())
        )
            .isSemanticValidationException()
            .hasValidationErrors("Extensions invalid");

        verify(genericNonJsonContentValidator, times(0)).validateModelContent(any(), any());

        verify(genericNonJsonExtensionsValidator, times(1))
            .validateModelExtensions(
                argThat(content -> new String(content).equals(new String(fileContent))),
                argThat(context -> context.isEmpty())
            );
    }
}
