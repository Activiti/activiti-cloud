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

package org.activiti.cloud.starter.tests.definition;

import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.identity.keycloak.interceptor.KeycloakSecurityContextClientRequestInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
public class ProcessDefinitionAccessControlIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KeycloakSecurityContextClientRequestInterceptor keycloakSecurityContextClientRequestInterceptor;

    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    private static final String PROCESS_WITH_VARIABLES_2 = "ProcessWithVariables2";
    private static final String PROCESS_POOL_LANE = "process_pool1";

    @Test
    public void shouldRetrieveListOfProcessDefinition() throws Exception {
        //given
        //processes are automatically deployed from src/test/resources/processes

        //when
        ResponseEntity<PagedResources<ProcessDefinition>> entity = getProcessDefinitions();

        //then - should only see process defs visible to this user (testuser)
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
                "ProcessWithVariables",
                PROCESS_POOL_LANE);
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).doesNotContain(
                PROCESS_WITH_VARIABLES_2,
                "SimpleProcess",
                "ProcessWithBoundarySignal");

        keycloakSecurityContextClientRequestInterceptor.setKeycloaktestuser("hruser");

        //but hruser should see everything
        entity = getProcessDefinitions();
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
                PROCESS_WITH_VARIABLES_2,
                "SimpleProcess",
                "ProcessWithBoundarySignal");
    }

    private ResponseEntity<PagedResources<ProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<ProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<ProcessDefinition>>() {
        };
        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                HttpMethod.GET,
                null,
                responseType);

    }


}