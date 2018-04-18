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
import org.activiti.cloud.services.events.ActivityCompletedEventImpl;
import org.activiti.cloud.services.events.ActivityStartedEventImpl;
import org.activiti.cloud.services.events.ProcessCompletedEventImpl;
import org.activiti.cloud.services.events.ProcessCreatedEventImpl;
import org.activiti.cloud.services.events.ProcessStartedEventImpl;
import org.activiti.cloud.services.events.SequenceFlowTakenEventImpl;
import org.activiti.cloud.services.events.TaskAssignedEventImpl;
import org.activiti.cloud.services.events.TaskCreatedEventImpl;
import org.activiti.cloud.services.events.tests.util.MockMessageChannel;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.task.Task;
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
@ContextConfiguration(classes = MessageProducerActivitiEventListenerIT.ContextConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {MessageProducerActivitiEventListenerIT.class, MessageProducerActivitiEventListener.class, MessageProducerCommandContextCloseListener.class})
public class MessageProducerActivitiEventListenerIT {

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
    public void executeListener() throws Exception {
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE).buildProcessEngine();
        deploy("SimpleProcess", processEngine);
        deploy("RollbackProcess", processEngine);
        deploy("AsyncErrorProcess", processEngine);
        processEngine.getRuntimeService().addEventListener(eventListener);
        processEngine.getRuntimeService().startProcessInstanceByKey("simpleProcess");

        ProcessEngineEvent[] events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        assertThat(events.length).isEqualTo(8);
        assertThat(events[0].getClass()).isEqualTo(ProcessCreatedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(ProcessStartedEventImpl.class);
        assertThat(events[2].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[3].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[4].getClass()).isEqualTo(SequenceFlowTakenEventImpl.class);
        assertThat(events[5].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[6].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[7].getClass()).isEqualTo(ProcessCompletedEventImpl.class);

        MockMessageChannel.messageResult = null;
        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("rollbackProcess");
        } catch (Exception e) {
            //nothing to do
        }
        assertThat(MockMessageChannel.messageResult).isEqualTo(null);

        MockMessageChannel.messageResult = null;
        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("asyncErrorProcess");
        } catch (Exception e) {
            //nothing to do
        }
        assertThat(MockMessageChannel.messageResult).isNotNull();
        events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        assertThat(events.length).isEqualTo(5);
        assertThat(events[0].getClass()).isEqualTo(ProcessCreatedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(ProcessStartedEventImpl.class);
        assertThat(events[2].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[3].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[4].getClass()).isEqualTo(SequenceFlowTakenEventImpl.class);
    }

    /**
     * This test is here for the default behavior of the engine when a new standalone task is created
     * First is send the TASK_ASSIGNED event then the TASK_CREATED event
     * Note: If it's decided to change the behavior this test should fail
     * @throws Exception
     */
    @Test
    public void executeListenerForTaskCreated() throws Exception {
        // given
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE).buildProcessEngine();
        processEngine.getRuntimeService().addEventListener(eventListener);

        // when
        final Task newTask = processEngine.getTaskService().newTask();
        newTask.setName("new-task");
        newTask.setDescription("new-task-description");
        newTask.setAssignee("huser");
        try {
            processEngine.getTaskService().saveTask(newTask);
        } catch (Exception e) {
            // nothing to do
        }

        // then
        ProcessEngineEvent[] events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        assertThat(events.length).isEqualTo(2);
        assertThat(events[0].getClass()).isEqualTo(TaskAssignedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(TaskCreatedEventImpl.class);

    }

    public static void deploy(final String processDefinitionKey, ProcessEngine processEngine) throws IOException {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("processes/" + processDefinitionKey + ".bpmn")) {
            processEngine.getRepositoryService()
                         .createDeployment()
                         .addInputStream(processDefinitionKey + ".bpmn", is)
                         .deploy();
        }
    }
}