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
package org.activiti.cloud.services.query.rest;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.postProcessors;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
class ProcessInstanceEntitySearchAdminControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeEach
    void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testadmin")
    void should_return_AllProcessInstances() {
        ProcessInstanceEntity processInstance1 = createProcessInstance("process1", Map.of());
        ProcessInstanceEntity processInstance2 = createProcessInstance("process2", Map.of());

        processInstance1.setInitiator("user");
        processInstanceRepository.save(processInstance1);
        processInstance2.setInitiator("another-user");
        processInstanceRepository.save(processInstance2);

        RestAssuredMockMvc
            .given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post("/admin/v1/process-instances/search")
            .then()
            .statusCode(200)
            .body("_embedded.processInstances", hasSize(2))
            .body("_embedded.processInstances[0].id", equalTo(processInstance1.getId()))
            .body("_embedded.processInstances[1].id", equalTo(processInstance2.getId()));
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefKey, Map<String, Object> variables) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName(UUID.randomUUID().toString());
        processInstanceEntity.setProcessDefinitionKey(processDefKey);
        processInstanceEntity.setAppVersion(UUID.randomUUID().toString());
        processInstanceRepository.save(processInstanceEntity);

        Set<ProcessVariableEntity> processVariables = variables
            .entrySet()
            .stream()
            .map(entry -> {
                ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
                processVariableEntity.setName(entry.getKey());
                processVariableEntity.setValue(entry.getValue());
                processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
                processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
                variableRepository.save(processVariableEntity);
                return processVariableEntity;
            })
            .collect(Collectors.toSet());

        processInstanceEntity.setVariables(processVariables);
        return processInstanceRepository.save(processInstanceEntity);
    }
}
