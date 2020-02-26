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

package org.activiti.cloud.starter.tests.helper;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;

import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@TestComponent
public class MessageRestTemplate {

    private TestRestTemplate testRestTemplate;

    public MessageRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    public ResponseEntity<CloudProcessInstance> message(StartMessagePayload payload) {
        return testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/message",
                                         HttpMethod.POST,
                                         new HttpEntity<>(payload),
                                         new ParameterizedTypeReference<CloudProcessInstance>() {
                                         });
    }

    public ResponseEntity<Void> message(ReceiveMessagePayload payload) {
        return testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/message",
                                         HttpMethod.PUT,
                                         new HttpEntity<>(payload),
                                         new ParameterizedTypeReference<Void>() {
                                         });
    }     
}
