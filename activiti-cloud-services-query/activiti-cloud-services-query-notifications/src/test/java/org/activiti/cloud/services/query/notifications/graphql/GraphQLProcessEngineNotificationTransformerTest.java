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
package org.activiti.cloud.services.query.notifications.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.query.notifications.consumer.ProcessEngineNotificationTransformer;
import org.activiti.cloud.services.query.notifications.model.ProcessEngineNotification;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLProcessEngineNotificationTransformerTest {

    private static Logger LOGGER = LoggerFactory.getLogger(GraphQLProcessEngineNotificationTransformerTest.class);

    private static String engineEventAttributeKeys = "serviceName,appName,processInstanceId,processDefinitionId";
    private static String eventTypeKey = "eventType";

    ProcessEngineNotificationTransformer subject = new GraphQLProcessEngineNotificationTransformer(
                                                          Arrays.asList(engineEventAttributeKeys.split(",")), eventTypeKey);

    @Test
    public void transform() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("executionId","e1");
            }});

        }};

        // when
        List<ProcessEngineNotification> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(2);

        assertThat(notifications.get(0).get("serviceName")).isEqualTo("rb");
        assertThat(notifications.get(0).keySet())
                .containsOnly("processInstanceId","serviceName","appName","processDefinitionId","type1","type2");
        assertThat(notifications.get(0).get("type2")).asList().hasSize(2);


        assertThat(notifications.get(1).get("serviceName")).isEqualTo("rb1");
        assertThat(notifications.get(1).keySet())
                .containsOnly("processInstanceId","serviceName","appName","processDefinitionId","type1");
        assertThat(notifications.get(1).get("type1")).asList().hasSize(1);

    }

    @Test
    public void transformFilterNullAttributes() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName", null);
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId",null);
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("executionId","e1");
            }});

        }};

        // when
        List<ProcessEngineNotification> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(2);

        assertThat(notifications.get(0).get("serviceName")).isEqualTo("rb");
        assertThat(notifications.get(0).keySet())
                .containsOnly("processInstanceId","serviceName","appName","processDefinitionId","type2");
        assertThat(notifications.get(0).get("type2")).asList().hasSize(1);

        assertThat(notifications.get(1).get("serviceName")).isEqualTo("rb1");
        assertThat(notifications.get(1).keySet())
                .containsOnly("processInstanceId","serviceName","appName","processDefinitionId","type1");
        assertThat(notifications.get(1).get("type1")).asList().hasSize(1);


    }

    @Test
    public void transformFilterMissingAttributes() throws JsonProcessingException {
        // given
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;
        {
            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                //put("serviceName", null);
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type1");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                //put("processInstanceId",null);
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                put("eventType","type2");
                put("executionId","e1");
            }});

            add(new GraphQLProcessEngineNotification() {
                private static final long serialVersionUID = 1L;
            {
                put("serviceName","rb1");
                put("appName","app");
                put("processInstanceId","p1");
                put("processDefinitionId","pd1");
                //put("eventType","type1");
                put("executionId","e1");
            }});

        }};

        // when
        List<ProcessEngineNotification> notifications = subject.transform(events);

        LOGGER.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        // then
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).get("serviceName")).isEqualTo("rb");
        assertThat(notifications.get(0).keySet())
            .containsOnly("processInstanceId","serviceName","appName","processDefinitionId","type2");
        assertThat(notifications.get(0).get("type2")).asList().hasSize(1);

    }

}
