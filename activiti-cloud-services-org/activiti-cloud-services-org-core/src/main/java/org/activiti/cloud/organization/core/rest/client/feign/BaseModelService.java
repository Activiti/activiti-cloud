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

package org.activiti.cloud.organization.core.rest.client.feign;

import java.util.List;

import org.activiti.cloud.organization.core.service.ValidationErrorRepresentation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Base rest client for a model REST service
 */
public interface BaseModelService<T> {

    @RequestMapping(method = GET, path = "/{id}", consumes = APPLICATION_JSON_VALUE, produces = HAL_JSON_VALUE)
    T getResource(@PathVariable("id") String id);

    @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
    void createResource(@RequestBody T resource);

    @RequestMapping(method = PUT, path = "/{id}", consumes = APPLICATION_JSON_VALUE)
    void updateResource(@PathVariable("id") String id,
                        @RequestBody T resource);

    @RequestMapping(method = DELETE, path = "/{id}")
    void deleteResource(@PathVariable("id") String id);

    @RequestMapping(method = POST, path = "/validate", consumes = APPLICATION_OCTET_STREAM_VALUE)
    List<ValidationErrorRepresentation> validateResourceContent(@RequestBody byte[] file);
}
