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

package org.activiti.cloud.organization.core.rest.client;

import java.util.List;

import org.activiti.cloud.organization.api.ModelValidationError;

/**
 * Rest client interface
 */
public interface RestClientService<T, R, K> {

    R getResource(T resourceType,
                  K resourceId);

    void createResource(T resourceType,
                        R resource);

    void updateResource(T resourceType,
                        K resourceId,
                        R resource);

    void deleteResource(T resourceType,
                        K resourceId);

    List<ModelValidationError> validateResourceContent(T resourceType,
                                                       byte[] file);
}
