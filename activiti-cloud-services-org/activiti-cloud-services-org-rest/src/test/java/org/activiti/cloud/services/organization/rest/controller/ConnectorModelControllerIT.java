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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
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
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for models rest api dealing with connector models
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class ConnectorModelControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Before
    public void setUp() {
        webAppContextSetup(context);
    }

    @Test
    public void testCreateConnectorModel() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        given()
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .body(objectMapper.writeValueAsString(connectorModel("connector-name")))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isCreated())
                .body("name",
                      equalTo("connector-name"));
    }

    @Test
    public void testCreateConnectorModelInvalidPayloadNameNull() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel(null)))
                        // WHEN
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        // THEN
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("model.invalid.name.empty");
    }

    @Test
    public void testCreateConnectorModelInvalidPayloadNameEmpty() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("")))
                        // WHEN
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        // THEN
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("model.invalid.name.empty");
    }

    @Test
    public void testCreateConnectorModelInvalidPayloadNameTooLong() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567")))
                        // WHEN
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        // THEN
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name.length",
                                         "connector.invalid.name");
    }

    @Test
    public void testCreateConnectorModelInvalidPayloadNameWithUnderscore() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("name_with_underscore")))
                        // WHEN
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        // THEN
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name");
    }

    @Test
    public void testCreateConnectorModelInvalidPayloadNameWithUppercase() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("NameWithUppercase")))
                        // WHEN
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        // THEN
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name");
    }

    @Test
    public void testUpdateConnectorModel() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .contentType(APPLICATION_JSON_VALUE)
                .body(objectMapper.writeValueAsString(connectorModel("updated-connector-name")))
                .put("/v1/models/{modelId}",
                     connectorModel.getId())
                .then().log().all().expect(status().isOk())
                .body("name",
                      equalTo("updated-connector-name"));
    }

    @Test
    public void testUpdateConnectorModelInvalidPayloadNameNull() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        given()
                .contentType(APPLICATION_JSON_VALUE)
                .body(objectMapper.writeValueAsString(connectorModel(null)))
                .put("/v1/models/{modelId}",
                     connectorModel.getId())
                .then().expect(status().isOk())
                .body("name",
                      equalTo("connector-name"));
    }

    @Test
    public void testUpdateConnectorModelInvalidPayloadNameEmpty() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("model.invalid.name.empty");
    }

    @Test
    public void testUpdateConnectorModelInvalidPayloadNameTooLong() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name.length",
                                         "connector.invalid.name");
    }

    @Test
    public void testUpdateConnectorModelInvalidPayloadNameWithUnderscore() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("name_with_underscore")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name");
    }

    @Test
    public void testUpdateConnectorModelInvalidPayloadNameWithUppercase() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("NameWithUppercase")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("connector.invalid.name");
    }
}
