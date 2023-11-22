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

import static org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.common.error.attributes.ErrorAttributesMessageSanitizer;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.VariableEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QueryProcessInstancesEntityIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final String ADMIN_PROC_URL = "/admin/v1/process-instances";

    private static final ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>>() {};

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private BPMNSequenceFlowRepository sequenceFlowRepository;

    @Autowired
    private BPMNActivityRepository activityRepository;

    @Autowired
    private MyProducer producer;

    @Autowired
    private SubscribableChannel errorChannel;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceBuilder;

    private VariableEventContainedBuilder variableBuilder;

    private TaskEventContainedBuilder taskEventBuilder;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer).errorChannel(errorChannel);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        variableBuilder = new VariableEventContainedBuilder(eventsAggregator);
        taskEventBuilder = new TaskEventContainedBuilder(eventsAggregator);
        identityTokenProducer.withTestUser("testuser");
    }

    @AfterEach
    public void tearDown() {
        sequenceFlowRepository.deleteAll();
        activityRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcInstancesAndFilteredProcessInstances() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("first");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("second");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
                assertThat(processInstanceEntities)
                    .extracting(
                        ProcessInstanceEntity::getId,
                        ProcessInstanceEntity::getName,
                        ProcessInstanceEntity::getStatus,
                        ProcessInstanceEntity::getProcessDefinitionName
                    )
                    .contains(
                        tuple(
                            completedProcess.getId(),
                            "first",
                            ProcessInstance.ProcessInstanceStatus.COMPLETED,
                            completedProcess.getProcessDefinitionName()
                        ),
                        tuple(runningProcess.getId(), "second", RUNNING, runningProcess.getProcessDefinitionName())
                    );
            });

        await()
            .untilAsserted(() -> {
                //and filter by status
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?status={status}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
                    ProcessInstance.ProcessInstanceStatus.COMPLETED
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId, ProcessInstanceEntity::getStatus)
                    .containsExactly(tuple(completedProcess.getId(), ProcessInstance.ProcessInstanceStatus.COMPLETED));
            });
    }

    @Test
    public void shouldGetProcessWithUpdatedInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
                assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId, ProcessInstanceEntity::getStatus)
                    .contains(tuple(process.getId(), RUNNING));
            });

        //when
        ProcessInstanceImpl updatedProcess = new ProcessInstanceImpl();
        updatedProcess.setId(process.getId());
        updatedProcess.setBusinessKey("businessKey");
        updatedProcess.setName("name");

        producer.send(new CloudProcessUpdatedEventImpl(updatedProcess));

        await()
            .untilAsserted(() -> {
                ProcessInstance responseProcess = shouldGetProcessInstance(process.getId());
                assertThat(responseProcess.getBusinessKey()).isEqualTo(updatedProcess.getBusinessKey());
                assertThat(responseProcess.getName()).isEqualTo(updatedProcess.getName());
            });
    }

    @Test
    public void shouldGetAdminProcessInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                identityTokenProducer.withTestUser("hradmin");

                ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(
                    ADMIN_PROC_URL + "/" + process.getId(),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    new ParameterizedTypeReference<ProcessInstance>() {}
                );
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
        process.setInitiator("testuser");
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionName("process-definition-name");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process, null, null)
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                ProcessInstance responseProcess = shouldGetProcessInstance(process.getId());
                assertThat(responseProcess.getProcessDefinitionVersion()).isEqualTo(10);
                assertThat(responseProcess.getProcessDefinitionName()).isEqualTo("process-definition-name");
            });
    }

    @Test
    public void shouldSuspendResumeProcess() {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setInitiator("testuser");
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process, null, null)
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                ProcessInstance responseProcess = shouldGetProcessInstance(process.getId());
                assertThat(responseProcess.getProcessDefinitionVersion()).isEqualTo(10);
            });

        eventsAggregator.addEvents(new CloudProcessSuspendedEventImpl(process));

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                ProcessInstance responseProcess = shouldGetProcessInstance(process.getId());

                assertThat(responseProcess.getProcessDefinitionVersion()).isEqualTo(10);
                assertThat(responseProcess.getProcessDefinitionKey()).isEqualTo("process-definition-key");
                assertThat(responseProcess.getStatus()).isEqualTo(ProcessInstanceStatus.SUSPENDED);
            });

        eventsAggregator.addEvents(new CloudProcessResumedEventImpl(process));

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                ProcessInstance responseProcess = shouldGetProcessInstance(process.getId());

                assertThat(responseProcess.getProcessDefinitionVersion()).isEqualTo(10);
                assertThat(responseProcess.getProcessDefinitionKey()).isEqualTo("process-definition-key");
                assertThat(responseProcess.getStatus()).isEqualTo(RUNNING);
            });
    }

    @Test
    public void shouldGetProcessInstancesFilteredByNameDescription() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstancesFiltered(
                    "for filter",
                    null
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
                assertThat(processInstanceEntities)
                    .extracting(
                        ProcessInstanceEntity::getId,
                        ProcessInstanceEntity::getName,
                        ProcessInstanceEntity::getStatus
                    )
                    .contains(
                        tuple(
                            completedProcess.getId(),
                            completedProcess.getName(),
                            ProcessInstance.ProcessInstanceStatus.COMPLETED
                        )
                    );
            });
    }

    @Test
    public void shouldGetProcessInstancesAsAdmin() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("hradmin");
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<CloudProcessInstance>> responseEntity = testRestTemplate.exchange(
                    ADMIN_PROC_URL + "?page=0&size=10",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    new ParameterizedTypeReference<PagedModel<CloudProcessInstance>>() {}
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody())
                    .extracting(
                        CloudProcessInstance::getId,
                        CloudProcessInstance::getName,
                        CloudProcessInstance::getStatus,
                        CloudProcessInstance::getProcessDefinitionName
                    )
                    .containsExactly(
                        tuple(
                            completedProcess.getId(),
                            completedProcess.getName(),
                            ProcessInstanceStatus.COMPLETED,
                            completedProcess.getProcessDefinitionName()
                        ),
                        tuple(
                            runningProcess.getId(),
                            runningProcess.getName(),
                            RUNNING,
                            runningProcess.getProcessDefinitionName()
                        )
                    );
            });
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstances() {
        return testRestTemplate.exchange(
            PROC_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
        );
    }

    private ResponseEntity<ProcessInstance> executeRequestGetProcessInstance(String processInstanceId) {
        return testRestTemplate.exchange(
            PROC_URL + "/" + processInstanceId,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            new ParameterizedTypeReference<ProcessInstance>() {}
        );
    }

    private ProcessInstance shouldGetProcessInstance(String processInstanceId) {
        ResponseEntity<ProcessInstance> responseEntity = executeRequestGetProcessInstance(processInstanceId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();

        ProcessInstance responseProcess = responseEntity.getBody();
        assertThat(responseProcess.getId()).isEqualTo(processInstanceId);
        return responseProcess;
    }

    private void shouldNotGetProcessInstance(String processInstanceId) {
        ResponseEntity<ProcessInstance> responseEntity = executeRequestGetProcessInstance(processInstanceId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

        ProcessInstance processInstanceStartedNextDay = processInstanceBuilder.aRunningProcessInstanceWithStartDate(
            "first",
            nextDay
        );

        inThreeDays.setTime(now.getTime() + Duration.ofDays(3).toMillis());
        ProcessInstance processInstanceStartedInThreeDays = processInstanceBuilder.aRunningProcessInstanceWithStartDate(
            "second",
            inThreeDays
        );

        threeDaysAgo.setTime(now.getTime() - Duration.ofDays(3).toMillis());
        ProcessInstance processInstanceStartedThreeDaysAgo = processInstanceBuilder.aRunningProcessInstanceWithStartDate(
            "third",
            threeDaysAgo
        );

        eventsAggregator.sendAll();

        // Filter using date range
        await()
            .untilAsserted(() -> {
                //when
                //set from date to current date
                Date fromDate = now;
                // to date, from date plus 2 days
                Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?startFrom=" + sdf.format(fromDate) + "&startTo=" + sdf.format(toDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId, ProcessInstanceEntity::getStatus)
                    .containsExactly(tuple(processInstanceStartedNextDay.getId(), RUNNING));
            });

        // Filter using static date
        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?startDate=" + sdf.format(nextDay),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId, ProcessInstanceEntity::getStatus)
                    .containsExactly(tuple(processInstanceStartedNextDay.getId(), RUNNING));
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
        ProcessInstance processInstanceCompletedToday = processInstanceBuilder.aRunningProcessInstanceWithCompletedDate(
            "completedDateToday",
            completedDateToday
        );

        //Start a process and set it's completed date as current date minus two days
        completedDateTwoDaysAgo.setTime(now.getTime() - Duration.ofDays(2).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithCompletedDate(
            "completedDateTwoDaysAgo",
            completedDateTwoDaysAgo
        );

        //Start a process and set it's completed date as current date plus five days
        completedDateFiveDaysAfter.setTime(now.getTime() + Duration.ofDays(5).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithCompletedDate(
            "completedDateFiveDaysAfter",
            completedDateFiveDaysAfter
        );

        eventsAggregator.sendAll();

        // Filter using date range
        await()
            .untilAsserted(() -> {
                //when
                //set from date to yesterday date
                Date fromDate = new Date(now.getTime() - Duration.ofDays(1).toMillis());
                // to date, from date plus 2 days
                Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?completedFrom=" + sdf.format(fromDate) + "&completedTo=" + sdf.format(toDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getName)
                    .containsExactly(processInstanceCompletedToday.getName());
            });

        // Filter using static date
        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?completedDate=" + sdf.format(completedDateToday),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getName)
                    .containsExactly(processInstanceCompletedToday.getName());
            });
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstancesFiltered(
        String name,
        String description
    ) {
        String url = PROC_URL;
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
        return testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
        );
    }

    @Test
    public void shouldGetProcessInstancesInitiatedByCurrentUser() {
        ProcessInstance processInstanceCurrentUserInitiator = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "currentUser",
            "testuser"
        );
        processInstanceBuilder.aRunningProcessInstanceWithInitiator("first", "User1");
        processInstanceBuilder.aRunningProcessInstanceWithInitiator("second", "User2");
        eventsAggregator.sendAll();

        shouldGetProcessInstancesFilteredBySingleValue(
            processInstanceCurrentUserInitiator.getId(),
            "initiator=testuser"
        );
        shouldGetProcessInstancesFilteredBySingleValue(
            processInstanceCurrentUserInitiator.getId(),
            "initiator=User1,testuser"
        );
        shouldGetProcessInstancesFilteredByList(Collections.emptyList(), "initiator=User1,User2");
    }

    @Test
    public void shouldGetProcessInstanceInitiatedByCurrentUser() {
        ProcessInstance processInstanceCurrentUserInitiator = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "currentUser",
            "testuser"
        );
        ProcessInstance processInstanceInitiatedByUser1 = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "first",
            "User1"
        );
        eventsAggregator.sendAll();

        shouldGetProcessInstance(processInstanceCurrentUserInitiator.getId());
        shouldNotGetProcessInstance(processInstanceInitiatedByUser1.getId());
    }

    @Test
    public void shouldGetProcessInstancesWhenCurrentUserIsTaskAssignee() {
        ProcessInstance firstProcessInstanceWithTestUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "testuser", firstProcessInstanceWithTestUserAssignedToATask);

        ProcessInstance secondProcessInstanceWithTestUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "testuser", secondProcessInstanceWithTestUserAssignedToATask);

        ProcessInstance thirdProcessInstanceWithRandomUserAssignedToTask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "arandomuser", thirdProcessInstanceWithRandomUserAssignedToTask);

        eventsAggregator.sendAll();

        shouldGetProcessInstancesList(
            List.of(
                firstProcessInstanceWithTestUserAssignedToATask.getId(),
                secondProcessInstanceWithTestUserAssignedToATask.getId()
            )
        );
    }

    @Test
    public void shouldGetProcessInstancesWhenCurrentUserIsTaskCandidate() {
        ProcessInstance firstProcessInstanceWithTestUserCandidateOfATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate("ATask", "testuser", firstProcessInstanceWithTestUserCandidateOfATask);

        ProcessInstance secondProcessInstanceWithTestUserCandidateOfATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate("ATask", "testuser", secondProcessInstanceWithTestUserCandidateOfATask);

        ProcessInstance thirdProcessInstanceWithRandomUserCandidateOfATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate(
            "ATask",
            "arandomuser",
            thirdProcessInstanceWithRandomUserCandidateOfATask
        );

        eventsAggregator.sendAll();

        shouldGetProcessInstancesList(
            List.of(
                firstProcessInstanceWithTestUserCandidateOfATask.getId(),
                secondProcessInstanceWithTestUserCandidateOfATask.getId()
            )
        );
    }

    @Test
    public void shouldGetProcessInstancesWhenCurrentUserIsTaskAssigneeOrCandidate() {
        ProcessInstance firstProcessInstanceWithTestUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "testuser", firstProcessInstanceWithTestUserAssignedToATask);

        ProcessInstance secondProcessInstanceWithTestUserCandidateOfATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate("ATask", "testuser", secondProcessInstanceWithTestUserCandidateOfATask);

        ProcessInstance thirdProcessInstanceWithRandomUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "arandomuser", thirdProcessInstanceWithRandomUserAssignedToATask);

        eventsAggregator.sendAll();

        shouldGetProcessInstancesList(
            List.of(
                firstProcessInstanceWithTestUserAssignedToATask.getId(),
                secondProcessInstanceWithTestUserCandidateOfATask.getId()
            )
        );
    }

    @Test
    public void shouldGetProcessInstancesWhenCurrentUserIsInitiatorOrTaskAssigneeOrCandidate() {
        ProcessInstance firstProcessInstanceWithTestUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "testuser", firstProcessInstanceWithTestUserAssignedToATask);

        ProcessInstance secondProcessInstanceWithTestUserCandidateOfATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate("ATask", "testuser", secondProcessInstanceWithTestUserCandidateOfATask);

        ProcessInstance thirdProcessInstanceWithRandomUserAssignedToATask = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "arandomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "arandomuser", thirdProcessInstanceWithRandomUserAssignedToATask);

        ProcessInstance fourthProcessInstanceWithTestUserAsInitiator = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "process",
            "testuser"
        );

        eventsAggregator.sendAll();

        shouldGetProcessInstancesList(
            List.of(
                firstProcessInstanceWithTestUserAssignedToATask.getId(),
                secondProcessInstanceWithTestUserCandidateOfATask.getId(),
                fourthProcessInstanceWithTestUserAsInitiator.getId()
            )
        );
    }

    @Test
    public void shouldGetProcessInstanceWhenCurrentUserIsTaskAssignee() {
        ProcessInstance runningProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "auser",
            "randomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "testuser", runningProcessInstance);

        eventsAggregator.sendAll();

        shouldGetProcessInstance(runningProcessInstance.getId());
    }

    @Test
    public void shouldGetProcessInstanceWhenCurrentUserIsTaskCandidateUser() {
        ProcessInstance runningProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "auser",
            "randomuser"
        );

        taskEventBuilder.aTaskWithUserCandidate("ATask", "testuser", runningProcessInstance);

        eventsAggregator.sendAll();

        shouldGetProcessInstance(runningProcessInstance.getId());
    }

    @Test
    public void shouldGetProcessInstanceWhenCurrentUserIsMemberOfTaskCandidateGroup() {
        ProcessInstance runningProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "auser",
            "randomuser"
        );

        taskEventBuilder.aTaskWithGroupCandidate("ATask", "testgroup", runningProcessInstance);

        eventsAggregator.sendAll();

        shouldGetProcessInstance(runningProcessInstance.getId());
    }

    @Test
    public void shouldNotGetProcessInstanceWhenCurrentUserIsNotInitiatorOrTaskInvolved() {
        ProcessInstance runningProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "auser",
            "randomuser"
        );

        taskEventBuilder.anAssignedTask("ATask", "anotheruser", runningProcessInstance);

        eventsAggregator.sendAll();

        shouldNotGetProcessInstance(runningProcessInstance.getId());
    }

    @Test
    public void shouldGetProcessInstancesFilteredByAppVersion() {
        ProcessInstance processInstanceAppVersion1 = processInstanceBuilder.aRunningProcessInstanceWithAppVersion(
            "first",
            "1"
        );
        ProcessInstance processInstanceAppVersion2 = processInstanceBuilder.aRunningProcessInstanceWithAppVersion(
            "second",
            "2"
        );
        processInstanceBuilder.aRunningProcessInstanceWithAppVersion("third", "3");
        eventsAggregator.sendAll();

        List<String> processInstanceIds = List.of(
            processInstanceAppVersion1.getId(),
            processInstanceAppVersion2.getId()
        );

        shouldGetProcessInstancesFilteredBySingleValue(processInstanceAppVersion1.getId(), "appVersion=1");
        shouldGetProcessInstancesFilteredByList(processInstanceIds, "appVersion=1,2");
    }

    @Test
    public void shouldGetProcessInstancesFilteredBySuspendedDate() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date suspendedDateToday = new Date();
        Date suspendedDateTwoDaysAgo = new Date();
        Date suspendedDateFiveDaysAfter = new Date();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        //Start a process and set it's suspended date as current date
        suspendedDateToday.setTime(now.getTime());
        ProcessInstance processInstanceSuspendedToday = processInstanceBuilder.aRunningProcessInstanceWithSuspendedDate(
            "suspendedDateToday",
            suspendedDateToday
        );

        //Start a process and set it's suspended date as current date minus two days
        suspendedDateTwoDaysAgo.setTime(now.getTime() - Duration.ofDays(2).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithSuspendedDate(
            "suspendedDateTwoDaysAgo",
            suspendedDateTwoDaysAgo
        );

        //Start a process and set it's suspended date as current date plus five days
        suspendedDateFiveDaysAfter.setTime(now.getTime() + Duration.ofDays(5).toMillis());
        processInstanceBuilder.aRunningProcessInstanceWithSuspendedDate(
            "suspendedDateFiveDaysAfter",
            suspendedDateFiveDaysAfter
        );

        eventsAggregator.sendAll();

        // Filter using date range
        await()
            .untilAsserted(() -> {
                //when
                //set from date to yesterday date
                Date fromDate = new Date(now.getTime() - Duration.ofDays(1).toMillis());
                // to date, from date plus 2 days
                Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?suspendedFrom=" + sdf.format(fromDate) + "&suspendedTo=" + sdf.format(toDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getName)
                    .containsExactly(processInstanceSuspendedToday.getName());
            });

        // Filter using static date
        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    PROC_URL + "?suspendedDate=" + sdf.format(suspendedDateToday),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getName)
                    .containsExactly(processInstanceSuspendedToday.getName());
            });
    }

    private void shouldGetProcessInstancesFilteredBySingleValue(String processId, String queryString) {
        shouldGetProcessInstancesFilteredByList(List.of(processId), queryString);
    }

    private void shouldGetProcessInstancesList(List<String> processInstanceIds, String url) {
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE
                );

                assertThat(responseEntityFiltered).isNotNull();
                assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered
                    .getBody()
                    .getContent();
                assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId)
                    .containsExactly(processInstanceIds.toArray(String[]::new));
            });
    }

    private void shouldGetProcessInstancesList(List<String> processInstanceIds) {
        shouldGetProcessInstancesList(processInstanceIds, PROC_URL);
    }

    private void shouldGetProcessInstancesFilteredByList(List<String> processInstanceIds, String queryString) {
        shouldGetProcessInstancesList(processInstanceIds, PROC_URL + "?" + queryString);
    }

    @ParameterizedTest
    @MethodSource({ "processInstanceWithVariablesData" })
    void should_getProcessInstanceWithVariables(
        String variableKeys,
        int expectedSize,
        List<String> expectedVariableValues
    ) {
        ProcessInstance runningProcess1 = processInstanceBuilder.aRunningProcessInstance("first");
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcess1);
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcess1);
        eventsAggregator.sendAll();

        ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
            PROC_URL + (variableKeys == null ? "" : "?variableKeys=" + variableKeys),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
            RUNNING
        );
        assertThat(responseEntityFiltered.getBody().getContent())
            .flatExtracting(ProcessInstanceEntity::getVariables)
            .hasSize(expectedSize);
        if (expectedSize > 0) {
            assertThat(responseEntityFiltered.getBody().getContent())
                .flatExtracting(ProcessInstanceEntity::getVariables)
                .extracting(AbstractVariableEntity::getValue)
                .containsExactlyInAnyOrderElementsOf(expectedVariableValues);
        }
    }

    public static Stream<Arguments> processInstanceWithVariablesData() {
        return Stream.of(
            Arguments.of("varAProcessDefinitionKey/varAName", 1, List.of("varAValue")),
            Arguments.of("varBProcessDefinitionKey/varBName", 1, List.of("varBValue")),
            Arguments.of(
                "varAProcessDefinitionKey/varAName,varBProcessDefinitionKey/varBName",
                2,
                List.of("varAValue", "varBValue")
            ),
            Arguments.of("other", 0, null),
            Arguments.of(null, 0, null)
        );
    }

    @Test
    void should_getAllProcessInstancesWithVariables() {
        ProcessInstance runningProcess1 = processInstanceBuilder.aRunningProcessInstance("first");
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "111", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcess1);
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcess1);
        ProcessInstance runningProcess2 = processInstanceBuilder.aRunningProcessInstance("second");
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "222", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcess2);
        variableBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcess2);
        eventsAggregator.sendAll();

        ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(
            PROC_URL + "?variableKeys=varAProcessDefinitionKey/varAName",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
            RUNNING
        );
        assertThat(responseEntityFiltered.getBody().getContent())
            .flatExtracting(ProcessInstanceEntity::getVariables)
            .hasSize(2);
        assertThat(responseEntityFiltered.getBody().getContent())
            .flatExtracting(ProcessInstanceEntity::getVariables)
            .extracting(AbstractVariableEntity::getValue)
            .containsExactly("111", "222");
    }

    @Test
    void should_containMessageNotDisclosed_whenExceptionMessageIsNotHandled() {
        ResponseEntity<ActivitiErrorMessageImpl> responseEntity = testRestTemplate.exchange(
            PROC_URL + "?startDate=2022-14-14T000000",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            new ParameterizedTypeReference<ActivitiErrorMessageImpl>() {}
        );

        assertThat(responseEntity.getBody().getMessage())
            .isEqualTo(ErrorAttributesMessageSanitizer.ERROR_NOT_DISCLOSED_MESSAGE);
    }

    @Test
    @Transactional
    void should_handleDuplicateSimpleProcessInstanceEvents() {
        // given
        var simpleProcessInstance = processInstanceBuilder.startSimpleProcessInstance("sampleDefinitionId");

        // when
        var sentEvents = eventsAggregator.sendAll();

        // then
        assertThat(eventsAggregator.getException()).isNull();
        assertThat(processInstanceRepository.findById(simpleProcessInstance.getId()))
            .isNotEmpty()
            .get()
            .extracting(ProcessInstanceEntity::getStatus)
            .isEqualTo(RUNNING);

        assertThat(sequenceFlowRepository.findAll())
            .filteredOn(it -> simpleProcessInstance.getId().equals(it.getProcessInstanceId()))
            .extracting(
                BPMNSequenceFlowEntity::getElementId,
                BPMNSequenceFlowEntity::getSourceActivityElementId,
                BPMNSequenceFlowEntity::getTargetActivityElementId
            )
            .containsExactly(
                tuple(
                    "sid-68945AF1-396F-4B8A-B836-FC318F62313F",
                    "startEvent1",
                    "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94"
                )
            );

        assertThat(activityRepository.findAll())
            .filteredOn(it -> simpleProcessInstance.getId().equals(it.getProcessInstanceId()))
            .extracting(
                BPMNActivityEntity::getElementId,
                BPMNActivityEntity::getActivityName,
                BPMNActivityEntity::getActivityType
            )
            .containsExactly(
                tuple("startEvent1", "", "startEvent"),
                tuple("sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94", "Perform Action", "userTask")
            );

        // and when duplicates are sent
        eventsAggregator.addEvents(sentEvents).sendAll();

        // and then
        assertThat(eventsAggregator.getException()).isNull();
    }
}
