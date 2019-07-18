/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.tests.services.audit;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.RUNTIME_BUNDLE_INFO_HEADERS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TimerAuditProducerIT {

    private static final String PROCESS_INTERMEDIATE_TIMER_EVENT = "intermediateTimerEventExample";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;
    
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    AsyncExecutor asyncExecutor;

    private Logger logger = LoggerFactory.getLogger(TimerAuditProducerIT.class);
    
    @TestConfiguration
    static class JobExecutorITProcessEngineConfigurer implements ProcessEngineConfigurationConfigurer {
        
        @Override
        public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
            processEngineConfiguration.setAsyncExecutorDefaultTimerJobAcquireWaitTime(100);
            processEngineConfiguration.setAsyncExecutorDefaultAsyncJobAcquireWaitTime(100);
            processEngineConfiguration.setAsyncExecutorActivate(true);
        }
    }
    
    @After
    public void tearDown() {
        processEngineConfiguration.getClock().reset();
    }

    @Test
    public void shouldProduceEventsForIntermediateTimerEvent() {

        logger.info("Async config: " + asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis());

        //given
        Date startTime = new Date();
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(
                new StartProcessPayloadBuilder()
                        .withProcessDefinitionKey(PROCESS_INTERMEDIATE_TIMER_EVENT)
                        .withName("processInstanceName")
                        .withBusinessKey("businessKey")
                        .build());
 
        //when
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting( CloudRuntimeEvent::getEventType,
                                 CloudRuntimeEvent::getEntityId)
                    .contains(
                            tuple(ACTIVITY_STARTED,
                                  "timer"),
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED,
                                  "timer")
                    );
            
            List<CloudBPMNTimerScheduledEvent> timerEvents = receivedEvents
                    .stream()
                    .filter(CloudBPMNTimerScheduledEvent.class::isInstance)
                    .map(CloudBPMNTimerScheduledEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(timerEvents)
                    .extracting( CloudRuntimeEvent::getEventType,
                                 CloudRuntimeEvent::getBusinessKey,
                                 CloudRuntimeEvent::getProcessDefinitionId,
                                 CloudRuntimeEvent::getProcessInstanceId,
                                 CloudRuntimeEvent::getProcessDefinitionKey,
                                 CloudRuntimeEvent::getProcessDefinitionVersion,
                                 event -> event.getEntity().getProcessDefinitionId(),
                                 event -> event.getEntity().getProcessInstanceId(),
                                 event -> event.getEntityId()
                    )
                    .contains(
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED,
                                  "businessKey",
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  startProcessEntity.getBody().getProcessDefinitionKey(),
                                  startProcessEntity.getBody().getProcessDefinitionVersion(),
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  "timer" 
                            )
                    );
        });
           
        //when
        long waitTime = 5 * 60 * 1000;
        Date dueDate = new Date(startTime.getTime() + waitTime);

        // After setting the clock to time '5minutes and 5 seconds', the second timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(dueDate.getTime() + 5000));
       
        //when
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting( CloudRuntimeEvent::getEventType,
                                 CloudRuntimeEvent::getEntityId)
                    .contains( 
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_FIRED,
                                  "timer"),
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_EXECUTED,
                                  "timer"),
                            tuple(ACTIVITY_COMPLETED,
                                  "timer")
                    );
            
            List<CloudBPMNTimerEvent> timerEvents = receivedEvents
                    .stream()
                    .filter(event -> (CloudBPMNTimerFiredEvent.class.isInstance(event) ||
                                      CloudBPMNTimerExecutedEvent.class.isInstance(event)))
                    .map(CloudBPMNTimerEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(timerEvents)
                    .extracting( CloudRuntimeEvent::getEventType,
                                 CloudRuntimeEvent::getBusinessKey,
                                 CloudRuntimeEvent::getProcessDefinitionId,
                                 CloudRuntimeEvent::getProcessInstanceId,
                                 CloudRuntimeEvent::getProcessDefinitionKey,
                                 CloudRuntimeEvent::getProcessDefinitionVersion,
                                 event -> event.getEntity().getProcessDefinitionId(),
                                 event -> event.getEntity().getProcessInstanceId(),
                                 event -> event.getEntityId()
                    )
                    .containsOnly(
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_FIRED,
                                  "businessKey",
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  startProcessEntity.getBody().getProcessDefinitionKey(),
                                  startProcessEntity.getBody().getProcessDefinitionVersion(),
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  "timer"),
                            tuple(BPMNTimerEvent.TimerEvents.TIMER_EXECUTED,
                                  "businessKey",
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  startProcessEntity.getBody().getProcessDefinitionKey(),
                                  startProcessEntity.getBody().getProcessDefinitionVersion(),
                                  startProcessEntity.getBody().getProcessDefinitionId(),
                                  startProcessEntity.getBody().getId(),
                                  "timer")
                    );
        });        
    }
    
    @Test
    public void shouldGetTimerCanceledEventByProcessDelete() {
        // GIVEN
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
                                                                                                           .withProcessDefinitionKey(PROCESS_INTERMEDIATE_TIMER_EVENT)
                                                                                                           .withName("processInstanceName")
                                                                                                           .withBusinessKey("businessKey")
                                                                                                           .build());
        // WHEN
        processInstanceRestTemplate.delete(startProcessEntity);
        
        //when
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            List<CloudBPMNTimerEvent> timerEvents = receivedEvents
                    .stream()
                    .filter(CloudBPMNTimerCancelledEvent.class::isInstance)
                    .map(CloudBPMNTimerEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(timerEvents)
            .extracting( CloudRuntimeEvent::getEventType,
                         CloudRuntimeEvent::getBusinessKey,
                         CloudRuntimeEvent::getProcessDefinitionId,
                         CloudRuntimeEvent::getProcessInstanceId,
                         CloudRuntimeEvent::getProcessDefinitionKey,
                         CloudRuntimeEvent::getProcessDefinitionVersion,
                         event -> event.getEntity().getProcessDefinitionId(),
                         event -> event.getEntity().getProcessInstanceId(),
                         event -> event.getEntityId()
            )
            .contains(
                    tuple(BPMNTimerEvent.TimerEvents.TIMER_CANCELLED,
                          "businessKey",
                          startProcessEntity.getBody().getProcessDefinitionId(),
                          startProcessEntity.getBody().getId(),
                          startProcessEntity.getBody().getProcessDefinitionKey(),
                          startProcessEntity.getBody().getProcessDefinitionVersion(),
                          startProcessEntity.getBody().getProcessDefinitionId(),
                          startProcessEntity.getBody().getId(),
                          "timer")
            );
        });
    }

}
