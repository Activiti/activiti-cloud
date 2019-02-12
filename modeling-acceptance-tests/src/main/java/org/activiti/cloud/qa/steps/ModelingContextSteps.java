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

package org.activiti.cloud.qa.steps;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import feign.Response;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.shared.rest.DirtyContextHandler;
import org.activiti.cloud.acc.shared.rest.EnableDirtyContext;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.ModelingContextHandler;
import org.activiti.cloud.qa.model.modeling.ModelingIdentifier;
import org.activiti.cloud.services.common.file.FileContent;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.services.common.util.HttpUtils.HEADER_ATTACHEMNT_FILENAME;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling context steps
 */
@EnableDirtyContext
public abstract class ModelingContextSteps<M> {

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    @Autowired
    protected ModelingContextHandler modelingContextHandler;

    @Autowired
    private ModelingTestsConfigurationProperties config;

    protected Resource<M> create(M m) {
        Resource<M> model = modelingContextHandler
                .getCurrentModelingContext()
                .flatMap(this::getRelUri)
                .map(this::modelingUri)
                .map(uri -> service().createByUri(uri,
                                                  m))
                .orElseGet(() -> service().create(m));
        return dirty(model);
    }

    @Step
    public void checkExists(ModelingIdentifier<M> identifier) {
        assertThat(exists(identifier)).isTrue();
    }

    @Step
    public void checkExistsInCurrentContext(ModelingIdentifier<M> identifier) {
        assertThat(existsInCurrentContext(identifier)).isTrue();
    }

    @Step
    public void deleteAll(ModelingIdentifier<M> identifier) {
        service().findAll()
                .getContent()
                .stream()
                .filter(resource -> identifier.test(resource.getContent()))
                .map(resource -> resource.getLink(REL_SELF))
                .map(Link::getHref)
                .map(this::modelingUri)
                .collect(Collectors.toList())
                .forEach(service()::deleteByUri);
    }

    @Step
    public boolean exists(ModelingIdentifier<M> identifier) {
        return existsInCollection(identifier,
                                  service().findAll().getContent());
    }

    protected Optional<String> getRelUri(Resource<?> resource) {
        return getRel()
                .map(resource::getLink)
                .map(Link::getHref)
                .map(this::cutQueryParams);
    }

    protected String cutQueryParams(String uri) {
        int index = uri.indexOf('{');
        if (index <= 0) {
            return uri;
        }
        return uri.substring(0,
                             index);
    }

    protected void updateCurrentModelingObject() {
        modelingContextHandler
                .getCurrentModelingContext()
                .map(resource -> resource.getLink(REL_SELF))
                .map(Link::getHref)
                .map(this::modelingUri)
                .map(this::findByUri)
                .ifPresent(modelingContextHandler::setCurrentModelingObject);
    }

    protected Resource<M> checkAndGetCurrentContext(Class<M> expectedCurrentContextClass) {
        Optional<Resource<?>> optionalModelingContext = modelingContextHandler.getCurrentModelingContext();
        assertThat(optionalModelingContext
                           .map(Resource::getContent)
                           .filter(expectedCurrentContextClass::isInstance)
                           .map(expectedCurrentContextClass::cast)).isNotEmpty();
        return (Resource<M>) optionalModelingContext.get();
    }

    @Step
    public void openModelingObject(ModelingIdentifier<M> identifier) {
        Optional<Resource<M>> currentModelingObject = getAvailableModelingObjects()
                .stream()
                .filter(modelingObject -> identifier.test(modelingObject.getContent()))
                .findFirst()
                .map(resource -> resource.getLink("self"))
                .map(Link::getHref)
                .map(this::modelingUri)
                .map(this::findByUri);
        assertThat(currentModelingObject.isPresent()).isTrue();

        modelingContextHandler.setCurrentModelingObject(currentModelingObject.get());
    }

    protected Collection<Resource<M>> getAvailableModelingObjects() {
        return modelingContextHandler
                .getCurrentModelingContext()
                .flatMap(this::getRelUri)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .orElseGet(() -> findAll().getContent());
    }

    @Step
    public boolean existsInCurrentContext(ModelingIdentifier<M> identifier) {
        return modelingContextHandler
                .getCurrentModelingContext()
                .flatMap(this::getRelUri)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .map(resources -> existsInCollection(identifier,
                                                     resources))
                .orElse(false);
    }

    protected boolean existsInCollection(ModelingIdentifier<M> identifier,
                                         Collection<Resource<M>> modelingObjects) {
        return modelingObjects
                .stream()
                .map(Resource::getContent)
                .filter(Objects::nonNull)
                .filter(identifier)
                .findFirst()
                .isPresent();
    }

    protected Resource<M> dirty(Resource<M> resource) {
        dirtyContextHandler.dirty(modelingUri(resource.getLink("self").getHref()));
        return resource;
    }

    protected Resource<M> findByUri(String uri) {
        return service().findByUri(uri);
    }

    protected PagedResources<Resource<M>> findAllByUri(String uri) {
        return service().findAllByUri(modelingUri(uri));
    }

    protected PagedResources<Resource<M>> findAll() {
        return service().findAll();
    }

    protected FileContent toFileContent(Response response) throws IOException {
        String contentType = Optional.ofNullable(response.headers().get("Content-Type"))
                .map(contentTypes -> contentTypes
                        .stream()
                        .map(Object::toString)
                        .findFirst()
                        .orElse(null))
                .orElseThrow(() -> new RuntimeException("No Content-Type header in feign response"));

        String filename = Optional.ofNullable(response.headers().get("Content-Disposition"))
                .map(contentTypes -> contentTypes
                        .stream()
                        .map(Object::toString)
                        .findFirst()
                        .filter(contentDisposition -> contentDisposition.startsWith(HEADER_ATTACHEMNT_FILENAME))
                        .map(contentDisposition -> contentDisposition.substring(HEADER_ATTACHEMNT_FILENAME.length()))
                        .orElse(null))
                .orElseThrow(() -> new RuntimeException("No Content-Disposition header in feign response"));
        return new FileContent(filename,
                               contentType,
                               IOUtils.toByteArray(response.body().asInputStream()));
    }

    protected String modelingUri(String uri) {
        return uri.replace("http://activiti-cloud-modeling-backend",
                           config.getModelingUrl());
    }

    protected abstract Optional<String> getRel();

    public abstract <S extends FeignRestDataClient<S, M>> S service();
}
