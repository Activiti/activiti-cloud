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
package org.activiti.cloud.starter.tests.runtime;

import java.io.ByteArrayInputStream;
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
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.converter.util.InputStreamProvider;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.util.TestResourceUtil;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ProcessInstanceIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String SUB_PROCESS = "SubProcess";
    private static final String PARENT_PROCESS = "ParentProcess";

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessDiagramGenerator processDiagramGenerator;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Autowired
    private RuntimeBundleProperties runtimeBundleProperties;

    @BeforeEach
    public void setUp() {
        keycloakTestUser = "hruser";
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = processDefinitionRestTemplate.getProcessDefinitions();
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
        assertThat(returnedProcInst.getServiceName()).isEqualTo(runtimeBundleProperties.getServiceName());
        assertThat(returnedProcInst.getServiceFullName()).isEqualTo(runtimeBundleProperties.getServiceFullName());
        assertThat(returnedProcInst.getServiceType()).isEqualTo(runtimeBundleProperties.getServiceType());
        assertThat(returnedProcInst.getServiceVersion()).isEqualTo(runtimeBundleProperties.getServiceVersion());
    }

    @Test
    public void shouldCreateProcessInstanceWithoutStartingIt() {
        //when
        ResponseEntity<CloudProcessInstance> entity = processInstanceRestTemplate.createProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        //then
        assertThat(entity).isNotNull();
        CloudProcessInstance returnedProcInst = entity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.CREATED);
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getInitiator()).isNotNull();
        assertThat(returnedProcInst.getInitiator()).isEqualTo(keycloakTestUser);//will only match if using username not id
        assertThat(returnedProcInst.getBusinessKey()).isEqualTo("business_key");
        assertThat(returnedProcInst.getAppName()).isEqualTo(runtimeBundleProperties.getAppName());
        assertThat(returnedProcInst.getServiceName()).isEqualTo(runtimeBundleProperties.getServiceName());
        assertThat(returnedProcInst.getServiceFullName()).isEqualTo(runtimeBundleProperties.getServiceFullName());
        assertThat(returnedProcInst.getServiceType()).isEqualTo(runtimeBundleProperties.getServiceType());
        assertThat(returnedProcInst.getServiceVersion()).isEqualTo(runtimeBundleProperties.getServiceVersion());
    }

    @Test
    public void shouldStartAnAlreadyCreatedProcess() {
        //when
        ResponseEntity<CloudProcessInstance> createdEntity = processInstanceRestTemplate.createProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");
        CloudProcessInstance createdProcInst = createdEntity.getBody();
        assertThat(createdProcInst).isNotNull();
        assertThat(createdProcInst.getId()).isNotNull();
        assertThat(createdProcInst.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.CREATED);

        ResponseEntity<CloudProcessInstance> startedEntity =
            processInstanceRestTemplate.startCreatedProcess(createdEntity.getBody().getId());

        //then
        assertThat(startedEntity).isNotNull();
        assertThat(startedEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        CloudProcessInstance startedProcInst = startedEntity.getBody();
        assertThat(startedProcInst).isNotNull();
        assertThat(startedProcInst.getId()).isNotNull();
        assertThat(startedProcInst.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
        assertThat(startedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(startedProcInst.getInitiator()).isNotNull();
        assertThat(startedProcInst.getInitiator()).isEqualTo(keycloakTestUser);//will only match if using username not id
        assertThat(startedProcInst.getBusinessKey()).isEqualTo("business_key");
        assertThat(startedProcInst.getAppName()).isEqualTo(runtimeBundleProperties.getAppName());
        assertThat(startedProcInst.getAppVersion()).isEqualTo("1");
        assertThat(startedProcInst.getServiceName()).isEqualTo(runtimeBundleProperties.getServiceName());
        assertThat(startedProcInst.getServiceFullName()).isEqualTo(runtimeBundleProperties.getServiceFullName());
        assertThat(startedProcInst.getServiceType()).isEqualTo(runtimeBundleProperties.getServiceType());
        assertThat(startedProcInst.getServiceVersion()).isEqualTo(runtimeBundleProperties.getServiceVersion());
    }

    @Test
    public void shouldThrowAnError_when_StartingAnAlreadyStartedProcess() {
        //when
        ResponseEntity<CloudProcessInstance> entity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        //then
        assertThat(entity).isNotNull();

        CloudProcessInstance startedProcessInstance = entity.getBody();
        ResponseEntity<ActivitiErrorMessageImpl> failEntity =
            processInstanceRestTemplate.startCreatedProcessFailing(startedProcessInstance.getId());
        assertThat(failEntity.getBody().getMessage()).isEqualTo("Process instance " + startedProcessInstance.getId() + " has already been started");
        assertThat(failEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
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

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
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

        ResponseEntity<CloudProcessInstance> entity = processInstanceRestTemplate.adminStartProcess(startProcess);

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
        ResponseEntity<CloudProcessInstance> retrievedEntity = processInstanceRestTemplate.getProcessInstance(startedProcessEntity);

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
        String responseData = processInstanceRestTemplate.getModel(startedProcessEntity.getBody().getId());

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
        ResponseEntity<PagedModel<ProcessInstance>> processInstancesPage = processInstanceRestTemplate.getPagedProcessInstances();

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
        ResponseEntity<PagedModel<ProcessInstance>> processInstancesPage = processInstanceRestTemplate.getPagedProcessInstances();

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent()).hasSize(0);

        //but testadmin should see process instances at admin endpoint
        //when
        processInstancesPage = processInstanceRestTemplate.adminGetPagedProcessInstances();

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
        ResponseEntity<Void> responseEntity = processInstanceRestTemplate.suspend(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
    }

    @Test
    public void adminSuspendShouldPutProcessInstanceInSuspendedState() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<Void> responseEntity = processInstanceRestTemplate.adminSuspend(startProcessEntity);

        //then
        //No User specified: should get an error, because admin endpoint
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        //when
        //testadmin should see process instances at admin endpoint
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        responseEntity = processInstanceRestTemplate.adminSuspend(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
    }

    @Test
    public void resumeShouldPutASuspendedProcessInstanceBackToActiveState() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.suspend(startProcessEntity);

        //when
        ResponseEntity<Void> responseEntity = processInstanceRestTemplate.resume(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
    }

    @Test
    public void adminResumeShouldPutASuspendedProcessInstanceBackToActiveState() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //First suspend process and check that everything is OK
        //testadmin should see process instances at admin endpoint
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<Void> responseEntity = processInstanceRestTemplate.adminSuspend(startProcessEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        //Check that process is really in a suspended state
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);

        //when
        //change user
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
        responseEntity = processInstanceRestTemplate.adminResume(startProcessEntity);

        //then
        //Bad user specified: should get an error, because admin endpoint
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        //when
        //testadmin should see process instances at admin endpoint
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        responseEntity = processInstanceRestTemplate.adminResume(startProcessEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        processInstanceEntity = processInstanceRestTemplate.getProcessInstance(startProcessEntity);
        assertThat(processInstanceEntity.getBody().getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
    }

    @Test
    public void shouldUpdateProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        assertThat(startProcessEntity).isNotNull();
        CloudProcessInstance returnedProcInst = startProcessEntity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getBusinessKey()).contains("business_key");

        //when
        String newBusinessKey = startProcessEntity.getBody().getBusinessKey() != null ? startProcessEntity.getBody().getBusinessKey() + " UPDATED" : " UPDATED";
        String newName = startProcessEntity.getBody().getName() != null ? startProcessEntity.getBody().getName() + " UPDATED" : " UPDATED";

        ResponseEntity<CloudProcessInstance> responseEntity = processInstanceRestTemplate.update(startProcessEntity,
            newBusinessKey,
            newName
        );

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.getProcessInstance(responseEntity);

        assertThat(processInstanceEntity.getBody().getBusinessKey()).isEqualTo(newBusinessKey);
        assertThat(processInstanceEntity.getBody().getName()).isEqualTo(newName);
    }

    @Test
    public void adminShouldUpdateProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        assertThat(startProcessEntity).isNotNull();
        CloudProcessInstance returnedProcInst = startProcessEntity.getBody();
        assertThat(returnedProcInst).isNotNull();
        assertThat(returnedProcInst.getId()).isNotNull();
        assertThat(returnedProcInst.getProcessDefinitionId()).contains("SimpleProcess:");
        assertThat(returnedProcInst.getBusinessKey()).contains("business_key");

        //when
        String newBusinessKey = startProcessEntity.getBody().getBusinessKey() != null ? startProcessEntity.getBody().getBusinessKey() + " UPDATED" : " UPDATED";
        String newName = startProcessEntity.getBody().getName() != null ? startProcessEntity.getBody().getName() + " UPDATED" : " UPDATED";

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        ResponseEntity<CloudProcessInstance> responseEntity = processInstanceRestTemplate.adminUpdate(startProcessEntity,
            newBusinessKey,
            newName
        );

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.adminGetProcessInstance(responseEntity);

        assertThat(processInstanceEntity.getBody().getBusinessKey()).isEqualTo(newBusinessKey);
        assertThat(processInstanceEntity.getBody().getName()).isEqualTo(newName);
    }

    @Test
    public void shouldGetSubprocesses() {

        //given
        ResponseEntity<CloudProcessInstance> startedProcessEntity = processInstanceRestTemplate.startProcessByKey(PARENT_PROCESS,
            null,
            "business_key");
        //when
        ResponseEntity<PagedModel<ProcessInstance>> processInstancesPage = processInstanceRestTemplate.getSubprocesses(startedProcessEntity.getBody().getId());

        //then
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody()).isNotNull();

        assertThat(processInstancesPage.getBody().getContent().size()).isEqualTo(1);

        assertThat(processInstancesPage.getBody().getContent().iterator().next().getProcessDefinitionKey()).isEqualTo(SUB_PROCESS);
    }

    @Test
    public void nonAdminShouldBeAbleToDeleteProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> processEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        assertThat(processEntity).isNotNull();
        assertThat(processEntity.getBody()).isNotNull();
        assertThat(processEntity.getBody().getId()).isNotNull();
        assertThat(processEntity.getBody().getProcessDefinitionId()).contains("SimpleProcess:");

        //when
        ResponseEntity<CloudProcessInstance> responseEntity = processInstanceRestTemplate.delete(processEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            processInstanceRestTemplate.getProcessInstance(processEntity));
    }

    @Test
    public void adminShouldDeleteProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> processEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null,
            "business_key");

        assertThat(processEntity).isNotNull();
        assertThat(processEntity.getBody()).isNotNull();
        assertThat(processEntity.getBody().getId()).isNotNull();
        assertThat(processEntity.getBody().getProcessDefinitionId()).contains("SimpleProcess:");

        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<CloudProcessInstance> responseEntity = processInstanceRestTemplate.adminDelete(processEntity);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            processInstanceRestTemplate.getProcessInstance(processEntity));
    }
}
