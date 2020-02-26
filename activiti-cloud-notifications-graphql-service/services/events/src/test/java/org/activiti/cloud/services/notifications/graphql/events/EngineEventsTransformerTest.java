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
package org.activiti.cloud.services.notifications.graphql.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.notifications.graphql.events.consumer.EngineEventsConsumerProperties;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.EngineEventsTransformer;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineEventsTransformerTest {

    private static Logger LOGGER = LoggerFactory.getLogger(EngineEventsTransformerTest.class);

    private EngineEventsConsumerProperties properties = new EngineEventsConsumerProperties();
    
    private Transformer subject;

    @Before
    public void setUp() {
        String engineEventAttributeKeys = properties.getProcessEngineEventAttributeKeys();
        String eventTypeKey = properties.getProcessEngineEventTypeKey();

    	subject = new EngineEventsTransformer(
                Arrays.asList(engineEventAttributeKeys.split(",")), eventTypeKey);
    }
    
    
    @Test
    public void transform() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processDefinitionKey","pd1");
                put("businessKey","bk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processDefinitionKey","pd1");
                put("businessKey","bk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processDefinitionKey","pd1");
                put("businessKey","bk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("processDefinitionKey","pd1");
                put("businessKey","bk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("entityId","e1");
            }});

        }};

        // when
        List<EngineEvent> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(4);

        assertThat(notifications).extracting(EngineEvent::getEventType)
                                 .containsExactly("type1",
                                                  "type2",
                                                  "type2",
                                                  "type1");

    }

    @Test
    public void transformNullAttributes() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName", null);
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("processDefinitionKey","pdk1");
                put("businessKey","bk1");
                put("eventType","type1");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId",null);
                put("processDefinitionId","pd1");
                put("processDefinitionKey","pdk1");
                put("businessKey","bk1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("processDefinitionKey","pdk1");
                put("businessKey","bk1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("processDefinitionKey","pdk1");
                put("businessKey","bk1");
                put("eventType","type1");
                put("entityId","e1");
            }});

        }};

        // when
        List<EngineEvent> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(4);
    }

    @Test
    public void transformFilterMissingAttributes() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                // put("serviceName","rb"); <- missing
                put("appName","app");
                put("processInstanceId","p1");
                put("businessKey","bk1");
                put("processDefinitionKey","pdk1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("businessKey","bk1");
                put("processDefinitionKey","pdk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("businessKey","bk1");
                put("processDefinitionKey","pdk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("entityId","e1");
            }});

            add(new EngineEvent() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("businessKey","bk1");
                put("processDefinitionKey","pdk1");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("entityId","e1");
                // put("eventType","type2"); <- missing
            }});

        }};

        // when
        List<EngineEvent> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(3);
        assertThat(notifications.get(0).get("serviceName")).isNull();
        assertThat(notifications.get(1).get("serviceName")).isEqualTo("rb");
        assertThat(notifications.get(2).get("serviceName")).isEqualTo("rb");
        
    }
    
    @Test
    public void testJson() throws JsonParseException, JsonMappingException, IOException {
    	// given
    	List<Map<String,Object>> events = new ObjectMapper().readValue(json, new TypeReference<List<Map<String,Object>>>(){});
    	JsonNode jsonNode = new ObjectMapper().readTree(json);
    	
        // when
        List<EngineEvent> notifications = subject.transform(events);
        
        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(14);
        assertThat(notifications).extracting(EngineEvent::getEventType)
                                 .containsOnly("PROCESS_CREATED",
                                               "VARIABLE_CREATED",
                                               "PROCESS_STARTED",
                                               "ACTIVITY_COMPLETED",
                                               "SEQUENCE_FLOW_TAKEN",
                                               "ACTIVITY_STARTED",
                                               "TASK_CANDIDATE_GROUP_ADDED",
                                               "TASK_CREATED");

    }
    
    private String json =
    "[  \r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"PROCESS_CREATED\",\r\n" + 
    "      \"id\":\"ebe2cb3b-c9ea-4d22-9c01-57c363405617\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"id\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "         \"initiator\":\"hruser\",\r\n" + 
    "         \"startDate\":\"2018-12-25T01:25:39.583+0000\",\r\n" + 
    "         \"status\":\"RUNNING\",\r\n" + 
    "         \"processDefinitionVersion\":1\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"0b127388-3a2e-4b32-a19c-5de775fafa8f\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"firstName\",\r\n" + 
    "         \"type\":\"string\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":\"Paulo\",\r\n" + 
    "         \"taskVariable\":false\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"firstName\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"19b30145-8109-424b-9437-2f0fbb07031e\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"lastName\",\r\n" + 
    "         \"type\":\"string\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":\"Silva\",\r\n" + 
    "         \"taskVariable\":false\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"lastName\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"f743f526-34a1-45b1-b040-7c94ca742c58\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"age\",\r\n" + 
    "         \"type\":\"integer\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":25,\r\n" + 
    "         \"taskVariable\":false\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"age\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"PROCESS_STARTED\",\r\n" + 
    "      \"id\":\"743aa709-2450-42c4-ac53-b4f2ac92e558\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"id\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "         \"initiator\":\"hruser\",\r\n" + 
    "         \"startDate\":\"2018-12-25T01:25:39.583+0000\",\r\n" + 
    "         \"status\":\"RUNNING\",\r\n" + 
    "         \"processDefinitionVersion\":1\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"ACTIVITY_STARTED\",\r\n" + 
    "      \"id\":\"c57c3f3c-b54a-4312-be70-73662cf4a79d\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"elementId\":\"startEvent1\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"activityType\":\"startEvent\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"startEvent1\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"ACTIVITY_COMPLETED\",\r\n" + 
    "      \"id\":\"73811f1b-2d77-4558-846a-8ae0327e844a\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"elementId\":\"startEvent1\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"activityType\":\"startEvent\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"startEvent1\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"SEQUENCE_FLOW_TAKEN\",\r\n" + 
    "      \"id\":\"0dfb8430-86a5-4932-8d40-1ae45cc46ae1\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"elementId\":\"sid-68945AF1-396F-4B8A-B836-FC318F62313F\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"sourceActivityElementId\":\"startEvent1\",\r\n" + 
    "         \"sourceActivityType\":\"org.activiti.bpmn.model.StartEvent\",\r\n" + 
    "         \"targetActivityElementId\":\"sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94\",\r\n" + 
    "         \"targetActivityName\":\"Perform action\",\r\n" + 
    "         \"targetActivityType\":\"org.activiti.bpmn.model.UserTask\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"sid-68945AF1-396F-4B8A-B836-FC318F62313F\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"ACTIVITY_STARTED\",\r\n" + 
    "      \"id\":\"56e3b7d3-61f7-4918-a729-2b9e3205bde2\",\r\n" + 
    "      \"timestamp\":1545701139583,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"elementId\":\"sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"activityName\":\"Perform action\",\r\n" + 
    "         \"activityType\":\"userTask\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"36c3ef0a-af93-4181-8a83-a389b09c1c47\",\r\n" + 
    "      \"timestamp\":1545701139584,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"firstName\",\r\n" + 
    "         \"type\":\"string\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":\"Paulo\",\r\n" + 
    "         \"taskId\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"taskVariable\":true\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"firstName\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"219cb834-88f3-461b-bcb7-61876b0a882c\",\r\n" + 
    "      \"timestamp\":1545701139584,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"lastName\",\r\n" + 
    "         \"type\":\"string\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":\"Silva\",\r\n" + 
    "         \"taskId\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"taskVariable\":true\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"lastName\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"VARIABLE_CREATED\",\r\n" + 
    "      \"id\":\"fa844c56-8081-4b28-b683-01b172406f17\",\r\n" + 
    "      \"timestamp\":1545701139584,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"name\":\"age\",\r\n" + 
    "         \"type\":\"integer\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"value\":25,\r\n" + 
    "         \"taskId\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"taskVariable\":true\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"age\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"TASK_CANDIDATE_GROUP_ADDED\",\r\n" + 
    "      \"id\":\"ce5bc8ec-f3d1-424f-a683-914a58213630\",\r\n" + 
    "      \"timestamp\":1545701139585,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"taskId\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"groupId\":\"hr\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"hr\"\r\n" + 
    "   },\r\n" + 
    "   {  \r\n" + 
    "      \"eventType\":\"TASK_CREATED\",\r\n" + 
    "      \"id\":\"cf639247-bdd9-4f9f-99ba-1efec7d301d3\",\r\n" + 
    "      \"timestamp\":1545701139585,\r\n" + 
    "      \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "      \"processDefinitionKey\":\"SimpleProcess\",\r\n" + 
    "      \"processDefinitionVersion\":1,\r\n" + 
    "      \"entity\":{  \r\n" + 
    "         \"id\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"name\":\"Perform action\",\r\n" + 
    "         \"status\":\"CREATED\",\r\n" + 
    "         \"createdDate\":\"2018-12-25T01:25:39.583+0000\",\r\n" + 
    "         \"priority\":50,\r\n" + 
    "         \"processDefinitionId\":\"SimpleProcess:1:715f2fd2-07b1-11e9-a0d3-0a580a2c00cd\",\r\n" + 
    "         \"processInstanceId\":\"fd0ae1ca-07e3-11e9-a0d3-0a580a2c00cd\"\r\n" + 
    "      },\r\n" + 
    "      \"appName\":\"default-app\",\r\n" + 
    "      \"serviceFullName\":\"rb-my-app\",\r\n" + 
    "      \"appVersion\":\"\",\r\n" + 
    "      \"serviceName\":\"rb-my-app\",\r\n" + 
    "      \"serviceVersion\":\"\",\r\n" + 
    "      \"serviceType\":\"runtime-bundle\",\r\n" + 
    "      \"entityId\":\"fd0b08e0-07e3-11e9-a0d3-0a580a2c00cd\"\r\n" + 
    "   }\r\n" + 
    "]";

}
