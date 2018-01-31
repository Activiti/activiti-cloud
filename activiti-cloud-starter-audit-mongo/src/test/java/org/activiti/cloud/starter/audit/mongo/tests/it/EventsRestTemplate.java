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

package org.activiti.cloud.starter.audit.mongo.tests.it;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class EventsRestTemplate {

    private static final String RELATIVE_EVENTS_ENDPOINT = "/v1/events";

    @Autowired
    private TestRestTemplate restTemplate;

    public ResponseEntity<PagedResources<ProcessEngineEventDocument>> executeFindAll() {
        ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsResponse = restTemplate.exchange(RELATIVE_EVENTS_ENDPOINT,
                                                                                                        HttpMethod.GET,
                                                                                                        null,
                                                                                                          new ParameterizedTypeReference<PagedResources<ProcessEngineEventDocument>>() {
                                                                                                        });
        assertEquals(eventsResponse.getStatusCode(), HttpStatus.OK);
        return eventsResponse;
    }

    public ResponseEntity<PagedResources<ProcessEngineEventDocument>> executeFind(Map<String, Object> filters) {

        StringBuilder endPointBuilder = new StringBuilder(RELATIVE_EVENTS_ENDPOINT).append("?");
        for (String filter : filters.keySet()) {
            endPointBuilder.append(filter)
                           .append("={")
                           .append(filter)
                           .append("}")
                           .append("&");
        }
        ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsResponse = restTemplate.exchange(endPointBuilder.toString(),
                                                                                                          HttpMethod.GET,
                                                                                                          null,
                                                                                                          new ParameterizedTypeReference<PagedResources<ProcessEngineEventDocument>>() {
                                                                                                          },
                                                                                                          filters);
        assertEquals(eventsResponse.getStatusCode(), HttpStatus.OK);
        return eventsResponse;
    }

    public ResponseEntity<ProcessEngineEventDocument> executeFindById(String id) {
        ResponseEntity<ProcessEngineEventDocument> responseEntity = restTemplate.exchange(RELATIVE_EVENTS_ENDPOINT + "/" + id,
                                                                                        HttpMethod.GET,
                                                                                        null,
                                                                                          new ParameterizedTypeReference<ProcessEngineEventDocument>() {
                                                                                        });
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        return responseEntity;
    }
}
