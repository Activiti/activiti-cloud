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

package org.activiti.cloud.connectors.starter.test.it;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.connectors.starter.model.IntegrationRequestEvent;
import org.activiti.cloud.connectors.starter.model.IntegrationResultEvent;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnableBinding(RuntimeMockStreams.class)
public class ActivitiCloudConnectorServiceIT {


    @Autowired
    private MessageChannel integrationEventsProducer;

    @ClassRule
    public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

    public static boolean integrationResultArrived = false;

    @Before
    public void setUp() throws Exception {

    }

    @EnableAutoConfiguration
    public static class StreamHandler {

        @StreamListener(value = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
        public void consumeIntegrationResults(IntegrationResultEvent integrationResultEvent) throws InterruptedException {

            System.out.println(">>> Result Recieved Back from Connector: " + integrationResultEvent);
            String executionId = integrationResultEvent.getExecutionId();
            assertThat(integrationResultEvent.getVariables().get("var2")).isEqualTo(2);


            integrationResultArrived = true;
        }
    }

    @Test
    public void findAllShouldReturnAllAvailableEvents() throws Exception {
        //given

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                      "value1");
        variables.put("var2",
                      new Long(1));
        String processDefId = "myProcessDefinitionId";
        IntegrationRequestEvent ire = new IntegrationRequestEvent("processInstanceId-" + UUID.randomUUID().toString(),
                                                                  processDefId,
                                                                  "executionId-" + UUID.randomUUID().toString(),
                                                                  variables);

        Message<IntegrationRequestEvent> message = MessageBuilder.withPayload(ire)
                .setHeader("type",
                           "Mock")
                .setHeader("processDefinitionId",
                           // this is option and only if we are interested in filtering by processDefinitionId
                           processDefId)
                .build();
        integrationEventsProducer.send(message);

        while (!integrationResultArrived) {
            System.out.println("Waiting for result to arrive ...");
            Thread.sleep(100);
        }


        assertThat(integrationResultArrived).isTrue();

    }
}


