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

package org.activiti.cloud.services.organization.mock;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Mock rest server for Model resources
 */
public class MockModelRestServiceServer {

    private static final String LOCALHOST = "http://localhost:8088";

    private final MockRestServiceServer mockRestServer;

    private MockModelRestServiceServer(final RestTemplate restTemplate) {
        mockRestServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * Create a mock for models rest server
     * @param restTemplate the rest template
     * @return the created rmock est server
     */
    public static MockModelRestServiceServer createServer(final RestTemplate restTemplate) {
        return new MockModelRestServiceServer(restTemplate);
    }

    /**
     * Expect a form model to be created with success
     * @return this
     */
    public MockModelRestServiceServer expectFormModelCreation() {
        mockRestServer
                .expect(requestToUriTemplate("{url}{version}/forms",
                                             LOCALHOST,
                                             API_VERSION))
                .andExpect(method(POST))
                .andRespond(withStatus(CREATED));
        return this;
    }

    /**
     * Expect a process model to be created with success
     * @return this
     */
    public MockModelRestServiceServer expectProcessModelCreation() {
        mockRestServer
                .expect(requestToUriTemplate("{url}{version}/process-models",
                                             LOCALHOST,
                                             API_VERSION))
                .andExpect(method(POST))
                .andRespond(withStatus(CREATED));
        return this;
    }

    /**
     * Expect a certain form model to be requested
     * @return this
     */
    public MockModelRestServiceServer expectFormModelRequest(ModelReference expectedFormModel) throws JsonProcessingException {
        mockRestServer
                .expect(requestToUriTemplate("{url}{version}/forms/{formId}",
                                             LOCALHOST,
                                             API_VERSION,
                                             expectedFormModel.getModelId()))
                .andExpect(method(GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(expectedFormModel),
                                        APPLICATION_JSON));
        return this;
    }

    /**
     * Expect a certain process model to be requested
     * @return this
     */
    public MockModelRestServiceServer expectProcessModelRequest(ModelReference expectedProcessModel) throws JsonProcessingException {
        mockRestServer
                .expect(requestToUriTemplate("{url}{version}/process-models/{formId}",
                                             LOCALHOST,
                                             API_VERSION,
                                             expectedProcessModel.getModelId()))
                .andExpect(method(GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(expectedProcessModel),
                                        APPLICATION_JSON));
        return this;
    }

    /**
     * Expect a certain process-model to be validated
     * @param validationErrorRepresentations
     * @throws JsonProcessingException
     */
    public void expectProcessModelValidation(List<ModelValidationError> validationErrorRepresentations) throws JsonProcessingException {
        mockRestServer
                .expect(requestToUriTemplate("{url}{version}/process-models/validate",
                                             LOCALHOST,
                                             API_VERSION))
                .andExpect(method(POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(validationErrorRepresentations),
                                        APPLICATION_JSON));
    }
}
