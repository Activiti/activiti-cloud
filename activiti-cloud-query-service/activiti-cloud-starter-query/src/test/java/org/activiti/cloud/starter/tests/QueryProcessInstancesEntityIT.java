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
package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.List;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryProcessInstancesEntityIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final String ADMIN_PROC_URL = "/admin/v1/process-instances";

    private static final ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceBuilder;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
    }

    @AfterEach
    public void tearDown() {
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcInstancesAndFilteredProcessInstances() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("first");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("second");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getName,
                                ProcessInstanceEntity::getStatus,
                                ProcessInstanceEntity::getProcessDefinitionName)
                    .contains(tuple(completedProcess.getId(),
                                    "first",
                                    ProcessInstance.ProcessInstanceStatus.COMPLETED,
                                    completedProcess.getProcessDefinitionName()),
                              tuple(runningProcess.getId(),
                                    "second",
                                    ProcessInstance.ProcessInstanceStatus.RUNNING,
                                    runningProcess.getProcessDefinitionName()));
        });

        await().untilAsserted(() -> {

            //and filter by status
            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(PROC_URL + "?status={status}",
                                                                                                                     HttpMethod.GET,
                                                                                                                     keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                                     PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
                                                                                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);

            //then
            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered.getBody().getContent();
            assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getStatus)
                    .containsExactly(tuple(completedProcess.getId(),
                                           ProcessInstance.ProcessInstanceStatus.COMPLETED));
        });
    }

    @Test
    public void shouldGetProcessWithUpdatedInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(process.getId(),
                                    ProcessInstance.ProcessInstanceStatus.RUNNING));
        });

        //when
        ProcessInstanceImpl updatedProcess = new ProcessInstanceImpl();
        updatedProcess.setId(process.getId());
        updatedProcess.setBusinessKey("businessKey");
        updatedProcess.setName("name");


        producer.send(new CloudProcessUpdatedEventImpl(updatedProcess));

        await().untilAsserted(() -> {

             ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                            HttpMethod.GET,
                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                            new ParameterizedTypeReference<ProcessInstance>() {
                                                                            });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();

            ProcessInstance responseProcess = responseEntity.getBody();
            assertThat(responseProcess.getBusinessKey()).isEqualTo(updatedProcess.getBusinessKey());
            assertThat(responseProcess.getName()).isEqualTo(updatedProcess.getName());

        });
    }


    @Test
    public void shouldGetAdminProcessInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");


        eventsAggregator.sendAll();


        await().untilAsserted(() -> {
             keycloakTokenProducer.setKeycloakTestUser("hradmin");

             ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(ADMIN_PROC_URL + "/" + process.getId(),
                                                                            HttpMethod.GET,
                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                            new ParameterizedTypeReference<ProcessInstance>() {
                                                                            });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();

            ProcessInstance responseProcess = responseEntity.getBody();
            assertThat(responseProcess.getId()).isEqualTo(process.getId());

        });
    }

    @Test
    public void shouldGetProcessDefinitionVersion() {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionName("process-definition-name");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process,
                                                       null,
                                                       null));


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);
            assertThat(responseEntity.getBody().getProcessDefinitionName()).isEqualTo("process-definition-name");


        });
    }

    @Test
    public void shouldSuspendResumeProcess() {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process,
                                                       null,
                                                       null));


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);

        });

        eventsAggregator.addEvents(new CloudProcessSuspendedEventImpl(process));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);
            assertThat(responseEntity.getBody().getProcessDefinitionKey()).isEqualTo("process-definition-key");
            assertThat(responseEntity.getBody().getStatus()).isEqualTo(ProcessInstanceStatus.SUSPENDED);

        });

        eventsAggregator.addEvents(new CloudProcessResumedEventImpl(process));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);
            assertThat(responseEntity.getBody().getProcessDefinitionKey()).isEqualTo("process-definition-key");
            assertThat(responseEntity.getBody().getStatus()).isEqualTo(ProcessInstanceStatus.RUNNING);

        });

    }

    @Test
    public void shouldGetProcessInstancesFilteredByNameDescription() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstancesFiltered("for filter",null);

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getName,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(completedProcess.getId(),
                                    completedProcess.getName(),
                                    ProcessInstance.ProcessInstanceStatus.COMPLETED));
        });
    }

    @Test
    public void shouldGetProcessInstancesAsAdmin() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        keycloakTokenProducer.setKeycloakTestUser("hradmin");
        await().untilAsserted(() -> {

            ResponseEntity<PagedModel<CloudProcessInstance>> responseEntity = testRestTemplate.exchange(ADMIN_PROC_URL + "?page=0&size=10",
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<PagedModel<CloudProcessInstance>>() {
                                                                                       });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .extracting(
                    CloudProcessInstance::getId,
                    CloudProcessInstance::getName,
                    CloudProcessInstance::getStatus,
                    CloudProcessInstance::getProcessDefinitionName)
                    .containsExactly(
                            tuple(completedProcess.getId(),
                                  completedProcess.getName(),
                                  ProcessInstanceStatus.COMPLETED,
                                  completedProcess.getProcessDefinitionName()),
                            tuple(runningProcess.getId(),
                                  runningProcess.getName(),
                                  ProcessInstanceStatus.RUNNING,
                                  runningProcess.getProcessDefinitionName())
                    );
        });
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstances() {

        return testRestTemplate.exchange(PROC_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }

    @Test
    public void shouldGetProcessInstancesFilteredByStartDate() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date nextDay = new Date();
        Date inThreeDays = new Date();
        Date threeDaysAgo = new Date();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        //set start date as current date + 1
        nextDay.setTime(now.getTime() + Duration.ofDays(1).toMillis());

        ProcessInstance processInstanceStartedNextDay = processInstanceBuilder
            .aRunningProcessInstanceWithStartDate("first", nextDay);

        inThreeDays.setTime(now.getTime() + Duration.ofDays(3).toMillis());
        ProcessInstance processInstanceStartedInThreeDays = processInstanceBuilder
            .aRunningProcessInstanceWithStartDate("second", inThreeDays);

        threeDaysAgo.setTime(now.getTime() - Duration.ofDays(3).toMillis());
        ProcessInstance processInstanceStartedThreeDaysAgo = processInstanceBuilder
            .aRunningProcessInstanceWithStartDate("third", threeDaysAgo);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            //set from date to current date
            Date fromDate = now;
            // to date, from date plus 2 days
            Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate
                .exchange(PROC_URL + "?startFrom=" + sdf.format(fromDate) + "&startTo=" + sdf
                        .format(toDate),
                    HttpMethod.GET,
                    keycloakTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);

            //then
            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                .getBody().getContent();
            assertThat(filteredProcessInstanceEntities)
                .extracting(ProcessInstanceEntity::getId,
                    ProcessInstanceEntity::getStatus)
                .containsExactly(tuple(processInstanceStartedNextDay.getId(),
                    ProcessInstanceStatus.RUNNING));
        });
    }

    @Test
    public void shouldGetProcessInstancesFilteredByCompletedDate() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date completedDateToday = new Date();
        Date completedDateTwoDaysAgo = new Date();
        Date completedDateFiveDaysAfter = new Date();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        //Start a process and set it's completed date as current date
        completedDateToday.setTime(now.getTime());
        ProcessInstance processInstanceCompletedToday = processInstanceBuilder
            .aRunningProcessInstanceWithCompletedDate("completedDateToday", completedDateToday);

        //Start a process and set it's completed date as current date minus two days
        completedDateTwoDaysAgo.setTime(now.getTime() - Duration.ofDays(2).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithCompletedDate("completedDateTwoDaysAgo", completedDateTwoDaysAgo);

        //Start a process and set it's completed date as current date plus five days
        completedDateFiveDaysAfter.setTime(now.getTime() + Duration.ofDays(5).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithCompletedDate("completedDateFiveDaysAfter", completedDateFiveDaysAfter);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            //set from date to yesterday date
            Date fromDate = new Date(now.getTime() - Duration.ofDays(1).toMillis());
            // to date, from date plus 2 days
            Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate
                .exchange(PROC_URL + "?completedFrom=" + sdf.format(fromDate) + "&completedTo=" + sdf
                        .format(toDate),
                    HttpMethod.GET,
                    keycloakTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);

            //then
            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                .getBody().getContent();
            assertThat(filteredProcessInstanceEntities)
                .extracting(ProcessInstanceEntity::getName)
                .containsExactly(processInstanceCompletedToday.getName());
        });
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstancesFiltered(String name,String description) {
        String url=PROC_URL;
        boolean add = false;
        if (name != null || description != null) {
            url += "?";
            if (name != null) {
                url += "name=" + name;
                add = true;
            }
            if (description != null) {
                if (add) {
                    url += "&";
                }
                url += "description=" + description;
            }

        }
        return testRestTemplate.exchange(url,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }

    @Test
    public void shouldGetProcessInstancesFilteredByInitiator() {
        
        ProcessInstance processInstanceInitiatorUser1 = processInstanceBuilder
                .aRunningProcessInstanceWithInitiator("first", "User1");
        ProcessInstance processInstanceInitiatorUser2 = processInstanceBuilder
                .aRunningProcessInstanceWithInitiator("second", "User2");
        ProcessInstance processInstanceInitiatorUser3 = processInstanceBuilder
                .aRunningProcessInstanceWithInitiator("third", "User3");
        eventsAggregator.sendAll();

        List<String> processInstanceIds = Arrays.asList(processInstanceInitiatorUser1.getId(),
                processInstanceInitiatorUser2.getId());

        shouldGetProcessInstancesFilteredBySingleValue(processInstanceInitiatorUser1.getId(), "initiator=User1");
        shouldGetProcessInstancesFilteredByList(processInstanceIds,"initiator=User1,User2");
    }

    @Test
    public void shouldGetProcessInstancesFilteredByAppVersion() {

        ProcessInstance processInstanceAppVersion1 = processInstanceBuilder
                .aRunningProcessInstanceWithAppVersion("first", "1");
        ProcessInstance processInstanceAppVersion2 = processInstanceBuilder
                .aRunningProcessInstanceWithAppVersion("second", "2");
        ProcessInstance processInstanceAppVersion3 = processInstanceBuilder
                .aRunningProcessInstanceWithAppVersion("third", "3");
        eventsAggregator.sendAll();
        
        List<String> processInstanceIds = List.of(processInstanceAppVersion1.getId(),
                processInstanceAppVersion2.getId());

        shouldGetProcessInstancesFilteredBySingleValue(processInstanceAppVersion1.getId(), "appVersion=1" );
        shouldGetProcessInstancesFilteredByList(processInstanceIds, "appVersion=1,2");
    }

    private void shouldGetProcessInstancesFilteredBySingleValue(String processId, String queryString) {
        shouldGetProcessInstancesFilteredByList(List.of(processId), queryString);
    }

    private void shouldGetProcessInstancesFilteredByList(List<String> processInstanceIds, String queryString) {
        await().untilAsserted(() -> {
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate
                    .exchange(PROC_URL + "?" + queryString,
                            HttpMethod.GET,
                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                            PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);

            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody().getContent();
            assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId)
                    .containsExactly(processInstanceIds.toArray(String[]::new));
        });
    }
}
