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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.CustomPostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@WithMockUser(username = AbstractProcessInstanceEntitySearchControllerIT.USER, roles = "ACTIVITI_ADMIN")
class ProcessInstanceEntitySearchAdminControllerIT extends AbstractProcessInstanceEntitySearchControllerIT {

    @Override
    protected String getSearchEndpoint() {
        return "/admin/v1/process-instances/search";
    }

    @Test
    void should_return_AllProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("another-user")
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(2))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()))
            .body(processInstanceIdJsonPath, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByInitiator() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user2")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("user3").buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withInitiators("user1", "user2");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(2))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()))
            .body(processInstanceIdJsonPath, hasItem(processInstance2.getId()));
    }
}
