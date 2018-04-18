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

package org.activiti.cloud.services.events.listeners;

import java.io.IOException;
import java.io.InputStream;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ActivityCancelledEventImpl;
import org.activiti.cloud.services.events.ActivityCompletedEventImpl;
import org.activiti.cloud.services.events.ActivityStartedEventImpl;
import org.activiti.cloud.services.events.ProcessActivatedEventImpl;
import org.activiti.cloud.services.events.ProcessCancelledEventImpl;
import org.activiti.cloud.services.events.ProcessCreatedEventImpl;
import org.activiti.cloud.services.events.ProcessStartedEventImpl;
import org.activiti.cloud.services.events.ProcessSuspendedEventImpl;
import org.activiti.cloud.services.events.SequenceFlowTakenEventImpl;
import org.activiti.cloud.services.events.TaskActivatedEventImpl;
import org.activiti.cloud.services.events.TaskCreatedEventImpl;
import org.activiti.cloud.services.events.TaskSuspendedEventImpl;
import org.activiti.cloud.services.events.tests.util.MockMessageChannel;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MessageProducerActivitiEventActivateSuspendIT.ContextConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {MessageProducerActivitiEventActivateSuspendIT.class, MessageProducerActivitiEventListener.class, MessageProducerCommandContextCloseListener.class})
public class MessageProducerActivitiEventActivateSuspendIT {

    @Autowired
    private MessageProducerActivitiEventListener eventListener;

    @Configuration
    @ComponentScan(
            {
                    "org.activiti.cloud.services.events.tests.util",
                    "org.activiti.cloud.services.events.converter",
                    "org.activiti.cloud.services.events.builders",
                    "org.activiti.cloud.services.api.model.converter",
                    "org.activiti.cloud.services.events.listeners"
            })
    public class ContextConfig {

    }

    @Test
    public void testActivitiEventsSuspendAndActivateProcessInstance() throws Exception {
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE)
                .buildProcessEngine();
        deploy("SimpleUserTaskProcess",
               processEngine);

        processEngine.getRuntimeService().addEventListener(eventListener);

        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("simpleUserTaskProcess");

        ProcessEngineEvent[] events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();

        assertThat(events.length).isEqualTo(7);
        assertThat(events[0].getClass()).isEqualTo(ProcessCreatedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(ProcessStartedEventImpl.class);
        assertThat(events[2].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[3].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[4].getClass()).isEqualTo(SequenceFlowTakenEventImpl.class);
        assertThat(events[5].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[6].getClass()).isEqualTo(TaskCreatedEventImpl.class);

        processEngine.getRuntimeService().suspendProcessInstanceById(processInstance.getId());
        events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        for (ProcessEngineEvent e : events) {
            System.out.println(e);
        }

        assertThat(events.length).isEqualTo(2);
        assertThat(events[0].getClass()).isEqualTo(ProcessSuspendedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(TaskSuspendedEventImpl.class);

        processEngine.getRuntimeService().activateProcessInstanceById(processInstance.getId());
        events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();

        assertThat(events.length).isEqualTo(2);
        assertThat(events[0].getClass()).isEqualTo(ProcessActivatedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(TaskActivatedEventImpl.class);
    }

    @Test
    public void testActivitiEventsDeleteProcessInstance() throws Exception {
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE)
                .buildProcessEngine();
        deploy("SimpleUserTaskProcess",
               processEngine);
        processEngine.getRuntimeService().addEventListener(eventListener);

        ProcessInstance processInstance =
                processEngine.getRuntimeService().startProcessInstanceByKey("simpleUserTaskProcess");
        processEngine.getRuntimeService().deleteProcessInstance(processInstance.getId(),
                                                                "test");

        ProcessEngineEvent[] events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        assertThat(events.length).isEqualTo(2);
        assertThat(events[0]).isInstanceOf(ActivityCancelledEventImpl.class);
        assertThat(events[1]).isInstanceOf(ProcessCancelledEventImpl.class);
    }

    public static void deploy(final String processDefinitionKey,
                              ProcessEngine processEngine) throws IOException {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("processes/" + processDefinitionKey + ".bpmn")) {
            processEngine.getRepositoryService()
                    .createDeployment()
                    .addInputStream(processDefinitionKey + ".bpmn",
                                    is)
                    .deploy();
        }
    }
}