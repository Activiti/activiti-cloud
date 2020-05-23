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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.starter.tests.helper.MessageRestTemplate;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class MessageIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private MessageRestTemplate messageRestTemplate;

    @Test
    public void shouldDeliverMessagesViaRestApi() {
        //given
        StartMessagePayload startMessage = MessagePayloadBuilder.start("startMessage")
                                                                .withBusinessKey("businessId")
                                                                .withVariable("correlationKey", "correlationId")
                                                                .build();
        //when
        ResponseEntity<CloudProcessInstance> startResponse = messageRestTemplate.message(startMessage);

        //then
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .includeProcessVariables()
                                 .processDefinitionKey("shouldDeliverMessagesViaRestApi")
                                 .list()).hasSize(1)
                                         .extracting(ProcessInstance::getProcessVariables)
                                         .extracting("correlationKey")
                                         .contains("correlationId");

        //given
        ReceiveMessagePayload boundaryMessage = MessagePayloadBuilder.receive("boundaryMessage")
                                                                     .withCorrelationKey("correlationId")
                                                                     .withVariable("customerKey", "customerId")
                                                                     .build();

        //when
        ResponseEntity<Void> boundaryResponse = messageRestTemplate.message(boundaryMessage);

        //then
        assertThat(boundaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .includeProcessVariables()
                                 .processDefinitionKey("shouldDeliverMessagesViaRestApi")
                                 .list()).hasSize(1)
                                         .extracting(ProcessInstance::getProcessVariables)
                                         .extracting("customerKey")
                                         .contains("customerId");
        //given
        ReceiveMessagePayload catchMessage = MessagePayloadBuilder.receive("catchMessage")
                                                                  .withCorrelationKey("customerId")
                                                                  .build();

        // when
        ResponseEntity<Void> catchResponse = messageRestTemplate.message(catchMessage);

        // then
        assertThat(catchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .processDefinitionKey("shouldDeliverMessagesViaRestApi")
                                 .list()).isEmpty();
    }

    @Test
    public void shouldReceive404NotFoundIfWrongMessageName() {
        //given
        StartMessagePayload startMessage = MessagePayloadBuilder.start("notFound")
                                                                .withBusinessKey("businessId")
                                                                .withVariable("correlationKey", "correlationId")
                                                                .build();
        //when
        ResponseEntity<CloudProcessInstance> startResponse = messageRestTemplate.message(startMessage);

        //then
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
