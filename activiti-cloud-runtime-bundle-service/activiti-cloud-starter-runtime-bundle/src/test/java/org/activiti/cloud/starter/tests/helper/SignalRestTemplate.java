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
package org.activiti.cloud.starter.tests.helper;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;

@TestComponent
public class SignalRestTemplate {

    public static final LinkedMultiValueMap<String, String> CONTENT_TYPE_HEADER =
        new LinkedMultiValueMap<>(Map.of("Content-type", List.of("application/json")));

    private TestRestTemplate testRestTemplate;

    public SignalRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    public void signal(SignalPayload signalProcessInstancesCmd) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                                                                    HttpMethod.POST,
                                                                    new HttpEntity<>(signalProcessInstancesCmd, CONTENT_TYPE_HEADER),
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
