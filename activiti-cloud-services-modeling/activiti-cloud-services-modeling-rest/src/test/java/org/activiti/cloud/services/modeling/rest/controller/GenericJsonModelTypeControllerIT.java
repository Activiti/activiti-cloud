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
import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class GenericJsonModelTypeControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private static final String GENERIC_PROJECT_NAME = "project-with-generic-model";

    @Before
    public void setUp() {
        webAppContextSetup(context);
    }

    @Test
    public void should_returnStatusCreatedAndModelName_when_creatingGenericJsonModel() throws Exception {
        String name = GENERIC_MODEL_NAME;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                        genericJsonModelType.getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isCreated()).body("entry.name",
                                                          equalTo(GENERIC_MODEL_NAME));
    }

    @Test
    public void should_throwRequiredFieldException_when_creatingGenericJsonModelWithNameNull() throws Exception {
        String name = null;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.required").hasValidationErrorMessages("The model name is required");
    }

    @Test
    public void should_throwEmptyNameException_when_creatingGenericJsonModelWithNameEmpty() throws Exception {
        String name = "";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.empty",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name cannot be empty",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void should_throwTooLongNameException_when_creatingGenericJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("length.greater",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name length cannot be greater than 26: '123456789_123456789_1234567'",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwBadNameException_when_creatingGenericJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwBadNameException_when_creatingGenericJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void should_returnStatusOKAndModelName_when_updatingGenericJsonModel() throws Exception {
        String name = "updated-connector-name";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                         genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().log().all().expect(status().isOk()).body("name",
                                                                 equalTo("updated-connector-name"));
    }

    @Test
    public void should_returnStatusOKAndModelName_when_updatingGenericJsonModelWithNameNull() throws Exception {
        String name = null;

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                         genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isOk()).body("name",
                                                     equalTo(GENERIC_MODEL_NAME));
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameEmpty() throws Exception {
        String name = "";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.empty",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name cannot be empty",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void should_returnStatusCreatedAndEmptyExtensions_when_creatingGenericJsonModelWithNullExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = null;

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(genericJsonModel))
            .post("/v1/projects/{projectId}/models", project.getId())
            .then().expect(status().isCreated())
            .body("entry.extensions", notNullValue());
    }

    @Test
    public void should_returnStatusCreatedAndNotNullExtensions_when_creatingGenericJsonModelWithEmptyExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(genericJsonModel)).post("/v1/projects/{projectId}/models",
                                                                                                                                                project.getId())
                .then().expect(status().isCreated()).body("entry.extensions",
                                                          notNullValue());
    }

    @Test
    public void should_returnStatusCreatedAndExtensions_when_creatingGenericJsonModelWithValidExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();
        extensions.put("string",
                       "value");
        extensions.put("number",
                       2f);
        extensions.put("array",
                       new String[] { "a", "b", "c" });
        extensions.put("list",
                       Arrays.asList("a",
                                     "b",
                                     "c",
                                     "d"));

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(genericJsonModel)).post("/v1/projects/{projectId}/models",
                                                                                                                                                project.getId())
                .then().expect(status().isCreated()).body("entry.extensions",
                                                          notNullValue())
                .body("entry.extensions.string",
                      equalTo("value"))
                .body("entry.extensions.number",
                      equalTo(2f))
                .body("entry.extensions.array",
                      org.hamcrest.Matchers.hasSize(3))
                .body("entry.extensions.list",
                      org.hamcrest.Matchers.hasSize(4));
    }
}
