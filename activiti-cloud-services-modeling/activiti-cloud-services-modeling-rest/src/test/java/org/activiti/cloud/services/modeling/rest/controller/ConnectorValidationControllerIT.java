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
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.modeling.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.modeling.validation.DNSNameValidator.DNS_LABEL_REGEX;
import static org.hamcrest.Matchers.isEmptyString;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

/**
 * Integration tests for connector models validation rest api
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class ConnectorValidationControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ModelRepository modelRepository;

    @Before
    public void setUp() {
        webAppContextSetup(context);
    }

    @Test
    public void should_returnStatusNoContent_when_validatingSimpleConnector() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .multiPart("file",
                           "simple-connector.json",
                           resourceAsByteArray("connector/connector-simple.json"),
                           "application/json")
                .post("/v1/models/{modelId}/validate",
                      connectorModel.getId())
                .then()
                .expect(status().isNoContent())
                .body(isEmptyString());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorTextContentType() throws IOException {
       Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .multiPart("file",
                           "simple-connector.json",
                           resourceAsByteArray("connector/connector-simple.json"),
                           "text/plain")
                .post("/v1/models/{modelId}/validate",
                      connectorModel.getId())
                .then()
                .expect(status().isNoContent())
                .body(isEmptyString());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorWithEvents() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .multiPart("file",
                        "connector-with-events.json",
                        resourceAsByteArray("connector/connector-with-events.json"),
                        "text/plain")
                .post("/v1/models/{modelId}/validate",
                        connectorModel.getId())
                .then()
                .expect(status().isNoContent())
                .body(isEmptyString());
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingInvalidSimpleConnector() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-simple-connector.json",
                                   resourceAsByteArray("connector/invalid-simple-connector.json"),
                                   "application/json")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("extraneous key [icon] is not permitted",
                                     "extraneous key [output] is not permitted",
                                     "extraneous key [input] is not permitted",
                                     "required key [id] not found",
                                     "required key [name] not found");
    }

    @Test
    public void should_throwSyntacticValidationException_when_validatingJsonInvalidConnector() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-json-connector.json",
                                   resourceAsByteArray("connector/invalid-json-connector.json"),
                                   "application/json")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSyntacticValidationException()
                .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");
    }

    @Test
    public void should_throwSyntacticValidationException_when_validatingInvalidConnectorTextContentType() throws IOException {
       Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-json-connector.json",
                                   resourceAsByteArray("connector/invalid-json-connector.json"),
                                   "text/plain")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSyntacticValidationException()
                .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingInvalidConnectorNameTooLong() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-too-long.json",
                                   resourceAsByteArray("connector/invalid-connector-name-too-long.json"),
                                   "text/plain")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("expected maxLength: 26, actual: 27",
                                     "string [123456789_123456789_1234567] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingInvalidConnectorNameEmpty() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-empty.json",
                                   resourceAsByteArray("connector/invalid-connector-name-empty.json"),
                                   "text/plain")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("expected minLength: 1, actual: 0",
                                     "string [] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingInvalidConnectorNameWithUnderscore() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-with-underscore.json",
                                   resourceAsByteArray("connector/invalid-connector-name-with-underscore.json"),
                                   "text/plain")
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("string [name_with_underscore] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void should_throwSemanticValidationException_when_validatingInvalidConnectorNameWithUppercase() throws IOException {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-with-uppercase.json",
                                   resourceAsByteArray("connector/invalid-connector-name-with-uppercase.json"),
                                   CONTENT_TYPE_JSON)
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                    .hasValidationErrors("string [NameWithUppercase] does not match pattern " + DNS_LABEL_REGEX);
    }
}
