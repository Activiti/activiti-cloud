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

package org.activiti.cloud.starter.tests.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.converter.util.InputStreamProvider;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.util.TestResourceUtil;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_ADMIN_RELATIVE_URL;
import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProcessInstanceIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessDiagramGenerator processDiagramGenerator;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Autowired
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() {
        keycloakTestUser = "hruser";
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldStartProcess() {
        //when
        ResponseEntity<CloudProcessInstance> entity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
                                                                                               null,
                                                                                               "business_key");

        //then
        assertThat(entity).isNotNull();
        CloudProcessInstance returnedProcInst = entity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getInitiator()).isNotNull();
        assertThat(returnedProcInst.getInitiator()).isEqualTo(keycloakTestUser);//will only match if using username not id
        assertThat(returnedProcInst.getBusinessKey()).isEqualTo("business_key");
        assertThat(returnedProcInst.getAppName()).isEqualTo(runtimeBundleProperties.getAppName());
        assertThat(returnedProcInst.getAppVersion()).isEqualTo(runtimeBundleProperties.getAppVersion());
        assertThat(returnedProcInst.getServiceName()).isEqualTo(runtimeBundleProperties.getServiceName());
        assertThat(returnedProcInst.getServiceFullName()).isEqualTo(runtimeBundleProperties.getServiceFullName());
        assertThat(returnedProcInst.getServiceType()).isEqualTo(runtimeBundleProperties.getServiceType());
        assertThat(returnedProcInst.getServiceVersion()).isEqualTo(runtimeBundleProperties.getServiceVersion());
    }

    @Test
    public void shouldStartProcessByKey() {
        //when
        ResponseEntity<CloudProcessInstance> entity = processInstanceRestTemplate.startProcessByKey(SIMPLE_PROCESS,
                                                                                                    null,
                                                                                                    "business_key");

        //then
        assertThat(entity).isNotNull();
        ProcessInstance returnedProcInst = entity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getInitiator()).isNotNull();
        assertThat(returnedProcInst.getInitiator()).isEqualTo(keycloakTestUser);//will only match if using username not id
        assertThat(returnedProcInst.getBusinessKey()).isEqualTo("business_key");
    }

    @Test
    public void shouldNotStartProcessWithoutPermission() {
        //testuser does not have access to SIMPLE_PROCESS according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        assertThatExceptionOfType(RestClientException.class).isThrownBy(() ->
                                                                                processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS)));
    }

    @Test
    public void shouldStartProcessIfAdmin() {
        //testadmin does not have access to SIMPLE_PROCESS according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        StartProcessPayload startProcess = ProcessPayloadBuilder.start()

                .withProcessDefinitionKey(SIMPLE_PROCESS)
                .withBusinessKey("business_key")
                .build();

        HttpEntity<StartProcessPayload> requestEntity = new HttpEntity<>(startProcess);

        ResponseEntity<CloudProcessInstance> entity = restTemplate.exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<CloudProcessInstance>() {
                });

        //then
        assertThat(entity).isNotNull();
        ProcessInstance returnedProcInst = entity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getInitiator()).isNotNull();
        assertThat(returnedProcInst.getInitiator()).isEqualTo("testadmin");//will only match if using username not id
        assertThat(returnedProcInst.getBusinessKey()).isEqualTo("business_key");
    }

    @Test
    public void shouldRetrieveProcessInstanceById() {

        //given
        ResponseEntity<CloudProcessInstance> startedProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when

        ResponseEntity<CloudProcessInstance> retrievedEntity = restTemplate.exchange(
                PROCESS_INSTANCES_RELATIVE_URL + startedProcessEntity.getBody().getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CloudProcessInstance>() {
                });

        //then
        assertThat(retrievedEntity.getBody()).isNotNull();
        assertThat(retrievedEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retrievedEntity.getBody().getId()).isNotNull();
    }

    @Test
    public void shouldRetrieveProcessInstanceDiagram() throws Exception {

        //given
        ResponseEntity<CloudProcessInstance> startedProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        String responseData = executeRequest(
                PROCESS_INSTANCES_RELATIVE_URL + startedProcessEntity.getBody()
                        .getId() + "/model",
                HttpMethod.GET,
                "image/svg+xml");

        //then
        assertThat(responseData).isNotNull();

        final InputStream byteArrayInputStream = new ByteArrayInputStream(TestResourceUtil.getProcessXml(startedProcessEntity.getBody()
                                                                                                                 .getProcessDefinitionId()
                                                                                                                 .split(":")[0]).getBytes());
        BpmnModel sourceModel = new BpmnXMLConverter().convertToBpmnModel(new InputStreamProvider() {

                                                                              @Override
                                                                              public InputStream getInputStream() {
                                                                                  return byteArrayInputStream;
                                                                              }
                                                                          },
                                                                          false,
                                                                          false);
        String activityFontName = processDiagramGenerator.getDefaultActivityFontName();
        String labelFontName = processDiagramGenerator.getDefaultLabelFontName();
        String annotationFontName = processDiagramGenerator.getDefaultAnnotationFontName();
        List<String> activityIds = Arrays.asList("sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94");
        try (InputStream is = processDiagramGenerator.generateDiagram(sourceModel,
                                                                      activityIds,
                                                                      Collections.emptyList(),
                                                                      activityFontName,
                                                                      labelFontName,
                                                                      annotationFontName)) {
            String sourceSvg = new String(IoUtil.readInputStream(is,
                                                                 null),
                                          "UTF-8");
            assertThat(responseData).isEqualTo(sourceSvg);
        }
    }

    @Test
    public void shouldRetrieveListOfProcessInstances() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page=0&size=2",
                                                                                                     HttpMethod.GET,
                                                                                                     null,
                                                                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                     });

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent()).hasSize(2);
        assertThat(processInstancesPage.getBody().getMetadata().getTotalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void shouldNotSeeProcessInstancesWithoutPermission() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //testadmin does not have access to SIMPLE_PROCESS according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        //when
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                });

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent()).hasSize(0);

        //but testadmin should see process instances at admin endpoint
        //when
        processInstancesPage = restTemplate
                .exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL + "?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                });

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent()).hasSize(2);
        assertThat(processInstancesPage.getBody().getMetadata().getTotalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void suspendShouldPutProcessInstanceInSuspendedState() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<Void> responseEntity = executeRequestSuspendProcess(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
    }

    private ResponseEntity<Void> executeRequestSuspendProcess(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId() + "/suspend",
                                                                    HttpMethod.POST,
                                                                    null,
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    @Test
    public void resumeShouldPutASuspendedProcessInstanceBackToActiveState() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        executeRequestSuspendProcess(startProcessEntity);

        //when
        ResponseEntity<Void> responseEntity = processInstanceRestTemplate.resume(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }

    private String executeRequest(String url,
                                  HttpMethod method,
                                  String contentType) {
        return restTemplate.execute(url,
                                    method,
                                    new RequestCallback() {
                                        @Override
                                        public void doWithRequest(ClientHttpRequest request) throws IOException {
                                            if (contentType != null && !contentType.isEmpty()) {
                                                request.getHeaders().add("Content-Type",
                                                                         contentType);
                                            }
                                        }
                                    },
                                    new ResponseExtractor<String>() {

                                        @Override
                                        public String extractData(ClientHttpResponse response)
                                                throws IOException {
                                            return new String(IoUtil.readInputStream(response.getBody(),
                                                                                     null),
                                                              "UTF-8");
                                        }
                                    });
    }
}