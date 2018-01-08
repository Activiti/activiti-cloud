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

import org.activiti.cloud.organization.core.rest.context.RestContextProvider;
import org.activiti.cloud.organization.core.rest.context.RestResourceContext;
import org.activiti.cloud.organization.core.service.RestClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static org.activiti.cloud.organization.core.util.ReflectionUtils.getFieldClass;
import static org.activiti.cloud.organization.core.util.ReflectionUtils.getFieldValue;
import static org.activiti.cloud.organization.core.util.ReflectionUtils.setFieldValue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Processing rest resources logic.
 */
@Service
public class RestResourceService {

    private static Logger log = LoggerFactory.getLogger(RestResourceService.class);

    private RestContextProvider restContextProvider;

    private EntityLinks entityLinks;

    private RestClientService restClientService;

    @Autowired
    RestResourceService(final RestContextProvider restContextProvider,
                        final EntityLinks entityLinks,
                        final RestClientService restClientService) {
        this.restContextProvider = restContextProvider;
        this.entityLinks = entityLinks;
        this.restClientService = restClientService;
    }

    /**
     * Process an entity with a rest resource.
     * It loads the rest resource into the annotated field of the entity
     * and adds the link to the resource, if needed.
     * @param resource the resource to process
     * @param fieldName the entity field name associated with the rest resource
     * @param restResource the {@link RestResource} annotation of the resource
     * @param <T> the type of the entity
     */
    protected <T> void processResourceWithRestResource(final Resource<T> resource,
                                                       String fieldName,
                                                       RestResource restResource) {

        T entity = resource.getContent();
        log.trace("Processing entity with rest resource: " + entity);

        URL resourceURL = resourceURL(entity,
                                      fieldName,
                                      restResource,
                                      GET);

        String targetName = !StringUtils.isEmpty(restResource.targetField()) ?
                restResource.targetField() :
                fieldName;
        setEntityFieldWithRestResource(entity,
                                       targetName,
                                       resourceURL);

        if (!StringUtils.isEmpty(restResource.resourceRel())) {
            addLinkToRestResource(resource,
                                  restResource.resourceRel(),
                                  resourceURL,
                                  restResource.context().isExternal());
        }
    }

    /**
     * Perform write rest operation corresponding
     * to a save operation on an entity containing a rest resource.
     * @param entity the entity to be saved
     * @param fieldName the entity field name associated with the rest resource
     * @param restResource the {@link RestResource} annotation of the resource
     * @param update true is the save is an update
     */
    public void handleSaveOnEntityWithRestResource(Object entity,
                                                   String fieldName,
                                                   RestResource restResource,
                                                   boolean update) {

        log.trace("Handling saving entity with rest resource: " + entity);
        URL resourceURL = resourceURL(entity,
                                      fieldName,
                                      restResource,
                                      update ? PUT : POST);

        String targetName = !StringUtils.isEmpty(restResource.targetField()) ?
                restResource.targetField() :
                fieldName;

        saveRestResourceFromEntityField(entity,
                                        targetName,
                                        resourceURL,
                                        update);
    }

    /**
     * Get the URL of a rest resource contained by an entity corresponding to a HTTP method.
     * @param entity the entity
     * @param fieldName the field name
     * @param restResource the {@link RestResource} annotation of the resource
     * @param method http method context
     * @return the rest resource URL
     */
    protected URL resourceURL(Object entity,
                              String fieldName,
                              RestResource restResource,
                              HttpMethod method) {
        RestResourceContext restContext = restContextProvider.getContext(restResource.context());

        return new RestResourceUrlBuilder(entity,
                                          fieldName,
                                          restContext)
                .path(restResource.path())
                .resourceKey(restResource.resourceKeyField())
                .resourceId(restResource.resourceIdField())
                .toURL(method);
    }

    /**
     * Create a rest resource using the content of an entity field.
     * @param entity the entity
     * @param fieldName the field name
     * @param resourceURL the url of the rest resource to save
     * @param update true is the save is an update
     */
    public void saveRestResourceFromEntityField(Object entity,
                                                String fieldName,
                                                URL resourceURL,
                                                boolean update) {
        Object data = getFieldValue(
                entity,
                fieldName,
                () -> String.format(
                        "Cannot access field '%s' of entity type '%s' with rest resource",
                        fieldName,
                        entity.getClass()));
        if (data == null) {
            log.debug(String.format(
                    "No data found in field '%s' of entity type '%s' to save to '%s'",
                    fieldName,
                    entity.getClass(),
                    resourceURL.toString()));
        }

        restClientService.saveRestResource(resourceURL.toString(),
                                           data,
                                           update);
    }

    /**
     * Fill a field of the entity with the rest resource content.
     * @param entity the entity with rest resource to process
     * @param targetFieldName the target field of the entity to fill
     * @param resourceURL the url of the rest resource
     */
    public void setEntityFieldWithRestResource(Object entity,
                                               String targetFieldName,
                                               URL resourceURL) {
        Class<?> targetFieldType = getFieldClass(
                entity,
                targetFieldName,
                () -> String.format(
                        "Cannot access the field '%s' of entity type '%s'",
                        targetFieldName,
                        entity.getClass()));

        final Object resolvedResource;
        try {
            resolvedResource = restClientService
                    .getRestResource(resourceURL.toString(),
                                     targetFieldType);
        } catch (Exception ex) {
            // just log the error, don't break the processing entity mechanism
            log.error(String.format("Failed to fetch resource from URL '%s'",
                                    resourceURL.toString()),
                      ex);
            return;
        }

        setFieldValue(
                entity,
                targetFieldName,
                resolvedResource,
                () -> String.format(
                        "Cannot set rest resource content to the target field '%s' of entity type '%s'",
                        targetFieldName,
                        entity.getClass()));
    }

    /**
     * Add the rest resource link to another resource.
     * @param resource the resource to process
     * @param resourceRel the name of the link
     * @param resourceURL the url of the linked resource
     * @param external true if the linked resource is external
     */
    public void addLinkToRestResource(Resource<?> resource,
                                      String resourceRel,
                                      URL resourceURL,
                                      boolean external) {
        Class<?> entityType = resource.getContent().getClass();

        try {
            String link = resourceURL.toString();
            if (!external) {
                URL hateoasResourceURL = new URL(
                        entityLinks
                                .linkFor(entityType)
                                .withSelfRel()
                                .getHref());

                link = new URL(hateoasResourceURL.getProtocol(),
                               hateoasResourceURL.getHost(),
                               hateoasResourceURL.getPort(),
                               resourceURL.getFile())
                        .toString();
            }

            resource.add(new Link(link,
                                  resourceRel));
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    String.format("Cannot create hateoas link for field '%s' of entity type '%s'",
                                  resourceRel,
                                  entityType),
                    e);
        }
    }
}
