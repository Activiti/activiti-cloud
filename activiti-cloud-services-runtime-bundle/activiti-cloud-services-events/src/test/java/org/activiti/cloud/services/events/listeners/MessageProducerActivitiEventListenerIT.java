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

package org.activiti.cloud.services.events.listeners;

import java.io.IOException;
import java.io.InputStream;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ActivityCompletedEventImpl;
import org.activiti.cloud.services.events.ActivityStartedEventImpl;
import org.activiti.cloud.services.events.ProcessCompletedEventImpl;
import org.activiti.cloud.services.events.ProcessStartedEventImpl;
import org.activiti.cloud.services.events.SequenceFlowTakenEventImpl;
import org.activiti.cloud.services.events.listeners.MessageProducerActivitiEventListener;
import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.events.tests.util.MockMessageChannel;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
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
    @ComponentScan({"org.activiti.cloud.services.events.tests.util", "org.activiti.cloud.services.events.converter", "org.activiti.cloud.services.api.model.converter"})
    public class ContextConfig {
    }

    @Test
    public void executeListener() throws Exception {
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();
        deploy("SimpleProcess", processEngine);
        deploy("RollbackProcess", processEngine);
        deploy("AsyncErrorProcess", processEngine);
        processEngine.getRuntimeService().addEventListener(eventListener);
        processEngine.getRuntimeService().startProcessInstanceByKey("simpleProcess");

        ProcessEngineEvent[] events = (ProcessEngineEvent[]) MockMessageChannel.messageResult.getPayload();
        assertThat(events.length).isEqualTo(7);
        assertThat(events[0].getClass()).isEqualTo(ProcessStartedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[2].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[3].getClass()).isEqualTo(SequenceFlowTakenEventImpl.class);
        assertThat(events[4].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[5].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[6].getClass()).isEqualTo(ProcessCompletedEventImpl.class);

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
        assertThat(events.length).isEqualTo(4);
        assertThat(events[0].getClass()).isEqualTo(ProcessStartedEventImpl.class);
        assertThat(events[1].getClass()).isEqualTo(ActivityStartedEventImpl.class);
        assertThat(events[2].getClass()).isEqualTo(ActivityCompletedEventImpl.class);
        assertThat(events[3].getClass()).isEqualTo(SequenceFlowTakenEventImpl.class);
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