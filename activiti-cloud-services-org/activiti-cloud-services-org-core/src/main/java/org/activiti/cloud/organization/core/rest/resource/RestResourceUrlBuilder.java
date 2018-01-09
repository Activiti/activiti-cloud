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

package org.activiti.cloud.organization.core.rest.resource;

import java.net.MalformedURLException;
import java.net.URL;

import org.activiti.cloud.organization.core.rest.context.RestResourceContext;
import org.activiti.cloud.organization.core.rest.context.RestResourceContextItem;
import org.activiti.cloud.organization.core.util.ReflectionUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import static org.springframework.http.HttpMethod.POST;

/**
 * Rest resource URL builder
 */
public class RestResourceUrlBuilder {

    public static final String ID_PLACEHOLDER = "\\{#id\\}";

    public static final String ID_SELECTOR_PLACEHOLDER = "/\\{#id\\}$";

    public static final String NAME_PLACEHOLDER = "\\{#name\\}";

    public static final String API_KEY_PLACEHOLDER = "\\{#apiKey\\}";

    private Object entity;

    private String entityFieldName;

    private Object resourceKey;

    private Object resourceId;

    private RestResourceContext context;

    private String path;

    public RestResourceUrlBuilder(Object entity,
                                  String entityFieldName,
                                  RestResourceContext context) {
        this.entity = entity;
        this.entityFieldName = entityFieldName;
        this.context = context;
    }

    /**
     * Set the path to the rest resource.
     * @param path the path
     * @return this
     */
    public RestResourceUrlBuilder path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Set the rest resource key.
     * @param resourceKeyField the entity field name that contains the resource key
     * @return this
     */
    public RestResourceUrlBuilder resourceKey(String resourceKeyField) {
        if (!resourceKeyField.isEmpty()) {
            resourceKey = getEntityFieldValue(entity,
                                              resourceKeyField);
        }
        return this;
    }

    /**
     * Set the rest resource id
     * @param resourceIdField the entity field name that contains the resource id
     * @return this
     */
    public RestResourceUrlBuilder resourceId(String resourceIdField) {

        if (!resourceIdField.isEmpty()) {
            resourceId = getEntityFieldValue(entity,
                                             resourceIdField);
        }
        return this;
    }

    /**
     * Get entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @return the field value
     */
    protected Object getEntityFieldValue(Object entity,
                                         String fieldName) {
        return ReflectionUtils.getFieldValue(
                entity,
                fieldName,
                () -> String.format(
                        "Cannot access field '%s' of entity type '%s' with rest resource",
                        fieldName,
                        entity.getClass()));
    }

    /**
     * Build the rest resource URL corresponding to a HTTP method.
     * @param method the HTTP method
     * @return the built rest resource URL
     */
    public URL toURL(HttpMethod method) {
        return toURL(method == POST);
    }

    /**
     * Build the rest resource URL with or without the id selector at the end.
     * It does replace the placeholders for name, id and api key with the actual values.
     * If no id is provided but the placeholder for resource id is present, an exception is thrown.
     * @param withoutIdSelector true if the id selector from the end of the URL should be cut
     * @return the built rest resource URL
     */
    protected URL toURL(boolean withoutIdSelector) {
        RestResourceContextItem resource = context.getResource(resourceKey);
        if (resource == null) {
            throw new RuntimeException(
                    String.format(
                            "No resource found in context associated with field '%s' of entity type '%s'",
                            entityFieldName,
                            entity.getClass()));
        }

        String resourceSpec = (resource.getUrl() + path)
                .replaceAll(NAME_PLACEHOLDER,
                            resource.getName());

        if (withoutIdSelector) {
            resourceSpec = resourceSpec.replaceAll(ID_SELECTOR_PLACEHOLDER,
                                                   "");
        }

        if (resourceId != null) {
            resourceSpec = resourceSpec.replaceAll(ID_PLACEHOLDER,
                                                   resourceId.toString());
        } else if (resourceSpec.contains(ID_PLACEHOLDER)) {
            throw new RuntimeException(
                    String.format(
                            "No resource id provided to replace the placeholder in '%s' for rest resource corresponding to field '%s' of entity type '%s'",
                            resourceSpec,
                            entityFieldName,
                            entity.getClass()));
        }

        if (!(StringUtils.isEmpty(resource.getApiKey()))) {
            resourceSpec = resourceSpec
                    .replaceAll(API_KEY_PLACEHOLDER,
                                resource.getApiKey());
        }

        try {
            return new URL(resourceSpec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    String.format(
                            "Cannot build URL for rest resource corresponding to field '%s' of entity type '%s'",
                            entityFieldName,
                            entity.getClass()),
                    e);
        }
    }
}
