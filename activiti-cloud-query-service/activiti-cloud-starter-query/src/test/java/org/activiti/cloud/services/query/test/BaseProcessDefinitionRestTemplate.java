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
package org.activiti.cloud.services.query.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseProcessDefinitionRestTemplate {

    private static final ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> PAGED_PROCESS_DEFINITION_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {};

    private TestRestTemplate testRestTemplate;

    private IdentityTokenProducer identityTokenProducer;

    protected BaseProcessDefinitionRestTemplate(
        TestRestTemplate testRestTemplate,
        IdentityTokenProducer identityTokenProducer
    ) {
        this.testRestTemplate = testRestTemplate;
        this.identityTokenProducer = identityTokenProducer;
    }

    protected abstract String getProcessDefinitionsURL();

    public ResponseEntity<PagedModel<CloudProcessDefinition>> getProcDefinitions() {
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = testRestTemplate.exchange(
            getProcessDefinitionsURL(),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_DEFINITION_RESPONSE_TYPE
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<String> getProcDefinitionModel(String processDefinitionId) {
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(
            getProcessDefinitionsURL() + "/" + processDefinitionId + "/model",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            String.class
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<PagedModel<CloudProcessDefinition>> getProcDefinitionsFilteredOnKey(String key) {
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = testRestTemplate.exchange(
            getProcessDefinitionsURL() + "?key={key}",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_PROCESS_DEFINITION_RESPONSE_TYPE,
            key
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
}
