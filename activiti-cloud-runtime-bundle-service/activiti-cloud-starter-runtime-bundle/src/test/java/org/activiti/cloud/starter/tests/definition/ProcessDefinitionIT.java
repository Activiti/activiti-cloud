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
package org.activiti.cloud.starter.tests.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.util.TestResourceUtil;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties",
    "classpath:access-control.properties"})
@DirtiesContext
@ContextConfiguration(initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ProcessDefinitionIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessDiagramGenerator processDiagramGenerator;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;


    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    public static final String ADMIN_PROCESS_DEFINITIONS_URL = "/admin/v1/process-definitions/";

    private static final String PROCESS_WITH_VARIABLES = "ProcessWithVariables";
    private static final String PROCESS_WITH_VARIABLES_2 = "ProcessWithVariables2";
    private static final String PROCESS_POOL_LANE = "process_pool1";
    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String PROCESS_WITH_BOUNDARY_SIGNAL = "ProcessWithBoundarySignal";

    @Test
    public void shouldRetrieveListOfProcessDefinition() {
        //given
        //processes are automatically deployed from src/test/resources/processes

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> entity = getProcessDefinitions(
            PROCESS_DEFINITIONS_URL);

        //then
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
            PROCESS_WITH_VARIABLES,
            PROCESS_WITH_VARIABLES_2,
            PROCESS_POOL_LANE,
            SIMPLE_PROCESS,
            PROCESS_WITH_BOUNDARY_SIGNAL);
    }

    private ProcessDefinition getProcessDefinition(String name) {
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitionsEntity = getProcessDefinitions(
            PROCESS_DEFINITIONS_URL);
        Iterator<CloudProcessDefinition> it = processDefinitionsEntity.getBody().getContent()
            .iterator();
        ProcessDefinition aProcessDefinition;
        do {
            aProcessDefinition = it.next();
        } while (!aProcessDefinition.getName().equals(name));

        return aProcessDefinition;
    }

    private ResponseEntity<PagedModel<CloudProcessDefinition>> getProcessDefinitions(
        String url) {
        ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {
        };
        return restTemplate.exchange(url,
            HttpMethod.GET,
            null,
            responseType);
    }

    @Test
    public void shouldReturnProcessDefinitionById() {
        //given
        ParameterizedTypeReference<CloudProcessDefinition> responseType = new ParameterizedTypeReference<CloudProcessDefinition>() {
        };

        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitionsEntity = getProcessDefinitions(
            PROCESS_DEFINITIONS_URL);
        assertThat(processDefinitionsEntity).isNotNull();
        assertThat(processDefinitionsEntity.getBody()).isNotNull();
        assertThat(processDefinitionsEntity.getBody().getContent()).isNotEmpty();
        ProcessDefinition aProcessDefinition = processDefinitionsEntity.getBody().getContent().iterator().next();

        //when
        ResponseEntity<CloudProcessDefinition> entity = restTemplate
            .exchange(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId(),
                HttpMethod.GET,
                null,
                responseType);

        //then
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getId()).isEqualTo(aProcessDefinition.getId());
    }

    @Test
    public void shouldReturnProcessDefinitionMetadata() {
        //given
        ParameterizedTypeReference<ProcessDefinitionMeta> responseType = new ParameterizedTypeReference<ProcessDefinitionMeta>() {
        };

        ProcessDefinition aProcessDefinition = getProcessDefinition(PROCESS_WITH_VARIABLES_2);

        //when
        ResponseEntity<ProcessDefinitionMeta> entity = restTemplate.exchange(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId() + "/meta",
            HttpMethod.GET,
            null,
            responseType);
        //then
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getVariables()).hasSize(3);
        assertThat(entity.getBody().getUsers()).hasSize(4);
        assertThat(entity.getBody().getGroups()).hasSize(4);
        assertThat(entity.getBody().getUserTasks()).hasSize(2);
        assertThat(entity.getBody().getServiceTasks()).hasSize(2);
    }

    @Test
    public void shouldReturnProcessDefinitionMetadataForPoolLane() {
        //given
        ParameterizedTypeReference<ProcessDefinitionMeta> responseType = new ParameterizedTypeReference<ProcessDefinitionMeta>() {
        };

        ProcessDefinition aProcessDefinition = getProcessDefinition(PROCESS_POOL_LANE);

        //when
        ResponseEntity<ProcessDefinitionMeta> entity = restTemplate.exchange(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId() + "/meta",
            HttpMethod.GET,
            null,
            responseType);
        //then
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getVariables()).hasSize(6);
        assertThat(entity.getBody().getUsers()).hasSize(4);
        assertThat(entity.getBody().getGroups()).hasSize(4);
        assertThat(entity.getBody().getUserTasks()).hasSize(3);
        assertThat(entity.getBody().getServiceTasks()).hasSize(3);
    }

    @Test
    public void shouldRetrieveProcessModel() throws Exception {

        ProcessDefinition aProcessDefinition = getProcessDefinition(PROCESS_POOL_LANE);

        //when
        String responseData = executeRequest(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId() + "/model",
            HttpMethod.GET,
            "application/xml");

        //then
        assertThat(responseData).isNotNull();
        assertThat(responseData).isEqualTo(TestResourceUtil.getProcessXml(aProcessDefinition.getId().split(":")[0]));
    }

    @Test
    public void shouldRetriveBpmnModel() throws Exception {
        //given
        ProcessDefinition aProcessDefinition = getProcessDefinition(PROCESS_WITH_VARIABLES_2);

        //when
        JsonNode responseData = executeRequest(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId() + "/model",
            HttpMethod.GET,
            "application/json",
            JsonNode.class);

        //then
        assertThat(responseData).isNotNull();

        BpmnModel targetModel = new BpmnJsonConverter().convertToBpmnModel(responseData);
        final InputStream byteArrayInputStream = new ByteArrayInputStream(TestResourceUtil.getProcessXml(aProcessDefinition.getId()
            .split(":")[0]).getBytes());
        BpmnModel sourceModel = new BpmnXMLConverter().convertToBpmnModel(() -> byteArrayInputStream,
            false,
            false);
        assertThat(targetModel.getMainProcess().getId()).isEqualTo(sourceModel.getMainProcess().getId());
        for (FlowElement element : targetModel.getMainProcess().getFlowElements()) {
            assertThat(sourceModel.getFlowElement(element.getId())).isNotNull();
        }
    }

    @Test
    public void shouldRetrieveDiagram() throws Exception {

        ProcessDefinition aProcessDefinition = getProcessDefinition(PROCESS_POOL_LANE);

        //when
        String responseData = executeRequest(PROCESS_DEFINITIONS_URL + aProcessDefinition.getId() + "/model",
            HttpMethod.GET,
            "image/svg+xml");

        //then
        assertThat(responseData).isNotNull();
        final InputStream byteArrayInputStream = new ByteArrayInputStream(TestResourceUtil.getProcessXml(aProcessDefinition.getId()
            .split(":")[0]).getBytes());
        BpmnModel sourceModel = new BpmnXMLConverter().convertToBpmnModel(() -> byteArrayInputStream,
            false,
            false);
        String activityFontName = processDiagramGenerator.getDefaultActivityFontName();
        String labelFontName = processDiagramGenerator.getDefaultLabelFontName();
        String annotationFontName = processDiagramGenerator.getDefaultAnnotationFontName();
        try (InputStream is = processDiagramGenerator.generateDiagram(sourceModel,
            activityFontName,
            labelFontName,
            annotationFontName)) {
            String sourceSvg = new String(IoUtil.readInputStream(is,
                null),
                "UTF-8");
            assertThat(responseData).isEqualTo(sourceSvg);
        }
    }

    private <T> T executeRequest(String url,
        HttpMethod method,
        String contentType,
        Class<T> javaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", contentType);
        ResponseEntity<T> response = restTemplate.exchange(url,
            method,
            new HttpEntity<>(headers),
            javaType);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private String executeRequest(String url,
        HttpMethod method,
        String contentType) {
        return executeRequest(url,
            method,
            contentType,
            String.class);
    }

    @Test
    public void shouldRetrieveDifferentListOfProcessDefinitionAccordingToUserPolicies() {
        //given
        //processes are automatically deployed from src/test/resources/processes

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");
        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> entity = getProcessDefinitions(PROCESS_DEFINITIONS_URL);

        //then - should only see process defs visible to this user (testuser)
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
            PROCESS_WITH_VARIABLES,
            PROCESS_POOL_LANE);
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).doesNotContain(
            PROCESS_WITH_VARIABLES_2,
            SIMPLE_PROCESS,
            PROCESS_WITH_BOUNDARY_SIGNAL);

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        //but hruser should see different set according to access-control.properties
        entity = getProcessDefinitions(PROCESS_DEFINITIONS_URL);
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
            PROCESS_WITH_VARIABLES_2,
            SIMPLE_PROCESS,
            PROCESS_WITH_BOUNDARY_SIGNAL);
    }

    @Test
    public void adminShouldSeeLargerListAtAdminEndpoint() {
        //given
        //processes are automatically deployed from src/test/resources/processes

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        //testadmin should see restricted set at non-admin endpoint
        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> entity = getProcessDefinitions(
            PROCESS_DEFINITIONS_URL);

        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName)
            .doesNotContain(
                PROCESS_WITH_VARIABLES_2,
                SIMPLE_PROCESS,
                PROCESS_WITH_BOUNDARY_SIGNAL);

        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
            PROCESS_WITH_VARIABLES,
            PROCESS_POOL_LANE);

        //and a larger set at admin endpoint
        entity = getProcessDefinitions(ADMIN_PROCESS_DEFINITIONS_URL);
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).extracting(ProcessDefinition::getName).contains(
            PROCESS_WITH_VARIABLES_2,
            SIMPLE_PROCESS,
            PROCESS_WITH_BOUNDARY_SIGNAL);
    }

    @Test
    public void processDefinitionShouldHaveFormKey() {
        ProcessDefinition aProcessDefinition = getProcessDefinition(SIMPLE_PROCESS);
        assertThat(aProcessDefinition.getFormKey()).isEqualTo("startFormKey");
    }
}
