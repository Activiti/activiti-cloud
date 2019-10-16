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
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void should_returnStatusCreatedAndConnectorName_when_creatingConnectorModel() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        given()
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .body(objectMapper.writeValueAsString(connectorModel("connector-name")))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then()
                .expect(status().isCreated())
                .body("entry.name",
                      equalTo("connector-name"));
    }

    @Test
    public void should_throwRequiredFieldException_when_creatingConnectorWithNameNull() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel(null)))
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("field.required")
                .hasValidationErrorMessages("The model name is required");
    }

    @Test
    public void should_throwEmptyFieldException_when_creatingConnectorModelWithNameEmpty() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("")))
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("field.empty",
                                         "regex.mismatch")
                .hasValidationErrorMessages("The model name cannot be empty",
                                            "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void should_throwTooLongNameException_when_createConnectorModelWithNameTooLong() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567")))
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("length.greater",
                                         "regex.mismatch")
                .hasValidationErrorMessages("The model name length cannot be greater than 26: '123456789_123456789_1234567'",
                                            "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwBadNameException_when_creatingConnectorModelWithNameWithUnderscore() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("name_with_underscore")))
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("regex.mismatch")
                .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwBadNameException_when_creatingConnectorModelWithNameWithUppercase() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        assertThatResponse(
                given()
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("NameWithUppercase")))
                        .post("/v1/projects/{projectId}/models",
                              project.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("regex.mismatch")
                .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void should_returnStatusOKAndConnectorName_when_updatingConnectorModel() throws Exception {
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
    public void should_returnStatusOKAndConnectorName_when_updatingConnectorModelWithNameNull() throws Exception {
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
    public void should_throwEmptyNameException_when_updatingConnectorModelWithNameEmpty() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("field.empty",
                                         "regex.mismatch")
                .hasValidationErrorMessages("The model name cannot be empty",
                                            "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void should_throwBadNameException_when_updatingConnectorModelWithNameTooLong() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("regex.mismatch")
                .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwBadNameException_when_updatingConnectorModelWithNameWithUnderscore() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("name_with_underscore")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("regex.mismatch")
                .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwBadNameException_when_updatingConnectorModelWithNameWithUpercase() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        assertThatResponse(
                given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .body(objectMapper.writeValueAsString(connectorModel("NameWithUppercase")))
                        .put("/v1/models/{modelId}",
                             connectorModel.getId())
                        .then().expect(status().isBadRequest()))
                .isValidationException()
                .hasValidationErrorCodes("regex.mismatch")
                .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }
}
