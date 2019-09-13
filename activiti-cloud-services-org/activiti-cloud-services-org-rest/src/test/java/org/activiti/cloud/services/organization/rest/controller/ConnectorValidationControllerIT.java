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

import java.io.IOException;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.organization.api.ModelValidationErrorProducer.DNS_LABEL_REGEX;
import static org.hamcrest.Matchers.isEmptyString;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for connector models validation rest api
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
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
    public void testValidateSimpleConnector() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .multiPart("file",
                           "simple-connector.json",
                           resourceAsByteArray("connector/connector-simple.json"),
                           "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate",
                      connectorModel.getId())
                //THEN
                .then()
                .expect(status().isNoContent())
                .body(isEmptyString());
    }

    @Test
    public void testValidateConnectorTextContentType() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .multiPart("file",
                           "simple-connector.json",
                           resourceAsByteArray("connector/connector-simple.json"),
                           "text/plain")
                //WHEN
                .post("/v1/models/{modelId}/validate",
                      connectorModel.getId())
                //THEN
                .then()
                .expect(status().isNoContent())
                .body(isEmptyString());
    }

    @Test
    public void testInvalidSimpleConnector() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-simple-connector.json",
                                   resourceAsByteArray("connector/invalid-simple-connector.json"),
                                   "application/json")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
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
    public void testJsonInvalidConnector() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-json-connector.json",
                                   resourceAsByteArray("connector/invalid-json-connector.json"),
                                   "application/json")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSyntacticValidationException()
                .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");
    }

    @Test
    public void testInvalidConnectorTextContentType() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-json-connector.json",
                                   resourceAsByteArray("connector/invalid-json-connector.json"),
                                   "text/plain")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSyntacticValidationException()
                .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");
    }

    @Test
    public void testInvalidConnectorNameTooLong() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-too-long.json",
                                   resourceAsByteArray("connector/invalid-connector-name-too-long.json"),
                                   "text/plain")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("expected maxLength: 26, actual: 27",
                                     "string [123456789_123456789_1234567] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void testInvalidConnectorNameEmpty() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-empty.json",
                                   resourceAsByteArray("connector/invalid-connector-name-empty.json"),
                                   "text/plain")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("expected minLength: 1, actual: 0",
                                     "string [] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void testInvalidConnectorNameWithUnderscore() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-with-underscore.json",
                                   resourceAsByteArray("connector/invalid-connector-name-with-underscore.json"),
                                   "text/plain")
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                .hasValidationErrors("string [name_with_underscore] does not match pattern " + DNS_LABEL_REGEX);
    }

    @Test
    public void testInvalidConnectorNameWithUppercase() throws IOException {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .multiPart("file",
                                   "invalid-connector-name-with-uppercase.json",
                                   resourceAsByteArray("connector/invalid-connector-name-with-uppercase.json"),
                                   CONTENT_TYPE_JSON)
                        //WHEN
                        .post("/v1/models/{modelId}/validate",
                              connectorModel.getId())
                        //THEN
                        .then()
                        .log().all()
                        .expect(status().isBadRequest()))
                .isSemanticValidationException()
                    .hasValidationErrors("string [NameWithUppercase] does not match pattern " + DNS_LABEL_REGEX);
    }
}
