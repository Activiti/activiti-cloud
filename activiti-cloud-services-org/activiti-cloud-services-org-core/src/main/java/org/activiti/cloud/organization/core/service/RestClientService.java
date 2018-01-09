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

package org.activiti.cloud.organization.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Rest client service.
 */
@Service
public class RestClientService {

    private static Logger log = LoggerFactory.getLogger(RestClientService.class);

    private RestTemplate restTemplate;

    @Autowired
    public RestClientService(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Make the rest call to save a rest resource.
     * @param restResourceUrl the url of the rest resource
     * @param resource the resource to save
     * @param update true is the save is an update
     */
    public <T> void saveRestResource(String restResourceUrl,
                                     T resource,
                                     boolean update) {
        log.trace("Saving rest resource using URL " + restResourceUrl);
        restTemplate
                .exchange(restResourceUrl,
                          update ? PUT : POST,
                          new HttpEntity<T>(resource,
                                            jsonHeaders()),
                          String.class);
    }

    /**
     * Make the rest call and get the rest resource as json text.
     * @param restResourceUrl the url of the rest resource
     * @param clazz the expected class of the response body json
     * @return the resource as json string
     */
    public <T> T getRestResource(String restResourceUrl,
                                 Class<T> clazz) {
        log.trace("Fetching rest resource from URL " + restResourceUrl);
        return restTemplate
                .exchange(restResourceUrl,
                          GET,
                          new HttpEntity<T>(jsonHeaders()),
                          clazz)
                .getBody();
    }

    /**
     * Create and get simple http header for json request.
     * @return the created http headers
     */
    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
