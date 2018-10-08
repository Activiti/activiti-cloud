/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consumer side tests for communication with process models service
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@AutoConfigureStubRunner(ids = "org.activiti.cloud.process.model:activiti-cloud-services-process-model-rest:+:stubs:8088")
public class ProcessModelServiceRestClientIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ModelRepository modelRepository;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        webAppContextSetup(context);
    }

    @Test
    public void testCreateProcessModel() throws Exception {
        Model processModel = new ModelEntity("contractNewProcesModelId",
                                             "newProcesModelName",
                                             PROCESS);

        given()
                .contentType(APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(processModel))
                .post("/v1/models")
                .then().expect(status().isCreated());
    }

    @Test
    public void testUpdateProcessModel() throws Exception {
        Model processModel = new ModelEntity("contractUpdateProcesModelId",
                                             "newProcesModelNameUpdated",
                                             PROCESS);
        processModel.setContent("someContent");
        modelRepository.createModel(processModel);

        Model newProcessModel = new ModelEntity();
        newProcessModel.setType(PROCESS);
        newProcessModel.setName("newProcesModelNameUpdated");
        newProcessModel.setContent("newContent");

        given()
                .contentType(APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(newProcessModel))
                .put("/v1/models/contractUpdateProcesModelId")
                .then().expect(status().isOk());
    }

    @Test
    public void testGetProcessModel() throws Exception {
        Model processModel = new ModelEntity("contractUpdateProcesModelId",
                                             "testProcesModelName",
                                             PROCESS);
        modelRepository.createModel(processModel);

        given()
                .contentType(APPLICATION_JSON_VALUE)
                .get("/v1/models/contractUpdateProcesModelId")
                .then().expect(status().isOk())
                .and().body("name",
                            equalTo("contractUpdateProcesModelNameUpdated"))
                .and().body("version",
                            equalTo("0.0.2"));
    }
}
