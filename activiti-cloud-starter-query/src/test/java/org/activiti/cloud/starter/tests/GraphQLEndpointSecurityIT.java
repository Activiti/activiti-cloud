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

package org.activiti.cloud.starter.tests;


import org.activiti.cloud.services.query.graphql.web.ActivitiGraphQLController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class GraphQLEndpointSecurityIT {

    private static final String GRAPHQL_URL = "/admin/graphql";

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void shouldNotSeeGraphQLEndpoint() throws Exception {

        ActivitiGraphQLController.GraphQLQueryRequest query = new ActivitiGraphQLController.GraphQLQueryRequest("{ \"query\": \"{ ProcessInstances { select { id, status } } }\" }");

        //when
        ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(GRAPHQL_URL, new HttpEntity<>(query,getHeader()), String.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }

    private HttpHeaders getHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", keycloakTokenProducer.getTokenString());
        return headers;
    }
}
