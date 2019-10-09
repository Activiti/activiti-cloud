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

package org.activiti.cloud.starter.audit.tests.it;

import static org.activiti.test.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@TestComponent
public class EventsRestTemplate {

    private static final String RELATIVE_EVENTS_ENDPOINT = "/v1/events";

    private ObjectMapper mapper;

    private KeycloakTokenProducer keycloakTokenProducer;

    public EventsRestTemplate(ObjectMapper mapper, KeycloakTokenProducer keycloakTokenProducer) {
        this.mapper = mapper;
        this.keycloakTokenProducer = keycloakTokenProducer;
    }

    @Autowired
    private TestRestTemplate restTemplate;

    public ResponseEntity<PagedResources<CloudRuntimeEvent>> executeFindAll() {
        ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsResponse = restTemplate.exchange(RELATIVE_EVENTS_ENDPOINT,
                                                                                                 HttpMethod.GET,
                                                                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                 new ParameterizedTypeReference<PagedResources<CloudRuntimeEvent>>() {
                                                                                                 });
        assertThat(eventsResponse).hasStatusCode(HttpStatus.OK);
        return eventsResponse;
    }

    public ResponseEntity<PagedResources<CloudRuntimeEvent>> executeFind(Map<String, Object> filters) {

        StringBuilder endPointBuilder = new StringBuilder(RELATIVE_EVENTS_ENDPOINT).append("?search=");
        for (String filter : filters.keySet()) {
            endPointBuilder.append(filter)
                    .append(":{")
                    .append(filter)
                    .append("}")
                    .append(",");
        }

        ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsResponse = restTemplate.exchange(endPointBuilder.toString(),
                                                                                                 HttpMethod.GET,
                                                                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                 new ParameterizedTypeReference<PagedResources<CloudRuntimeEvent>>() {
                                                                                                 },
                                                                                                 filters);
        assertThat(eventsResponse).hasStatusCode(HttpStatus.OK);
        return eventsResponse;
    }

    public ResponseEntity<CloudRuntimeEvent> executeFindById(String id) {
        ResponseEntity<CloudRuntimeEvent> responseEntity = restTemplate.exchange(RELATIVE_EVENTS_ENDPOINT + "/" + id,
                                                                                 HttpMethod.GET,
                                                                                keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                 new ParameterizedTypeReference<CloudRuntimeEvent>() {
                                                                                 });
        assertThat(responseEntity).hasStatusCode(HttpStatus.OK);
        return responseEntity;
    }
}
